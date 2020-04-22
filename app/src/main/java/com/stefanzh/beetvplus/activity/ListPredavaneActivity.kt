package com.stefanzh.beetvplus.activity

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefanzh.beetvplus.Category
import com.stefanzh.beetvplus.R
import com.stefanzh.beetvplus.SerialLink
import com.stefanzh.beetvplus.adapter.OnSerialLinkClickListener
import com.stefanzh.beetvplus.adapter.TvShowAdapter
import io.ktor.client.request.get
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.net.URL


class ListPredavaneActivity : CastingActivity() {

    companion object {
        const val EXTRA_CATEGORY = "com.stefanzh.beetvplus.CATEGORY"
    }

    // set up a click listener for each TV show row
    private val clickListener = object : OnSerialLinkClickListener {
        override fun onSerialLinkClick(tvShow: SerialLink) {
            val intent = Intent(this@ListPredavaneActivity, DisplaySerialActivity::class.java).apply {
                putExtra(DisplaySerialActivity.EXTRA_SERIAL, tvShow)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // start the loading screen
        setContentView(R.layout.activity_loading)

        // Get the TV show category
        val category = intent.getParcelableExtra<Category>(EXTRA_CATEGORY)!!
        title = category.title

        CoroutineScope(Dispatchers.Main).launch {
            // launch this call on UI thread but its result will be computed on IO thread
            val tvShows = getTvShows(category.link)

            // if shows are retrieved successfully, set the view to the tv shows activity
            setContentView(R.layout.activity_list_predavane)
            val layoutManager = LinearLayoutManager(this@ListPredavaneActivity)
            val adapter = TvShowAdapter(tvShows, clickListener)
            findViewById<RecyclerView>(R.id.tv_show_list).prepare(layoutManager, adapter)
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
