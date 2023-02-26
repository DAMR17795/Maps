package com.example.maps

import android.Manifest
import android.content.Context
import android.content.Intent
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
    private lateinit var btnGuardar:Button
    private lateinit var ubicacion:LatLng

    private var start:String=""
    private var end:String=""

    var poly:Polyline? = null
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnCalculate = findViewById(R.id.btnCalculateRoute)
        btnGuardar = findViewById(R.id.btnAceptar)

        //Este boton guarda la ubicacion para el marcador
        //y para calcular la ruta en la actividad final
        btnGuardar.setOnClickListener{
            if (end!="") {
                val i = Intent(this, ElegirActivity::class.java).apply {
                    putExtra("UBI", ubicacion)
                    putExtra("END", end)
                }
                startActivity(i)
            } else {
                Toast.makeText(this, "Debe seleccionar una ubicacion para continuar", Toast.LENGTH_SHORT).show()
            }
        }

        //Este boton calcula la ruta a modo de prueba si se quiere
        //(seguramente lo quitemos)
        btnCalculate.setOnClickListener {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "HABILITE GPS", Toast.LENGTH_SHORT).show()
            }
            poly?.remove()
            start=""
            //end=""
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
                            start="${longitude},${latitude}"
                            createRoute()
                        }
                    }
            }
        }
        createFragment()
    }

    //Este metodo permite crear un marcador
    //al pinchar en el mapa, le pone de titulo
    //ubicación por defecto y hace la animación
    //para situar el mapa en el marcador colocado
    private fun metodo() {
        map.setOnMapClickListener {
            map.clear()
            end = "${it.longitude},${it.latitude}"
            val coordinates = LatLng(it.latitude, it.longitude)
            ubicacion = LatLng(it.latitude, it.longitude)
            val marker:MarkerOptions = MarkerOptions().position(coordinates).title("Ubicación")
            map.addMarker(marker)
            //map.moveCamera(CameraUpdateFactory.newLatLng(coordinates))
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(coordinates, 15f),
                1000,
                null
            )
        }
    }

    //Metodo para obtener la api de las rutas
    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    //Metodo para crear la ruta
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

    //Metodo para dibujar las lineas de la ruta
    private fun drawRoute(routeResponse: RouteResponse?) {
        val polyLineOptions = PolylineOptions()
        routeResponse?.features?.first()?.geometry?.coordinates?.forEach {
            polyLineOptions.add(LatLng(it[1], it[0]))
        }
        runOnUiThread {
            poly = map.addPolyline(polyLineOptions)
        }

    }

    //Metodo para cargar el fragment del mapa
    private fun createFragment() {
        val mapFragment:SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        //Permite tener el boton que al pulsarlo
        //te manda a la ubicación en tiempo real del gps
        map.setOnMyLocationButtonClickListener(this)
        enableLocation()
        metodo()

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

    //Metodo para comprobar si está el permiso
    //de localización garatinzado
    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    //Metodo habilita la localización
    //cuando el mapa está inicializado
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

    //Metodo para pedir los permisos de ubicación
    private fun requestLocationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
        }
    }

    //Según la respuesta de la peticiónd de permisos
    //lanza el toast o habilita la localización
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

    //Mensaje cuando pulsa el botoon de posicionar
    //en la localización
    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "Boton Pulsado", Toast.LENGTH_SHORT).show()
        return false
    }
}
