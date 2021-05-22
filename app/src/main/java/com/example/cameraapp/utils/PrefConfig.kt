package com.example.cameraapp.utils

import android.content.Context

class PrefConfig {

    private val MY_PREFERENCE_NAME = "com.example.cameraapp"
    private val SAVED_URI = "SECURITY_CODE"

     fun saveCodeToPref(context: Context, code:String){
        val pref = context.getSharedPreferences(MY_PREFERENCE_NAME,Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(SAVED_URI, code)
        editor.apply()
    }

    fun loadCodeFromPref(context: Context): String? {
        val pref = context.getSharedPreferences(MY_PREFERENCE_NAME,Context.MODE_PRIVATE)
        return pref.getString(SAVED_URI,"")
    }

    fun removeCodeFromPref(context: Context){
        val pref = context.getSharedPreferences(MY_PREFERENCE_NAME,Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.remove(SAVED_URI)
       // editor.remove("URI")//if want to remove multiple keys
       // editor.clear()//if want to remove all keys
        editor.apply()

    }


}