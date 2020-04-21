package com.stefanzh.beetvplus

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : CastingActivity() {

    // the categories of TV shows
    private val categories = listOf(
        Category("Предавания", "https://btvplus.bg/predavaniya/"),
        Category("Сериали", "https://btvplus.bg/seriali/")
    )

    // set up a click listener for each TV show row
    private val clickListener = object : OnCategoryClickListener {
        override fun onCategoryClick(category: Category) {
            val intent = Intent(this@MainActivity, ListPredavaneActivity::class.java).apply {
                putExtra(ListPredavaneActivity.EXTRA_CATEGORY, category)
            }
            startActivity(intent)
        }
    }

    /**
     * Displays a list of Categories with TV shows.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // list the categories
        val layoutManager = LinearLayoutManager(this)
        val adapter = CategoryAdapter(categories, clickListener)
        findViewById<RecyclerView>(R.id.category_list).prepare(layoutManager, adapter)
    }
}
