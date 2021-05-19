package com.example.cameraapp.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.File

@Parcelize
data class row(var imagePath:File,var isencrypted: Boolean = false):Parcelable
