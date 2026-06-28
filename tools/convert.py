#!/usr/bin/env python3
"""
One-time conversion of the UACE Math Question Bank page images to Mathpix Markdown (MMD)
via an OpenRouter-hosted vision LLM.

Replaces the former Mathpix v3/pdf step (ADR-0003, superseded by ADR-0004): MathPix
signup/payment failed on Stripe and was never usable, so the OCR is done by a vision LLM
instead. The downstream contract is unchanged — we still produce the same MMD that
tools/split.py parses, so split.py is untouched except for its input path.

Pipeline:
  tools/ocr_out/pages/p-NN.png  --(per page, vision LLM)-->  tools/ocr_out/p-NN.mmd
  concatenate p-NN.mmd with \\pagebreak separators       -->  tools/ocr_out/question_bank.mmd
  python3 tools/split.py                                 -->  app/.../questions_1993_2015.json

Each page is one API call, run with a small concurrency cap. Per-page MMD is cached to
disk, so a failed/aborted run resumes by re-running (cached pages are skipped) and a
single bad page can be re-OCR'd in isolation by deleting its .mmd.

Credentials: OPENROUTER_API_KEY env var (never committed). Get one at
https://openrouter.ai/keys.

Usage:
    export OPENROUTER_API_KEY=...

    # Dry run: OCR a single representative page, print its MMD, do not concatenate.
    python3 tools/convert.py --dry-run 12

    # Full run: OCR every page not yet cached, then concatenate.
    python3 tools/convert.py

    # Override the model (default google/gemini-2.5-pro; fallback openai/gpt-4.1).
    python3 tools/convert.py --model openai/gpt-4.1

See docs/adr/0004-openrouter-vision-llm-conversion.md for the rationale.
"""

import argparse
import base64
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

try:
    import requests
except ImportError:
    sys.exit("This script needs the `requests` package. Install it: pip install requests")

REPO = Path(__file__).resolve().parent.parent
OUT_DIR = REPO / "tools" / "ocr_out"
PAGES_DIR = OUT_DIR / "pages"
MMD_PATH = OUT_DIR / "question_bank.mmd"

API_URL = "https://openrouter.ai/api/v1/chat/completions"
DEFAULT_MODEL = "google/gemini-2.5-pro"
FALLBACK_MODEL = "openai/gpt-4.1"
CONCURRENCY = 4
MAX_RETRIES = 3
# Cap output tokens per page. A dense two-column math page yields well under 2000 tokens
# of MMD; capping avoids OpenRouter's credit pre-check rejecting the call when it would
# otherwise reserve the model's full output ceiling (e.g. 65536 for Gemini).
# 6000 leaves headroom for the small amount of thinking Gemini still does at effort:"low"
# (reasoning is mandatory on OpenRouter's gemini-2.5-pro endpoint and cannot be disabled).
MAX_TOKENS_DEFAULT = 6000

