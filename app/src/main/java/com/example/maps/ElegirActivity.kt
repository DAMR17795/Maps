package com.example.maps

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng

class ElegirActivity:AppCompatActivity() {
    private lateinit var aniadir:Button
    private lateinit var buscar:Button
    private lateinit var aeropuertos:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.elegir)

        aniadir = findViewById(R.id.button)
        buscar = findViewById(R.id.button2)
        aeropuertos = findViewById(R.id.button3)

        val bundle = intent
        val coordinates = bundle.getParcelableExtra<LatLng>("UBI")
        val end = bundle?.getStringExtra("END")

        buscar.setOnClickListener {
            if (coordinates==null || end==null) {
                Toast.makeText(this, "Debe añadir una ubicacion primero", Toast.LENGTH_SHORT).show()
            } else {
                val i = Intent(this, MapaFinal::class.java).apply {
                    putExtra("UBIFINAL", coordinates)
                    putExtra("ENDFINAL", end)
                }
                startActivity(i)
            }
        }
        aniadir.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }

        aeropuertos.setOnClickListener {
            startActivity(Intent(this, MapaAeropuertos::class.java))
        }
    }
}