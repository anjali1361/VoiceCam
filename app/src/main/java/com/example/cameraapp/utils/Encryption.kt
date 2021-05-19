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

package com.example.cameraapp.utils

import android.annotation.TargetApi
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.HashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal class Encryption {

  fun encrypt(dataToEncrypt: ByteArray,
              password: CharArray): HashMap<String, ByteArray> {
    val map = HashMap<String, ByteArray>()

    try {
      // 1
      //Random salt for next step
      val random = SecureRandom()//cryptographically strong random generator, SecureRandom class make sure that the output is diificult to predict
      val salt = ByteArray(256)
      random.nextBytes(salt)

      // 2
      //PBKDF2 - derive the key from the password, don't use passwords directly
      val pbKeySpec = PBEKeySpec(password, salt, 1324, 256)// PBEKeySpec is a password based encryption object
      val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded//generating key as a ByteArray
      val keySpec = SecretKeySpec(keyBytes, "AES")//wrap the raw ByteArray into SecretKeySpec object

      // 3
      /**
       * AES works on different mode. The standard mode is called cipher block chaining(CBC), it encrypts the data one chunk at a time.
       * CBC is secure bec. each block of data in the pipeline is XOR'd with the previous block that is eencrypted. this dependency on the
       * previous blocks makes the encryption strong, but what about the 1st block?
       * if u encrypty a message that starts off the same as another message, the first encrypted block would be samw,
       * which gives clue for an attacker, that is why IV came to picture.
       * IV is a fancy term for a block of random data that gets XORd with that of first block.
       * Remember that each block relies on all blocks processed up until that point, this means the identical sets of data
       * encrypted with the same key will not produce identical outputs
       *
       */
      //Create initialization vector for AES
      val ivRandom = SecureRandom() //not caching previous seeded instance of SecureRandom
      val iv = ByteArray(16)
      ivRandom.nextBytes(iv)//created 16bytes of random data
      val ivSpec = IvParameterSpec(iv)//and packed it into IvParameterSpec object

      // 4
      //Encrypt
      val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")//"AES/CBC/PKCS7Padding" called specification string, it chooses AES with cipher block chainig mode, PKCS7Padding is well known padding standard
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
      val encrypted = cipher.doFinal(dataToEncrypt)//actual encryption done

      // 5
      /**
       * storing some these values is necessay for performing decryption, here we strong into an hashmap
       */
      map["salt"] = salt
      map["iv"] = iv
      map["encrypted"] = encrypted
    } catch (e: Exception) {
      Log.e("MYAPP", "encryption exception", e)
    }

    return map

  }

  fun decrypt(map: HashMap<String, ByteArray>, password: CharArray): ByteArray? {
    var decrypted: ByteArray? = null
    try {
      // 1
      val salt = map["salt"]
      val iv = map["iv"]
      val encrypted = map["encrypted"]

      // 2
      //regenerate key from password
      val pbKeySpec = PBEKeySpec(password, salt, 1324, 256)
      val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded
      val keySpec = SecretKeySpec(keyBytes, "AES")

      // 3
      //Decrypt
      val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
      val ivSpec = IvParameterSpec(iv)
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
      decrypted = cipher.doFinal(encrypted)
    } catch (e: Exception) {
      Log.e("MYAPP", "decryption exception", e)
    }

    return decrypted
  }

  fun keystoreEncrypt(dataToEncrypt: ByteArray): HashMap<String, ByteArray> {
    val map = HashMap<String, ByteArray>()
    try {

      // 1
      //Get the key
      val keyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)

      val secretKeyEntry =
          keyStore.getEntry("MyKeyAlias", null) as KeyStore.SecretKeyEntry
      val secretKey = secretKeyEntry.secretKey

      // 2
      //Encrypt data
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      val ivBytes = cipher.iv
      val encryptedBytes = cipher.doFinal(dataToEncrypt)

      // 3
      map["iv"] = ivBytes
      map["encrypted"] = encryptedBytes
    } catch (e: Throwable) {
      e.printStackTrace()
    }

    return map
  }

  fun keystoreDecrypt(map: HashMap<String, ByteArray>): ByteArray? {
    var decrypted: ByteArray? = null
    try {
      // 1
      //Get the key
      val keyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)

      val secretKeyEntry =
          keyStore.getEntry("MyKeyAlias", null) as KeyStore.SecretKeyEntry
      val secretKey = secretKeyEntry.secretKey

      // 2
      //Extract info from map
      val encryptedBytes = map["encrypted"]
      val ivBytes = map["iv"]

      // 3
      //Decrypt data
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val spec = GCMParameterSpec(128, ivBytes)
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
      decrypted = cipher.doFinal(encryptedBytes)
    } catch (e: Throwable) {
      e.printStackTrace()
    }

    return decrypted
  }

  @TargetApi(23)
  fun keystoreTest() {

    //TODO - Add Test code here
    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    val keyGenParameterSpec = KeyGenParameterSpec.Builder("MyKeyAlias",
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        //.setUserAuthenticationRequired(true) // 2 requires lock screen, invalidated if lock screen is disabled
        //.setUserAuthenticationValidityDurationSeconds(120) // 3 only available x seconds from password authentication. -1 requires finger print - every time
        .setRandomizedEncryptionRequired(true) // 4 different ciphertext for same plaintext on each call
        .build()
    keyGenerator.init(keyGenParameterSpec)
    keyGenerator.generateKey()

    val map = keystoreEncrypt("My very sensitive string!".toByteArray(Charsets.UTF_8))
    val decryptedBytes = keystoreDecrypt(map)
    decryptedBytes?.let {
      val decryptedString = String(it, Charsets.UTF_8)
      Log.e("MyApp", "The decrypted string is: $decryptedString")
    }
  }
}

