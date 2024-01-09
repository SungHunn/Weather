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

    private fun transformRainType(forecastEntity: ForecastEntity): String {
        return when (forecastEntity.forecastValue.toInt()) {
            0 -> "없음"
            1 -> "비"
            2 -> "비/눈"
            3 -> "눈"
            4 -> "소나기"
            else -> ""
        }
    }

    private fun transformSky(forecast: ForecastEntity): String {
        return when (forecast.forecastValue.toInt()) {
            1 -> "맑음"
            3 -> "구름많음"
            4 -> "흐림"
            else -> ""
        }
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
                            location.latitude,location.longitude,1
                        )

                        Log.d("loc" , addressList?.get(0)?.thoroughfare.orEmpty())
                        runOnUiThread {
                            binding.locationTextView.text = addressList?.get(0)?.thoroughfare.orEmpty()
                        }

                    } catch (e : Exception) {
                        e.printStackTrace()
                    }


                }.start()



                val retrofit = Retrofit.Builder()
                    .baseUrl("http://apis.data.go.kr/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create<WeatherService>()
                val baseDateTime = BaseDateTime.getBaseDateTime()
                val converter = GeoPointConverter()
                val point = converter.convert(lat = location.latitude, lon = location.longitude)

                service.getVillageForecast(
                    serviceKey = "7IPyIZpVDQ1qMeC9/wbBSRX32BG0zCqNV0PyG2KAeOEKSjSk+aVXPAobsOyfx5y3jzVq4HIM4wbjRfE0m3+lqw==",
                    baseDate = baseDateTime.baseDate,
                    baseTime = baseDateTime.baseTime,
                    nx = point.nx,
                    ny = point.ny
                ).enqueue(object : Callback<WeatherEntity> {
                    override fun onResponse(
                        call: Call<WeatherEntity>,
                        response: Response<WeatherEntity>
                    ) {

                        val forecastDateTimeMap = mutableMapOf<String, Forecast>()
                        val forecastList =
                            response.body()?.response?.body?.items?.forecastEntities.orEmpty()

                        for (forecast in forecastList) {


                            if (forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] == null) {
                                forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] =
                                    Forecast(
                                        forecastDate = forecast.forecastDate,
                                        forecastTime = forecast.forecastTime
                                    )
                            }

                            forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"]?.apply {
                                when (forecast.category) {
                                    Category.POP -> precipitation = forecast.forecastValue.toInt()
                                    Category.PTY -> precipitationType = transformRainType(forecast)
                                    Category.SKY -> sky = transformSky(forecast)
                                    Category.TMP -> temperature = forecast.forecastValue.toDouble()
                                    else -> {}
                                }
                            }

                        }

                        val list = forecastDateTimeMap.values.toMutableList()
                        list.sortWith { f1, f2 ->
                            val f1DateTime = "${f1.forecastDate}${f1.forecastTime}"
                            val f2DateTime = "${f2.forecastDate}${f2.forecastTime}"

                            return@sortWith f1DateTime.compareTo(f2DateTime)
                        }

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

                        Log.d("forecast", forecastDateTimeMap.toString())
                    }

                    override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                        t.printStackTrace()
                    }

                })
            }
    }
}