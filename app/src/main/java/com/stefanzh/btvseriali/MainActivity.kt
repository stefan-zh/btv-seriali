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
import io.ktor.client.request.get
import kotlinx.coroutines.*
import org.jsoup.Jsoup
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
    }

    // set up a click listener for each TV show row
    private val clickListener = object : OnSerialLinkClickListener {
        override fun onSerialLinkClick(tvShow: SerialLink) {
            val intent = Intent(this@MainActivity, DisplaySerialActivity::class.java).apply {
                putExtra(DisplaySerialActivity.EXTRA_SERIAL, tvShow.link)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // start the loading screen
        setContentView(R.layout.activity_loading)

        CoroutineScope(Dispatchers.Main).launch {
            // launch this call on UI thread but its result will be computed on IO thread
            val tvShows = getTvShows()

            // if shows are retrieved successfully, set the view to the tv shows activity
            setContentView(R.layout.activity_main)
            val viewManager = LinearLayoutManager(this@MainActivity)
            val viewAdapter = TvShowAdapter(tvShows, clickListener)

            val recyclerView = findViewById<RecyclerView>(R.id.tv_show_list).apply {
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
     * Extracts the links to the TV shows.
     * The HTTP request for shows is executed on the IO-optimized thread.
     */
    private suspend fun getTvShows(): List<SerialLink> = withContext(Dispatchers.IO) {
        val response = client.get<String>("https://btvplus.bg/seriali/")
        // parses the HTML and extracts the links to the TV shows
        val doc = Jsoup.parse(response)
        val categories = doc.select("div.bg-order > ul > li")
        val asyncTasks = categories.flatMap { cat ->
            val anchors = cat.select("div.image > a")
            anchors.map {
                async {
                    val href = it.attr("href")
                    val link = "https://btvplus.bg$href"
                    val imgSrc = it.select("img").attr("src").prefixURL()
                    val image = URL(imgSrc).toImageBitmap()

                    // extract TV show name
                    val showPage = client.get<String>(link)
                    val title = Jsoup.parse(showPage).select("div.title > h2").text()
                    SerialLink(link, imgSrc, image, title)
                }
            }
        }
        asyncTasks.awaitAll()
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
