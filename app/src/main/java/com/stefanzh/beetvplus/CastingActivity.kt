package com.stefanzh.beetvplus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * This is an abstract Base Activity that supports casting content to a Chromecast device.
 * It populates the menu with the Cast icon in the same place in every activity as required
 * by the Google Cast Guide.
 * https://developers.google.com/cast/docs/design_checklist/cast-button#sender-cast-icon-available
 */
abstract class CastingActivity : AppCompatActivity() {

    // set up HTTP Client
    val client = HttpClient(Android) {
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }

    // the Cast context
    private lateinit var castContext: CastContext
    private lateinit var castButton: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        castContext = CastContext.getSharedInstance(this)
    }

    /**
     * We need to populate the Cast button across all activities as suggested by Google Cast Guide:
     * https://developers.google.com/cast/docs/design_checklist/cast-button#sender-cast-icon-available
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.browse, menu)
        castButton = CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
        return result
    }
}

/**
 * Reads the image bitmap on the IO threads
 */
suspend fun URL.toImageBitmap(): Bitmap {
    return withContext(Dispatchers.IO) {
        this@toImageBitmap.openStream().use {
            BitmapFactory.decodeStream(it)
        }
    }
}

// Prefixes the URL correctly
fun String.prefixURL(): String {
    return if (this.startsWith("http")) this else "https:$this"
}

/**
 * Prepares a RecyclerView with a LinearLayoutManager and a data Adapter.
 */
fun <T : RecyclerView.ViewHolder> RecyclerView.prepare(
    viewManager: LinearLayoutManager,
    viewAdapter: RecyclerView.Adapter<T>
): RecyclerView {
    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    setHasFixedSize(true)

    // use a linear layout manager, it's a convenient default for lists
    layoutManager = viewManager

    // specify an viewAdapter
    adapter = viewAdapter

    // apply a divider on the RecyclerView to nicely outline each item
    val divider = DividerItemDecoration(context, viewManager.orientation)
    val drawable = ContextCompat.getDrawable(context, R.drawable.recycler_view_divider)
    drawable?.let {
        divider.setDrawable(it)
        addItemDecoration(divider)
    }
    return this
}