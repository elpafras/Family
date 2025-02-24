package org.sabda.family.utility

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        outRect.left = if (position == 0) spacing else spacing / 2
        outRect.right = if (position == parent.adapter?.itemCount?.minus(1)) spacing else spacing / 2
    }
}