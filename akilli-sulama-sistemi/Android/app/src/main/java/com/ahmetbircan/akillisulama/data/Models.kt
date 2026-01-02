package com.ahmetbircan.akillisulama.data

/**
 * Open-Meteo API yanıt modeli
 */
data class WeatherResponse(
    val daily: DailyWeather
)

data class DailyWeather(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_probability_max: List<Int>,
    val precipitation_sum: List<Double>
)

/**
 * Uygulama içi hava durumu modeli
 */
data class GunlukHavaDurumu(
    val tarih: String,
    val sicaklikMax: Int,
    val sicaklikMin: Int,
    val yagisOlasiligi: Int,
    val yagisMiktari: Double,
    val sulamaOnerisi: Boolean
)

/**
 * Arduino'dan gelen sensör verileri
 */
data class SensorVerileri(
    val nemDegeri: Int = 0,
    val isikDegeri: Int = 0,
    val pompaDurumu: Boolean = false,
    val gunIndex: Int = 0,
    val yagisOlasiligi: Int = 0
)

/**
 * Bluetooth bağlantı durumu
 */
enum class BluetoothDurumu {
    BAGLI_DEGIL,
    ARANIYOR,
    BAGLANIYOR,
    BAGLI,
    HATA
}

/**
 * Mevsim enum
 */
enum class Mevsim(val kod: Int, val isim: String) {
    ILKBAHAR(1, "İlkbahar"),
    YAZ(2, "Yaz"),
    SONBAHAR(3, "Sonbahar"),
    KIS(4, "Kış");
    
    companion object {
        fun simdiGetir(): Mevsim {
            val ay = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
            return when (ay) {
                in 3..5 -> ILKBAHAR
                in 6..8 -> YAZ
                in 9..11 -> SONBAHAR
                else -> KIS
            }
        }
    }
}
