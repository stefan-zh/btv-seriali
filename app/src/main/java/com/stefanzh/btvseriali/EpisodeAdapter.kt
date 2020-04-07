package com.stefanzh.btvseriali


import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
        val imageToLeft = BitmapDrawable(row.episodeRow.resources, episode.image)
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
}
