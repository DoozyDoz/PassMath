package com.kh69.passmath

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.kh69.passmath.data.source.QtnRepository
import com.kh69.passmath.data.source.local.MathDatabase

/**
 * A Service Locator for the [QtnRepository]
 *
 */
object ServiceLocator {
    private var database: MathDatabase? = null

    @Volatile
    var questionsRepository: QtnRepository? = null
        @VisibleForTesting set

    fun provideQuestionsRepository(context: Context): QtnRepository {
        synchronized(this) {
            return questionsRepository ?: questionsRepository ?: createQuestionsRepository(context)
        }
    }

    private fun createQuestionsRepository(context: Context): QtnRepository {
        val newRepo = QtnRepository(
            AppExecutors(),
            db = database ?: createDataBase(context),
            dao = (database ?: createDataBase(context)).questionDao(),
//            service = APIUtils.getMathService()
        )
        questionsRepository = newRepo
        return newRepo
    }

    @VisibleForTesting
    fun createDataBase(
        context: Context,
        inMemory: Boolean = false
    ): MathDatabase {
        val result = if (inMemory) {
            // Use a faster in-memory database for tests
            Room.inMemoryDatabaseBuilder(context.applicationContext, MathDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        } else {
            // Real database using SQLite
            Room.databaseBuilder(
                context.applicationContext,
                MathDatabase::class.java, "Questions.db"
            ).build()
        }
        database = result
        return result
    }

}
