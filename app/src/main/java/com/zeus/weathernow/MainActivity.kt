package com.zeus.weathernow

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import com.zeus.smartlearner.chatSection.NetworkConnectionLiveData
import com.zeus.weathernow.databinding.ActivityMainBinding
import org.json.JSONException
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding
    lateinit var recyclerView: RecyclerView;
    lateinit var loadingBar : ProgressBar
    lateinit var homeRl : RelativeLayout
    lateinit var searchIv  : ImageView
    lateinit var iconIv  : ImageView
    lateinit var cityNameTv : TextView
    lateinit var temperatureTV : TextView
    lateinit var conditionTv : TextView
    lateinit var cityET : TextInputEditText
    lateinit var weatherModelArrayList : ArrayList<WeatherModel>
    lateinit var weatherAdapter: WeatherAdapter
    lateinit var locationManager: LocationManager
    lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private var PERMISSION_CODE = 1
    private var cityName : String? = null
    private var isGPSOn = false
    private var backPressed = 0L
    lateinit var networkConnection : NetworkConnectionLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS , WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        networkConnection = NetworkConnectionLiveData(this)
        recyclerView = binding.recyclerView
        loadingBar = binding.loadidngBar
        homeRl = binding.rl1
        searchIv = binding.searchBtn
        iconIv = binding.temperatureIcon
        cityNameTv = binding.cityNameTV
        temperatureTV = binding.temperatureTV
        conditionTv = binding.conditionTV
        cityET = binding.enterCityTV

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        isGPSOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) //Check if location is turned on or off in user's device

        weatherModelArrayList = ArrayList()
        weatherAdapter = WeatherAdapter(this , weatherModelArrayList)
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = weatherAdapter

        if (ActivityCompat.checkSelfPermission(this ,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this , Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this , arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),PERMISSION_CODE)
        }
        else {
                if (isGPSOn) {
                    locationRequest()
                    var location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                    networkConnection.observe(this, androidx.lifecycle.Observer { isConnected ->
                        if(isConnected) {
                            try {
                                cityName = getCityName(location!!.longitude, location.latitude)
                                getWeatherInfo(cityName!!)
                            } catch ( e : NullPointerException) {
                                Toast.makeText(this , "Error getting location. Please restart this app.", Toast.LENGTH_SHORT).show()
                            }
                            searchIv.setOnClickListener {
                                var city = cityET.text.toString()
                                if (city.isEmpty()) {
                                    Toast.makeText(this , "Please enter city name", Toast.LENGTH_SHORT).show()
                                } else {
                                    cityNameTv.text = cityName
                                    getWeatherInfo(city)
                                }
                            }

                        } else {
                            Toast.makeText(this, "No Internet Connection.", Toast.LENGTH_SHORT).show()

                            searchIv.setOnClickListener {
                                Toast.makeText(this, "No Internet Connection.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                } else {
                    Toast.makeText(this, "Please turn on your phone's location inorder to use this app.", Toast.LENGTH_LONG).show()
                    Handler().postDelayed({
                        finish()
                    }, 2000)
                }
        }

    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_CODE) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        if (isGPSOn) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }else {
                            Toast.makeText(this, "Please turn on your phone's location inorder to use this app.", Toast.LENGTH_LONG).show()
                            Handler().postDelayed({
                                finish()
                            }, 2000)
                        }
                    } catch (e : NullPointerException) {
                        e.printStackTrace()
                    }
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please Provide Permission", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

    }

    private fun getCityName(longitude : Double , latitude : Double) : String {
        var cityName = "Not Found"
        var geocoder = Geocoder(baseContext, Locale.getDefault())
        try {
            var addresses : List<Address> = geocoder.getFromLocation(latitude, longitude , 10)
            for (adr in addresses) {
                if (adr != null) {
                    var city = adr.locality
                    if (city != null && !city.equals("")) {
                        cityName = city;
                    } else {
                        Toast.makeText(this, "Trying to get exact location", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
        return cityName;
    }

    private fun getWeatherInfo(cityName : String) {
        var apiKey = ""
        var url = "http://api.weatherapi.com/v1/forecast.json?key=$apiKey=$cityName&days=1&aqi=yes&alerts=yes"
        cityNameTv.text = cityName
        var requestQueue : RequestQueue = Volley.newRequestQueue(this)

        var jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url , null , {
            loadingBar.visibility = View.GONE
            homeRl.visibility = View.VISIBLE
            weatherModelArrayList.clear()

            try {
                var temperature = it.getJSONObject("current").getString("temp_c")
                temperatureTV.text = ("$temperature°C")
                var isDay = it.getJSONObject("current").getInt("is_day")
                var condition = it.getJSONObject("current").getJSONObject("condition").getString("text")
                var conditionIcon = it.getJSONObject("current").getJSONObject("condition").getString("icon")
                Picasso.get().load("http:$conditionIcon").into(iconIv)
                conditionTv.text = condition
                if (isDay == 1) {
                    //morning
                }
                else {
                    //night
                }
                var forecastObj = it.getJSONObject("forecast")
                var forecast0 = forecastObj.getJSONArray("forecastday").getJSONObject(0)
                var hourArray = forecast0.getJSONArray("hour")

                for (i in 0 until hourArray.length()) {
                    var hourObject = hourArray.getJSONObject(i)
                    var time = hourObject.getString("time")
                    var temp = hourObject.getString("temp_c")
                    var icon = hourObject.getJSONObject("condition").getString("icon")
                    var wind = hourObject.getString("wind_kph")
                    weatherModelArrayList.add(WeatherModel(time , temp , icon , wind))
                }
                weatherAdapter.notifyDataSetChanged()


            } catch (e : JSONException) {
                e.printStackTrace()
            }

        }, {
            //error listener
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        })
        requestQueue.add(jsonObjectRequest)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            try {
                cityName = getCityName(location.longitude, location.latitude)

            } catch (e: java.lang.Exception) {
                locationRequest()
            }
        }
    }

    //Start Location Request, get the current location and save it to firebase database
    @SuppressLint("MissingPermission")
    private fun locationRequest() {
        try {
            val locationRequest: LocationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            locationRequest.interval = 1000
            locationRequest.fastestInterval = 3000
            val locationCallback: LocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    if (locationResult.locations.isEmpty()) {
                        return
                    }
                }
            }
            fusedLocationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).addOnSuccessListener(
                OnSuccessListener<Any?> { location -> if (location != null) {
                } })
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        if (backPressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            Animatoo.animateSlideRight(this)
            finish()
            finishAffinity()

        } else {
            Toast.makeText(this, "Press back again to exit.", Toast.LENGTH_SHORT).show()
        }
        backPressed = System.currentTimeMillis()
    }

    override fun onDestroy() {
        networkConnection.removeObservers(this)
        super.onDestroy()
    }
}