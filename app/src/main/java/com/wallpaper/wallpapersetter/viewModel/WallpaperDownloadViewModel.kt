package com.wallpaper.wallpapersetter.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallpaper.wallpapersetter.repository.ImageDownloadRepository
import com.wallpaper.wallpapersetter.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class WallpaperDownloadViewModel @Inject constructor(private val imageDownloadRepository: ImageDownloadRepository) :
    ViewModel() {
    private var _imageDownloadLiveData = MutableLiveData<NetworkResult<Response<ResponseBody>>>()
    val imageDownloadLiveData: LiveData<NetworkResult<Response<ResponseBody>>>
        get() = _imageDownloadLiveData

    fun downloadImage(width: Int, height: Int) {
        viewModelScope.launch {
            _imageDownloadLiveData.postValue(NetworkResult.Loading())
            var response: NetworkResult<Response<ResponseBody>>
            withContext(Dispatchers.IO) {
                response = imageDownloadRepository.getImage(width, height)
            }
            withContext(Dispatchers.Main) {
                _imageDownloadLiveData.value = response
            }
        }
    }

    private var currentIndex = 0
    private val imageList = ArrayList<ByteArray>()

    fun bitmapAddToViewModel(bmp: ByteArray) {
        imageList.add(bmp)
        currentIndex = imageList.size - 1
    }

    fun isImageAvailable(leftBtn: Boolean): Boolean {
        if (imageList.isEmpty()) {
            return false
        }
        return if (leftBtn) {
            if (currentIndex - 1 >= 0 && currentIndex - 1 <= imageList.size - 1) {
                currentIndex--
                true
            } else {
                false
            }
        } else {
            if (currentIndex + 1 >= 0 && currentIndex + 1 <= imageList.size - 1) {
                currentIndex++
                true
            } else {
                false
            }
        }
    }

    fun getCurrentImage() : ByteArray?{
        return if (imageList.isNotEmpty() && currentIndex >= 0 && currentIndex <= imageList.size - 1){
            imageList[currentIndex]
        } else {
            null
        }
    }


}