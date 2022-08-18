package com.kh69.passmath.ui.dashboard

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.kh69.passmath.R
import com.kh69.passmath.Tools2
import com.kh69.passmath.Tools2.setSystemBarColor
import com.kh69.passmath.Tools2.setSystemBarLight
import com.kh69.passmath.extensions.launchSettings
import com.kh69.passmath.extensions.rateApp
import com.kh69.passmath.getViewModel
import com.kh69.passmath.ui.questionCards.QuestionCards

class DashboardActivity : AppCompatActivity() {
    private var tab_layout: TabLayout? = null
    private var nested_scroll_view: NestedScrollView? = null
    private var card_form_6: LinearLayout? = null

    private val viewModel: DashboardViewModel by lazy {
        getViewModel {
            DashboardViewModel(
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        initComponent()
    }

    private fun initComponent() {
        nested_scroll_view = findViewById(R.id.nested_scroll_view)
        tab_layout = findViewById(R.id.tab_layout)

        card_form_6 = findViewById(R.id.card_form_6)

        card_form_6?.setOnClickListener { view: View? ->
            startActivity(
                Intent(
                    this,
                    QuestionCards::class.java
                )
            )
        }
        tab_layout?.let {
            it.addTab(it.newTab().setIcon(R.drawable.ic_home), 0)
            it.addTab(it.newTab().setIcon(R.drawable.ic_data_usage), 1)
            it.addTab(it.newTab().setIcon(R.drawable.ic_chat), 2)
            it.addTab(it.newTab().setIcon(R.drawable.ic_settings), 3)

            // set icon color pre-selected
            it.getTabAt(0)!!
                .icon!!.setColorFilter(
                    resources.getColor(R.color.blue_grey_400),
                    PorterDuff.Mode.SRC_IN
                )
            it.getTabAt(1)!!
                .icon!!.setColorFilter(resources.getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN)
            it.getTabAt(2)!!
                .icon!!.setColorFilter(resources.getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN)
            it.getTabAt(3)!!
                .icon!!.setColorFilter(resources.getColor(R.color.grey_20), PorterDuff.Mode.SRC_IN)
            it.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    viewModel.tabSelected(tab, this@DashboardActivity)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    tab.icon!!.setColorFilter(
                        resources.getColor(R.color.grey_20),
                        PorterDuff.Mode.SRC_IN
                    )
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    onTabClicked(tab)
                }
            })
        }
        setSystemBarColor(this, R.color.grey_5)
        setSystemBarLight(this)
    }

    private fun onTabClicked(tab: TabLayout.Tab) {
        when (tab.position) {
            0 -> Toast.makeText(applicationContext, "Home", Toast.LENGTH_SHORT).show()
            1 -> Toast.makeText(applicationContext, "Statistics", Toast.LENGTH_SHORT).show()
            2 -> {
                Toast.makeText(applicationContext, "Communication", Toast.LENGTH_SHORT).show()
                rateApp()
            }
            3 -> {
                Toast.makeText(applicationContext, "Settings", Toast.LENGTH_SHORT).show()
                launchSettings()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search_setting, menu)
        Tools2.changeMenuIconColor(menu, resources.getColor(R.color.grey_60))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        } else {
            Toast.makeText(applicationContext, item.title, Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }

}