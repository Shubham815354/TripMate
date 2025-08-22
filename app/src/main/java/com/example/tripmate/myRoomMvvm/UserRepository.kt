package com.example.tripmate.myRoomMvvm

import com.example.tripmate.myRoom.UserDao
import com.example.tripmate.myRoom.UserDatabase

class UserRepository(val dao: UserDao) {

    suspend fun check_exisiting_data(user: UserDatabase){
        val exist = dao.check_existing_data(user.title.toString())
        if(exist == null){
            dao.add_in_room(user)
        }else{
            dao.delete_data(exist)
        }
    }

    suspend fun display_all():List<UserDatabase>{
        return dao.display_data()
    }
}