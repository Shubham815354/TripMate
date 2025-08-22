package com.example.tripmate.MyAuthMVVM

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tripmate.MyAuthMVVM.UserRepository

class UserViewModelFactory(val repository: UserRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create (modelclass:Class<T>):T{
        if(modelclass.isAssignableFrom(UserViewModel::class.java)){
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Invalid View Model Class")
    }
}