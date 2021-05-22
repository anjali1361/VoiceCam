package com.example.cameraapp.fragment

import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.cameraapp.BuildConfig
import com.example.cameraapp.R
import com.example.cameraapp.model.ImageViewModel
import com.example.cameraapp.model.row
import com.example.cameraapp.utils.OtpEditText
import com.example.cameraapp.utils.PrefConfig
import com.example.cameraapp.utils.showImmersive
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File


/** Fragment used for each individual page showing a photo inside of [GalleryFragment] */
class PhotoFragment internal constructor() : Fragment() {

    private val args: PhotoFragmentArgs by navArgs()
    private lateinit var row: row
    lateinit var file:File
    var isEncrypted:Boolean = false
    lateinit var image:ImageView
    lateinit var back:ImageView
    lateinit var bottom_nav:BottomNavigationView
    var bottom_nav_decrypt=false

    val prefConfig = PrefConfig()

    private val viewModel: ImageViewModel by lazy {
        ViewModelProviders.of(this).get(ImageViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       // row = args.row
        row= args.row
        file = row.imagePath
        isEncrypted = row.isencrypted

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_photo, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        image = view.findViewById(R.id.image)
        back = view.findViewById(R.id.back)
        bottom_nav = view.findViewById(R.id.bottom_nav)

        bottom_nav.menu.getItem(0).setCheckable(false)//to not to select share icon by default in bottom nav


        if(isEncrypted){
            changeBottomNav(true)
        }

        bottom_nav.setOnNavigationItemSelectedListener {

            when(it.itemId){
                R.id.delete -> {
                   it.setCheckable(true)
                    deleteSelectedImage()
                }
                R.id.share -> {
                    it.setCheckable(true)
                    shareSelectedImage()
                }
                R.id.encrypt ->{
                    it.setCheckable(true)
                    setUpSecurityCodeAndEncrypt()
                }
                R.id.decrypt ->{
                    it.setCheckable(true)
                    setUpSecurityCodeAndEncrypt()
                }
            }
           return@setOnNavigationItemSelectedListener true
        }

        back.setOnClickListener{
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                PhotoFragmentDirections.actionPhotoToCamera())

        }

        Glide.with(requireActivity())
            .load(file)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(image)

    }

    private fun setUpSecurityCodeAndEncrypt() {

        val builder =  AlertDialog.Builder(requireContext())
        val inflator = requireActivity().layoutInflater
        val view =  inflator.inflate(R.layout.code_creation,null)
        val enterCode = view.findViewById<OtpEditText>(R.id.enterCode)
//        val cancel2 = view.findViewById<Button>(R.id.cancel2)
//        val ok2 = view.findViewById<Button>(R.id.ok2)

        builder.setView(view)
        builder.setCancelable(false)
        val dialog = builder.create()
//        ok2.setOnClickListener{
//
//            val code = enterCode.text.toString()
//            if(code.equals("")){
//                Toast.makeText(requireContext(),"Please set the security code first",Toast.LENGTH_SHORT).show()
//                dialog.dismiss()
//                changeBottomNav(false)
////                bottom_nav.menu.getItem(2).setCheckable(false)
//            }else{
//                //saveSecurityCodeToSharedPref(code)
//                encryptImage(code.toCharArray())
//                dialog.dismiss()
//                changeBottomNav(true)
//            }
//
//        }
//
//        cancel2.setOnClickListener{
//            dialog.dismiss()
//            changeBottomNav(false)
//        }
        dialog.showImmersive()
    }

    private fun saveSecurityCodeToSharedPref(code:String) {
        file.let { prefConfig.saveCodeToPref(requireContext(), code) }
    }

    private fun encryptImage(code: CharArray) {
        bottom_nav_decrypt = true
        isEncrypted = true
        viewModel.getImageEncrypted(file,code,image)
        Glide.with(requireActivity())
            .load(file)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(image)
             changeBottomNav(true)

        changeBottomNav(true)
    }

    private fun changeBottomNav(b: Boolean) {
         if(b){
             bottom_nav.menu.removeGroup(Menu.NONE)
             bottom_nav.menu.add(Menu.NONE,2,Menu.NONE,R.string.decrypt).setIcon(R.drawable.ic_decrypt)
             bottom_nav.menu.getItem(0).setCheckable(false)
         }
        else{
            bottom_nav.menu.get(2).setCheckable(false)
         }
    }

//    private fun decryptImage(code: CharArray) {
//        val r: row? = file.let { viewModel.getImageDecypted(it,code) }
//        Glide.with(requireActivity())
//            .load(r?.imagePath)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(image)
//    }

    private fun shareSelectedImage() {
            // Create a sharing intent
            val intent = Intent().apply {
                // Infer media type from file extension
                val mediaType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(file.extension)
                // Get URI from our FileProvider implementation
                val uri = file.let {

                        FileProvider.getUriForFile(
                            requireContext(), BuildConfig.APPLICATION_ID + ".provider", it
                        )
                }
                // Set the appropriate intent extra, type, action and flags
                putExtra(Intent.EXTRA_STREAM, uri)
                type = mediaType
                action = Intent.ACTION_SEND
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Launch the intent letting the user choose which app to share with
            startActivity(Intent.createChooser(intent, getString(R.string.share_hint)))
           bottom_nav.menu.getItem(0).setCheckable(false)


    }

    private fun deleteSelectedImage() {

          val builder =  AlertDialog.Builder(requireContext())
          val inflator = requireActivity().layoutInflater
          val view =  inflator.inflate(R.layout.delete_dialog,null)
          val cancel = view.findViewById<Button>(R.id.cancel)
          val ok = view.findViewById<Button>(R.id.ok)

          builder.setView(view)
          builder.setCancelable(false)
          val dialog = builder.create()
          ok.setOnClickListener{

                     //Delete current photo
                   file.delete()

                    // Send relevant broadcast to notify other apps of deletion
                    MediaScannerConnection.scanFile(
                        view?.context, arrayOf(file.absolutePath), null, null)

                    Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                        PhotoFragmentDirections.actionPhotoToCamera()
                    )

                  dialog.dismiss()
                 bottom_nav.menu.getItem(1).setCheckable(false)

                }

        cancel.setOnClickListener {
              dialog.dismiss()
            bottom_nav.menu.getItem(1).setCheckable(false)

        }
        dialog.showImmersive()


    }

    companion object {
        private const val FILE_NAME_KEY = "file_name"

        fun create(image: File) = PhotoFragment().apply {
            arguments = Bundle().apply {
                putString(FILE_NAME_KEY, image.absolutePath)
            }
        }
    }
}