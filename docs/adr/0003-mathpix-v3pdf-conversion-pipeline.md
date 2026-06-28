# Convert the question bank via Mathpix v3/pdf (full-page, MMD → JSON)

**Status: superseded by ADR-0004.** MathPix was never usable (signup/payment failed on
Stripe; no API key was ever obtained). ADR-0004 keeps the MMD → `split.py` contract but
swaps the OCR step to an OpenRouter vision LLM. The rationale below is retained for
history.

The UACE Math Question Bank 1993-2015 is a 67-page, two-column PDF with ~612 numbered
questions. Automated `pdftotext` extraction garbles the math (fractions, exponents,
integrals, matrices come out broken), so the conversion must use a math-aware OCR.

We use the Mathpix `v3/pdf` API on the **whole PDF** and download its Mathpix Markdown
(MMD) output, configured with `math_inline_delimiters: ["$","$"]`,
`math_display_delimiters: ["$$","$$"]`, `idiomatic_eqn_arrays: true`, and
`include_page_breaks: true`. MMD is plain prose with `$…$` / `$$…$$` math — exactly the
representation the custom renderer (ADR-0001) consumes — so a splitter script turns it into
JSON rows with near-zero normalization.

## Considered Options

- **Manual Mathpix Snip app, one snip per question.** ~$5/month but ~612 manual
  snip→paste→metadata operations (~6-7 hours of tedious labor). Rejected on effort.
- **`v3/pdf` full-page (chosen).** ~$20 one-time setup + ~$0.34 pages (covered by the $29
  credit); ~1-2 hours of human time, mostly review. The setup fee is one-time, so
  converting the 50+ other past papers in the source folder later costs only ~$0.005/page.
- **Slice each page into 6, send each to `v3/text`.** Rejected: more expensive (each dense
  slice bills at the $0.005 PDF rate → ~$2 vs $0.34) and slices cut questions that span
  column boundaries, forcing an error-prone stitch step.

## Consequences

- Two-column reading order is handled automatically by `v3/pdf`; `lines.json` (per-line
  `column` field) is the safety net if any page's MMD order looks wrong.
- Conversion is a one-time offline step run by the developer with their own Mathpix
  credentials (`MATHPIX_APP_ID` / `MATHPIX_APP_KEY` env vars, never committed). The
  resulting `.mmd` / `lines.json` and the generated JSON are committed; the API output
  directory is gitignored.
- v1 converts questions only; answers (fragmented inline `(Ans: …)`) are a later pass.