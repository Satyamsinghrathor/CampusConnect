package com.example.campusconnect.screens.profilescreen

import androidx.lifecycle.ViewModel
import com.example.campusconnect.auth.firebase.customauth.Login

class LogoutViewModel: ViewModel() {

    private val logout = Login()

    fun logout(){
        logout.logout()
    }
}