package com.kh69.passmath.ui;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.tabs.TabLayout;
import com.kh69.passmath.R;
import com.kh69.passmath.Tools2;
import com.kh69.passmath.view.QuestionsActivity;

public class DashboardFinance extends AppCompatActivity {

    private TabLayout        tab_layout;
    private NestedScrollView nested_scroll_view;
    private LinearLayout     card_form_6;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_finance);
        initComponent();
    }

    private void initComponent() {
        nested_scroll_view = findViewById(R.id.nested_scroll_view);
        tab_layout         = findViewById(R.id.tab_layout);
        card_form_6        = findViewById(R.id.card_form_6);

        card_form_6.setOnClickListener(view -> {
            startActivity(new Intent(DashboardFinance.this, QuestionCards.class));
        });

        tab_layout.addTab(tab_layout.newTab().setIcon(R.drawable.ic_home), 0);
        tab_layout.addTab(tab_layout.newTab().setIcon(R.drawable.ic_data_usage), 1);
        tab_layout.addTab(tab_layout.newTab().setIcon(R.drawable.ic_chat), 2);
        tab_layout.addTab(tab_layout.newTab().setIcon(R.drawable.ic_menu), 3);

        // set icon color pre-selected
        tab_layout.getTabAt(0).getIcon().setColorFilter(getResources().getColor(R.color.blue_grey_400), PorterDuff.Mode.SRC_IN);
        tab_layout.getTabAt(1).getIcon().setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);
        tab_layout.getTabAt(2).getIcon().setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);
        tab_layout.getTabAt(3).getIcon().setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);

        tab_layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(getResources().getColor(R.color.blue_grey_400), PorterDuff.Mode.SRC_IN);
                onTabClicked(tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getIcon().setColorFilter(getResources().getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabClicked(tab);
            }
        });

        Tools2.setSystemBarColor(this, R.color.grey_5);
        Tools2.setSystemBarLight(this);
    }

    private void onTabClicked(TabLayout.Tab tab) {
        switch (tab.getPosition()) {
            case 0:
                Toast.makeText(getApplicationContext(), "Home", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(getApplicationContext(), "Statistics", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(getApplicationContext(), "Communication", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(getApplicationContext(), "History", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_setting, menu);
        Tools2.changeMenuIconColor(menu, getResources().getColor(R.color.grey_60));
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