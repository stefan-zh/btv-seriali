package com.stefanzh.beetvplus.activity

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.stefanzh.beetvplus.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // on click on the view, close the Activity
        findViewById<LinearLayout>(R.id.about_layout).setOnClickListener {
            finish()
        }
    }
}
