package com.kh69.passmath.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.kh69.passmath.helpers.isOnMainThread
import com.kh69.passmath.ui.SettingsActivity

fun Activity.launchSettings() {
    hideKeyboard()
    startActivity(Intent(applicationContext, SettingsActivity::class.java))
}

fun Activity.rateApp() {
    val manager = ReviewManagerFactory.create(this)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { request ->
        if (request.isSuccessful) {
            // We got the ReviewInfo object
            val reviewInfo = request.result
            val flow = manager.launchReviewFlow(this, reviewInfo)
            flow.addOnCompleteListener { _ ->
                // The flow has finished. The API does not indicate whether the user
                // reviewed or not, or even whether the review dialog was shown. Thus, no
                // matter the result, we continue our app flow.
            }
        } else {
            // There was some problem, continue regardless of the result.
        }
    }
}

fun Activity.hideKeyboard() {
    if (isOnMainThread()) {
        hideKeyboardSync()
    } else {
        Handler(Looper.getMainLooper()).post {
            hideKeyboardSync()
        }
    }
}

fun Activity.hideKeyboardSync() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    currentFocus?.clearFocus()
}
