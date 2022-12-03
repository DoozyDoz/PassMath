package com.kh69.passmath

import android.app.Application
import com.kh69.passmath.data.source.QtnRepository

class MathApp : Application() {
//    private val DB_NAME = "quiz_database"

    companion object {
        private var instance: MathApp? = null

        fun getContext(): MathApp {
            return instance!!
        }
    }

    val questionRepository: QtnRepository
        get() = ServiceLocator.provideQuestionsRepository(this)

    override fun onCreate() {
        super.onCreate()
        instance = this

    }
}