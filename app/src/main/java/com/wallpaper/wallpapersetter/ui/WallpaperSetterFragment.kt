package com.wallpaper.wallpapersetter.ui

import android.Manifest
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.wallpapersetter.R
import com.example.wallpapersetter.databinding.FragmentWallpaperSetterBinding
import com.wallpaper.common.component.AlertBox
import com.wallpaper.wallpapersetter.utils.NetworkResult
import com.wallpaper.wallpapersetter.utils.Utils
import com.wallpaper.wallpapersetter.utils.openAppSystemSettings
import com.wallpaper.wallpapersetter.utils.sdk29AndUp
import com.wallpaper.wallpapersetter.viewModel.WallpaperDownloadViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class WallpaperSetterFragment : Fragment() {

    @Inject
    @Named("APPLICATION_CONTEXT")
    lateinit var appContext: Context

    private lateinit var binding: FragmentWallpaperSetterBinding
    private lateinit var wallpaperDownloadViewModel: WallpaperDownloadViewModel
    private var wallpaperManager: WallpaperManager? = null
    private var width: Int = 0
    private var height: Int = 0

    private var permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            readPermissionGranted =
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
            writePermissionGranted =
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted
            if (readPermissionGranted && writePermissionGranted){
                context?.let {
                    binding.downloadButton.performClick()
                }
            } else {
                callPermissionViaSettings()
            }
        }
    private var readPermissionGranted = false
    private var writePermissionGranted = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWallpaperSetterBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wallpaperDownloadViewModel = ViewModelProvider(this)[WallpaperDownloadViewModel::class.java]
    }

    companion object {
        fun getInstance() = WallpaperSetterFragment().apply {}
        enum class WallpaperOptions {
            LOCKSCREEN,
            HOMESCREEN,
            BOTH
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupHeight()
        setActions()
        callImageApi()
        observer()
    }

    private fun checkIfFragmentAttached(operation: Context.() -> Unit) {
        if (isAdded && context != null) {
            operation(requireContext())
        }
    }

    private fun callPermissionViaSettings(){
        context?.let {
            AlertBox.showDialog(
                it,
                header = "Permission Required",
                message = "You have not allowed download permission. Do you want to redirect to app setting to allow access?",
                positiveText = "Yes",
                negativeText = "No",
                positiveCallback = {
                    it.openAppSystemSettings()
                },
                negativeCallback = {
                    // Nothing to do
                }
            )
        }
    }

    private fun setupHeight() {
        checkIfFragmentAttached {
            width = Utils.getWidth(this)
            height = Utils.getHeight(this)
        }
    }

    private fun showImage(){
        wallpaperDownloadViewModel.getCurrentImage()?.let {
            binding.imageSource.setImageBitmap(decodeByteToImage(it))
        }
    }

    private fun enableDisableButtonView(view: View, enable: Boolean){
        view.apply {
            isEnabled = enable
            alpha = if (enable) 1f else 0.5f
        }
    }

    private fun setActions() {
        binding.leftButton.setOnClickListener {
            if (wallpaperDownloadViewModel.isImageAvailable(leftBtn = true)) {
                showImage()
            } else {
                enableDisableButtonView(binding.leftButton, enable = false)
                checkIfFragmentAttached {
                    Toast.makeText(
                        this,
                        getString(R.string.no_more_images),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        binding.rightButton.setOnClickListener {
            if (wallpaperDownloadViewModel.isImageAvailable(leftBtn = false)){
                showImage()
            } else {
                callImageApi()
            }
            enableDisableButtonView(binding.leftButton, enable = true)
        }
        binding.wallpaperSetButton.setOnClickListener {
            checkIfFragmentAttached {
                AlertBox.showDialog(
                    this,
                    getString(R.string.wallpaper_set_header),
                    getString(R.string.wallpaper_set_message),
                    getString(R.string.homescreen),
                    getString(R.string.lockscreen),
                    getString(R.string.both),
                    positiveCallback = {
                        setWallpaper(WallpaperOptions.HOMESCREEN)
                    },
                    negativeCallback = {
                        setWallpaper(WallpaperOptions.LOCKSCREEN)
                    },
                    neutralCallback = {
                        setWallpaper(WallpaperOptions.BOTH)
                    }
                )
            }
        }
        binding.downloadButton.setOnClickListener {
            updatePermission()
            if (readPermissionGranted && writePermissionGranted) {
                wallpaperDownloadViewModel.getCurrentImage()?.let {
                    savePhotoToExternalStorage(UUID.randomUUID().toString(), decodeByteToImage(it))
                }
            } else {
                checkIfFragmentAttached {
                    AlertBox.showDialog(
                        this,
                        header = getString(R.string.require_permission_header),
                        message = getString(R.string.require_permission_message),
                        positiveText = getString(R.string.allow),
                        negativeText = getString(R.string.deny),
                        positiveCallback = {
                            permissionCall()
                        },
                        negativeCallback = {
                            // Nothing to do
                        }
                    )
                }
            }
        }
    }

    private fun permissionCall() {
        updateOrRequestPermissions()
    }

    private fun updatePermission() {
        checkIfFragmentAttached {
            readPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            writePermissionGranted = (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) || minSdk29
        }
    }

    private fun updateOrRequestPermissions() {
        checkIfFragmentAttached {
            val hasReadPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val hasWritePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

            readPermissionGranted = hasReadPermission
            writePermissionGranted = hasWritePermission || minSdk29

            val permissionsToRequest = mutableListOf<String>()
            if (!writePermissionGranted) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (!readPermissionGranted) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (permissionsToRequest.isNotEmpty()) {
                permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap): Boolean {
        val imageCollection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }
        return try {
            requireActivity().contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                requireActivity().contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        throw IOException(getString(R.string.cannot_save_bitmap))
                    }
                }
            } ?: throw IOException(getString(R.string.cannot_create_mediastore_entry))
            showToast(getString(R.string.file_download_success))
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun setWallpaper(wallpaperOptions: WallpaperOptions) {
        wallpaperManager?.let { wallpaperManager ->
            wallpaperDownloadViewModel.getCurrentImage()?.let {
                try {
                    when (wallpaperOptions) {
                        WallpaperOptions.HOMESCREEN -> {
                            val result = wallpaperManager.setStream(
                                ByteArrayInputStream(it),
                                null,
                                true,
                                WallpaperManager.FLAG_SYSTEM
                            )
                            if (result == 0) {
                                showToast(getString(R.string.wallpaper_set_unsuccessful))
                            } else {
                                showToast(getString(R.string.wallpaper_set_successful))
                            }
                        }
                        WallpaperOptions.LOCKSCREEN -> {
                            val result = wallpaperManager.setStream(
                                ByteArrayInputStream(it),
                                null,
                                true,
                                WallpaperManager.FLAG_LOCK
                            )
                            if (result == 0) {
                                showToast(getString(R.string.wallpaper_set_unsuccessful))
                            } else {
                                showToast(getString(R.string.wallpaper_set_successful))
                            }
                        }
                        WallpaperOptions.BOTH -> {
                            wallpaperManager.setStream(ByteArrayInputStream(it))
                            showToast(getString(R.string.wallpaper_set_successful))
                        }
                    }
                } catch (e: Exception){
                    showToast(getString(R.string.wallpaper_set_unsuccessful))
                }
            } ?: callImageApi()
        } ?: kotlin.run {
            initialiseWallpaper(wallpaperOptions)
        }
    }

    private fun showToast(msg: String) {
        checkIfFragmentAttached {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initialiseWallpaper(wallpaperOptions: WallpaperOptions) {
        try {
            wallpaperManager = WallpaperManager.getInstance(appContext)
            setWallpaper(wallpaperOptions)
        } catch (e: Exception) {
            // Nothing to do
        }
    }

    private fun callImageApi(hdFactor: Float = 1.2f) {
        wallpaperDownloadViewModel.downloadImage(
            width = (this@WallpaperSetterFragment.width.toFloat() * hdFactor).toInt(),
            height = (this@WallpaperSetterFragment.height.toFloat() * hdFactor).toInt()
        )
    }

    private fun observer() {
        wallpaperDownloadViewModel.imageDownloadLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is NetworkResult.Success -> {
                    binding.circularProgressImage.visibility = View.GONE
                    it.data?.body()?.bytes()?.let {
                        wallpaperDownloadViewModel.bitmapAddToViewModel(it)
                        showImage()
                    }
                }
                is NetworkResult.Loading -> {
                    binding.circularProgressImage.run {
                        visibility = View.VISIBLE
                        animate()
                    }
                }
                is NetworkResult.Error -> {
                    binding.circularProgressImage.visibility = View.GONE
                    checkIfFragmentAttached {
                        AlertBox.showDialog(
                            this,
                            header = getString(R.string.error),
                            message = getString(R.string.try_again_later),
                            positiveText = getString(R.string.retry),
                            negativeText = getString(R.string.okay),
                            positiveCallback = {
                                callImageApi()
                            },
                            negativeCallback = {
                                // do nothing
                            }
                        )
                    }
                }
            }

        }
    }

    private fun decodeByteToImage(byteArray: ByteArray) : Bitmap {
        return BitmapFactory.decodeStream(ByteArrayInputStream(byteArray))
    }
}