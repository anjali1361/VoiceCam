package com.example.cameraapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.File

class PrefConfig {

    private val MY_PREFERENCE_NAME = "com.example.cameraapp"
    private val SAVED_URI = "SAVED_URI"

     fun saveUriToPref(context: Context, uri: File){
        val pref = context.getSharedPreferences(MY_PREFERENCE_NAME,Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(SAVED_URI, uri.toString())
        editor.apply()
    }

    fun loadUriFromPref(context: Context): String? {
        val pref = context.getSharedPreferences(MY_PREFERENCE_NAME,Context.MODE_PRIVATE)
        return pref.getString(SAVED_URI,"")
    }

    fun removeUriFromPref(context: Context){
        val pref = context.getSharedPreferences(MY_PREFERENCE_NAME,Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.remove(SAVED_URI)
       // editor.remove("URI")//if want to remove multiple keys
       // editor.clear()//if want to remove all keys
        editor.apply()

    }


}