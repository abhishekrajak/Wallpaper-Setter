package com.wallpaper.wallpapersetter.repository

import com.wallpaper.wallpapersetter.api.ImageDownloadApi
import com.wallpaper.wallpapersetter.utils.NetworkResult
import okhttp3.ResponseBody
import retrofit2.Response
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Named

class ImageDownloadRepository @Inject constructor(
    @Named("IMAGE_DOWNLOAD") private val api: ImageDownloadApi
) {
    suspend fun getImage(width: Int, height: Int): NetworkResult<Response<ResponseBody>> {
        val response: Response<ResponseBody>?
        val errorMessage: String?
        return try {
            response = api.downloadImage(width, height)
            parseApi(response)
        } catch (e: Exception) {
            errorMessage = e.message
            NetworkResult.Error(errorMessage, null)
        }
    }

    private fun <T> parseApi(response: T?): NetworkResult<T> {
        return if (response != null) {
            NetworkResult.Success(response)
        } else {
            NetworkResult.Error(message = "Body is null")
        }
    }
}