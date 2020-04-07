package com.stefanzh.btvseriali

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : AppCompatActivity() {

    companion object {
        // set up HTTP Client
        val client = HttpClient(Android) {
            engine {
                connectTimeout = 30_000
                socketTimeout = 30_000
            }
        }

        // the categories of TV shows
        val categories = listOf(
            Category("Предавания", "https://btvplus.bg/predavaniya/"),
            Category("Сериали", "https://btvplus.bg/seriali/")
        )
    }

    // set up a click listener for each TV show row
    private val clickListener = object : OnCategoryClickListener {
        override fun onCategoryClick(category: Category) {
            val intent = Intent(this@MainActivity, ListPredavaneActivity::class.java).apply {
                putExtra(ListPredavaneActivity.EXTRA_CATEGORY, category.link)
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
        val viewManager = LinearLayoutManager(this)
        val viewAdapter = CategoryAdapter(categories, clickListener)
        val recyclerView = findViewById<RecyclerView>(R.id.category_list).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

        // apply a divider on the RecyclerView
        val divider = DividerItemDecoration(recyclerView.context, viewManager.orientation)
        val drawable = ContextCompat.getDrawable(recyclerView.context, R.drawable.recycler_view_divider)
        drawable?.let {
            divider.setDrawable(it)
            recyclerView.addItemDecoration(divider)
        }
    }
}


/**
 * Reads the image bitmap on the IO threads
 */
suspend fun URL.toImageBitmap(): Bitmap {
    return withContext(Dispatchers.IO) {
        this@toImageBitmap.openStream().use {
            BitmapFactory.decodeStream(it)
        }
    }
}

// Prefixes the URL correctly
fun String.prefixURL(): String {
    return if (this.startsWith("http")) this else "https:$this"
}
