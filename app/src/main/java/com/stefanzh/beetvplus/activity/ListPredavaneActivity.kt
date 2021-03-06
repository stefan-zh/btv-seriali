package com.stefanzh.beetvplus.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.stefanzh.beetvplus.Category
import com.stefanzh.beetvplus.R
import com.stefanzh.beetvplus.SerialLink
import com.stefanzh.beetvplus.adapter.OnSerialLinkClickListener
import com.stefanzh.beetvplus.adapter.TvShowAdapter
import io.ktor.client.request.get
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URL


class ListPredavaneActivity : CastingActivity() {

    // set up a click listener for each TV show row
    private val clickListener = object : OnSerialLinkClickListener {
        override fun onSerialLinkClick(tvShow: SerialLink) {
            val intent = Intent(this@ListPredavaneActivity, DisplaySerialActivity::class.java).apply {
                putExtra(EXTRA_SERIAL, tvShow)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_predavane)

        // Get the TV show category
        val category = intent.getParcelableExtra<Category>(EXTRA_CATEGORY)!!
        title = category.title

        CoroutineScope(Dispatchers.Main).launch {
            // launch this call on UI thread but its result will be computed on IO thread
            val tvShows = getTvShows(category.link)

            // if shows are retrieved successfully, stop spinner and display the tv shows
            findViewById<ProgressBar>(R.id.tv_show_spinner).visibility = View.GONE
            findViewById<RecyclerView>(R.id.tv_show_list).visibility = View.VISIBLE

            // create RecyclerView with TV shows
            val adapter = TvShowAdapter(tvShows, clickListener)
            findViewById<RecyclerView>(R.id.tv_show_list).prepare(this@ListPredavaneActivity, adapter)
        }
    }

    /**
     * Get the data for an individual TV show. Sometimes the DOM for the TV show may be
     * corrupted or incomplete so we'll return null for such shows (and will later skip them).
     */
    private suspend fun getTvShow(el: Element): SerialLink? = withContext(Dispatchers.IO) {
        val href = el.attr("href")
        val link = "https://btvplus.bg$href"
        val imgSrc = el.select("img").attr("src").prefixURL()
        try {
            val image = URL(imgSrc).toImageBitmap()
            // extract TV show name
            val showPage = client.get<String>(link)
            val title = Jsoup.parse(showPage).select("div.title > h2").text().ifBlank {
                // we'll consider a show without a title to be improper
                return@withContext null
            }
            SerialLink(link, imgSrc, image, title)
        } catch (e: Exception) {
            // it's possible that there are errors on the DOM
            // so we'll skip such shows
            null
        }
    }

    /**
     * Extracts the links to the TV shows.
     * The HTTP request for shows is executed on the IO-optimized thread.
     */
    private suspend fun getTvShows(categoryLink: String): List<SerialLink> = withContext(Dispatchers.IO) {
        val response = client.get<String>(categoryLink)
        // parses the HTML and extracts the links to the TV shows
        val doc = Jsoup.parse(response)
        val categories = doc.select("li.rows > ul > li")
        val asyncTasks = categories.flatMap { cat ->
            val anchors = cat.select("div.image > a")
            anchors.map {
                async { getTvShow(it) }
            }
        }
        asyncTasks.awaitAll().filterNotNull()
    }
}
