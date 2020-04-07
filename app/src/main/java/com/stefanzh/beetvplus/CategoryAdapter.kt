package com.stefanzh.beetvplus


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_category.view.*

interface OnCategoryClickListener {
    fun onCategoryClick(category: Category)
}

/**
 * [RecyclerView.Adapter] that can display a [Category] and makes a call to the
 * specified [OnCategoryClickListener].
 */
class CategoryAdapter(
    private val categories: List<Category>,
    private val clickListener: OnCategoryClickListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryRow>() {

    /**
     * Creates each [CategoryRow].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryRow {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_category, parent, false)
        return CategoryRow(view)
    }

    /**
     * Places the Category row into the view.
     */
    override fun onBindViewHolder(row: CategoryRow, position: Int) {
        val category = categories[position]
        row.categoryRow.text = category.title

        row.rowView.setOnClickListener {
            clickListener.onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size

    // Helper class that represents each Category row in the list
    inner class CategoryRow(val rowView: View) : RecyclerView.ViewHolder(rowView) {
        val categoryRow: TextView = rowView.category_row

        override fun toString(): String {
            return super.toString() + " '" + categoryRow.text + "'"
        }
    }
}
