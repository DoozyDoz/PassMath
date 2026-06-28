package com.kh69.passmath.data.source.local

/**
 * A (year, paper) pair, returned by `QuestionsDao.observeDistinctPapers()`. Used by the
 * dashboard's paper selector so the list of choosable papers stays in sync with whatever
 * is actually seeded in the DB (see ADR-0002). Room maps the `year` / `paper` columns of
 * the `Questions` table onto these fields by name.
 */
data class PaperYear(
    val year: Int,
    val paper: Int
)