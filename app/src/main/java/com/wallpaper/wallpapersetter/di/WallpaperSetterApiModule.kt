package com.wallpaper.wallpapersetter.di

import com.wallpaper.wallpapersetter.api.ImageDownloadApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named

@InstallIn(ViewModelComponent::class)
@Module
class WallpaperSetterApiModule {

    @Named("IMAGE_DOWNLOAD")
    @Provides
    fun getImageDownloadApi(
        retrofitBuilder: Retrofit.Builder,
        okHttpClient: OkHttpClient
    ): ImageDownloadApi {
        return retrofitBuilder.client(okHttpClient).build().create(ImageDownloadApi::class.java)
    }

}