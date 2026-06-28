# PassMath

An Android app that lets UACE (Uganda Advanced Certificate of Education) Mathematics
candidates practice past-paper questions, rendered as properly typeset math via KaTeX.

## Language

**Question Bank**:
A curated collection of UACE Mathematics past-paper questions, organized by year and paper.
The current conversion source is the "UACE Math Question Bank 1993-2015" PDF.
_Avoid_: past papers (when referring to the curated single document), question paper

**Paper**:
A single UACE Mathematics exam paper for a given year. Pure Math has Paper One and Paper Two.
Stored as an int (1 or 2).
_Avoid_: exam, paper one/two (as free text — use the int)

**Sitting**:
A specific exam session within a year. Most years have one sitting; 1998 has two
(`MARCH` and `NOV/DEC`). The `Question` entity has no sitting field, so the sitting is
encoded only in the question ID. The main UACE sitting is Nov/Dec.
_Avoid_: session, attempt

**Section**:
A division of a Paper. UACE papers are split into Section A (short questions, 40 marks)
and Section B (long questions, 60 marks). 1993-1994 papers in the bank carry no section
markers; for those the field is null rather than fabricated.
_Avoid_: part, division

**Topic**:
The mathematical topic a question belongs to (e.g. vectors, integration). The question
bank contains no topic tags; the field is null for all converted questions, to be
populated by a future inference pass.
_Avoid_: category, theme

**Question**:
One numbered item from a Paper, comprising a stem (the problem text, possibly with parts
a/b/i/ii) and, in v1, no converted answer. Rendered from the `katex_question` field as
mixed prose + `$…$` inline / `$$…$$` display math.
_Avoid_: item, problem

**Answer**:
The solution to a Question. In the source PDF, answers are inline and fragmented
(`(Ans: …)` per part). v1 of the conversion pipeline does **not** convert answers;
`katex_answer` is left empty and `answer` holds the raw PDF text for reference.
_Avoid_: solution, marking guide