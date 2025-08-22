package com.example.tripmate.MyAuthMVVM

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class UserRepository {
    private var auth = FirebaseAuth.getInstance()
    private var database = FirebaseDatabase.getInstance().reference.child("User")

    suspend fun login(email:String , password:String): Result<Boolean>{
          try{
              val result = auth.signInWithEmailAndPassword(email,password).await()
              val verified = result.user?.isEmailVerified == true
              return Result.success(true)

          } catch(e: Exception){
              return Result.failure(e)
          }
    }

    suspend fun signup(firstname:String,lastname:String , email:String , password:String):Result<Boolean>{
        try{
            val result = auth.createUserWithEmailAndPassword(email,password).await()
            val user = result.user?: throw Exception("User creation Failed")
            user.sendEmailVerification().await()
            val uid = user.uid
            val usermap = mapOf(
                "firstname" to firstname,
                "lastname" to lastname,
                "email" to email
            )
            database.child(uid).setValue(usermap).await()
            return Result.success(true)
        }
        catch (e: Exception){
            return Result.failure(e)
        }
    }

    suspend fun resetpass(email:String): Result<Boolean>{
        try{
            val result = auth.sendPasswordResetEmail(email).await()
            return Result.success(true)
        }catch(e: Exception){
            return Result.failure(e)
        }
    }

    suspend fun fetchUserName(): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val snapshot = database.child(uid).get().await()
            val firstName = snapshot.child("firstname").getValue(String::class.java) ?: ""
            val lastName = snapshot.child("lastname").getValue(String::class.java) ?: ""
            val fullName = "$firstName $lastName".trim()
            Result.success(fullName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Boolean> {
        return try {
            auth.signOut()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




}