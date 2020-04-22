package com.stefanzh.beetvplus.adapter


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stefanzh.beetvplus.Epizod
import com.stefanzh.beetvplus.R
import kotlinx.android.synthetic.main.fragment_episode.view.*

interface OnEpizodClickListener {
    fun onEpizodClick(episode: Epizod)
}

/**
 * [RecyclerView.Adapter] that can display an [Epizod] and makes a call to the
 * specified [OnEpizodClickListener].
 */
class EpisodeAdapter(
    private val episodes: List<Epizod>,
    private val clickListener: OnEpizodClickListener
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeRow>() {

    companion object {
        // UTF-8 string that represents the lock icon 🔒
        const val LOCK_ICON = "\uD83D\uDD12"
    }

    /**
     * Creates each [EpisodeRow].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeRow {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_episode, parent, false)
        return EpisodeRow(view)
    }

    /**
     * Places the Episode row into the view. It includes an image of the Episode to the left
     * and the title of the episode along with its duration next to the image.
     */
    override fun onBindViewHolder(row: EpisodeRow, position: Int) {
        val episode = episodes[position]
        val bitmap = if (episode.isAvailable) episode.image else episode.image.toLocked()
        val imageToLeft = BitmapDrawable(row.episodeRow.resources, bitmap)
        row.episodeRow.setCompoundDrawablesWithIntrinsicBounds(imageToLeft, null, null, null)
        // construct the episode name with duration from the string template
        val episodeName = row.episodeRow.resources.getString(R.string.episode_name, episode.name, episode.length)
        row.episodeRow.text = episodeName

        row.rowView.setOnClickListener {
            clickListener.onEpizodClick(episode)
        }
    }

    override fun getItemCount(): Int = episodes.size

    // Helper class that represents each episode row in the list
    inner class EpisodeRow(val rowView: View) : RecyclerView.ViewHolder(rowView) {
        val episodeRow: TextView = rowView.episode_row

        override fun toString(): String {
            return super.toString() + " '" + episodeRow.text + "'"
        }
    }

    /**
     * Creates a bitmap from the original image with a black overlay on top
     * and a lock icon. This signifies that the video is not freely available to play.
     */
    private fun Bitmap.toLocked(): Bitmap {
        // we create a new bitmap and attach it to the canvas
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // we draw this image on the canvas
        canvas.drawBitmap(this, 0f, 0f, null)

        // we make black paint with 85% opacity and draw over the canvas
        // https://stackoverflow.com/a/5372500/9698467
        val blackOverlay = Paint()
        blackOverlay.color = Color.BLACK
        blackOverlay.alpha = 217 // 85% opacity of range [0, 255]
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), blackOverlay)

        // we add a lock icon on top in the center:
        // https://stackoverflow.com/a/9912010/9698467
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        iconPaint.textSize = 48f
        iconPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(LOCK_ICON, width.toFloat() / 2, height.toFloat() / 2, iconPaint)

        return bitmap
    }
}
