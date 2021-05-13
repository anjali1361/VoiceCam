package com.example.cameraapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cameraapp.fragment.CameraFragment
import com.example.cameraapp.fragment.GalleryFragment
import com.example.cameraapp.utils.FLAGS_FULLSCREEN
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val IMMERSIVE_FLAG_TIMEOUT = 500L

class MainActivity : AppCompatActivity()
    //, BottomNavigationView.OnNavigationItemSelectedListener {
{
    private lateinit var container: FrameLayout
//    val manager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.fragment_container)

    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RecordAudioRequestCode && grantResults.size > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(
                this,
                "Permission Granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

//    private fun showFragmentCamera() {
//
//        val transaction = manager.beginTransaction()
//        transaction.replace(R.id.fragment_holder,CameraFragment())
//        transaction.commit()
//
//    }


//    override fun onNavigationItemSelected(item: MenuItem): Boolean {
//
//        var selectedFragment: Fragment? = null
//        when(item.itemId){
//            R.id.camera -> selectedFragment= CameraFragment()
//            R.id.photo_view_button -> selectedFragment = GalleryFragment()
//
//        }
//
//        val transaction = manager.beginTransaction()
//        val fragment = selectedFragment
//        if (fragment != null) {
//            transaction.replace(R.id.fragment_holder,fragment)
//        }
//        transaction.commit()
//
//
//        return true
//
//    }

    companion object {
        private const val TAG = "VoiceCam"
        const val RecordAudioRequestCode = 1

        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

}