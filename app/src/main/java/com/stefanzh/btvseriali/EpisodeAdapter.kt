package com.stefanzh.btvseriali


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
     * Places the Tv show row into the view
     */
    override fun onBindViewHolder(row: EpisodeRow, position: Int) {
        val episode = episodes[position]
        row.episodeIcon.episode_icon.setImageBitmap(episode.image)
        // construct the episode name from the string template
        val episodeName = row.episodeText.resources.getString(R.string.episode_name, episode.name, episode.length)
        row.episodeText.text = episodeName

        row.rowView.setOnClickListener {
            clickListener.onEpizodClick(episode)
        }
    }

    override fun getItemCount(): Int = episodes.size

    // Helper class that represents each episode row in the list
    inner class EpisodeRow(val rowView: View) : RecyclerView.ViewHolder(rowView) {
        val episodeIcon: ImageView = rowView.episode_icon
        val episodeText: TextView = rowView.episode_text

        override fun toString(): String {
            return super.toString() + " '" + episodeText.text + "'"
        }
    }
}
