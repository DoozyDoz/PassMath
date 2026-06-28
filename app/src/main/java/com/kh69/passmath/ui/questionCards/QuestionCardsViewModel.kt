package com.kh69.passmath.ui.questionCards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kh69.passmath.data.Status
import com.kh69.passmath.data.model.QuizState
import com.kh69.passmath.data.source.QtnRepository

class QuestionCardsViewModel constructor(
    repository: QtnRepository,
    year: Int,
    paper: Int
) : ViewModel() {
    val questions = repository.getQuestions(year, paper)
    private val selectedQuestion = MutableLiveData<Int>()
    private val currentState = MediatorLiveData<QuizState>()

    fun getCurrentState(): LiveData<QuizState> = currentState

    private fun setSelectedQuestionTo(position: Int) {
        selectedQuestion.postValue(position)
    }

    private fun addStateSources() {
        currentState.addSource(selectedQuestion)
        { currentQuestionPosition ->
            currentState.postValue(QuizState.SelectedState(currentQuestionPosition))
        }

        currentState.addSource(questions)
        { questions ->
            when (questions.status) {
                Status.SUCCESS -> {
                    currentState.postValue(QuizState.DataState(questions.data!!))
                }
                Status.LOADING -> {
                    currentState.postValue(QuizState.LoadingState)
                }
                else           -> {
                    currentState.postValue(QuizState.EmptyState)
                }
            }
        }
    }

//    fun nextQuestion(position: Int) {
//        setSelectedQuestionTo(position)
//    }


    init {
        currentState.postValue(QuizState.LoadingState)
        addStateSources()
    }

}