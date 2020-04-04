package com.stefanzh.btvseriali

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DisplayClipActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EPIZOD = "com.stefanzh.btvseriali.EPIZOD"
    }

    private lateinit var videoView: VideoView

    // RegEx to capture the video clip source location on the bTV website
    private val clipRegEx = Regex("(//vid\\.btv\\.bg[\\w\\d/-]+\\.mp4)", RegexOption.MULTILINE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // start the loading screen
        setContentView(R.layout.activity_loading)

        // Get the link to the TV show episode
        val tvShowEpisodeLink = intent.getStringExtra(EXTRA_EPIZOD)!!

        CoroutineScope(Dispatchers.Main).launch {
            val videoClipUrl = getEpisodeClipUrl(tvShowEpisodeLink)

            // if TV show episode is retrieved successfully, set the view to the clip display activity
            setContentView(R.layout.activity_display_clip)

            // set the layout and play the clip
            videoView = findViewById(R.id.episode_clip)
            val uri = Uri.parse(videoClipUrl)

            // create media controller
            val mediaController = MediaController(this@DisplayClipActivity)
            mediaController.setAnchorView(videoView)
            mediaController.setMediaPlayer(videoView)

            // video view
            videoView.setVideoURI(uri)
            videoView.setMediaController(mediaController)
            videoView.start()
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
     * When the orientation changes, we need to resize the VideoView
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        videoView.holder.setSizeFromLayout()
    }

    /**
     * Extracts the video clip link of the TV show.
     * The HTTP request for the video clip link is executed on the IO-optimized thread.
     */
    private suspend fun getEpisodeClipUrl(link: String): String {
        return withContext(Dispatchers.IO) {
            // parses the HTML and extracts the links to the TV shows
            val response = MainActivity.client.get<String>(link)
            clipRegEx.find(response)!!.value.prefixURL()
        }
    }
}
