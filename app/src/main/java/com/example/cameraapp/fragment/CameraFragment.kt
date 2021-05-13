package com.example.cameraapp.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.cameraapp.MainActivity
import com.example.cameraapp.R
import com.example.cameraapp.utils.PrefConfig
import com.example.cameraapp.utils.Request
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias LumaListener = (luma: Double) -> Unit

class CameraFragment:Fragment() {

    val prefConfig = PrefConfig()

    private lateinit var speech: SpeechRecognizer
    private lateinit var recognizerIntent: Intent

    lateinit var viewFinder:PreviewView
    lateinit var camera_switch_button: ImageButton
    lateinit var text: TextView
    lateinit var button: ImageButton
    private lateinit var gallery:ImageButton
    lateinit var constraint:ConstraintLayout

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var preview: Preview
    private lateinit var imageAnalyzer: ImageAnalysis
    lateinit var camera: Camera

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraProvider: ProcessCameraProvider

    var mic_tap = false
    private var displayId: Int = -1

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera,container,false)

        return view

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewFinder = view.findViewById(R.id.view_finder)
        camera_switch_button = view.findViewById(R.id.camera_switch_button)
        gallery = view.findViewById(R.id.gallery)
        text = view.findViewById(R.id.text)
        button = view.findViewById(R.id.button)
        constraint=view.findViewById(R.id.container)

        gallery.setOnClickListener{
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraToGallery(outputDirectory.toString()))
        }


        outputDirectory = MainActivity.getOutputDirectory(requireContext())

        context?.let { prefConfig.saveUriToPref(it,outputDirectory) }
        Log.d(Request.TAG, outputDirectory.toString())

        // Wait for the views to be properly laid out
        viewFinder.post {

            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            // Set up the camera and its use cases
            startCamera()
        }

        if(mic_tap){
            listenToSpeech()
            output()
        }
