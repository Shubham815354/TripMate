package com.example.tripmate.myRoom

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "User_Fav")
data class UserDatabase( @PrimaryKey (autoGenerate = true) val id:Int=0,
    val title:String? , val thumbnail:String,
                        val description:String)
