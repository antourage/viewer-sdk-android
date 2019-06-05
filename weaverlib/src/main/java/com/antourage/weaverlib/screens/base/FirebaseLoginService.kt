package com.antourage.weaverlib.screens.base

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class FirebaseLoginService(context: Context) {
    private var auth: FirebaseAuth

    init {
        auth = FirebaseAuth.getInstance()
    }
    fun handleSignIn(){
        if (!isLoggedInToFirebase()){
            signInAnonymously()
        }
    }
    private fun isLoggedInToFirebase():Boolean{
        val currentUser = auth.currentUser
        return currentUser != null
    }
    private fun signInAnonymously() {
        auth.signInAnonymously()
            .addOnSuccessListener {
                Log.d(FirebaseLoginService::class.java.simpleName, "Firebase Login Successful")
                val currentUser = auth.currentUser
                Log.d("test","test")
            }
            .addOnFailureListener {
                Log.d(FirebaseLoginService::class.java.simpleName, "Firebase Login Failed")
            }
    }
}