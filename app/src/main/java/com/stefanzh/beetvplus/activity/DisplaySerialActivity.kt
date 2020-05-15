package com.stefanzh.beetvplus.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.stefanzh.beetvplus.Epizod
import com.stefanzh.beetvplus.R
import com.stefanzh.beetvplus.Serial
import com.stefanzh.beetvplus.SerialLink
import com.stefanzh.beetvplus.adapter.EpisodeAdapter
import com.stefanzh.beetvplus.adapter.OnEpizodClickListener
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL

class DisplaySerialActivity : CastingActivity() {

    private lateinit var tvShow: SerialLink

    // set up a click listener for each episode row
    private val clickListener = object : OnEpizodClickListener {
        override fun onEpizodClick(episode: Epizod) {
            if (!episode.isAvailable) {
                // send toast that the episode is locked
                val episodeLockedMsg = applicationContext.resources.getString(R.string.episode_locked)
                applicationContext.toastLong(episodeLockedMsg)
            } else {
                val intent = Intent(this@DisplaySerialActivity, DisplayClipActivity::class.java).apply {
                    putExtra(EXTRA_EPIZOD, episode)
                    putExtra(EXTRA_SERIAL, tvShow)
                }
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_serial)

        // Get the TV show
        tvShow = intent.getParcelableExtra(EXTRA_SERIAL)!!
        title = tvShow.title

        CoroutineScope(Dispatchers.Main).launch {
            val serial = getSerial(tvShow.link)

            // if TV show is retrieved successfully, stop the spinner and display tv show details
            findViewById<ProgressBar>(R.id.display_serial_spinner).visibility = View.GONE
            findViewById<NestedScrollView>(R.id.display_serial_view).visibility = View.VISIBLE

            // set the layout
            findViewById<TextView>(R.id.serial_title).apply { text = serial.title }
            findViewById<ImageView>(R.id.serial_image).apply { setImageBitmap(serial.image) }
            findViewById<TextView>(R.id.serial_description).apply { text = serial.description }

            // display the list of episodes when available
            val episodesListView = findViewById<RecyclerView>(R.id.episode_list)
            if (serial.episodes.isEmpty()) {
                episodesListView.visibility = View.GONE
                findViewById<TextView>(R.id.empty_episodes).apply { visibility = View.VISIBLE }
            } else {
                val adapter = EpisodeAdapter(serial.episodes, clickListener)
                episodesListView.prepare(this@DisplaySerialActivity, adapter)
            }
        }
    }

    /**
     * Extracts the episodes and other info about the TV show.
     * The HTTP request for shows is executed on the IO-optimized thread.
     */
    private suspend fun getSerial(link: String): Serial = withContext(Dispatchers.IO) {
        // parses the HTML and extracts the links to the TV shows
        val response = client.get<String>(link)
        val doc = Jsoup.parse(response)

        // extract metadata
        val content = doc.select("div.pproduct-content")
        val title = content.select("div.title > h2").text()
        val imageUrl = content.select("div.image > img").attr("src").prefixURL()
        val description = content.select("div.pproduct-description").text()

        // extract episodes
        val episodes = doc.select("div.parent-products > ul > li")
        val epizodi = episodes.map { episode ->
            val anchor = episode.select("div.image > a")
            val elink = anchor.attr("href")
            val eImageUrl = anchor.select("img").attr("src").prefixURL()
            val eLength = anchor.select("div.video_length").text()
            val eIsAvailable = anchor.select("div[class=archive]").isEmpty()
            val eName = episode.select("div.meta a").text()
            Epizod(
                link = "https://btvplus.bg$elink",
                imageUrl = eImageUrl,
                image = URL(eImageUrl).toImageBitmap(),
                name = eName,
                length = eLength,
                isAvailable = eIsAvailable
            )
        }
        Serial(
            title = title,
            imageUrl = imageUrl,
            image = URL(imageUrl).toImageBitmap(),
            description = description,
            episodes = epizodi
        )
    }
}
