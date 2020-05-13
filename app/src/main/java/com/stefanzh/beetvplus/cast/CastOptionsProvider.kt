package com.stefanzh.beetvplus.cast

import android.content.Context
import com.google.android.exoplayer2.ext.cast.DefaultCastOptionsProvider
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * A convenience [OptionsProvider] to target the default cast receiver app.
 * Base on the implementation of [DefaultCastOptionsProvider].
 */
class CastOptionsProvider : OptionsProvider {

    /**
     * Provides the Cast Options for the Notifications and Lock Screen.
     * Also connects the Mini Controller with the Expanded Controller - when the mini controller
     * is clicked it will launch the expanded controller.
     */
    override fun getCastOptions(p0: Context?): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(CastExpandedController::class.java.name)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(CastExpandedController::class.java.name)
            .build()

        // build the Cast Options
        return CastOptions.Builder()
            .setReceiverApplicationId(DefaultCastOptionsProvider.APP_ID_DEFAULT_RECEIVER)
            .setStopReceiverApplicationWhenEndingSession(true)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(p0: Context?): MutableList<SessionProvider> {
        return mutableListOf()
    }
}
