package com.kh69.passmath.data.source.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kh69.passmath.data.Question

/**
 * Loads the bundled question-bank JSON asset into [Question] objects for one-time Room
 * seeding. Offline-first: the app ships `assets/questions/questions_1993_2015.json` and
 * seeds the `questions` table from it on first launch instead of fetching from the (dead)
 * Heroku backend. See `docs/adr/0002-offline-first-bundled-json.md`.
 */
class DatabaseSeeder(private val context: Context) {

    private val assetPath = "questions/questions_1993_2015.json"

    /** True if the bundled JSON asset is present (i.e. the conversion pipeline has been run). */
    fun isAvailable(): Boolean {
        return try {
            context.assets.open(assetPath).use { true }
        } catch (e: Exception) {
            false
        }
    }

    /** Parse the bundled JSON into a list of questions. Throws if the asset is missing. */
    fun load(): List<Question> {
        val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        val type = TypeToken.getParameterized(List::class.java, Question::class.java).type
        return Gson().fromJson(json, type)
    }

    /**
     * Load and insert into [dao] only if the table is empty. Safe to call on every launch:
     * the count check means the asset is only read once (the first launch with an empty DB).
     */
    fun seedIfEmpty(dao: QuestionsDao) {
        if (dao.count() != 0) return
        try {
            val questions = load()
            if (questions.isNotEmpty()) {
                dao.insertQuestions(questions)
                Log.i(TAG, "Seeded ${questions.size} questions from bundled asset.")
            }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Bundled question asset not found at $assetPath; skipping seed. " +
                    "Run tools/convert.py + tools/split.py to generate it.",
                e
            )
        }
    }

    companion object {
        private const val TAG = "DatabaseSeeder"
    }
}