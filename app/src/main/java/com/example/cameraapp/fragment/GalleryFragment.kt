package com.example.cameraapp.fragment

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.cameraapp.R
import com.example.cameraapp.adapter.StaggeredRecyclerAdapter
import com.example.cameraapp.model.row
import com.example.cameraapp.utils.padWithDisplayCutout
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

val EXTENSION_WHITELIST = arrayOf("JPG")

/** Fragment used to present the user with a gallery of photos taken */
class GalleryFragment internal constructor() : Fragment() {

    /** AndroidX navigation arguments */
    private val args: GalleryFragmentArgs by navArgs()

    lateinit var cutout_safe_area:ConstraintLayout
    lateinit var staggered_view:RecyclerView
    lateinit var  adapter:StaggeredRecyclerAdapter
    lateinit var manager:StaggeredGridLayoutManager
    lateinit var back_button:ImageButton
   // lateinit var progressDialog:ProgressBar
    lateinit var blank:Button

   // var show=true

    private lateinit var mediaList: MutableList<File>
    private var imageList:ArrayList<row> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get root directory of media from navigation arguments
        val rootDirectory = File(args.rootDirectory)

        // We reverse the order of the list to present the last photos first
        mediaList = rootDirectory.listFiles { file ->
            EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
        }?.sortedDescending()?.toMutableList() ?: mutableListOf()

        if(mediaList.size != 0){
            for(i in 0..mediaList.size-1){
                Log.d("Gallery","index :"+i)
                imageList.add(row(mediaList[i]))
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_gallery, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cutout_safe_area = view.findViewById(R.id.cutout_safe_area)
      //  progressDialog=view.findViewById(R.id.progressDialog)
        staggered_view= view.findViewById(R.id.staggered_view)
        back_button = view.findViewById(R.id.back_button)
        blank = view.findViewById(R.id.blank)

       // showHideProgress(show)

        setUpListView()

        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            view.findViewById<ConstraintLayout>(R.id.cutout_safe_area).padWithDisplayCutout()
        }

        back_button.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigateUp()
        }

    }

    private fun setUpListView() {

        if(imageList.isEmpty()){
          //  showHideProgress(false)
              back_button.visibility = View.VISIBLE
              blank.visibility = View.VISIBLE

        }else{
            adapter = StaggeredRecyclerAdapter(requireContext(),imageList)
            manager= StaggeredGridLayoutManager(3,StaggeredGridLayoutManager.VERTICAL)

            adapter.notifyDataSetChanged()
            staggered_view.layoutManager = manager
            staggered_view.adapter = adapter
            // showHideProgress(false)
        }

    }

//    private fun showHideProgress(show: Boolean) {
//        if (show) {
//            progressDialog.visibility = View.VISIBLE
//        } else {
//            progressDialog.visibility = View.GONE
//        }
//    }


}
