package com.kh69.passmath.ui.katex

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.webkit.WebView

/**
 * A WebView that renders a string of **prose mixed with KaTeX math** using `$…$` inline
 * and `$$…$$` display delimiters, via KaTeX auto-render. Replaces the old
 * `katex.hourglass.in.mathlib.MathView`, which wrapped the whole string in math mode with
 * `white-space: nowrap` and so could not word-wrap prose on a phone.
 *
 * KaTeX is bundled offline under `assets/katex/` (no network). See
 * `docs/adr/0001-custom-katex-webview.md`.
 *
 * Question structure (paragraph gaps between parts, bold part labels) is inferred from
 * the `\n`-separated text at render time — the data itself is not re-OCR'd. See
 * `docs/adr/0005-renderer-side-structural-inference.md`.
 *
 * Drop-in replacement: keeps the `setDisplayText(String)` API the call sites already use,
 * and inherits `getSettings()` so the existing zoom-control setup still works.
 */
class KatexView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var textColor: Int = Color.parseColor("#1a1a1a")
    private var textSizePx: Int = 16

    init {
        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Match the old view's zoom behaviour (zoom enabled, on-screen controls hidden).
            builtInZoomControls = true
            displayZoomControls = false
            // Allow loading the bundled KaTeX assets.
            allowFileAccess = true
        }
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(Color.TRANSPARENT)
    }

    /** Render `text` as prose + `$…$` / `$$…$$` math. Called the same way as the old MathView. */
    fun setDisplayText(text: String?) {
        val body = text ?: ""
        val html = buildHtml(body)
        loadDataWithBaseURL(
            "file:///android_asset/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    fun setTextColor(color: Int) {
        textColor = color
    }

    fun setTextSize(px: Int) {
        textSizePx = px
    }

    private fun buildHtml(body: String): String {
        val structured = structureBody(body)
        return """<!doctype html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=5, user-scalable=yes">
<link rel="stylesheet" href="katex/katex.min.css">
<style>
  html, body {
    margin: 0; padding: 0;
    color: ${htmlRgba(textColor)};
    font-size: ${textSizePx}px;
    line-height: 1.5;
    -webkit-text-size-adjust: 100%;
  }
  /* Each logical line of a question is its own block (see structureBody), so prose wraps
     naturally and parts/display-math/answers no longer collapse into one run-on paragraph.
     Structural boundaries get a top margin; roman siblings stay tight; labels are bold. */
  body { word-wrap: break-word; overflow-wrap: anywhere; }
  .line { margin: 0; }
  .line.boundary { margin-top: 0.6em; }
  .line.first { margin-top: 0; }
  .line .lbl { font-weight: 700; }
  .katex-display { overflow-x: auto; overflow-y: hidden; margin: 0.6em 0; }
</style>
</head>
<body>$structured
<script src="katex/katex.min.js"></script>
<script src="katex/contrib/auto-render.min.js"></script>
<script>
  if (window.renderMathInElement) {
    renderMathInElement(document.body, {
      delimiters: [
        {left: "$$", right: "$$", display: true},
        {left: "$",  right: "$",  display: false}
      ],
      throwOnError: false
    });
  }
</script>
</body>
</html>"""
    }

    /**
     * Turns a raw `katex_question` string into block-per-line HTML so a multi-part question
     * renders with paragraph gaps and bold part labels. Structure is *inferred* from the
     * `\n`-separated text; the data is not re-OCR'd (ADR-0005).
     *
     * - `$$…$$` display-math is extracted whole (its internal `\n` are LaTeX row breaks,
     *   not paragraph breaks) and emitted as its own block.
     * - Each remaining prose line is classified by its leading label:
     *     boundary  = `1.` / `1.a)` / `a)` / `b)` / `(Ans:` / `(ans:` / bare `Ans:`
     *     roman     = `i)` / `ii)` / `(i)` / `(ii)` … (tight siblings, no gap)
     *     else      = continuation (no gap, no bold)
     * - The matched label token is wrapped in `<span class="lbl">` (bold); the first line
     *   of the question never gets a top gap.
     */
    private fun structureBody(body: String): String {
        if (body.isBlank()) return ""
        val out = StringBuilder()
        var first = true
        for (seg in tokenizeDisplayMath(body)) {
            if (seg.isDisplay) {
                out.append("<div class=\"line boundary")
                if (first) out.append(" first")
                // Escape so `&`/`<`/`>` survive into the text node; the browser decodes the
                // entities back before KaTeX auto-render reads them, so alignment `&=` works.
                out.append("\">").append(htmlEscape(seg.text)).append("</div>")
                first = false
                continue
            }
            for (rawLine in seg.text.split('\n')) {
                val line = rawLine.trimEnd()
                if (line.isBlank()) continue
                val (label, rest) = splitLabel(line)
                val cls = when {
                    first -> "line first"
                    label != null && isBoundaryLabel(label) -> "line boundary"
                    else -> "line"
                }
                out.append("<div class=\"").append(cls).append("\">")
                if (label != null) {
                    out.append("<span class=\"lbl\">").append(htmlEscape(label)).append("</span>")
                }
                out.append(htmlEscape(rest)).append("</div>")
                first = false
            }
        }
        return out.toString()
    }

    /** A body segment: either display math (`$$…$$`) kept whole, or prose to be line-split. */
    private class Segment(val text: String, val isDisplay: Boolean)

    private fun tokenizeDisplayMath(body: String): List<Segment> {
        val segs = ArrayList<Segment>()
        var i = 0
        val prose = StringBuilder()
        while (i < body.length) {
            if (body.regionMatches(i, "$$", 0, 2, ignoreCase = false)) {
                if (prose.isNotEmpty()) {
                    segs.add(Segment(prose.toString(), false))
                    prose.setLength(0)
                }
                val end = body.indexOf("$$", i + 2)
                if (end < 0) {
                    prose.append(body.substring(i))
                    i = body.length
                } else {
                    segs.add(Segment(body.substring(i, end + 2), true))
                    i = end + 2
                }
            } else {
                prose.append(body[i])
                i++
            }
        }
        if (prose.isNotEmpty()) segs.add(Segment(prose.toString(), false))
        return segs
    }

    // Leading-label grammar observed across the 692-question bank (ADR-0005). The more
    // specific `1.a)` is listed before bare `1.` so the part letter isn't left in `rest`.
    // Roman numerals and `Ans:` may be parenthesized `(ii)` / `(Ans:` or bare `ii)` / `Ans:`.
    private val LABEL = Regex(
        """^\s*(\d+\.[a-z]\)|\d+\.|[a-z]\)|\(?[ivxIVX]+\)|\([a-z]\)|\(?[Aa]ns:|Ans:)"""
    )

    /** Splits a line into (label, rest); label is null for a continuation line. */
    private fun splitLabel(line: String): Pair<String?, String> {
        val m = LABEL.find(line) ?: return Pair(null, line)
        val rest = line.substring(m.range.last + 1)
        return Pair(m.value, rest)
    }

    /** Boundaries get a gap; roman siblings do not (they group the steps of one part). */
    private fun isBoundaryLabel(label: String): Boolean {
        val s = label.trim()
        if (s.endsWith(")")) {
            // `i)` / `(ii)` / `(a)` are roman/part siblings — tight, no gap.
            if (Regex("""^\(?[ivxIVX]+\)$""").matches(s)) return false
            if (Regex("""^\([a-z]\)$""").matches(s)) return false
        }
        return true
    }

    private fun htmlEscape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun htmlRgba(color: Int): String {
        val a = Color.alpha(color) / 255f
        return "rgba(${Color.red(color)}, ${Color.green(color)}, ${Color.blue(color)}, $a)"
    }
}