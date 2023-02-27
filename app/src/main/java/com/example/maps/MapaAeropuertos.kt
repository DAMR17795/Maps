package com.example.maps

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapaAeropuertos : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener {
    private lateinit var map:GoogleMap
    private lateinit var btnCalculate: Button
    private lateinit var btnVamonos:Button

    private var start:String=""
    private var end:String=""

    var poly:Polyline? = null

    //Aeropuertos
    private lateinit var coordinatesGranada:LatLng
    private lateinit var coordinatesAlmeria:LatLng
    private lateinit var coordinatesMalaga:LatLng
    private lateinit var coordinatesMadrid:LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.aeropuertos)
        btnCalculate = findViewById(R.id.btnIrA)

        btnCalculate.setOnClickListener {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "HABILITE GPS", Toast.LENGTH_SHORT).show()
            }
            poly?.remove()
            start=""
            end=""
            poly = null
            if(::map.isInitialized) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@setOnClickListener
                }

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Se ha obtenido la ubicación actual
                        if (location != null) {
                            val latitude = location?.latitude
                            val longitude = location?.longitude
                            val coordinates = LatLng(latitude!!, longitude!!)
                            start="${longitude},${latitude}"
                            var options = listOf("Aeropuerto Madrid", "Aeropuerto Granada", "Aeropuerto Málaga", "Aeropuerto Almería")
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Elige el aeropuerto al que deseas llegar")
                            builder.setItems(options.toTypedArray()){ _, item ->
                                when {
                                    options[item] == "Aeropuerto Madrid" -> {
                                        end = "${-3.593806}, ${40.492153}"
                                        createRoute()
                                        map.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(coordinates, 6f),
                                            2000,
                                            null
                                        )
                                    }
                                    options[item] == "Aeropuerto Granada" -> {
                                        end = "${-3.778166}, ${37.187297}"
                                        createRoute()
                                        map.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(coordinates, 8f),
                                            2000,
                                            null
                                        )
                                    }
                                    options[item] == "Aeropuerto Málaga" -> {
                                        end = "${-4.491321}, ${36.676559}"
                                        createRoute()
                                        map.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(coordinates, 7f),
                                            2000,
                                            null
                                        )
                                    }
                                    options[item] == "Aeropuerto Almería" -> {
                                        end = "${-2.370763}, ${36.844627}"
                                        createRoute()
                                        map.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(coordinates, 9f),
                                            2000,
                                            null
                                        )
                                    }
                                }
                            }
                            builder.show()

                        }
                    }
            }
        }
        createFragment()
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    private fun createRoute(){
        CoroutineScope(Dispatchers.IO).launch {
            val call = getRetrofit().create(ApiService::class.java)
                .getRoute("5b3ce3597851110001cf624808c292df8dea4e369babd5796e166540", start, end)
            if (call.isSuccessful){
                Log.i("aris", "OK")
                drawRoute(call.body())
            } else {
                Log.i("aris", "KO")
            }
        }
    }

    private fun drawRoute(routeResponse: RouteResponse?) {
        val polyLineOptions = PolylineOptions()
        routeResponse?.features?.first()?.geometry?.coordinates?.forEach {
            polyLineOptions.add(LatLng(it[1], it[0]))
        }
        runOnUiThread {
            poly = map.addPolyline(polyLineOptions)
        }

    }

    private fun createFragment() {
        val mapFragment:SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMyLocationButtonClickListener(this)
        enableLocation()
        //Ponemos valores a los aeropuertos
        coordinatesGranada = LatLng(37.187297, -3.778166)
        coordinatesAlmeria = LatLng(36.844627, -2.370763)
        coordinatesMalaga = LatLng(36.676559, -4.491321)
        coordinatesMadrid = LatLng(40.492153, -3.593806)
        createMarker("${-3.778166}, ${37.187297}", coordinatesGranada, "Aeropuerto Granada")
        createMarker("${-2.370763}, ${36.844627}", coordinatesAlmeria, "Aeropuerto Almería")
        createMarker("${-4.491321}, ${36.676559}", coordinatesMalaga, "Aeropuerto Málaga")
        createMarker("${-3.593806}, ${40.492153}", coordinatesMadrid, "Aeropuerto Madrid")
        //Este codigo es para cuando al estar el mapa
        //listo hace una animación para posicionarlo
        //justo donde esté la ubicación el tiempo real
        //del gps
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Se ha obtenido la ubicación actual
                if (location != null) {
                    val latitude = location?.latitude
                    val longitude = location?.longitude
                    val coordinates = LatLng(latitude!!, longitude!!)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(coordinates, 15f),
                        4000,
                        null
                    )

                }
            }
    }

    private fun createMarker(ubicacion:String, marcador:LatLng, aeropuerto:String) {
        val coordinates = marcador
        end = ubicacion.toString()
        //val coordinates = LatLng(36.781853, -2.815791)
        val marker:MarkerOptions = MarkerOptions().position(coordinates!!).title(aeropuerto)
        map.addMarker(marker)
        /*map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(coordinates, 15f),
            4000,
            null
        )*/
    }

    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun enableLocation() {
        if (!::map.isInitialized) return
        if (isLocationPermissionGranted()) {
            //si
            map.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
            //no
        }
    }

    private fun requestLocationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
            } else {
                Toast.makeText(this, "Para activar la localización ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
            }
            else ->{}
            //hola
            //añadido desde github
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::map.isInitialized) return
        if (!isLocationPermissionGranted()) {
            map.isMyLocationEnabled = false
            Toast.makeText(this, "Para activar la localización ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()

        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "Boton Pulsado", Toast.LENGTH_SHORT).show()
        return false
    }
}