//
//        val uri = activity?.let { prefConfig.loadUriFromPref(it) }
//        if(uri != ""){
//            savedUri = Uri.parse(uri)
//            if (uri != null) {
//                setGalleryThumbnail(uri,photo_view_button)
//            }
//        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        camera_switch_button.setOnClickListener{
            switchCamera()
        }

        button.setOnClickListener{
            mic_tap = true
            if (mic_tap){
                listenToSpeech()
                output()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()

    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(),R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraToPermissions()
            )
        }
    }

    private fun setGalleryThumbnail(uri: String, view:View) {
        val thumbnail = view
        // Run the operations in the view's thread
        thumbnail.post {

            // Remove thumbnail padding
            thumbnail.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

            // Load thumbnail into circular button using Glide
            Glide.with(thumbnail)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .into(thumbnail as ImageView)
        }
    }


    private fun output() {

        if(text.text.toString() == "cheese" || text.text.toString()== "hello" || text.text.toString()== "hello hello" || text.text.toString()== "hello" || text.text.toString()== "hallo"){
            Log.d(Request.TAG,"Stop Listening To Speech")
            // speech.stopListening()
            takePhoto()
            text.setText(R.string.say_cheese)
            mic_tap=false

        }

    }

    private fun listenToSpeech() {
        speech = SpeechRecognizer.createSpeechRecognizer(requireContext())
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "US-en")
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        Log.i(Request.TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(requireContext()))
        speech.setRecognitionListener(object: RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Log.i(Request.TAG, "RedayForSpeech")
                text.setText("")
                text.setText(R.string.listening)
            }

            override fun onBeginningOfSpeech() {
                Log.i(Request.TAG, "BeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.i(Request.TAG, "onRmsChaged")
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.i(Request.TAG, "onBufferReceived")
            }

            override fun onEndOfSpeech() {
                Log.i(Request.TAG, "EndOfSpeech")
            }

            override fun onError(error: Int) {
                val errorMessage: String = getErrorText(error)
                Log.d(Request.TAG, "FAILED $errorMessage")
                text.setText(errorMessage)
                output()
                //   toggleButton.isChecked = false
            }

            override fun onResults(results: Bundle?) {
                Log.d(Request.TAG,"Inside onResult")
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                text.setText(data!![0])
                output()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.i(Request.TAG, "onPartialResult")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.i(Request.TAG, "onEvent")
            }

        })
        speech.startListening(recognizerIntent)
    }

    private fun switchCamera() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            Log.d(Request.TAG,"inside click listener --> lens facing back")
            CameraSelector.LENS_FACING_BACK
        } else {
            Log.d(Request.TAG,"inside click listener --> lens facing front")
            CameraSelector.LENS_FACING_FRONT
        }
        // Re-bind use cases to update selected camera
        bindCameraUseCases()
    }



    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(Request.TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(Request.TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .build()

        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                    // Values returned from our analyzer are passed to the attached listener
                    // We log image analysis results here - you should do something useful
                    // instead!

                    // Log.d(Request.TAG, "Average luminosity: $luma")

                })
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(Request.TAG, "Use case binding failed", exc)
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - Request.RATIO_4_3_VALUE) <= abs(previewRatio - Request.RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            updateCameraSwitchButton()

            // Build and bind the camera use cases
           bindCameraUseCases()

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {

        imageCapture?.let { imageCapture ->

            // Create output file to hold the image
            val photoFile = Request.createFile(outputDirectory, Request.FILENAME, Request.PHOTO_EXTENSION)

            Log.d(Request.TAG, photoFile.toString())//to delete

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {

                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

            Log.d(Request.TAG, outputOptions.toString())

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(Request.TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(Request.TAG, "Photo capture succeeded: $savedUri")


                        // We can only change the foreground Drawable using API level 23+ API
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Update the gallery thumbnail with latest picture taken
                            savedUri?.let {
//                                activity?.let {
//                                        it1 -> prefConfig.saveUriToPref(it1, savedUri!!) }
                              //  setGalleryThumbnail(it.toString(),photo_view_button)
                            }
                        }

                        // Implicit broadcasts will be ignored for devices running API level >= 24
                        // so if you only target API level 24+ you can remove this statement
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            requireActivity().sendBroadcast(
                                Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                            )
                        }

                        // If the folder selected is an external media directory, this is
                        // unnecessary but otherwise other apps will not be able to access our
                        // images unless we scan them using [MediaScannerConnection]
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(savedUri?.toFile()?.extension)
                        MediaScannerConnection.scanFile(
                            requireContext(),
                            arrayOf(savedUri?.toFile()?.absolutePath),
                            arrayOf(mimeType)
                        ) { _, uri ->
                            Log.d(Request.TAG, "Image capture scanned into media store: $uri")
                        }
                    }
                })

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Display flash animation to indicate that photo was captured
                constraint.postDelayed({
                    constraint.foreground = ColorDrawable(Color.WHITE)
                    constraint.postDelayed(
                        { constraint.foreground = null }, Request.ANIMATION_FAST_MILLIS)
                }, Request.ANIMATION_SLOW_MILLIS)
            }
        }
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private fun updateCameraSwitchButton() {
        val switchCamerasButton = camera_switch_button
        try {
            switchCamerasButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchCamerasButton.isEnabled = false
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        /**
         * Used to add listeners that will be called with each luma computed
         */
        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        /**
         * Analyzes an image to produce a result.
         *
         * <p>The caller is responsible for ensuring this analysis method can be executed quickly
         * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
         * images will not be acquired and analyzed.
         *
         * <p>The image passed to this method becomes invalid after this method returns. The caller
         * should not store external references to this image, as these references will become
         * invalid.
         *
         * @param image image being analyzed VERY IMPORTANT: Analyzer method implementation must
         * call image.close() on received images when finished using them. Otherwise, new images
         * may not be received or the camera may stall, depending on back pressure setting.
         *
         */
        override fun analyze(image: ImageProxy) {
            // If there are no listeners attached, we don't need to perform analysis
            if (listeners.isEmpty()) {
                image.close()
                return
            }

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            // Analysis could take an arbitrarily long amount of time
            // Since we are running in a different thread, it won't stall other use cases

            lastAnalyzedTimestamp = frameTimestamps.first

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

            // Call all listeners with new value
            listeners.forEach { it(luma) }

            image.close()
        }
    }

//    companion object {
//
//        private const val Request.TAG = "CameraXBasic"
//        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
//        private const val PHOTO_EXTENSION = ".jpg"
//        private const val RATIO_4_3_VALUE = 4.0 / 3.0
//        private const val RATIO_16_9_VALUE = 16.0 / 9.0
//
//        const val ANIMATION_FAST_MILLIS = 50L
//        const val ANIMATION_SLOW_MILLIS = 100L
//
//        private const val REQUEST_CODE_PERMISSIONS = 10
//        //        private const val REQUEST_CODE_PERMISSIONS_Audio = 20
//        private val REQUIRED_PERMISSIONS= arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.RECORD_AUDIO)
////        private val REQUIRED_PERMISSIONS_AUDIO = arrayOf(android.Manifest.permission.RECORD_AUDIO)
//
//        /** Helper function used to create a timestamped file */
//        private fun createFile(baseFolder: File, format: String, extension: String) =
//            File(baseFolder, SimpleDateFormat(format, Locale.US)
//                .format(System.currentTimeMillis()) + extension)
//    }

    private fun getErrorText(error: Int): String {
        var message = ""
        message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match, Try Again"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
        return message
    }

}