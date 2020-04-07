package com.stefanzh.btvseriali

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL

class DisplaySerialActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SERIAL = "com.stefanzh.btvseriali.SERIAL"
    }

    // set up a click listener for each episode row
    private val clickListener = object : OnEpizodClickListener {
        override fun onEpizodClick(episode: Epizod) {
            val intent = Intent(this@DisplaySerialActivity, DisplayClipActivity::class.java).apply {
                putExtra(DisplayClipActivity.EXTRA_EPIZOD, episode.link)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // start the loading screen
        setContentView(R.layout.activity_loading)

        // Get the link to the TV show
        val tvShowLink = intent.getStringExtra(EXTRA_SERIAL)!!

        CoroutineScope(Dispatchers.Main).launch {
            val serial = getSerial(tvShowLink)

            // if TV show is retrieved successfully, set the view to the tv show display activity
            setContentView(R.layout.activity_display_serial)

            // set the layout
            findViewById<TextView>(R.id.serial_title).apply { text = serial.title }
            findViewById<ImageView>(R.id.serial_image).apply { setImageBitmap(serial.image) }
            findViewById<TextView>(R.id.serial_description).apply { text = serial.description }

            // set the episodes
            val viewManager = LinearLayoutManager(this@DisplaySerialActivity)
            val recyclerView = findViewById<RecyclerView>(R.id.episode_list).apply {
                // use this setting to improve performance if you know that changes
                // in content do not change the layout size of the RecyclerView
                setHasFixedSize(true)

                // use a linear layout manager, it's a convenient default for lists
                layoutManager = viewManager

                // specify an viewAdapter
                adapter = EpisodeAdapter(serial.episodes, clickListener)
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

    // When back button is pressed, close this activity, which will go back to previous screen
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Extracts the episodes and other info about the TV show.
     * The HTTP request for shows is executed on the IO-optimized thread.
     */
    private suspend fun getSerial(link: String): Serial = withContext(Dispatchers.IO) {
        // parses the HTML and extracts the links to the TV shows
        val response = MainActivity.client.get<String>(link)
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
            episodes = epizodi.filter { it.isAvailable }
        )
    }
}
