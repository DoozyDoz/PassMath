# tools/ — one-time question-bank conversion pipeline

Converts the UACE Math Question Bank 1993-2015 PDF into a JSON asset the app bundles
and renders with KaTeX. See `docs/adr/0004-openrouter-vision-llm-conversion.md`
(supersedes ADR-0003 — MathPix was never usable, so the OCR step now uses an
OpenRouter-hosted vision LLM; the MMD → `split.py` contract is unchanged).

## Prerequisites

- Python 3.10+ with `requests`: `pip install requests`
- An OpenRouter API key → <https://openrouter.ai/keys> (the run costs well under $1;
  see ADR-0004 for why a vision LLM over a dedicated math-OCR service)
- The source PDF rendered to page images at `tools/ocr_out/pages/p-01.png` … `p-67.png`
  (the PDF is not in the repo; render it yourself, e.g. `pdftoppm -png -r 200`)

## Steps

```bash
# 1. Set credentials (never committed)
export OPENROUTER_API_KEY=...

# 2. Dry run on one dense page to validate the prompt + model before the full fan-out.
#    Eyeball the printed MMD: delimiters, array styles, and the
#    "1995 PAPER ONE" / "SECTION A" / "1." headings must match what split.py expects.
python3 tools/convert.py --dry-run 12

# 3. OCR every page (per-page, cached; re-run skips already-done pages) and
#    concatenate into tools/ocr_out/question_bank.mmd.
python3 tools/convert.py
#    Override the model if needed:  python3 tools/convert.py --model openai/gpt-4.1

# 4. Split the MMD into per-question JSON rows.
python3 tools/split.py
#    -> app/src/main/assets/questions/questions_1993_2015.json
#    prints a per-year/paper breakdown and runs a structural tripwire:
#    expect ~612 questions across 42 papers, years 1993-2015. It exits non-zero
#    if a year/paper looks collapsed (a page heading wasn't recognized).

# 5. Visually review against the PDF
#    Open tools/preview.html in a browser, load the JSON file it generated.
#    Flag any rows that look wrong; the flagged ids are logged to the browser console.
#    To re-OCR just one bad page, delete tools/ocr_out/p-NN.mmd and re-run convert.py.
```

`tools/ocr_out/` is gitignored — it holds the page PNGs, per-page `p-NN.mmd` cache, and
the concatenated `question_bank.mmd`, all regenerable from the API. Only the generated
`questions_1993_2015.json` is committed (it's what the app ships).

## Notes

- `split.py` is pattern-based and expects the PDF's regular structure. The first real run
  will likely need one tuning pass (heading/number regexes); the printed summary surfaces
  unparsed lines and empty bodies immediately, and the structural tripwire fails loudly
  if a whole year collapses.
- Two-column reading order is the model's responsibility — there is no `lines.json` column
  fallback as there was with MathPix (ADR-0004 records this loss). `preview.html` review is
  the safety net; re-OCR any page whose order looks wrong.
- **Credits / 402 errors.** OpenRouter pre-checks affordability against `max_tokens`. If a
  call fails `402 ... can only afford N tokens`, either add credit at
  <https://openrouter.ai/settings/credits> or lower `--max-tokens` (default 2000). The
  full 67-page run needs a few dollars of balance; a fresh account's free credit may only
  cover a page or two.
- v1 converts questions only. Answers are a later pass.