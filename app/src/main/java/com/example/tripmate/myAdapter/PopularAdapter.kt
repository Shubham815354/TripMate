package com.example.tripmate.myAdapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tripmate.R
import com.example.tripmate.databinding.ItemPopularBinding
import com.example.tripmate.myModel.Details
import com.example.tripmate.myRoom.UserDatabase

class PopularAdapter: ListAdapter<Details, PopularAdapter.MyHolder>(DiffUtillGetback()) {
    private var onItemClickListener :((Details) ->Unit)?=null
    fun onSetclickListner(listner : (Details) ->Unit){
        onItemClickListener = listner
    }
    private var onchoose : ((UserDatabase) -> Unit)?=null
    private var fav_list : List<UserDatabase> = emptyList()
    fun setOnchoose(listner : (UserDatabase) -> Unit){
        onchoose = listner
    }
    fun setOnFav(list : List<UserDatabase>){
        fav_list = list
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyHolder {
        val binding : ItemPopularBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context),R.layout.item_popular,parent,false)
        return MyHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MyHolder,
        position: Int
    ) {
        holder.onbind(getItem(position))
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(getItem(position))
        }
    }

    inner class MyHolder(val binding : ItemPopularBinding): RecyclerView.ViewHolder(binding.root){
        fun onbind(items: Details){
            var image = items.thumbnail?.source?:""
            if(image!=null && image.isNotEmpty()){
                Glide.with(itemView)
                    .load(image)
                    . error(R.drawable.ic_launcher_background)
                    .into(binding.imageDestination)
            }else{
                binding.imageDestination.setImageResource(R.drawable.img)
            }

            binding.textCityName.text = items.title.toString()?:"No Image Found"
            val des = items.description.toString()?:"No Description found"
            val trimmed = if(des.length>25){
                des.substring(0,25)+"...."
            }else{
                des
            }
            binding.textDestinationName.text = trimmed
            // In your adapter's onbind() method
            val isfav = fav_list.any{it.title == items.title}

            if (isfav) {
                // Keep circular background, just tint it red
                binding.btnFavorite.setBackgroundResource(R.drawable.heart_button_background)
                binding.btnFavorite.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                )
            } else {
                // Keep circular background, tint it gray
                binding.btnFavorite.setBackgroundResource(R.drawable.heart_button_background)
                binding.btnFavorite.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                )
            }

            binding.btnFavorite.setOnClickListener {
                val thumbnailSource = items.thumbnail?.source ?: ""
                val fav = UserDatabase(
                    title = items.title,
                    thumbnail = thumbnailSource,
                    description = items.description.toString()
                )
                onchoose?.invoke(fav)
            }
        }
    }
}
class DiffUtillGetback: DiffUtil.ItemCallback<Details>(){
    override fun areItemsTheSame(
        oldItem: Details,
        newItem: Details
    ) = oldItem == newItem

    override fun areContentsTheSame(
        oldItem: Details,
        newItem: Details
    ) = oldItem == newItem

}