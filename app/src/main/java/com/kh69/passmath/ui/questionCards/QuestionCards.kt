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
import com.kh69.passmath.util.ViewAnimation
import katex.hourglass.`in`.mathlib.MathView
import kotlinx.android.synthetic.main.activity_stepper_text.*
import kotlinx.android.synthetic.main.item_card_question.*

class QuestionCards : AppCompatActivity() {

    companion object {
        const val MAX_QUESTIONS = 4
    }

    private var currentQtn = 1
    val answerIsVisible = booleanArrayOf(false)


    private lateinit var binding: ActivityCardWizardOverlapBinding
    private lateinit var questions: ArrayList<Question>

    private val viewModel: QuestionCardsViewModel by lazy {
        getViewModel {
            QuestionCardsViewModel(
                MathApp.getContext().questionRepository
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stepper_text)

        initToolbar()

//        binding = ActivityCardWizardOverlapBinding.inflate(layoutInflater)
//        setContentView(binding.root)

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
        val str_progress =
            String.format(getString(R.string.question_of), currentQtn, MAX_QUESTIONS)

        tv_steps.text = str_progress
        status.text = str_progress

        bottomProgressDots(0)

    }

    private fun nextStep(progress: Int) {
        var progress = progress
        if (progress < MAX_QUESTIONS) {
            progress++
            currentQtn = progress
            ViewAnimation.fadeOutIn(status)
        }
        populateCard(currentQtn)
        val str_progress =
            String.format(getString(R.string.question_of), currentQtn, MAX_QUESTIONS)
        (findViewById<View>(R.id.tv_steps) as TextView).text = str_progress
        status.text = str_progress
    }

    private fun backStep(progress: Int) {
        var progress = progress
        if (progress > 1) {
            progress--
            currentQtn = progress
            ViewAnimation.fadeOutIn(status)
        }
        populateCard(currentQtn)
        val str_progress =
            String.format(getString(R.string.question_of), currentQtn, MAX_QUESTIONS)
        (findViewById<View>(R.id.tv_steps) as TextView).text = str_progress
        status.text = str_progress
    }

    private fun bottomProgressDots(current_index: Int) {
        val dots = arrayOfNulls<ImageView>(MAX_QUESTIONS)
//        binding.layoutDots.removeAllViews()

        for (i in dots.indices) {
            dots[i] = ImageView(this)
            val width_height = 15
            val params =
                LinearLayout.LayoutParams(ViewGroup.LayoutParams(width_height, width_height))
            params.setMargins(10, 10, 10, 10)
            dots[i]!!.layoutParams = params
            dots[i]!!.setImageResource(R.drawable.shape_circle)
            dots[i]!!.setColorFilter(resources.getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN)
//            binding.layoutDots.addView(dots[i])
        }
        if (dots.isNotEmpty()) {
            dots[current_index]!!.setImageResource(R.drawable.shape_circle)
            dots[current_index]!!.setColorFilter(
                resources.getColor(R.color.light_green_600),
                PorterDuff.Mode.SRC_IN
            );
        }
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
//        binding.viewPager.adapter = MyViewPagerAdapter(quizState.data as ArrayList<Question>, this)
    }

    private fun populateCard(index: Int) {
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

    private fun removeZoomControls(mathViews: Array<MathView>) {
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