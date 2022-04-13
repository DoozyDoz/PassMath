package com.kh69.passmath;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;

import com.kh69.passmath.data.cache.Question;

import java.util.ArrayList;
import java.util.List;

import katex.hourglass.in.mathlib.MathView;

public class MyViewPagerAdapter extends PagerAdapter {

    private LayoutInflater layoutInflater;
    private List<Question> mQuestions = new ArrayList<>();
    Context mContext;

    public MyViewPagerAdapter() {
    }

    public MyViewPagerAdapter(ArrayList<Question> questions, Context context) {
        mQuestions = questions;
        mContext = context;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Question question = mQuestions.get(position);
        layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = layoutInflater.inflate(R.layout.item_card_question, container, false);
        ((MathView) view.findViewById(R.id.kv_question)).setDisplayText(question.getKatex_question());

        container.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        return 8;
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
