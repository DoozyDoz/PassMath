package com.kh69.passmath.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.kh69.passmath.R;
import com.kh69.passmath.Tools2;
import com.kh69.passmath.util.ViewAnimation;

public class StepperText extends AppCompatActivity {

    private static final int MAX_STEP = 5;
    private int current_step = 1;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stepper_text);

        initToolbar();
        initComponent();
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Text");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Tools2.setSystemBarColor(this);
    }

    private void initComponent() {
        status = (TextView) findViewById(R.id.status);

        ((LinearLayout) findViewById(R.id.lyt_back)).setOnClickListener(view -> backStep(current_step));

        ((LinearLayout) findViewById(R.id.lyt_next)).setOnClickListener(view -> nextStep(current_step));

        String str_progress = String.format(getString(R.string.question_of), current_step, MAX_STEP);
        ((TextView) findViewById(R.id.tv_steps)).setText(str_progress);
        status.setText(str_progress);
    }

    private void nextStep(int progress) {
        if (progress < MAX_STEP) {
            progress++;
            current_step = progress;
            ViewAnimation.fadeOutIn(status);
        }
        String str_progress = String.format(getString(R.string.question_of), current_step, MAX_STEP);
        ((TextView) findViewById(R.id.tv_steps)).setText(str_progress);
        status.setText(str_progress);
    }

    private void backStep(int progress) {
        if (progress > 1) {
            progress--;
            current_step = progress;
            ViewAnimation.fadeOutIn(status);
        }
        String str_progress = String.format(getString(R.string.question_of), current_step, MAX_STEP);
        ((TextView) findViewById(R.id.tv_steps)).setText(str_progress);
        status.setText(str_progress);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else {
            Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }
}
