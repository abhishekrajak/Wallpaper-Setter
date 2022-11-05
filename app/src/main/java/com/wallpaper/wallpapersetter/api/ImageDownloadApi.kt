package com.wallpaper.wallpapersetter.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ImageDownloadApi {

    @GET("{width}/{height}")
    suspend fun downloadImage(@Path("width") width: Int, @Path("height") height: Int): Response<ResponseBody>
}