# The system prompt pins the format split.py expects. See ADR-0004 + the contract in
# the design session: math delimiters, array styles, prose-numbers-as-prose, exact
# structural headings/numbering, verbatim transcription, [illegible] for unreadable spans.
SYSTEM_PROMPT = """\
You are a math OCR engine. Transcribe a single page image from a UACE (Uganda Advanced \
Certificate of Education) Mathematics question bank covering years 1993-2015 into \
Mathpix Markdown (MMD): plain prose with inline math in $...$ and display math in \
$$...$$. Output ONLY the transcribed MMD for the page, no commentary.

Format rules:
- Inline math: $...$. Display math: $$...$$ on its own lines. No other delimiters.
- Multi-line equations use aligned, cases, or gathered environments (KaTeX-supported). \
Never use a bare array environment.
- Plain numbers in prose stay as prose, NOT wrapped in math: e.g. the year 1995, \
"12 marks", and the question number "1." are plain text. Only mathematical expressions \
go in math delimiters.
- Paper headings, exactly as printed: "<year> PAPER ONE" or "<year> PAPER TWO", e.g. \
"1995 PAPER ONE". If a sitting is printed, include it: "1998 MARCH PAPER ONE" or \
"1998 NOV/DEC PAPER ONE".
- Section headings, exactly as printed: "SECTION A" or "SECTION B".
- Each question begins on its own line as "<n>." (e.g. "1." or "12."), optionally \
followed directly by a part letter like "1.a)". Sub-parts (a), b), i), ii) stay inline \
and do NOT start a new question.
- Two-column pages: read top-to-bottom, left column fully then right column, following \
the printed flow.

Transcription discipline:
- Transcribe verbatim. Do NOT solve, do NOT explain, do NOT reformat or rearrange.
- Keep inline "(Ans: ...)" fragments in the body exactly as printed; a later step \
extracts them.
- The degree symbol is ^\\circ (e.g. 45^\\circ, 0^\\circ, 180^\\circ), NEVER a bare \
superscript 0. "90 degrees" is $90^\\circ$, not $90^0$.
- Omit page numbers, running headers, and footers (e.g. a standalone "- 383 -" line); \
they are not part of the questions.
- If a span is illegible, emit [illegible] rather than guessing silently.
- Do NOT emit a page-break marker; the caller inserts page breaks between pages.

Example:
1995 PAPER ONE
SECTION A
1. Given that $f(x) = \\frac{x^2-1}{x+1}$, find $f(3)$. (Ans: 2)
2. (a) Solve $\\begin{aligned} 2x + y &= 5 \\\\ x - y &= 1 \\end{aligned}$. (Ans: x=2, y=1)
$$\\int_0^1 x^2 \\, dx = \\frac{1}{3}$$
"""


def auth_headers() -> dict:
    key = os.environ.get("OPENROUTER_API_KEY")
    if not key:
        sys.exit(
            "Set OPENROUTER_API_KEY first. Get one at https://openrouter.ai/keys"
        )
    return {
        "Authorization": f"Bearer {key}",
        # Optional attribution headers OpenRouter accepts.
        "X-Title": "PassMath question-bank conversion",
    }


def page_mmd(page_path: Path, model: str, max_tokens: int) -> str:
    """Call the vision LLM on one page image and return its MMD text."""
    image_b64 = base64.b64encode(page_path.read_bytes()).decode("ascii")
    payload = {
        "model": model,
        "max_tokens": max_tokens,
        # Gemini 2.5 Pro mandates reasoning on OpenRouter (it 400s if you try to disable
        # it), so set it to the lowest level instead of "none". This keeps the thinking
        # budget small and predictable, leaving max_tokens for the actual MMD output.
        "reasoning": {"effort": "low"},
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": "Transcribe this page to MMD, following the rules.",
                    },
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/png;base64,{image_b64}"
                        },
                    },
                ],
            },
        ],
    }
    headers = auth_headers()
    last_err = None
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            resp = requests.post(API_URL, headers=headers, json=payload, timeout=180)
            if resp.status_code == 429 or resp.status_code >= 500:
                # Transient: back off and retry.
                last_err = f"HTTP {resp.status_code}: {resp.text[:200]}"
                time.sleep(2 ** attempt)
                continue
            if resp.status_code != 200:
                sys.exit(f"Page {page_path.name} failed ({resp.status_code}): {resp.text[:500]}")
            body = resp.json()
            choices = body.get("choices") or []
            if not choices:
                sys.exit(f"Page {page_path.name}: no choices in response: {body}")
            content = choices[0].get("message", {}).get("content", "")
            if not content.strip():
                sys.exit(f"Page {page_path.name}: empty content in response")
            finish = choices[0].get("finish_reason")
            if finish == "length":
                # Output hit max_tokens — likely a very dense page. Mark it so the caller
                # can flag the page rather than silently shipping truncated math.
                print(f"  {page_path.name}: WARNING output truncated at max_tokens={max_tokens}")
            return content
        except requests.RequestException as exc:
            last_err = str(exc)
            time.sleep(2 ** attempt)
    sys.exit(f"Page {page_path.name} failed after {MAX_RETRIES} retries: {last_err}")


