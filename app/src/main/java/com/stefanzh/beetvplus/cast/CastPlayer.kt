package com.stefanzh.beetvplus.cast

import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.api.PendingResult

class CastPlayer(private val castContext: CastContext) {

    companion object {
        const val PROGRESS_REPORT_PERIOD_MS: Long = 1000
    }

    private val statusListener = StatusListener()
    private var sessionAvailabilityListener: SessionAvailabilityListener? = null
    private var remoteMediaClient: RemoteMediaClient? = null

    // Initialization
    init {
        val sessionManager = castContext.sessionManager
        sessionManager?.addSessionManagerListener(statusListener, CastSession::class.java)
        val session = sessionManager?.currentCastSession
        setRemoteMediaClient(session?.remoteMediaClient)
    }


    /**
     * Get the current playWhenReady state
     */
    private var _playWhenReady = false
    val playWhenReady: Boolean
        get() {
            remoteMediaClient?.let { return it.isPlaying }
            return _playWhenReady
        }

    /**
     * Get the current playback position
     */
    private var _currentPosition: Long = 0
    val currentPosition: Long
        get() {
            remoteMediaClient?.let { return it.approximateStreamPosition }
            return _currentPosition
        }

    /**
     * Get the current window index
     */
    val currentWindowIndex = 0

    /**
     * Checks if a Cast Session is available
     */
    fun isCastSessionAvailable(): Boolean {
        return remoteMediaClient != null
    }

    /**
     * Sets the Remote Media Client
     */
    private fun setRemoteMediaClient(client: RemoteMediaClient?) {
        if (remoteMediaClient == client) {
            // Do nothing.
            return
        }

        // assign new client
        remoteMediaClient?.removeProgressListener(statusListener)
        remoteMediaClient = client

        // trigger callbacks
        if (client != null) {
            client.addProgressListener(statusListener, PROGRESS_REPORT_PERIOD_MS)
            sessionAvailabilityListener?.onCastSessionAvailable()
        } else {
            sessionAvailabilityListener?.onCastSessionUnavailable()
        }
    }

    // Register a RemoteMediaClient callback
    fun registerCallback(callback: RemoteMediaClient.Callback) {
        remoteMediaClient?.registerCallback(callback)
    }

    // Unregister a RemoteMediaClient callback
    fun unregisterCallback(callback: RemoteMediaClient.Callback) {
        remoteMediaClient?.unregisterCallback(callback)
    }

    /**
     * Loads the [MediaLoadRequestData] onto the remote client.
     */
    fun loadItem(mediaLoadRequest: MediaLoadRequestData): PendingResult<RemoteMediaClient.MediaChannelResult>? {
        _playWhenReady = mediaLoadRequest.autoplay
        _currentPosition = mediaLoadRequest.currentTime
        return remoteMediaClient?.load(mediaLoadRequest)
    }

    /**
     * Stops playback on remote client.
     */
    fun stop() {
        _playWhenReady = false
        remoteMediaClient?.let {
            _currentPosition = it.approximateStreamPosition
        }
        remoteMediaClient?.stop()
    }

    fun release() {
        val sessionManager = castContext.sessionManager
        sessionManager?.removeSessionManagerListener(statusListener, CastSession::class.java)
    }

    /**
     * Sets a listener for updates on the cast session availability.
     *
     * @param listener The [SessionAvailabilityListener], or null to clear the listener.
     */
    fun setSessionAvailabilityListener(listener: SessionAvailabilityListener?) {
        sessionAvailabilityListener = listener
    }


    // Internal classes.
    private inner class StatusListener: RemoteMediaClient.ProgressListener, SessionManagerListener<CastSession> {

        // RemoteMediaClient.ProgressListener implementation.
        override fun onProgressUpdated(progressMs: Long, unusedDurationMs: Long) {
            _currentPosition = progressMs
        }

        // SessionManagerListener implementation.

        override fun onSessionStarted(castSession: CastSession?, sessionId: String?) {
            setRemoteMediaClient(castSession?.remoteMediaClient)
        }

        override fun onSessionResumed(castSession: CastSession?, wasSuspended: Boolean) {
            setRemoteMediaClient(castSession?.remoteMediaClient)
        }

        override fun onSessionEnded(castSession: CastSession?, error: Int) {
            setRemoteMediaClient(null)
        }

        override fun onSessionSuspended(castSession: CastSession?, reason: Int) {
            setRemoteMediaClient(null)
        }

        override fun onSessionResumeFailed(castSession: CastSession?, error: Int) {
            // Do nothing.
        }

        override fun onSessionStarting(castSession: CastSession?) {
            // Do nothing.
        }

        override fun onSessionStartFailed(castSession: CastSession?, error: Int) {
            // Do nothing.
        }

        override fun onSessionEnding(castSession: CastSession?) {
            // Do nothing.
        }

        override fun onSessionResuming(castSession: CastSession?, sessionId: String?) {
            // Do nothing.
        }
    }
}
