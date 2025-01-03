package org.sabda.family.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.sabda.family.model.FamilyData
import org.sabda.family.R

class FamilyAdapter (private var familyData: List<FamilyData>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<FamilyAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.family_recycler, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val familyData = familyData[position]
        holder.bind(familyData)
    }

    override fun getItemCount(): Int = familyData.size

    fun updateData(newFamilylist: List<FamilyData>) {
        familyData = newFamilylist
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(familyDatas: FamilyData) {
            itemView.findViewById<TextView>(R.id.titleTextView).text = familyDatas.title

            itemView.setOnClickListener { onClick(familyDatas.id) }
        }
    }
}