package org.sabda.family.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.sabda.family.model.FamilyData
import org.sabda.family.R

class FamilyPagerAdapter(private var familyData: List<FamilyData>, private val onClick: (String) -> Unit ) : RecyclerView.Adapter<FamilyPagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.family_recycler, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val familyData = familyData[position]
        holder.bind(familyData)
    }

    override fun getItemCount(): Int = familyData.size

    fun updateData(newFamilylist: List<FamilyData>) { familyData = newFamilylist }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        fun bind(familyDatas: FamilyData) {
            val titleTextView = itemView.findViewById<TextView>(R.id.titleTextView)
            titleTextView.text = trimText(familyDatas.title, 45, 70)

            val shortTextView = itemView.findViewById<TextView>(R.id.shortTextView)
            shortTextView.text = trimText(familyDatas.short, 40, 65)

            val seriesTextView = itemView.findViewById<TextView>(R.id.seriesTextView)
            seriesTextView.text = formatSeries(familyDatas.series)

            itemView.setOnClickListener { onClick(familyDatas.id) }
        }

        private fun trimText(text: String, maxLengthPhone: Int, maxLengthTablet: Int): String {
            val maxLength = if (isTablet()) maxLengthTablet else maxLengthPhone

            if (text.length <= maxLength) return text

            val trimmed = text.substring(0, maxLength)
            val lastSpaceIndex = trimmed.lastIndexOf(' ')

            return if (lastSpaceIndex != -1) "${trimmed.substring(0, lastSpaceIndex)}..." else "$trimmed..."
        }

        private fun isTablet(): Boolean {
            val screenWidthDp = itemView.resources.configuration.screenWidthDp
            return screenWidthDp >= 600 // Umumnya tablet memiliki lebar 600dp atau lebih
        }

        private fun formatSeries(series: List<String>?): String {
            return series?.joinToString(", ") ?: "No series available"
        }
    }
}