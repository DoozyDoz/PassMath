#!/usr/bin/env python3
"""
Split Mathpix Markdown (MMD) into per-question JSON rows for the PassMath app.

Input:  tools/ocr_out/question_bank.mmd  (produced by tools/convert.py)
Output: app/src/main/assets/questions/questions_1993_2015.json

Each row matches the Question entity's @SerializedName fields:
    _id, qtn_text, year, paper, section, topic, answer, katex_question, katex_answer, edited

ID scheme: {year}{sitting?}-p{paper}-{section?}-{nn}
    1995-p1-a-01          (1995, Paper 1, Section A, question 1)
    1998mar-p1-a-01       (1998 March sitting)
    1993-p1-01            (no section marker in source -> section omitted)

v1 converts questions only: katex_answer is empty; answer holds the raw "(Ans: ...)"
fragments for reference. topic is null (the bank has no topic tags).

This parser is pattern-based and expects the PDF's regular structure (year/paper/section
headings, numbered questions). It prints a summary so structural mismatches show up
immediately after the first real conversion — expect one tuning pass. See ADR-0003.
"""

import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
MMD_PATH = REPO / "tools" / "ocr_out" / "question_bank.mmd"
JSON_PATH = REPO / "app" / "src" / "main" / "assets" / "questions" / "questions_1993_2015.json"

ROMAN_PAPER = {"ONE": 1, "TWO": 2, "I": 1, "II": 2}
SITTING_CODE = {"MARCH": "mar", "NOV": "nov", "NOV/DEC": "nov", "NOV/DEC.": "nov"}

# Headings, allowing leading Markdown #'s and surrounding whitespace. Year matches both
# centuries (the bank runs 1993-2015); roman accepts words (ONE/TWO) and numerals (I/II),
# both of which the OCR emits. II is listed before I so the alternation matches the longer
# token first.
RE_PAPER_HEADING = re.compile(
    r"""^\s*\#*\s*                # optional markdown heading markers
        (?P<year>(?:19|20)\d{2})\s+      # e.g. 1995 or 2005
        (?:(?P<sitting>MARCH|NOV(?:\.?|/\s*DEC\.?))\s+)?   # optional sitting
        PAPER\s+(?P<roman>ONE|TWO|II|I)\b""",
    re.IGNORECASE | re.VERBOSE,
)
RE_SECTION_HEADING = re.compile(r"^\s*\#*\s*SECTION\s+(?P<sec>[AB])\b", re.IGNORECASE)

# A top-level question starts with "1." / "12." (optionally with a part letter in parens
# before the dot, e.g. "9(a).", or directly after, e.g. "1.a)"). Sub-parts (a)/b)/i)/ii)
# do NOT start a new question. The (?!\d) guard rejects decimals/data-grid rows like
# "1.0 1.1 1.2 ..." that the OCR emits for table data, which would otherwise be misread
# as question 1 and create duplicate IDs.
RE_QUESTION_START = re.compile(r"^\s*(?P<n>\d{1,2})(?:\([a-zA-Z]\))?\.(?!\d)")

# Inline answer fragments (raw, for reference). Removed from the question body so the
# show-answer UI hides them; kept in the `answer` field for a later katex_answer pass.
RE_ANSWER = re.compile(r"\(?Ans:?\s*(?P<body>[^)]*?)\)?(?=\s|$|\))", re.IGNORECASE)


def sitting_code(raw: str) -> str:
    if not raw:
        return ""
    key = raw.upper().replace(" ", "")
    return SITTING_CODE.get(key) or SITTING_CODE.get(raw.upper(), "")


def strip_math(text: str) -> str:
    """Crude plain-text version of a question for qtn_text (reference/search only)."""
    text = re.sub(r"\$\$.*?\$\$", " ", text, flags=re.DOTALL)
    text = re.sub(r"\$([^$]*)\$", r"\1", text)
    text = re.sub(r"^[#>\-\*]\s*", "", text, flags=re.MULTILINE)
    text = re.sub(r"\\\w+", " ", text)          # drop latex commands
    text = re.sub(r"[{}]", "", text)
    return re.sub(r"\s+", " ", text).strip()


def extract_answers(body: str) -> str:
    parts = []
    for m in RE_ANSWER.finditer(body):
        s = m.group("body").strip().strip("();")
        if s:
            parts.append(f"(Ans: {s})")
    return " | ".join(parts)


