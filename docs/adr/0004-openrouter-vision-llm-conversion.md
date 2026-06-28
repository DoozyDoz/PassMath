# Convert the question bank via an OpenRouter vision LLM (per-page MMD)

Supersedes ADR-0003. MathPix was never usable: signup/payment failed on Stripe, so no
API key was ever obtained and the `v3/pdf` run produced no output. We replace the OCR
step only, keeping ADR-0003's downstream contract intact: a vision LLM hosted on
OpenRouter transcribes each rendered page image to the same Mathpix Markdown that
`split.py` parses, and `split.py` is unchanged. Model is Gemini 2.5 Pro primary
(`google/gemini-2.5-pro`) with GPT-4.1 (`openai/gpt-4.1`) as fallback; chosen for
math-OCR and two-column reading-order quality at negligible cost for a one-time
67-page job (well under $1 total).

## Approach

- Render the PDF to 67 page PNGs (the canonical input; the PDF itself is not in the
  repo). `tools/convert.py` sends each page image to OpenRouter in a separate call
  (parallel, concurrency cap 4), with a system prompt pinning the format: `$…$` /
  `$$…$$` delimiters, `aligned`/`cases`/`gathered` arrays (not bare `array`,
  KaTeX-supported), prose numbers as prose, and the exact structural headings/numbering
  `split.py`'s regexes require (`1995 PAPER ONE`, `SECTION A`, `1.` question starts).
  `[illegible]` marks unreadable spans so they surface in review.
- Each page's MMD is cached to `tools/ocr_out/p-NN.mmd`; the run concatenates them with
  `\pagebreak` separators into `tools/ocr_out/question_bank.mmd`, which `split.py`
  consumes. The cache gives free idempotency and lets a single bad page be re-OCR'd in
  isolation.
- A single representative dense page is dry-run first to validate prompt + model before
  the full 67-page fan-out.

## Considered Options

- **Keep trying MathPix.** Rejected: signup/payment failed and we never got a key.
- **LLM emits structured JSON directly, bypassing `split.py`.** Rejected: it moves
  structural bookkeeping (year/paper/section assignment) into the model, where it is
  error-prone and hard to verify, and discards the tested `split.py` + `preview.html`
  review tooling. Keeping MMD leaves the LLM on transcription (its strength) and the
  structure in deterministic code.
- **One call for the whole document.** Rejected: 67 dense pages risk context/vision
  limits, don't parallelize, and a single failure re-bills everything. Per-page is
  cheaper to retry and `\pagebreak` reassembly is free; pages are self-contained at
  heading boundaries anyway.

## Consequences

- **The `lines.json` safety net is gone.** ADR-0003 relied on MathPix's per-line
  `column` field to reconstruct two-column reading order if a page's MMD looked wrong;
  a vision LLM returns no such metadata. Quality now rests on (a) `preview.html` visual
  review and (b) a structural sanity check: `split.py`'s per-year/paper breakdown must
  land near the expected ≈612 questions / 42 papers / years 1993–2015, failing loudly to
  pinpoint the page to re-OCR. Subtle math errors are still caught only by eyeball.
- Commit story, aligned with `tools/README` (resolving ADR-0003's inconsistency): only
  the generated `questions_1993_2015.json` is committed; `tools/ocr_out/` (page PNGs,
  per-page MMD cache, concatenated MMD) is gitignored scratch. Conversion remains a
  bring-your-own-PDF dev step. `OPENROUTER_API_KEY` lives in env, never committed.
- Scope is the 1993–2015 bank only, matching ADR-0003's v1. The 50+ other past papers
  remain a later pass; this pipeline is reusable for them at ~$0.005/page-equivalent
  once the prompt is validated.