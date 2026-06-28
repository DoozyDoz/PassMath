package com.kh69.passmath.ui.questionCards

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.kh69.passmath.MathApp
import com.kh69.passmath.R
import com.kh69.passmath.Tools2
import com.kh69.passmath.data.Question
import com.kh69.passmath.data.model.QuizState
import com.kh69.passmath.databinding.ActivityCardWizardOverlapBinding
import com.kh69.passmath.getViewModel
import com.kh69.passmath.ui.katex.KatexView
import com.kh69.passmath.util.ViewAnimation
import kotlinx.android.synthetic.main.activity_stepper_text.*
import kotlinx.android.synthetic.main.item_card_question.*

class QuestionCards : AppCompatActivity() {

    companion object {
        const val EXTRA_YEAR = "extra_year"
        const val EXTRA_PAPER = "extra_paper"
    }

    private var currentQtn = 1
    val answerIsVisible = booleanArrayOf(false)

    /** Number of questions in the selected paper; 0 until the filtered query returns. */
    private var maxQuestions = 0

    private lateinit var questions: ArrayList<Question>

    private val year: Int by lazy { intent.getIntExtra(EXTRA_YEAR, -1) }
    private val paper: Int by lazy { intent.getIntExtra(EXTRA_PAPER, -1) }

    private val viewModel: QuestionCardsViewModel by lazy {
        getViewModel {
            QuestionCardsViewModel(
                MathApp.getContext().questionRepository,
                year,
                paper
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stepper_text)

        initToolbar()
        setUpViews()
        getQuestions()
    }

    private fun initToolbar() {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.setNavigationIcon(R.drawable.ic_menu)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Text"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        Tools2.setSystemBarColor(this)
    }

    private fun setUpViews() {
        lyt_back.setOnClickListener { backStep(currentQtn) }
        lyt_next.setOnClickListener { nextStep(currentQtn) }
        updateProgressText()
    }

    private fun updateProgressText() {
        val str = String.format(getString(R.string.question_of), currentQtn, maxQuestions)
        tv_steps.text = str
        status.text = str
    }

    private fun nextStep(progress: Int) {
        if (maxQuestions == 0) return
        if (progress < maxQuestions) {
            currentQtn = progress + 1
            ViewAnimation.fadeOutIn(status)
        }
        populateCard(currentQtn)
        updateProgressText()
    }

    private fun backStep(progress: Int) {
        if (maxQuestions == 0) return
        if (progress > 1) {
            currentQtn = progress - 1
            ViewAnimation.fadeOutIn(status)
        }
        populateCard(currentQtn)
        updateProgressText()
    }

    private fun prepopulateQuestions() = viewModel.questions

    private fun getQuestions() {
        prepopulateQuestions()
        viewModel.getCurrentState().observe(this)
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
        questions = quizState.data as ArrayList<Question>
        maxQuestions = questions.size
        currentQtn = 1
        // Populate the first card now that data has arrived, so the screen is not blank on
        // entry (the original bug). Next/back then step through this paper's questions.
        populateCard(currentQtn)
        updateProgressText()
    }

    private fun populateCard(index: Int) {
        if (!::questions.isInitialized || questions.isEmpty()) return
        if (currentQtn < 1 || currentQtn > questions.size) return
        val current_question = questions[currentQtn - 1]
        removeZoomControls(arrayOf(kv_question, kv_answer))
        kv_question.setDisplayText(current_question.katex_question)
        kv_answer.setDisplayText(current_question.katex_answer)
        show_answer.setOnClickListener {
            answerIsVisible[0] = !answerIsVisible[0]
            toggleAnswerVisibility(
                arrayOf(image_to_blur, kv_answer),
                answerIsVisible.get(0),
                show_answer
            )
        }
    }

    private fun removeZoomControls(mathViews: Array<KatexView>) {
        for (mathView in mathViews) {
            mathView.settings.builtInZoomControls = true
            mathView.settings.displayZoomControls = false
        }
    }

    private fun toggleAnswerVisibility(
        views: Array<View>,
        ansVisible: Boolean,
        showAnswerBtn: ImageButton
    ) {
        if (ansVisible) {
            showAnswerBtn.setColorFilter(
                resources.getColor(R.color.light_green_600),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            showAnswerBtn.setColorFilter(
                resources.getColor(R.color.grey_20),
                PorterDuff.Mode.SRC_IN
            )
        }
        for (view in views) {
            if (view.visibility == View.VISIBLE) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
            }
        }
    }

}