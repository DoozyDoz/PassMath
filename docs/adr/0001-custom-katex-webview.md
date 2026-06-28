# Custom KaTeX WebView instead of the katexmathview library

The app previously rendered math via the `in.hourglass.mathrender:katexmathview:1.0.3`
library. Its `setDisplayText` wraps the *entire* string in a single `$…$` math
expression (splitting only on `\\`) and sets the body to `white-space: nowrap`. Past-paper
questions are prose-heavy ("When the quadratic expression… is divided by p − 1, p − 2…"),
so under that renderer prose must live inside `\text{…}` and will not word-wrap on a phone
screen — it overflows horizontally instead.

We replace it with a small custom WebView that runs KaTeX `renderMathInElement`
(auto-render) over a string of **plain prose with `$…$` inline and `$$…$$` display math**.
Prose wraps naturally, math renders inline, and the format matches Mathpix's MMD output
directly (see ADR-0003), so converted questions need almost no normalization.

## Considered Options

- **Keep `katexmathview`, store questions as pure math-mode LaTeX** (`\text{}` for prose).
  Rejected: prose won't word-wrap on mobile; fights the "beautifully show" goal.
- **Custom KaTeX auto-render WebView (chosen).** Prose + `$…$` math, word-wrapping.
- **Hybrid: keep the lib for answers, new WebView for questions.** Rejected: two renderers
  for no real benefit; answers are also prose+math.

## Consequences

- New view class + layout swap in `item_card_question.xml` (`kv_question` / `kv_answer`).
- The `katexmathview` dependency is removed from `app/build.gradle`.
- The `Question` entity and `katex_question` / `katex_answer` fields are unchanged — only
  their *interpretation* changes (mixed text+math instead of pure math-mode).
- KaTeX assets are bundled locally (offline-first, per ADR-0002), not via CDN.