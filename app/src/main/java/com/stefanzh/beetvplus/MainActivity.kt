package com.stefanzh.beetvplus

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

/**
 * Prepares a RecyclerView with a LinearLayoutManager and a data Adapter.
 */
fun <T : RecyclerView.ViewHolder> RecyclerView.prepare(
    viewManager: LinearLayoutManager,
    viewAdapter: RecyclerView.Adapter<T>
): RecyclerView {
    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    setHasFixedSize(true)

    // use a linear layout manager, it's a convenient default for lists
    layoutManager = viewManager

    // specify an viewAdapter
    adapter = viewAdapter

    // apply a divider on the RecyclerView
    val divider = DividerItemDecoration(context, viewManager.orientation)
    val drawable = ContextCompat.getDrawable(context, R.drawable.recycler_view_divider)
    drawable?.let {
        divider.setDrawable(it)
        addItemDecoration(divider)
    }
    return this
}
