# Renderer-side structural inference for question layout (no re-OCR)

The 692 converted questions render as a run-on block: the OCR data uses single `\n`
between a question's parts, sub-parts, display-math blocks, and inline answers, with no
blank lines. `KatexView` previously used `white-space: pre-line`, which preserves newlines
as line breaks but collapses runs of spaces — so parts land as consecutive tight lines
with no paragraph gaps and no visual hierarchy for the part labels (`1.`, `a)`, `i)`).

The OCR *content* is reviewed and trusted (see ADR-0004: 692 questions, 46 papers, 0
orphans, 0 empties). The problem is purely visual layout, not data quality.

## Decision

Infer question structure inside `KatexView` at render time, from the existing
`\n`-separated text. Do **not** re-OCR and do **not** change
`questions_1993_2015.json`.

`KatexView.buildHtml` gains a small tokenizer + label classifier that runs before KaTeX
auto-render:

1. **Tokenize** the body into prose segments and `$$…$$` display-math segments, never
   splitting on `\n` inside a `$$…$$` block (so multi-line `\begin{aligned}` systems stay
   one block).
2. **Split prose** on `\n` into logical lines and classify each line's leading label
   against the grammar observed in the data:
   `^\d+\.(?:[a-z]\))?` (NUM / NUMPART), `^[a-z]\)` (PART),
   `^\(?[ivxIVX]+\)` paren-or-not (ROMAN), `^\([a-z]\)` (paren part),
   `^\(?[Aa]ns` paren-or-bare (answer line).
3. **Emit one block element per logical line**, replacing `white-space: pre-line`:
   - **Boundary** lines (NUM, PART, `(Ans:`, display-math) get `margin-top` before them.
   - **Roman siblings** (`i)`/`ii)`/`(iii)`) stay tight — no inter-sibling gap — so the
     steps of one part read as a group; their labels are bold.
   - **Continuation** lines get no gap and no bold.
   - The label token only (e.g. `b)`, `1.a)`, `(Ans:`) is bolded, not the whole line.
   - The first line never gets a top gap.
4. Display math stays left-aligned with `overflow-x: auto` on overflow; it gets a margin
   before and after. `(Ans:)` gets a gap and a bold label at normal text color (the card's
   separate blurred Solution area already carries the answer; muting was considered and
   deferred).

`tools/preview.html` mirrors the same structure so the dev review tool shows exactly what
the app ships.

## Considered Options

- **Renderer-side structural inference (chosen).** Keeps the reviewed data untouched; ships
  the visual fix immediately; the inference is deterministic and lives in one place
  (`KatexView`), shared by the card stack and the list.
- **Re-OCR with a structural prompt** (emit blank lines / markers between parts, then
  re-run `split.py`). Cleaner separation in the data itself, but re-OCRs all 46 papers,
  risks regressing the reviewed 692-question set, and re-opens the review cycle — all to
  fix a problem the data doesn't have (content is fine; only layout is poor).
- **Pure CSS.** Rejected: CSS cannot select a line by its text content, so it cannot tell
  a part-label line (`b) …`) from a continuation line — structural gaps and bold labels
  need the tokenizer.
- **Nested indent by level** (1. flush, a)/b) indent 1, i)/ii) indent 2). Rejected: the OCR
  labels are inconsistent (`1.a)` fuses number+part; `b)` sometimes follows `i)` blocks),
  so inferred indent levels mislead more than they help. A flat layout with bold labels and
  boundary gaps is tolerant of the mess.
- **Mute the inline `(Ans:)` line.** Deferred: the user did not flag the answer's presence
  as a problem, and the card already has a separate blurred Solution area.

## Consequences

- `KatexView.buildHtml` gains a tokenizer + label classifier (~one screen of Kotlin); the
  `white-space: pre-line` rule is replaced by explicit block-per-line HTML.
- `tools/preview.html` is updated to mirror, so visual review stays honest.
- `questions_1993_2015.json` and the conversion pipeline are unchanged.
- Tuning (gap size, line-height, font-size) lives in one CSS block in `KatexView` and is
  easy to adjust without touching the data or the classifier.
- Future conversions *could* emit structure natively (blank lines between parts); the
  renderer-side inference then degrades to a harmless no-op/fallback rather than needing
  removal — but that is a separate, later decision (not taken here).