package com.example.tripmate.myMainMvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class UserViewModelFactory(val repository: UserRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create (modelclass:Class<T>):T{
        if(modelclass.isAssignableFrom(UserViewModel::class.java)){
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Invalid ViewMiodel Class")
    }
}