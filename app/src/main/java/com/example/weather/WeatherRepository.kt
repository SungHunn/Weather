package com.example.weather


import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WeatherRepository {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://apis.data.go.kr/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(WeatherService::class.java)

    fun getVillageForecast(
        longitude: Double,
        latitude: Double,
        successCallBack: (List<Forecast>) -> Unit,
        failureCallBack: (Throwable) -> Unit
    ) {

        val baseDateTime = BaseDateTime.getBaseDateTime()
        val converter = GeoPointConverter()
        val point = converter.convert(lat = latitude, lon = longitude)

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

                if (list.isEmpty()) {
                    failureCallBack(NullPointerException())
                } else {
                    successCallBack(list)
                }



            }

            override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                failureCallBack(t)
            }

        })
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
}