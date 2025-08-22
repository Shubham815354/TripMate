package com.example.tripmate.myRoomMvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tripmate.myRoomMvvm.UserViewModel
import com.example.tripmate.myRoom.UserDao

class UserViewModelFactory(val repository: UserRepository): ViewModelProvider.Factory {
    override fun <T: ViewModel> create (modelClass:Class<T>):T{
        if(modelClass.isAssignableFrom(UserViewModel::class.java)){
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}