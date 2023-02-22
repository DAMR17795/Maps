package com.example.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener {
    private lateinit var map:GoogleMap
    private lateinit var btnCalculate: Button

    private var start:String=""
    private var end:String=""

    var poly:Polyline? = null
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnCalculate = findViewById(R.id.btnCalculateRoute)
            //val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            //val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        //Metodo para hacer la ruta

        btnCalculate.setOnClickListener {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "HABILITE GPS", Toast.LENGTH_SHORT).show()
            }
            poly?.remove()
            start=""
            end=""
            poly = null

            //Toast.makeText(this, "Seleccione punto de origen y final", Toast.LENGTH_SHORT).show()
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
                            start="${longitude},${latitude}"
                            //Murgi
                            //end="-2.815780, 36.781763"
                            //Romelina
                            //end="-2.846159, 36.766384"
                            //createRoute()
                            // Hacer algo con la ubicación obtenida
                            map.setOnMapClickListener {
                                end = "${it.longitude},${it.latitude}"
                                println("Longitud: ${it.longitude}, Latitud: ${it.latitude}")
                                createRoute()
                            }
                        }
                    }

                //val latitude = location?.latitude
                //val longitude = location?.longitude
                //Toast.makeText(this, "Latitutd: " + location?.latitude + ", Longitud: " + location?.longitude, Toast.LENGTH_SHORT).show()
                //start="${longitude},${latitude}"
                //start="-2.833312, 36.782167"
                //end="-2.815151, 36.771005"
                //createRoute()


                /*if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    // Utiliza las variables "latitude" y "longitude" según sea necesario
                    start="${longitude},${latitude}"
                    //start="-2.833312, 36.782167"
                    end="-2.815151, 36.771005"
                    createRoute()

                    Log.i("Ubicacion", "OK")
                } else {
                    Log.i("Ubicacion", "KO")
                    //Toast.makeText(this, "ERROR: Active GPS", Toast.LENGTH_SHORT).show()
                }*/
                /*map.setOnMapClickListener {
                    if(start.isNotEmpty()){
                        start = "${it.longitude},${it.latitude}"
                    } else if (end.isEmpty()) {
                        end = "${it.longitude},${it.latitude}"
                        createRoute()
                    }
                }*/

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
        createMarker()
        map.setOnMyLocationButtonClickListener(this)
        enableLocation()
    }

    private fun createMarker() {
        val coordinates = LatLng(36.781853, -2.815791)
        val marker:MarkerOptions = MarkerOptions().position(coordinates).title("I.E.S Murgi")
        map.addMarker(marker)
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(coordinates, 18f),
            4000,
            null
        )
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
