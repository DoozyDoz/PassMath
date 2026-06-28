package com.kh69.passmath.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.kh69.passmath.R;
import com.kh69.passmath.data.Question;
import com.kh69.passmath.ui.katex.KatexView;
import com.kh69.passmath.ui.questionCards.QuestionCards;

import java.util.ArrayList;
import java.util.List;

public class MyViewPagerAdapter extends PagerAdapter {

    private       LayoutInflater layoutInflater;
    private final List<Question> mQuestions;
    Context mContext;

    public MyViewPagerAdapter(ArrayList<Question> questions, Context context) {
        mQuestions = questions;
        mContext   = context;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Question question = mQuestions.get(position);
        layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View            view             = layoutInflater.inflate(R.layout.item_card_question, container, false);
        KatexView       questionView     = view.findViewById(R.id.kv_question);
        KatexView       answerView       = view.findViewById(R.id.kv_answer);
        ImageButton     showAnswerButton = view.findViewById(R.id.show_answer);
        ImageView       blurryImageView  = view.findViewById(R.id.image_to_blur);
        final boolean[] answerIsVisible  = {false};

        removeZoomControls(new KatexView[]{questionView, answerView});
        questionView.setDisplayText(question.getKatex_question());
        answerView.setDisplayText(question.getKatex_answer());

        showAnswerButton.setOnClickListener(view1 -> {
            answerIsVisible[0] = !answerIsVisible[0];
            toggleAnswerVisibility(new View[]{blurryImageView, answerView}, answerIsVisible[0], showAnswerButton);
        });

        container.addView(view);
        return view;
    }


    private void toggleAnswerVisibility(View[] views, boolean ansVisible, ImageButton showAnswerBtn) {
        if (ansVisible) {
            showAnswerBtn.setColorFilter(
                    mContext.getResources().getColor(R.color.light_green_600),
                    PorterDuff.Mode.SRC_IN
            );
        } else {
            showAnswerBtn.setColorFilter(
                    mContext.getResources().getColor(R.color.grey_20),
                    PorterDuff.Mode.SRC_IN
            );
        }
        for (View view : views) {
            if (view.getVisibility() == View.VISIBLE) {
                view.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.VISIBLE);
            }
        }
    }

    private void removeZoomControls(KatexView[] mathViews) {
        for (KatexView mathView : mathViews) {
            mathView.getSettings().setBuiltInZoomControls(true);
            mathView.getSettings().setDisplayZoomControls(false);
        }

    }

    @Override
    public int getCount() {
        return QuestionCards.MAX_QUESTIONS;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        container.removeView(view);
    }
}
