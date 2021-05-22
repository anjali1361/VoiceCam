package com.example.cameraapp.model

import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import com.example.cameraapp.utils.Encryption
import com.example.cameraapp.utils.FileUtils
import org.simpleframework.xml.core.Persister
import java.io.*

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