package com.example.tripmate.MyAuthMVVM

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UserViewModel(val repository: UserRepository): ViewModel() {

    val _user = MutableLiveData<Result<Boolean>>()
    val user : LiveData<Result<Boolean>> = _user

    fun hitlogin(email:String , password :String){
        viewModelScope.launch {
            try{
                val reponse = repository.login(email,password)
                _user.postValue(reponse)
            }catch (e: Exception){
                Log.e("Error",e.localizedMessage)
            }

        }
    }

    fun hitSignup(firstname:String , lastname:String , email:String , password:String){
        viewModelScope.launch {
            try{
                val response = repository.signup(firstname,lastname,email,password)
                _user.postValue(response)
            }catch(e: Exception){
                Log.e("Error",e.localizedMessage)
            }
        }
    }

    fun hitreset(email:String){
        viewModelScope.launch {
            try{
                val response = repository.resetpass(email)
                _user.postValue(response)
            }
            catch(e: Exception){
                Log.e("Error",e.localizedMessage)
            }
        }
    }

    val _userName = MutableLiveData<Result<String>>()
    val userName: LiveData<Result<String>> = _userName

    fun getUserName() {
        viewModelScope.launch {
            try {
                val response = repository.fetchUserName()
                _userName.postValue(response)
            } catch (e: Exception) {
                _userName.postValue(Result.failure(e))
            }
        }
    }

    val _logout = MutableLiveData<Result<Boolean>>()
    val logout: LiveData<Result<Boolean>> = _logout

    fun hitLogout() {
        viewModelScope.launch {
            try {
                val response = repository.logout()
                _logout.postValue(response)
            } catch (e: Exception) {
                _logout.postValue(Result.failure(e))
            }
        }
    }



}