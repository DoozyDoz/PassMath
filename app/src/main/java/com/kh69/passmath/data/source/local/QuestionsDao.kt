package com.kh69.passmath.data.source.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kh69.passmath.data.Question
import com.kh69.passmath.util.AbsentLiveData


@Dao
interface QuestionsDao {

    /**
     * Observes list of questions.
     *
     * @return all questions.
     */
    @Query("SELECT * FROM Questions")
    fun observeQuestions(): LiveData<List<Question>>

    /**
     * Observes the questions of a single paper, in bank order. Drives the per-paper card
     * stepper (see ADR-0005 / the dashboard paper selector).
     */
    @Query("SELECT * FROM Questions WHERE year = :year AND paper = :paper ORDER BY questionId")
    fun observeQuestionsByYearAndPaper(year: Int, paper: Int): LiveData<List<Question>>

    /**
     * Observes every distinct (year, paper) actually present in the DB, ordered. Drives the
     * dashboard paper selector so the list only ever offers papers that were seeded.
     */
    @Query("SELECT DISTINCT year, paper FROM Questions ORDER BY year, paper")
    fun observeDistinctPapers(): LiveData<List<PaperYear>>

    /**
     * Observes a single question.
     *
     * @param questionId the question id.
     * @return the question with questionId.
     */
    @Query("SELECT * FROM Questions WHERE questionId = :questionId")
    fun observeQuestionById(questionId: String): LiveData<Question>

    /**
     * Select all questions from the questions table.
     *
     * @return all questions.
     */
    @Query("SELECT * FROM Questions")
    suspend fun getQuestions(): List<Question>

    /**
     * Number of questions in the table. Used to gate one-time seeding from the bundled
     * JSON asset (offline-first; see ADR-0002).
     */
    @Query("SELECT COUNT(*) FROM Questions")
    fun count(): Int

    /**
     * Select a question by id.
     *
     * @param questionId the question id.
     * @return the question with questionId.
     */
    @Query("SELECT * FROM Questions WHERE questionId = :questionId")
    suspend fun getQuestionById(questionId: String): Question?

    @Query("SELECT * FROM Questions WHERE questionId = :questionId")
    abstract fun load(questionId: String): LiveData<Question>

    /**
     * Insert a question in the database. If the question already exists, replace it.
     *
     * @param question the question to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg questions: Question)

    /**
     * Insert questions in the database. If the question already exists, replace it.
     *
     * @param questions the question to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertQuestions(questions: List<Question>)

    /**
     * Update a question.
     *
     * @param question question to be updated
     * @return the number of questions updated. This should always be 1.
     */
//    @Update
//    fun updateQuestion(question: Question): Int
//    @Update
//    fun updateQuestion(question: Question): LiveData<Int>

//    @Update
//    suspend fun updateQuestion(question: Question): Int

    /**
     * Delete a question by id.
     *
     * @return the number of questions deleted. This should always be 1.
     */
//    @Query("DELETE FROM Questions WHERE questionId = :questionId")
//    suspend fun deleteQuestionById(questionId: String): Int
//
//    @Query("DELETE FROM Questions WHERE questionId = :questionId")
//    fun deleteQuestionById(questionId: String): LiveData<Int>

    /**
     * Delete all questions.
     */
//    @Query("DELETE FROM Questions")
//    suspend fun deleteQuestions()
//    @Query("DELETE FROM Questions")
//    fun deleteQuestions():Int

}