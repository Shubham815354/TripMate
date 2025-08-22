package com.example.tripmate.myRoom

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserDatabase::class] , version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun user_dao(): UserDao

    companion object{
        @Volatile
        private var INSTANCE : AppDatabase? = null
        fun get_instance(context:Context): AppDatabase{
             return INSTANCE ?:synchronized(this){
                 val instance = Room.databaseBuilder(context.applicationContext,
                     AppDatabase::class.java,"user_db").build()
                 INSTANCE = instance
                 instance
             }
        }
    }
}