package com.example.maps

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Splash:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, ElegirActivity::class.java))
        finish()
    }
}