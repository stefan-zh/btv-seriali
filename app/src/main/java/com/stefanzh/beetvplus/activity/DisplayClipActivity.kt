package com.stefanzh.beetvplus.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.stefanzh.beetvplus.Epizod
import com.stefanzh.beetvplus.R
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DisplayClipActivity : CastingActivity() {

    companion object {
        const val EXTRA_EPIZOD = "com.stefanzh.beetvplus.EPIZOD"

        // RegEx to capture the video clip source location on the bTV website
        val CLIP_REGEX = Regex("(//vid\\.btv\\.bg[\\w\\d/-]+\\.mp4)", RegexOption.MULTILINE)
    }

    private lateinit var tvShowEpisode: Epizod
    private lateinit var videoClipUrl: String
    private lateinit var playerView: PlayerView
    private var player: SimpleExoPlayer? = null
    private lateinit var defaultLayoutParams: ViewGroup.LayoutParams
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
        tvShowEpisode = intent.getParcelableExtra(EXTRA_EPIZOD)!!
        title = tvShowEpisode.name

        // set the view to the clip display activity
        setContentView(R.layout.activity_display_clip)
        playerView = findViewById(R.id.episode_clip)
        fullScreenButton = findViewById(R.id.exo_fullscreen_icon)
        fullScreenButton.setOnClickListener { onFullScreenToggle() }
    }

    /**
     * Prepares the Player to play the Media for this Activity.
     */
    private fun initializePlayer() {
        // first thing to do is set up the player to avoid the double initialization that happens
        // sometimes if onStart() runs and then onResume() checks if the player is null
        player = SimpleExoPlayer.Builder(this@DisplayClipActivity).build()
        playerView.player = player

        // set up the playback on a background thread to free the main thread
        CoroutineScope(Dispatchers.Main).launch {
            // fetch the video clip URL if not initialized
            if (!::videoClipUrl.isInitialized) {
                videoClipUrl = getEpisodeClipUrl(tvShowEpisode.link)
            }

            // build the MediaSource from the URI
            val uri = Uri.parse(videoClipUrl)
            val dataSourceFactory = DefaultDataSourceFactory(this@DisplayClipActivity, "exoplayer-agent")
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

            // use stored state (if any) to resume (or start) playback
            player?.playWhenReady = playWhenReady
            player?.seekTo(currentWindow, playbackPosition)
            player?.prepare(mediaSource, false, false)

            // if previously in full screen, resume it
            if (isFullScreenMode) {
                enterFullScreen()
            }
        }
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

    /**
     * Extracts the video clip link of the TV show.
     * The HTTP request for the video clip link is executed on the IO-optimized thread.
     */
    private suspend fun getEpisodeClipUrl(link: String): String = withContext(Dispatchers.IO) {
        // parses the HTML and extracts the links to the TV shows
        val response = client.get<String>(link)
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
        defaultLayoutParams = playerView.layoutParams

        // set the PlayerView to occupy the whole screen
        playerView.layoutParams = LinearLayout.LayoutParams(
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
        playerView.layoutParams = defaultLayoutParams
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