def ocr_page(page_path: Path, model: str, fallback_model: str | None, max_tokens: int) -> str:
    """OCR one page; on empty/structural failure, retry once with the fallback model."""
    try:
        return page_mmd(page_path, model, max_tokens)
    except SystemExit as exc:
        # page_mmd calls sys.exit on hard failure; try the fallback before giving up.
        if not fallback_model or fallback_model == model:
            raise
        reason = exc.code if isinstance(exc.code, str) else "unknown"
        print(f"  {page_path.name}: primary ({model}) failed [{reason[:160]}], retrying with {fallback_model}")
        return page_mmd(page_path, fallback_model, max_tokens)


def ordered_pages() -> list[Path]:
    pages = sorted(PAGES_DIR.glob("p-*.png"))
    if not pages:
        sys.exit(f"No page images found in {PAGES_DIR}. Render the PDF to p-NN.png first.")
    return pages


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--model", default=DEFAULT_MODEL, help=f"OpenRouter model slug (default {DEFAULT_MODEL})")
    parser.add_argument(
        "--max-tokens",
        type=int,
        default=MAX_TOKENS_DEFAULT,
        help=f"Max output tokens per page (default {MAX_TOKENS_DEFAULT}); caps OpenRouter's credit pre-check.",
    )
    parser.add_argument(
        "--dry-run",
        metavar="N",
        type=int,
        help="OCR only page N (1-based) and print its MMD; do not write the concatenation.",
    )
    args = parser.parse_args()

    fallback = FALLBACK_MODEL if args.model == DEFAULT_MODEL else None
    pages = ordered_pages()

    if args.dry_run is not None:
        target = PAGES_DIR / f"p-{args.dry_run:02d}.png"
        if not target.is_file():
            sys.exit(f"No such page: {target}")
        print(f"Dry run: {target.name} with {args.model} (max_tokens={args.max_tokens})")
        mmd = ocr_page(target, args.model, fallback, args.max_tokens)
        print("\n----- MMD -----\n")
        print(mmd)
        return

    OUT_DIR.mkdir(parents=True, exist_ok=True)

    # Cache-aware: skip pages whose .mmd already exists.
    todo = [p for p in pages if not (OUT_DIR / f"{p.stem}.mmd").exists()]
    if todo:
        print(f"OCR {len(todo)} page(s) with {args.model} (concurrency {CONCURRENCY})...")
        failed: list[str] = []
        with ThreadPoolExecutor(max_workers=CONCURRENCY) as pool:
            futures = {pool.submit(ocr_page, p, args.model, fallback, args.max_tokens): p for p in todo}
            for fut in as_completed(futures):
                p = futures[fut]
                try:
                    mmd = fut.result()
                    (OUT_DIR / f"{p.stem}.mmd").write_text(mmd, encoding="utf-8")
                    print(f"  wrote {p.stem}.mmd")
                except SystemExit as exc:
                    failed.append(p.name)
                    print(f"  FAILED {p.name}: {exc}")
        if failed:
            print(f"\n{len(failed)} page(s) failed: {failed}")
            print("Re-run to retry just those (delete their .mmd if any partial).")
    else:
        print("All pages cached; concatenating.")

    # Concatenate all per-page MMD with \pagebreak separators (split.py drops \pagebreak).
    parts = []
    for p in pages:
        mmd_file = OUT_DIR / f"{p.stem}.mmd"
        if not mmd_file.exists():
            print(f"  NOTE missing {mmd_file.name}; skipping")
            continue
        parts.append(mmd_file.read_text(encoding="utf-8").rstrip())
    MMD_PATH.write_text("\n\n\\pagebreak\n\n".join(parts) + "\n", encoding="utf-8")
    print(f"\nWrote {MMD_PATH} ({len(parts)} pages). Next: python3 tools/split.py")


if __name__ == "__main__":
    main()