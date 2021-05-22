package com.example.cameraapp.utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Request {

    const val TAG = "CameraXBasic"
    const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
    const val PHOTO_EXTENSION = ".jpg"
    const val RATIO_4_3_VALUE = 4.0 / 3.0
    const val RATIO_16_9_VALUE = 16.0 / 9.0

    const val ANIMATION_FAST_MILLIS = 50L
    const val ANIMATION_SLOW_MILLIS = 100L

//    const val REQUEST_CODE_PERMISSIONS = 10
//    val REQUIRED_PERMISSIONS= arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.RECORD_AUDIO)

    /** Helper function used to create a timestamped file */
    fun createFile(baseFolder: File, format: String, extension: String) =
        File(baseFolder, SimpleDateFormat(format, Locale.US)
            .format(System.currentTimeMillis()) + extension)
}