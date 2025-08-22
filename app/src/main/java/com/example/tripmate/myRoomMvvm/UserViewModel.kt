package com.example.tripmate.myRoomMvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.myRoom.UserDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserViewModel(val repository: UserRepository): ViewModel() {
    private val _fav = MutableLiveData<List<UserDatabase>>()
    val fav_ : LiveData<List<UserDatabase>> = _fav

    fun check_existing(user: UserDatabase){
        viewModelScope.launch {
            try{
                val response = withContext(Dispatchers.IO){
                    repository.check_exisiting_data(user)
                }
                load_data()
            }catch(e:Exception){
                Log.e("Error Exisiting ", e.localizedMessage)
            }
        }
    }

    fun load_data(){
        viewModelScope.launch {
            try{
                val response = withContext(Dispatchers.IO){
                    repository.display_all()
                }
                _fav.postValue(response)
            }catch(e:Exception){
                Log.e("Error Loading room ", e.localizedMessage)
            }
        }
    }


}