package com.example.tripmate.myRoom

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface  UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add_in_room(user: UserDatabase)

    @Query("Select * From User_Fav Where title = :title Limit 1")
    suspend fun check_existing_data(title:String): UserDatabase?

    @Query("Select * From User_Fav")
    suspend fun display_data():List<UserDatabase>

    @Delete
    suspend fun delete_data(user: UserDatabase)
}