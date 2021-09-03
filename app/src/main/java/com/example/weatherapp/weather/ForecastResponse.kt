package com.example.weatherapp.weather

import com.google.gson.annotations.SerializedName

class ForecastResponse {
    @SerializedName("list")
    var list = ArrayList<List>()

    @SerializedName("city")
    var city: City? = null
}

class List {
    @SerializedName("dt")
    var dt: Number? = null

    @SerializedName("main")
    var main: Main? = null

    @SerializedName("weather")
    var weather = ArrayList<Weather>()
}

class City {
    @SerializedName("name")
    var name: String? = null
}