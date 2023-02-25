package com.example.maps

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng

class ElegirActivity:AppCompatActivity() {
    private lateinit var aniadir:Button
    private lateinit var buscar:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.elegir)

        aniadir = findViewById(R.id.button)
        buscar = findViewById(R.id.button2)

        val bundle = intent
        val coordinates = bundle.getParcelableExtra<LatLng>("UBI")
        val end = bundle?.getStringExtra("END")

        buscar.setOnClickListener {
            val i = Intent(this, MapaFinal::class.java).apply {
                putExtra("UBIFINAL", coordinates)
                putExtra("ENDFINAL", end)

            }
            startActivity(i)
        }

        aniadir.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}