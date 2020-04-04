package com.stefanzh.btvseriali


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_tv_show.view.*

interface OnSerialLinkClickListener {
    fun onSerialLinkClick(tvShow: SerialLink)
}

/**
 * [RecyclerView.Adapter] that can display a [SerialLink] and makes a call to the
 * specified [OnSerialLinkClickListener].
 */
class TvShowAdapter(
    private val tvShows: List<SerialLink>,
    private val clickListener: OnSerialLinkClickListener
) : RecyclerView.Adapter<TvShowAdapter.TvShowRow>() {

    /**
     * Creates each [TvShowRow].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TvShowRow {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_tv_show, parent, false)
        return TvShowRow(view)
    }

    /**
     * Places the Tv show row into the view
     */
    override fun onBindViewHolder(row: TvShowRow, position: Int) {
        val tvShow = tvShows[position]
        row.tvShowIcon.tv_show_icon.setImageBitmap(tvShow.image)
        row.tvShowLink.text = tvShow.link

        row.rowView.setOnClickListener {
            clickListener.onSerialLinkClick(tvShow)
        }
    }

    override fun getItemCount(): Int = tvShows.size

    // Helper class that represents each TV show row in the list
    inner class TvShowRow(val rowView: View) : RecyclerView.ViewHolder(rowView) {
        val tvShowIcon: ImageView = rowView.tv_show_icon
        val tvShowLink: TextView = rowView.tv_show_link

        override fun toString(): String {
            return super.toString() + " '" + tvShowLink.text + "'"
        }
    }
}
