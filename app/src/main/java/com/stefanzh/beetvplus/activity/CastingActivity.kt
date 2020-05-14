package com.stefanzh.beetvplus.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.stefanzh.beetvplus.R
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

    companion object {
        const val EXTRA_CATEGORY = "com.stefanzh.beetvplus.CATEGORY"
        const val EXTRA_SERIAL = "com.stefanzh.beetvplus.SERIAL"
        const val EXTRA_EPIZOD = "com.stefanzh.beetvplus.EPIZOD"

        // RegEx to capture the video clip source location on the bTV website
        val CLIP_REGEX = Regex("(//vid\\.btv\\.bg[\\w\\d/-]+\\.mp4)", RegexOption.MULTILINE)
    }

    // set up HTTP Client
    val client = HttpClient(Android) {
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }

    // the Cast context
    protected lateinit var castContext: CastContext
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

    /**
     * When back button is pressed, close this activity, which will go back to previous screen
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // when back arrow button is pressed
            android.R.id.home -> {
                finish()
                return true
            }
            // when the About page is selected
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

/**
 * Creates a Toast message.
 */
fun Context.toastLong(str: String) {
    val toast = Toast.makeText(this, str, Toast.LENGTH_LONG)
    toast.show()
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
