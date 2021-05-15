package com.example.cameraapp.fragment

import android.app.AlertDialog
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.io.File
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewpager.widget.ViewPager
import com.blackbox.imageutils.models.ImageItem
import com.example.cameraapp.BuildConfig
import com.example.cameraapp.R
import com.example.cameraapp.adapters.ImageAdapter
import com.example.cameraapp.utils.ImageCrypter
import com.example.cameraapp.utils.padWithDisplayCutout
import com.example.cameraapp.utils.showImmersive
import io.reactivex.rxkotlin.subscribeBy
import java.util.Locale
import java.util.concurrent.TimeUnit

val EXTENSION_WHITELIST = arrayOf("JPG")

/** Fragment used to present the user with a gallery of photos taken */
class GalleryFragment internal constructor() : Fragment() {

    /** AndroidX navigation arguments */
    private val args: GalleryFragmentArgs by navArgs()
    // Get root directory of media from navigation arguments
    var rootDirectory:File? = null

//    private val listOfEncryptedFilePaths = arrayListOf<String>()
//    private val listOfDecryptedFilePaths = arrayListOf<String>()
//    private val listOfImages = arrayListOf<ImageItem>()

//    lateinit var progressDialog:ProgressBar
//    lateinit var btn_decrypt:Button
    lateinit var cutout_safe_area:ConstraintLayout
    lateinit var photo_view_pager:ViewPager
    lateinit var btn_suggest_if_no_image:Button
  //  lateinit var photo_view_pager:ViewPager
   // var mediaViewPager:ViewPager?= null
//    private lateinit var layoutManager: StaggeredGridLayoutManager
//    private lateinit var recycler_view:RecyclerView
//    private lateinit var adapter: ImageAdapter

    private lateinit var mediaList: MutableList<File>

    /** Adapter class used to present a fragment containing one photo or video as a page */
    inner class MediaPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = mediaList.size
        override fun getItem(position: Int): Fragment = PhotoFragment.create(mediaList[position])
        override fun getItemPosition(obj: Any): Int = POSITION_NONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true

        rootDirectory = File(args.rootDirectory)

        // Walk through all files in the root directory
        // We reverse the order of the list to present the last photos first
        mediaList = rootDirectory?.listFiles { file ->
            EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
        }?.sortedDescending()?.toMutableList() ?: mutableListOf()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_gallery, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

     //   progressDialog = view.findViewById(R.id.progressDialog)
     //   btn_decrypt = view.findViewById(R.id.btn_decrypt)
        cutout_safe_area = view.findViewById(R.id.cutout_safe_area)
        photo_view_pager = view.findViewById(R.id.photo_view_pager)
        btn_suggest_if_no_image = view.findViewById(R.id.btn_suggest_if_no_image)
    //    recycler_view = view.findViewById(R.id.recycler_view)
      //  photo_view_pager = view.findViewById(R.id.photo_view_pager)

      // encryptImages()
//        Log.d("gallery", listOfEncryptedFilePaths.toString())
//        Log.d("gallery", listOfImages.toString())

       // setUpListAdapter()

        //Checking media files list
        if (mediaList.isEmpty()) {
            view.findViewById<ImageButton>(R.id.delete_button).isEnabled = false
            view.findViewById<ImageButton>(R.id.share_button).isEnabled = false
            btn_suggest_if_no_image.visibility = View.VISIBLE

        }

