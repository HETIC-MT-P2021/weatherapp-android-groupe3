package com.example.weatherapp

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.weatherapp.weather.ForecastResponse
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
import java.util.*
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor










class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var permissionId = 1234
    private lateinit var locationTxt: TextView
    private lateinit var countryName: String
    private lateinit var cityName: String
    private var weatherData: TextView? = null
    private var forecastData: TextView? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        weatherData = findViewById(R.id.currentWeatherTxt)
        forecastData = findViewById(R.id.currentWeatherTxt2)

        locationTxt = findViewById(R.id.locationTxt)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (isNetworkAvailable(applicationContext)) {
            GlobalScope.launch {
                getLastLocation()
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
                    getCurrentData()
                }
            } else {
                Toast.makeText(this, "Please enable your location service", Toast.LENGTH_SHORT).show()
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

    private fun getCityName(lat:Double, long:Double):String {
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
        var lang = "fr"
    }

    private fun getCurrentData() {
        val logging = HttpLoggingInterceptor()
// set your desired log level
// set your desired log level
        logging.level = HttpLoggingInterceptor.Level.BODY
        val httpClient = OkHttpClient.Builder()
// add your other interceptors …
// add logging as last interceptor
// add your other interceptors …
// add logging as last interceptor
        httpClient.addInterceptor(logging) // <-- this is the important line!


        val retrofit = Retrofit.Builder()
            .baseUrl(BaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build()
        val service = retrofit.create(WeatherService::class.java)

        val call = service.getCurrentWeatherData(cityName, AppId, lang)

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.code() == 200) {
                    val weatherResponse = response.body()!!

                    Log.e("Weather response", weatherResponse.toString())

                    val stringBuilder = weatherResponse.weather[0].description +
                            "\n" +
                            "Température: " +
                            weatherResponse.main!!.temp +
                            "\n" +
                            "Température(Min): " +
                            weatherResponse.main!!.temp_min +
                            "\n" +
                            "Température(Max): " +
                            weatherResponse.main!!.temp_max +
                            "\n" +
                            "Humidité: " +
                            weatherResponse.main!!.humidity +
                            "\n" +
                            "Pression: " +
                            weatherResponse.main!!.pressure

                    weatherData!!.text = stringBuilder
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                weatherData!!.text = t.message
            }
        })

        val call5Days = service.getCurrentForecastData("Paris", AppId, lang, "metric")
        call5Days.enqueue(object : Callback<ForecastResponse> {
            override fun onResponse(req: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                if (response.code() == 200) {
                    val forecastResponse = response.body()!!

                    val stringBuilder = forecastResponse.city!!.name +
                            "\n" +
                            "heure j1: " +
                            Date(forecastResponse.list[0]!!.dt!!.toLong() * 1000) +
                            "\n" +
                            "heure j2: " +
                            Date(forecastResponse.list[8]!!.dt!!.toLong() * 1000) +
                            "\n" +
                            "heure j3: " +
                            Date(forecastResponse.list[16]!!.dt!!.toLong() * 1000) +
                            "\n" +
                            "heure j4: " +
                            Date(forecastResponse.list[24]!!.dt!!.toLong() * 1000) +
                            "\n" +
                            "heure j5: " +
                            Date(forecastResponse.list[32]!!.dt!!.toLong() * 1000)

                    forecastData!!.text = stringBuilder
                }
            }

            override fun onFailure(req: Call<ForecastResponse>, t: Throwable) {
                forecastData!!.text = t.message
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
                Log.d("Debug: ", "Permission granted")
            }
        }
    }
}