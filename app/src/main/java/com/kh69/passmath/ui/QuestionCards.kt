package com.kh69.passmath.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kh69.passmath.MyViewPagerAdapter
import com.kh69.passmath.R
import com.kh69.passmath.data.Repository
import com.kh69.passmath.data.cache.Question
import com.kh69.passmath.data.model.QuizState
import com.kh69.passmath.databinding.ActivityCardWizardOverlapBinding
import com.kh69.passmath.getViewModel
import com.kh69.passmath.viewmodel.MainViewModel
import com.kh69.passmath.viewmodel.QuestionsViewModel

class QuestionCards : AppCompatActivity() {

    private lateinit var binding: ActivityCardWizardOverlapBinding
    private val viewModel by lazy { getViewModel { MainViewModel(Repository()) } }
    private val quizViewModel by lazy { getViewModel { QuestionsViewModel(Repository()) } }
    private val MAX_QUESTIONS = 8


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_wizard_overlap)

        binding = ActivityCardWizardOverlapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpViews()

        getQuestions()
    }

    private fun setUpViews() {
        binding.viewPager.offscreenPageLimit = 8
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem + 1
            if (current < MAX_QUESTIONS) {
                binding.viewPager.currentItem = current
            } else {
                finish()
            }

        }
    }

    private fun prepopulateQuestions() = viewModel.prepopulateQuestions()


    private fun getQuestions() {
        prepopulateQuestions()
        quizViewModel.getCurrentState().observe(this)
        {
            render(it)
        }
    }

    private fun render(state: QuizState) {
        when (state) {
//            is QuizState.EmptyState   -> renderEmptyState()
            is QuizState.DataState    -> renderDataState(state)
            is QuizState.LoadingState -> renderLoadingState()
        }
    }

    private fun renderLoadingState() {

    }

    private fun renderDataState(quizState: QuizState.DataState) {
        binding.viewPager.adapter = MyViewPagerAdapter(quizState.data as ArrayList<Question>, this)
    }
}