def parse(mmd: str):
    questions = []
    stats = {"papers": 0, "sections": 0, "unparsed_headings": []}

    ctx = {"year": None, "sitting": "", "paper": None, "section": None}
    current = None  # dict for the question being accumulated

    # Track display-math blocks so we don't misread lines inside $$...$$ as headings/Qs.
    in_display_math = False

    def flush():
        nonlocal current
        if current is None:
            return
        body = current.pop("_body")
        body = body.strip()
        current["katex_question"] = body
        current["qtn_text"] = strip_math(body)
        current["answer"] = extract_answers(body)
        current["katex_answer"] = ""
        current["topic"] = None
        current["edited"] = False
        if not body:
            stats.setdefault("empty_bodies", []).append(current["_id"])
        questions.append(current)
        current = None

    def make_id(n: int) -> str:
        y = ctx["year"]
        p = ctx["paper"]
        if y is None or p is None:
            return f"orphan-{len(questions) + 1:02d}-{n:02d}"
        sit = ctx["sitting"]
        sec = ctx["section"]
        parts = [f"{y}{sit}", f"p{p}"]
        if sec:
            parts.append(sec.lower())
        parts.append(f"{n:02d}")
        return "-".join(parts)

    for raw_line in mmd.splitlines():
        line = raw_line.rstrip()

        # Toggle display-math state on standalone $$ markers.
        if line.count("$$") % 2 == 1:
            in_display_math = not in_display_math
            # fall through: keep the $$ line in the current body if any

        if not in_display_math:
            mp = RE_PAPER_HEADING.match(line)
            if mp:
                flush()
                ctx["year"] = int(mp.group("year"))
                ctx["sitting"] = sitting_code(mp.group("sitting") or "")
                ctx["paper"] = ROMAN_PAPER[mp.group("roman").upper()]
                ctx["section"] = None  # reset; section heading follows if present
                stats["papers"] += 1
                continue
            ms = RE_SECTION_HEADING.match(line)
            if ms:
                flush()
                ctx["section"] = ms.group("sec").upper()
                stats["sections"] += 1
                continue

        # \pagebreak is a page boundary, not a question boundary; drop it.
        if line.strip() == r"\pagebreak":
            if current is not None:
                current["_body"] += "\n"
            continue

        if not in_display_math:
            mq = RE_QUESTION_START.match(line)
            if mq and not line.lstrip().startswith(("#",)):
                flush()
                n = int(mq.group("n"))
                current = {
                    "_id": make_id(n),
                    "year": ctx["year"] if ctx["year"] is not None else 0,
                    "paper": ctx["paper"] if ctx["paper"] is not None else 0,
                    "section": ctx["section"],
                    "_body": line + "\n",
                }
                continue

        # Otherwise: continuation of the current question (prose, math, sub-parts).
        if current is not None:
            current["_body"] += line + "\n"
        else:
            # Non-heading, non-question text before any question: note it.
            stripped = line.strip()
            if stripped and not stripped.startswith("$$"):
                stats["unparsed_headings"].append(stripped[:80])

    flush()
    return questions, stats


def main() -> None:
    if not MMD_PATH.exists():
        sys.exit(f"Missing {MMD_PATH}. Run tools/convert.py first.")
    mmd = MMD_PATH.read_text(encoding="utf-8")
    questions, stats = parse(mmd)

    JSON_PATH.parent.mkdir(parents=True, exist_ok=True)
    JSON_PATH.write_text(
        json.dumps(questions, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    print(f"Wrote {len(questions)} questions -> {JSON_PATH}")
    print(f"  papers recognized : {stats['papers']}")
    print(f"  sections recognized: {stats['sections']}")
    if stats.get("empty_bodies"):
        print(f"  WARNING empty bodies: {stats['empty_bodies']}")
    if stats["unparsed_headings"]:
        print(f"  NOTE unparsed lines before first question (first 10):")
        for s in stats["unparsed_headings"][:10]:
            print(f"    - {s}")

    # Per-year/paper breakdown for a quick sanity check.
    counts = {}
    for q in questions:
        counts.setdefault((q["year"], q["paper"]), 0)
        counts[(q["year"], q["paper"])] += 1
    print("  breakdown:")
    for (y, p), c in sorted(counts.items()):
        print(f"    {y} p{p}: {c}")

    # Structural tripwire (ADR-0004): the bank is ~612 questions across 42 papers,
    # years 1993-2015. A paper heading the LLM didn't reproduce collapses a whole year
    # into one question, which the per-year/paper count exposes immediately. Fail
    # loudly so convert.py's output is never silently shipped broken.
    years_present = {q["year"] for q in questions if q["year"]}
    # The source PDF runs 1993-2015 but omits 2010 (confirmed against the PDF).
    expected_years = set(range(1993, 2016)) - {2010}
    missing_years = sorted(expected_years - years_present)
    # Hard fail only on gross collapse (few papers/questions = many headings missed).
    # A single missing year is a warning, not a failure: it may be a legitimate source
    # gap (e.g. the bank skips a year) rather than a parse bug — verify against the PDF.
    problems = []
    if stats["papers"] < 35:
        problems.append(f"only {stats['papers']} papers recognized (expected ~42)")
    if len(questions) < 400:
        problems.append(f"only {len(questions)} questions parsed (expected ~612)")
    if problems:
        print("\n  *** STRUCTURAL CHECK FAILED ***")
        for p in problems:
            print(f"    - {p}")
        print("  A page heading or numbering likely wasn't recognized. Re-OCR the")
        print("  suspect page(s) by deleting their tools/ocr_out/p-NN.mmd and re-running")
        print("  tools/convert.py. JSON was still written for inspection but is suspect.")
        sys.exit(1)
    if missing_years:
        print("\n  NOTE: years with zero questions (verify against the source PDF; may be a")
        print(f"  legitimate gap, not a parse failure): {missing_years}")


if __name__ == "__main__":
    main()