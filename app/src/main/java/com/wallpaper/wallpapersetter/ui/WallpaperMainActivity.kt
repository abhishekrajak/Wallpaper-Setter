package com.wallpaper.wallpapersetter.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.wallpapersetter.databinding.ActivityWallpaperMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WallpaperMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        fragmentTransaction(WallpaperSetterFragment.getInstance())
    }

    private fun fragmentTransaction(fragment: Fragment){

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(binding.fragmentView.id, fragment)
        transaction.commit()
    }

}