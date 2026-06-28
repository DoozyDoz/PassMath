package com.kh69.passmath.ui.cardWizard;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.kh69.passmath.R;
import com.kh69.passmath.Tools2;
import com.kh69.passmath.data.Question;
import com.kh69.passmath.data.model.QuizState;

import java.util.ArrayList;
import java.util.List;

import com.kh69.passmath.ui.katex.KatexView;


public class CardWizardOverlap extends AppCompatActivity {

    private static final int MAX_STEP = 4;

    private ViewPager          viewPager;
    private Button             btnNext;
    private MyViewPagerAdapter myViewPagerAdapter;
    private String             about_title_array[]       = {
            "Ready to Travel",
            "Pick the Ticket",
            "Flight to Destination",
            "Enjoy Holiday"
    };
    private String             about_description_array[] = {
            "Choose your destination, plan Your trip. Pick the best place for Your holiday",
            "Select the day, pick Your ticket. We give you the best prices. We guarantee!",
            "Safe and Comfort flight is our priority. Professional crew and services.",
            "Enjoy your holiday, Don't forget to feel the moment and take a photo!",
    };
    private int                about_images_array[]      = {
            R.drawable.img_wizard_1,
            R.drawable.img_wizard_2,
            R.drawable.img_wizard_3,
            R.drawable.img_wizard_4
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_wizard_overlap);

        viewPager = findViewById(R.id.view_pager);
        btnNext   = findViewById(R.id.btn_next);

        // adding bottom dots
        bottomProgressDots(0);

        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        viewPager.setClipToPadding(false);
        viewPager.setPadding(0, 0, 0, 0);
//        viewPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.viewpager_margin_overlap));
        viewPager.setOffscreenPageLimit(4);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (viewPager.getCurrentItem() == about_title_array.length - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });


        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem() + 1;
            if (current < MAX_STEP) {
                // move to next screen
                viewPager.setCurrentItem(current);
            } else {
                finish();
            }
        });

        Tools2.setSystemBarColor(this, R.color.grey_10);
        Tools2.setSystemBarLight(this);
    }

    private void renderDataState(QuizState.DataState quizState) {
        viewPager.setAdapter(new MyViewPagerAdapter(new ArrayList<>(quizState.getData())));
//        displayQuestionsView()
//        binding.rvQuestionList.adapter = QuestionAdapter(quizState.data)
//        Toast.makeText(this,""+quizState.data.size,Toast.LENGTH_SHORT).show()
    }

    private void bottomProgressDots(int current_index) {
        LinearLayout dotsLayout = findViewById(R.id.layoutDots);
        ImageView[]  dots       = new ImageView[MAX_STEP];

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this);
            int                       width_height = 15;
            LinearLayout.LayoutParams params       = new LinearLayout.LayoutParams(new ViewGroup.LayoutParams(width_height, width_height));
            params.setMargins(10, 10, 10, 10);
            dots[i].setLayoutParams(params);
            dots[i].setImageResource(R.drawable.shape_circle);
            dots[i].setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[current_index].setImageResource(R.drawable.shape_circle);
            dots[current_index].setColorFilter(getResources().getColor(R.color.light_green_600), PorterDuff.Mode.SRC_IN);
        }
    }

    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(final int position) {
            bottomProgressDots(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    /**
     * View pager adapter
     */
    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;
        private List<Question> mQuestions = new ArrayList<>();

        public MyViewPagerAdapter() {
        }

        public MyViewPagerAdapter(ArrayList<Question> questions) {
            mQuestions = questions;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Question question = mQuestions.get(position);
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(R.layout.item_card_question, container, false);
            ((KatexView) view.findViewById(R.id.kv_question)).setDisplayText(question.getKatex_question());
            ((TextView) view.findViewById(R.id.title)).setText(about_title_array[position]);
            ((TextView) view.findViewById(R.id.description)).setText(about_description_array[position]);

            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return about_title_array.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}