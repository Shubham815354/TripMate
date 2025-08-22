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
import com.example.tripmate.databinding.ItemRecyBinding
import com.example.tripmate.myModel.Details
import com.example.tripmate.myRoom.UserDatabase

class RecyAdapter:ListAdapter<Details, RecyAdapter.MyHolder>(DiffUtilCalback()){
    private var onItemClickListener :((Details) -> Unit)?=null
    fun onSetclickListner(listner : (Details) -> Unit){
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
        val binding : ItemRecyBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context),R.layout.item_recy,parent,false)
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

    inner class MyHolder(val binding: ItemRecyBinding): RecyclerView.ViewHolder(binding.root){
        fun onbind(item:Details){
            var image = item.thumbnail?.source?:""
            if(image !=null && image.isNotEmpty()){
                Glide.with(itemView)
                    .load(image)
                    .error(R.drawable.ic_launcher_background)
                    .into(binding.imageDestination)
            }else{
                binding.imageDestination.setImageResource(R.drawable.img)
            }


            binding.textCityName.text = item.title.toString()?:"No Name Found"
            val des=item.description?:"No Description found"
            val trim = if(des.length>25){
                des.substring(0,25)+"...."
            }else{
                des
            }
            binding.textDestinationName.text = trim

            // In your adapter's onbind() method
            val isfav = fav_list.any{it.title == item.title}

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
                val thumbnailSource = item.thumbnail?.source ?: ""
                val fav = UserDatabase(
                    title = item.title,
                    thumbnail = thumbnailSource,
                    description = item.description.toString()
                )
                onchoose?.invoke(fav)
            }
        }
    }
}
class DiffUtilCalback : DiffUtil.ItemCallback<Details>(){
    override fun areItemsTheSame(
        oldItem: Details,
        newItem: Details
    )= oldItem == newItem

    override fun areContentsTheSame(
        oldItem: Details,
        newItem: Details
    )= oldItem == newItem

}