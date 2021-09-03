package com.example.weatherapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.weatherapp.weather.WeatherResponse
import com.example.weatherapp.weather.WeatherService
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.util.*
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var permissionId = 1234
    private lateinit var locationTxt: TextView
    private lateinit var countryName: String
    private var cityName: String? = null
    private var updatedAt: TextView? = null
    private lateinit var statusTxt: TextView
    private lateinit var temperatureTxt: TextView
    private lateinit var tempMinTxt: TextView
    private lateinit var tempMaxTxt: TextView
    private lateinit var windTxt: TextView
    private lateinit var pressureTxt: TextView
    private lateinit var humidityTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationTxt = findViewById(R.id.location_txt)
        statusTxt = findViewById(R.id.status_txt)
        temperatureTxt = findViewById(R.id.temperature_txt)
        tempMinTxt = findViewById(R.id.temp_min_txt)
        tempMaxTxt = findViewById(R.id.temp_max_txt)
        windTxt = findViewById(R.id.wind_txt)
        pressureTxt = findViewById(R.id.pressure_txt)
        humidityTxt = findViewById(R.id.humidity_txt)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (isNetworkAvailable(applicationContext)) {
            GlobalScope.launch {
                getLastLocation()
                delay(200)
                cityName?.let { getCurrentData(it) }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkAvailable(context: Context) = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
        getNetworkCapabilities(activeNetwork)?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } ?: false
    }

    private fun getLastLocation() {
        if (checkPermission()) {
            if (isLocationEnabled()) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                    displayLocation(task)
//                    cityName?.let {
//                        getCurrentData(it)
//                    }
                }
            } else {
                Toast.makeText(this, "Veuillez activer le service de géolocalisation", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermission()
        }
    }

    private fun displayLocation(task: Task<Location>) {
        var location = task.result
        if (location == null) {
            getNewLocation()
        } else {
            locationTxt.text = getCityName(location.latitude, location.longitude) + ", " + getCountryName(location.latitude, location.longitude)
        }
    }

    private fun getNewLocation() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 2
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
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
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            var lastLocation = p0?.lastLocation
            if (lastLocation != null) {
                locationTxt.text = getCityName(lastLocation.latitude, lastLocation.longitude) + ", " + getCountryName(lastLocation.latitude, lastLocation.longitude)
            }
        }
    }

    private fun checkPermission():Boolean {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), permissionId)
    }

    private fun isLocationEnabled():Boolean {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun getCityName(lat:Double, long:Double):String? {
        var geoCoder = Geocoder(this, Locale.getDefault())
        var address = geoCoder.getFromLocation(lat, long, 1)
        cityName = address[0].locality
        return cityName
    }

    private fun getCountryName(lat:Double, long:Double):String {
        var geoCoder = Geocoder(this, Locale.getDefault())
        var address = geoCoder.getFromLocation(lat, long, 1)
        countryName = address[0].countryName
        return countryName
    }

    companion object {
        var BaseUrl = "https://api.openweathermap.org/"
        var AppId = "f3aa9809cf0afaa4155a1dfe26f5d838"
        var units = "metric"
        var lang = "fr"
    }

    private fun getCurrentData(city: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(WeatherService::class.java)
        val call = service.getCurrentWeatherData(city, AppId, lang, units)
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.code() == 200) {
                    val weatherResponse = response.body()!!

                    statusTxt.text = weatherResponse.weather[0].description
                    temperatureTxt.text = NumberFormat.getInstance().format(round(weatherResponse.main!!.temp)).toString() + "°C"
                    tempMinTxt.text = NumberFormat.getInstance().format(round(weatherResponse.main!!.temp_min)).toString() + "°C"
                    tempMaxTxt.text = NumberFormat.getInstance().format(round(weatherResponse.main!!.temp_max)).toString() + "°C"
                    windTxt.text = weatherResponse.wind?.speed?.let { round(it).toString() }
                    pressureTxt.text = NumberFormat.getInstance().format(round(weatherResponse.main!!.pressure)).toString()
                    humidityTxt.text = NumberFormat.getInstance().format(round(weatherResponse.main!!.humidity)).toString()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                updatedAt!!.text = t.message
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionId) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Debug: ", "Permission accordée")
            }
        }
    }
}