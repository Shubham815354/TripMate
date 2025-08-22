package com.example.tripmate.myAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tripmate.R
import com.example.tripmate.myActivity.OnboardingItem

class ViewPagerAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder>() {

    inner class ViewPagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageSlide)
        val titleView: TextView = itemView.findViewById(R.id.slideTitle)
        val descView: TextView = itemView.findViewById(R.id.slideDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_slide, parent, false)
        return ViewPagerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageResource(item.imageRes)
        holder.titleView.text = item.title
        holder.descView.text = item.description
    }

    override fun getItemCount(): Int = items.size
}
