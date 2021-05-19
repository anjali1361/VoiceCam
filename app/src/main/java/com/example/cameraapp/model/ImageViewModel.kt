/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.example.cameraapp.model

import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.cameraapp.utils.Encryption
import com.example.cameraapp.utils.FileUtils
import org.simpleframework.xml.core.Persister
import java.io.*
import java.text.DateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream

class ImageViewModel() : ViewModel() {


  val TEMP_IMAGE_TAG = "temp_"

  private var image: row? = null

  fun getImageEncrypted(file: File, password: CharArray,view:ImageView) {
    loadImageEncrypted(file.toString(), password)

  }

  fun getImageDecypted(file: File, password: CharArray) : row? {
    if (image == null) {
      loadImageDecrypted(file, password)
    }

    return image
  }

  private fun loadImageEncrypted(file: String, password: CharArray){

    val encryptedImagePath = file.let { createCopyOfOriginalFile(it) }

    val fis = FileInputStream(file)
    val fs = FileOutputStream(File(encryptedImagePath))
    val map = Encryption().encrypt(fis.readBytes(),password)
    fs.write(map["encrypted"])
    fs.close()

    //Delete original file
    if (file != null) {
      deleteFile(file)
    }

    //Rename encrypted image file to original name
    val createdFile = encryptedImagePath.let { renameImageToOriginalFileName(it) }
    val newFile = File(createdFile)
    val result = newFile.createNewFile()
    if(result) Log.d("ImageViewModel","New file created")


  }
  private fun renameImageToOriginalFileName(path: String): String {
    val filePath = getImageParentPath(path)
    val imageName = getImageNameFromPath(path)

    val from = File(filePath, imageName)

    val renameTo = imageName!!.replace(TEMP_IMAGE_TAG, "")

    val to = File(filePath, renameTo)
    if (from.exists())
      from.renameTo(to)

    return to.path
  }

  private fun deleteFile(path: String) {
    val file = File(path)

    if (file.exists())
      file.delete()
  }

  private fun createCopyOfOriginalFile(originalFilePath: String): String {

    val filePath = getImageParentPath(originalFilePath)
    val imageName = getImageNameFromPath(originalFilePath)

    val originalFile = File(originalFilePath)
    val copyFile = File(filePath, "$TEMP_IMAGE_TAG$imageName")

    //Create a copy of original file
    try {
      FileUtils.copy(originalFile, copyFile)
    } catch (ex: IOException) {
      ex.printStackTrace()
    }

    return copyFile.path
  }

  private fun getImageParentPath(path: String?): String? {
    var newPath = ""
    path?.let {
      newPath = it.substring(0, it.lastIndexOf("/") + 1)
    }
    return newPath
  }

  private fun getImageNameFromPath(path: String?): String? {
    var newPath = ""
    path?.let {
      newPath = it.substring(it.lastIndexOf("/") + 1)
    }
    return newPath
  }



  private fun loadImageDecrypted(file: File, password: CharArray){

    var decrypted: ByteArray? = null
    ObjectInputStream(FileInputStream(file)).use { it ->
      val data = it.readObject()
      it.close()

      when(data) {
        is Map<*, *> -> {

          if (data.containsKey("iv") && data.containsKey("salt") && data.containsKey("encrypted")) {
            val iv = data["iv"]
            val salt = data["salt"]
            val encrypted = data["encrypted"]
            if (iv is ByteArray && salt is ByteArray && encrypted is ByteArray) {
              decrypted = Encryption().decrypt(
                  hashMapOf("iv" to iv, "salt" to salt, "encrypted" to encrypted), password)
            }
          }
        }
      }
    }

    if (decrypted != null) {
      val serializer = Persister()
      val inputStream = ByteArrayInputStream(decrypted)
      image = try { serializer.read(row::class.java, inputStream) } catch (e: Exception) {null}
//      pets?.list?.let {
//        this.pets = ArrayList(it)
//      }
    }
  }
}