       val mediaViewPager = photo_view_pager.apply {
            offscreenPageLimit = 2
            adapter = MediaPagerAdapter(childFragmentManager)
        }
//
//        btn_decrypt.setOnClickListener {
//            Log.d("gallery", listOfDecryptedFilePaths.toString())
//
//            if (listOfEncryptedFilePaths.isNotEmpty()) {
//                Log.d("gallery",listOfImages.toString())
//                showHideProgress(true)
//
//                decryptImages()
//
//            } else {
//                Toast.makeText(requireContext(), "There are no encrypted images!", Toast.LENGTH_SHORT).show()
//            }
//        }

        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            view.findViewById<ConstraintLayout>(R.id.cutout_safe_area).padWithDisplayCutout()
        }

        // Handle back button press
        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp()
        }

      //   Handle share button press
        view.findViewById<ImageButton>(R.id.share_button).setOnClickListener {

            mediaViewPager?.currentItem?.let { it1 ->
                mediaList.getOrNull(it1)?.let { mediaFile ->

                    // Create a sharing intent
                    val intent = Intent().apply {
                        // Infer media type from file extension
                        val mediaType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(mediaFile.extension)
                        // Get URI from our FileProvider implementation
                        val uri = FileProvider.getUriForFile(
                            view.context, BuildConfig.APPLICATION_ID + ".provider", mediaFile)
                        // Set the appropriate intent extra, type, action and flags
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = mediaType
                        action = Intent.ACTION_SEND
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }

                    // Launch the intent letting the user choose which app to share with
                    startActivity(Intent.createChooser(intent, getString(R.string.share_hint)))
                }
            }
        }

     //    Handle delete button press
        view.findViewById<ImageButton>(R.id.delete_button).setOnClickListener {

            mediaViewPager?.currentItem?.let { it1 ->
                mediaList.getOrNull(it1)?.let { mediaFile ->

                    androidx.appcompat.app.AlertDialog.Builder(view.context, android.R.style.Theme_Material_Dialog)
                        .setTitle(getString(R.string.delete_title))
                        .setMessage(getString(R.string.delete_dialog))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { _, _ ->

                            // Delete current photo
                            mediaFile.delete()

                            // Send relevant broadcast to notify other apps of deletion
                            MediaScannerConnection.scanFile(
                                view.context, arrayOf(mediaFile.absolutePath), null, null)

                            // Notify our view pager
                            mediaList.removeAt(mediaViewPager!!.currentItem)
                            mediaViewPager!!.adapter?.notifyDataSetChanged()

                            // If all photos have been deleted, return to camera
                            if (mediaList.isEmpty()) {
                                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp()
                            }

                        }

                        .setNegativeButton(android.R.string.no, null)
                        .create().showImmersive()
                }
            }
        }
    }

    /*
    * Setup RecyclerView list adapter.
    */
//    private fun setUpListAdapter() {
//        adapter = ImageAdapter(requireContext(),listOfImages)
//
//        layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
//        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
//
//        recycler_view.layoutManager = layoutManager
//        recycler_view.adapter = adapter
//    }

//    private fun encryptImages() {
//
//        showHideProgress(true)
//
//        val directory = File(rootDirectory?.toURI())
//        val listOfPaths = arrayListOf<String>()
//
//        if (directory.exists()) {
//
//            //Get list of files in storage directory
//            val list: Array<File> = directory.listFiles()
//
//            for (item in list) {
//                listOfPaths.add(item.path)
//            }
//
//            ImageCrypter.encryptImageList(listOfPaths)
//                .delay(1, TimeUnit.SECONDS)
//                .subscribeBy(
//                    onNext = {
//                        listOfEncryptedFilePaths.add(it.path)
//                    },
//                    onError = {
//                        it.printStackTrace()
//                        showHideProgress(false)
//                        Toast.makeText(requireContext(), "Something went wrong.", Toast.LENGTH_SHORT).show()
//                    },
//                    onComplete = {
//                        activity?.runOnUiThread {
//                            Toast.makeText(
//                                requireContext(),
//                                "All image files are encrypted!",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                            btn_decrypt.visibility = View.VISIBLE
//                            showHideProgress(false)
//                        }
//                    }
//                )
//        }
//    }

//    private fun decryptImages(){
//
//        ImageCrypter.decryptFiles(listOfEncryptedFilePaths)
//            .subscribeBy(
//                onNext = {
//                    it.forEach {
//                        listOfDecryptedFilePaths.add(it.path)
//                    }
//                },
//                onError = {
//                    it.printStackTrace()
//                    showHideProgress(false)
//                    Toast.makeText(requireContext(), "Something went wrong.", Toast.LENGTH_SHORT).show()
//                    btn_decrypt.visibility = View.VISIBLE
//                },
//                onComplete = {
//                    showHideProgress(false)
//                    btn_decrypt.visibility = View.GONE
//                    cutout_safe_area.visibility = View.VISIBLE
//
//                    //Get list of decrypted files
//                    for (item in listOfDecryptedFilePaths) {
//                        listOfImages.add(ImageItem(item))
//                    }
//
//                }
//            )
//    }
//
//    private fun showHideProgress(show: Boolean) {
//        if (show) {
//            progressDialog.visibility = View.VISIBLE
//        } else {
//            progressDialog.visibility = View.GONE
//        }
//    }


}
