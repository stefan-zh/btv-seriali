package com.stefanzh.beetvplus

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class DisplayClipActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EPIZOD = "com.stefanzh.beetvplus.EPIZOD"

        // RegEx to capture the video clip source location on the bTV website
        val CLIP_REGEX = Regex("(//vid\\.btv\\.bg[\\w\\d/-]+\\.mp4)", RegexOption.MULTILINE)
    }

    private lateinit var videoClipUrl: String
    private var playerView: PlayerView? = null
    private var player: SimpleExoPlayer? = null
    private var defaultLayoutParams: ViewGroup.LayoutParams? = null
    private lateinit var fullScreenButton: ImageButton

    // Player state params
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var isFullScreenMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // start the loading screen
        setContentView(R.layout.activity_loading)

        // Get the TV show episode
        val tvShowEpisode = intent.getParcelableExtra<Epizod>(EXTRA_EPIZOD)!!
        title = tvShowEpisode.name
        runBlocking {
            videoClipUrl = getEpisodeClipUrl(tvShowEpisode.link)
        }
    }

    /**
     * Prepares the Player to play the Media for this Activity.
     */
    private fun initializePlayer() {
        // if TV show episode is retrieved successfully, set the view to the clip display activity
        setContentView(R.layout.activity_display_clip)
        playerView = findViewById(R.id.episode_clip)
        fullScreenButton = findViewById(R.id.exo_fullscreen_icon)
        fullScreenButton.setOnClickListener { onFullScreenToggle() }

        // set the player
        player = SimpleExoPlayer.Builder(this@DisplayClipActivity).build()
        playerView?.player = player

        // build the MediaSource from the URI
        val uri = Uri.parse(videoClipUrl)
        val dataSourceFactory = DefaultDataSourceFactory(this@DisplayClipActivity, "exoplayer-agent")
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

        // use stored state (if any) to resume (or start) playback
        player?.playWhenReady = playWhenReady
        player?.seekTo(currentWindow, playbackPosition)
        player?.prepare(mediaSource, false, false)
    }

    /**
     * Starting with API level 24 Android supports multiple windows. As our app can be visible but
     * not active in split window mode, we need to initialize the player in onStart. Before API level
     * 24 we wait as long as possible until we grab resources, so we wait until onResume before
     * initializing the player.
     */
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24 || player == null) {
            initializePlayer()
        }
        if (isFullScreenMode) {
            enterFullScreen()
        }
    }

    /**
     * Before API Level 24 there is no guarantee of onStop being called. So we have to release the
     * player as early as possible in onPause. Starting with API Level 24 (which brought multi and
     * split window mode) onStop is guaranteed to be called. In the paused state our activity is still
     * visible so we wait to release the player until onStop.
     */
    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    /**
     * Releases the resources of the player to free up the system resources.
     */
    private fun releasePlayer() {
        player?.let {
            playWhenReady = it.playWhenReady
            playbackPosition = it.currentPosition
            currentWindow = it.currentWindowIndex
            it.release()
        }
        player = null
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
     * Extracts the video clip link of the TV show.
     * The HTTP request for the video clip link is executed on the IO-optimized thread.
     */
    private suspend fun getEpisodeClipUrl(link: String): String = withContext(Dispatchers.IO) {
        // parses the HTML and extracts the links to the TV shows
        val response = MainActivity.client.get<String>(link)
        CLIP_REGEX.find(response)!!.value.prefixURL()
    }

    /**
     * Handles entering FullScreen mode.
     */
    private fun enterFullScreen() {
        // change icon to "Exit" when you enter
        fullScreenButton.setImageDrawable(getDrawable(R.drawable.exo_controls_fullscreen_exit))

        // hide the status and navigation bars
        // lean-back fullscreen mode: https://developer.android.com/training/system-ui/immersive#leanback
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()

        // capture the layout parameters before going into fullscreen mode
        // we'll use this when we exit full screen
        defaultLayoutParams = playerView?.layoutParams

        // set the PlayerView to occupy the whole screen
        playerView?.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        isFullScreenMode = true
    }

    /**
     * Handles exiting FullScreen mode.
     */
    private fun exitFullScreen() {
        // change icon to "Enter" when you exit
        fullScreenButton.setImageDrawable(getDrawable(R.drawable.exo_controls_fullscreen_enter))

        // show the status and navigation bars
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        supportActionBar?.show()

        // return to the layout from before entering fullscreen mode
        playerView?.layoutParams = defaultLayoutParams
        isFullScreenMode = false
    }

    /**
     * If we are in fullscreen mode now, the click means to exit it,
     * else it means to enter it.
     */
    private fun onFullScreenToggle() {
        if (isFullScreenMode) {
            exitFullScreen()
        } else {
            enterFullScreen()
        }
    }
}