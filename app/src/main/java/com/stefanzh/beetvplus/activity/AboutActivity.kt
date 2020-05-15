package com.stefanzh.beetvplus.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.stefanzh.beetvplus.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        // show the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.action_about)
    }

    /**
     * In the other activities we extend from [CastingActivity], which already supports
     * the functionality below, but here we are using a simple [AppCompatActivity]
     * so we need to add the back button functionality.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
