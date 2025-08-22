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
import com.example.tripmate.databinding.ItemFavBinding
import com.example.tripmate.myRoom.UserDatabase

class FavAdapter: ListAdapter<UserDatabase, FavAdapter.MyHolder>(DiffUtilGetback()){
    private var on_remove : ((UserDatabase) -> Unit)?=null
    private var fav_list : List<UserDatabase> = emptyList()

    fun setomRemove(listner : (UserDatabase) ->Unit){
        on_remove = listner
    }

    fun setOnFav(list:List<UserDatabase>){
        fav_list = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyHolder {
        val binding : ItemFavBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context),R.layout.item_fav,parent,false)
        return MyHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MyHolder,
        position: Int
    ) {
        holder.onbind(getItem(position))
    }

    inner class MyHolder(val binding : ItemFavBinding): RecyclerView.ViewHolder(binding.root){
        fun onbind(item: UserDatabase){
            binding.textCityName.text = item.title.toString()
            binding.textDestinationName.text = item.description.toString()
            val image = item.thumbnail
            if(image!=null && image.isNotEmpty()){
                Glide.with(itemView.context)
                    .load(image)
                    .error(R.drawable.ic_launcher_background)
                    .into(binding.imageDestination)
            }else{
                binding.imageDestination.setImageResource(R.drawable.img)
            }

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
                val fav = UserDatabase(title = item.title, thumbnail = item.thumbnail , description = item.description)
                on_remove?.invoke(fav)
            }

        }
    }
}
class DiffUtilGetback: DiffUtil.ItemCallback<UserDatabase>(){
    override fun areItemsTheSame(
        oldItem: UserDatabase,
        newItem: UserDatabase
    ) = oldItem == newItem

    override fun areContentsTheSame(
        oldItem: UserDatabase,
        newItem: UserDatabase
    ) = oldItem == newItem

}