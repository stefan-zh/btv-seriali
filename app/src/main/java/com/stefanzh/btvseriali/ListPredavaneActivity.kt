package com.stefanzh.btvseriali

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ktor.client.request.get
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.net.URL


class ListPredavaneActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY = "com.stefanzh.btvseriali.CATEGORY"
    }

    // set up a click listener for each TV show row
    private val clickListener = object : OnSerialLinkClickListener {
        override fun onSerialLinkClick(tvShow: SerialLink) {
            val intent = Intent(this@ListPredavaneActivity, DisplaySerialActivity::class.java).apply {
                putExtra(DisplaySerialActivity.EXTRA_SERIAL, tvShow.link)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // start the loading screen
        setContentView(R.layout.activity_loading)

        // Get the link to the TV show episode
        val categoryLink = intent.getStringExtra(EXTRA_CATEGORY)!!

        CoroutineScope(Dispatchers.Main).launch {
            // launch this call on UI thread but its result will be computed on IO thread
            val tvShows = getTvShows(categoryLink)

            // if shows are retrieved successfully, set the view to the tv shows activity
            setContentView(R.layout.activity_list_predavane)
            val viewManager = LinearLayoutManager(this@ListPredavaneActivity)
            val recyclerView = findViewById<RecyclerView>(R.id.tv_show_list).apply {
                // use this setting to improve performance if you know that changes
                // in content do not change the layout size of the RecyclerView
                setHasFixedSize(true)

                // use a linear layout manager, it's a convenient default for lists
                layoutManager = viewManager

                // specify an viewAdapter
                adapter = TvShowAdapter(tvShows, clickListener)
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
    private suspend fun getTvShows(categoryLink: String): List<SerialLink> = withContext(Dispatchers.IO) {
        val response = MainActivity.client.get<String>(categoryLink)
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
                    val showPage = MainActivity.client.get<String>(link)
                    val title = Jsoup.parse(showPage).select("div.title > h2").text()
                    SerialLink(link, imgSrc, image, title)
                }
            }
        }
        asyncTasks.awaitAll()
    }
}
