package com.example.weather

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.weather.databinding.ActivityMainBinding
import com.example.weather.databinding.ItemForecastBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                updateLocation()
            }

            else -> {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))


    }

    private fun updateLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location ->

                Thread {
                    try {
                        val addressList = Geocoder(this, Locale.KOREA).getFromLocation(
                            location.latitude, location.longitude, 1
                        )

                        Log.d("loc", addressList?.get(0)?.thoroughfare.orEmpty())
                        runOnUiThread {
                            binding.locationTextView.text =
                                addressList?.get(0)?.thoroughfare.orEmpty()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }


                }.start()

                WeatherRepository.getVillageForecast(
                    longitude = location.longitude,
                    latitude = location.latitude,
                    successCallBack = { list ->

                        val currentForecast = list.first()

                        binding.tmpTextView.text =
                            getString(R.string.temperature_text, currentForecast.temperature)
                        binding.skyTextView.text = currentForecast.weather
                        binding.precipitationTextView.text =
                            getString(R.string.precipitation_text, currentForecast.precipitation)

                        if (currentForecast.weather.contains("비") || currentForecast.weather.contains(
                                "눈"
                            )
                        ) {
                            binding.imgView.setImageResource(R.drawable.baseline_umbrella_24)
                        } else if (currentForecast.weather.contains("구름") || currentForecast.weather.contains(
                                "흐림"
                            )
                        ) {
                            binding.imgView.setImageResource(R.drawable.baseline_cloud_24)
                        } else {
                            binding.imgView.setImageResource(R.drawable.baseline_wb_sunny_24)
                        }

                        binding.childForecastLayout.apply {

                            list.forEachIndexed { index, forecast ->
                                if (index == 0) return@forEachIndexed

                                val itemView = ItemForecastBinding.inflate(layoutInflater)

                                itemView.timeTextView.text = forecast.forecastTime
                                itemView.weatherTextView.text = forecast.weather
                                itemView.temperatureTextView.text =
                                    getString(R.string.temperature_text, forecast.temperature)

                                if (forecast.weather.contains("비") || forecast.weather.contains("눈")) {
                                    itemView.imgView.setImageResource(R.drawable.baseline_umbrella_24)
                                } else if (forecast.weather.contains("구름") || forecast.weather.contains(
                                        "흐림"
                                    )
                                ) {
                                    itemView.imgView.setImageResource(R.drawable.baseline_cloud_24)
                                } else {
                                    itemView.imgView.setImageResource(R.drawable.baseline_wb_sunny_24)
                                }

                                addView(itemView.root)
                            }
                        }

                        Log.d("forecast", list.toString())
                    },
                    failureCallBack = {
                        it.printStackTrace()
                    }
                )


            }
    }
}