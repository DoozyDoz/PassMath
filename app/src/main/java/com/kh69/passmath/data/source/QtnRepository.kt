package com.kh69.passmath.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.kh69.passmath.AppExecutors
import com.kh69.passmath.data.Question
import com.kh69.passmath.data.Resource
import com.kh69.passmath.data.source.local.DatabaseSeeder
import com.kh69.passmath.data.source.local.MathDatabase
import com.kh69.passmath.data.source.local.PaperYear
import com.kh69.passmath.data.source.local.QuestionsDao
import com.kh69.passmath.data.source.remote.APIUtils
import com.kh69.passmath.data.source.remote.MathService

/**
 * Repository that handles Repo instances.
 *
 * unfortunate naming :/ .
 * Repo - value object name
 * Repository - type of this class.
 */
class QtnRepository constructor(
    private val appExecutors: AppExecutors,
    private val db: MathDatabase,
    private val dao: QuestionsDao,
    private val seeder: DatabaseSeeder,
    private val service: MathService = APIUtils.getMathService()
) {

    /**
     * Offline-first: observe the DB and, if it is empty on first launch, seed it from the
     * bundled JSON asset instead of calling the (dead) Heroku backend. See ADR-0002.
     */
    fun getQuestions(): LiveData<Resource<List<Question>>> {
        val result = MediatorLiveData<Resource<List<Question>>>()
        result.value = Resource.loading(null)

        appExecutors.diskIO().execute { seeder.seedIfEmpty(dao) }

        result.addSource(dao.observeQuestions()) { questions ->
            result.setValue(Resource.success(questions))
        }
        return result
    }

    /**
     * Same as [getQuestions] but scoped to one paper, for the per-paper card stepper. Seeds
     * first (so the DB is populated on first launch) then observes just that paper's rows.
     */
    fun getQuestions(year: Int, paper: Int): LiveData<Resource<List<Question>>> {
        val result = MediatorLiveData<Resource<List<Question>>>()
        result.value = Resource.loading(null)

        appExecutors.diskIO().execute { seeder.seedIfEmpty(dao) }

        result.addSource(dao.observeQuestionsByYearAndPaper(year, paper)) { questions ->
            result.setValue(Resource.success(questions))
        }
        return result
    }

    /** Every distinct (year, paper) in the DB, for the dashboard paper selector. */
    fun distinctPapers(): LiveData<List<PaperYear>> = dao.observeDistinctPapers()

    /** Kick off one-time seeding from the bundled JSON asset (idempotent; no-op if filled). */
    fun seed() {
        appExecutors.diskIO().execute { seeder.seedIfEmpty(dao) }
    }

    fun getQuestion(id: String): LiveData<Resource<Question>> {
        return object : NetworkBoundResource<Question, Question>(appExecutors) {
            override fun saveCallResult(item: Question) {
                dao.insert(item)
            }

            override fun shouldFetch(data: Question?) = data == null

            override fun loadFromDb() = dao.load(id)

            override fun createCall() = service.getQuestion(id)

        }.asLiveData()
    }

//    fun updateQuestion(question: Question): LiveData<Resource<Int>> {
//        return object : NetworkBoundResource<Int, Question>(appExecutors) {
//            override fun saveCallResult(item: Question) {
//                dao.updateQuestion(item)
//            }
//
//            override fun shouldFetch(data: Int?) = data == 1
//
//            override fun loadFromDb() = dao.updateQuestion(question)
//
//            override fun createCall() = service.updateQuestion(question.id, question)
//
//        }.asLiveData()
//    }

//    fun deleteQuestion(id: String): LiveData<Resource<Int>> {
//        return object : NetworkBoundResource<Int, Question>(appExecutors) {
//            override fun saveCallResult(item: Question) {
//                dao.deleteQuestionById(item.id)
//            }
//
//            override fun shouldFetch(data: Int?) = data == 1
//
//            override fun loadFromDb() = dao.deleteQuestionById(id)
//
//            override fun createCall() = service.deleteQuestion(id)
//
//        }.asLiveData()
//    }

//    fun deleteQuestions(): LiveData<Resource<Int>> {
//        return object : NetworkBoundResource<Int, List<Question>>(appExecutors) {
//            override fun saveCallResult(item: List<Question>) {
//                dao.deleteQuestions()
//            }
//
//            override fun shouldFetch(data: Int?) = data == 1
//
//            override fun loadFromDb() {
//                return when (dao.deleteQuestions() == 1) {
//                    1    -> LiveData(1)
//                    else -> LiveData < Int(0) >
//                }
//            }
//
//            override fun createCall() = service.deleteQuestions()
//
//
//        }.asLiveData()
//    }

}
