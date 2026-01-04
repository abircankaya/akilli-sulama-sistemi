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
    val sulamaOnerisi: Boolean,
    val sulamaPlani: Boolean = false
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

/**
 * AI'dan gelen mahsul önerisi
 */
data class MahsulOnerisi(
    val mahsulAdi: String = "",
    val suIhtiyaci: String = "",
    val sulamaSikligi: Int = 1,
    val nemEsik: Int = 600,
    val sulamaSaati: String = "22:00",
    val sulamaSuresi: Int = 30,
    val aciklama: String = "",
    val mevsimNotu: String = ""
)

/**
 * Kullanıcı sulama ayarları
 */
data class SulamaAyarlari(
    val mahsulAdi: String = "",
    val sulamaSikligi: Int = 1,
    val sulamaSaati: String = "22:00",
    val sulamaSuresi: Int = 30,
    val nemEsik: Int = 600,
    val kullaniciDegistirdi: Boolean = false
)

/**
 * Gemini API yanıt modeli
 */
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

data class GeminiContent(
    val parts: List<GeminiPart>?
)

data class GeminiPart(
    val text: String?
)

/**
 * Gemini API istek modeli
 */
data class GeminiRequest(
    val contents: List<GeminiRequestContent>
)

data class GeminiRequestContent(
    val parts: List<GeminiRequestPart>
)

data class GeminiRequestPart(
    val text: String
)

/**
 * Fallback mahsul bilgileri - API çalışmazsa kullanılır
 */
object FallbackMahsulBilgileri {

    private val bilgiler = mapOf(
        "domates" to MahsulOnerisi(
            mahsulAdi = "Domates",
            suIhtiyaci = "Yüksek",
            sulamaSikligi = 1,
            nemEsik = 550,
            sulamaSaati = "21:00",
            sulamaSuresi = 45,
            aciklama = "Domates düzenli sulama ister, kök çürümesini önlemek için akşam sulayın.",
            mevsimNotu = "Yaz aylarında günde 2 kez sulama gerekebilir."
        ),
        "biber" to MahsulOnerisi(
            mahsulAdi = "Biber",
            suIhtiyaci = "Orta-Yüksek",
            sulamaSikligi = 1,
            nemEsik = 580,
            sulamaSaati = "21:00",
            sulamaSuresi = 40,
            aciklama = "Biber düzenli nem sever, aşırı sulamadan kaçının.",
            mevsimNotu = "Sıcak havalarda sabah da hafif sulama yapılabilir."
        ),
        "salatalık" to MahsulOnerisi(
            mahsulAdi = "Salatalık",
            suIhtiyaci = "Yüksek",
            sulamaSikligi = 1,
            nemEsik = 500,
            sulamaSaati = "20:00",
            sulamaSuresi = 50,
            aciklama = "Salatalık çok su ister, toprak sürekli nemli tutulmalı.",
            mevsimNotu = "Yaz aylarında günde 2 kez sulama şart."
        ),
        "marul" to MahsulOnerisi(
            mahsulAdi = "Marul",
            suIhtiyaci = "Orta",
            sulamaSikligi = 2,
            nemEsik = 600,
            sulamaSaati = "22:00",
            sulamaSuresi = 30,
            aciklama = "Marul serin ve nemli ortam sever, yaprakları ıslatmayın.",
            mevsimNotu = "Sıcak havalarda gölgede tutun."
        ),
        "havuç" to MahsulOnerisi(
            mahsulAdi = "Havuç",
            suIhtiyaci = "Orta",
            sulamaSikligi = 2,
            nemEsik = 620,
            sulamaSaati = "21:00",
            sulamaSuresi = 35,
            aciklama = "Havuç derin sulama ister, yüzeysel sulama kök gelişimini engeller.",
            mevsimNotu = "Hasat öncesi sulamayı azaltın."
        ),
        "soğan" to MahsulOnerisi(
            mahsulAdi = "Soğan",
            suIhtiyaci = "Düşük-Orta",
            sulamaSikligi = 3,
            nemEsik = 680,
            sulamaSaati = "22:00",
            sulamaSuresi = 25,
            aciklama = "Soğan fazla suyu sevmez, aşırı sulama çürümeye neden olur.",
            mevsimNotu = "Hasat öncesi 2 hafta sulamayı kesin."
        ),
        "patates" to MahsulOnerisi(
            mahsulAdi = "Patates",
            suIhtiyaci = "Orta",
            sulamaSikligi = 2,
            nemEsik = 600,
            sulamaSaati = "21:00",
            sulamaSuresi = 40,
            aciklama = "Patates düzenli nem ister, yumru oluşumu sırasında sulama kritik.",
            mevsimNotu = "Çiçeklenme döneminde sulamayı artırın."
        ),
        "patlıcan" to MahsulOnerisi(
            mahsulAdi = "Patlıcan",
            suIhtiyaci = "Yüksek",
            sulamaSikligi = 1,
            nemEsik = 550,
            sulamaSaati = "21:00",
            sulamaSuresi = 45,
            aciklama = "Patlıcan bol su ve sıcak sever, düzenli sulama şart.",
            mevsimNotu = "Meyve döneminde sulamayı aksatmayın."
        ),
        "fasulye" to MahsulOnerisi(
            mahsulAdi = "Fasulye",
            suIhtiyaci = "Orta",
            sulamaSikligi = 2,
            nemEsik = 620,
            sulamaSaati = "21:00",
            sulamaSuresi = 30,
            aciklama = "Fasulye çiçeklenme ve bakla döneminde suya ihtiyaç duyar.",
            mevsimNotu = "Yaprakları ıslatmaktan kaçının."
        ),
        "kavun" to MahsulOnerisi(
            mahsulAdi = "Kavun",
            suIhtiyaci = "Orta-Yüksek",
            sulamaSikligi = 2,
            nemEsik = 580,
            sulamaSaati = "20:00",
            sulamaSuresi = 45,
            aciklama = "Kavun derin kök yaptığı için derin sulama tercih edin.",
            mevsimNotu = "Olgunlaşma döneminde sulamayı azaltın, tat artar."
        ),
        "karpuz" to MahsulOnerisi(
            mahsulAdi = "Karpuz",
            suIhtiyaci = "Yüksek",
            sulamaSikligi = 1,
            nemEsik = 550,
            sulamaSaati = "20:00",
            sulamaSuresi = 50,
            aciklama = "Karpuz çok su ister, özellikle meyve büyütme döneminde.",
            mevsimNotu = "Hasat öncesi 1 hafta sulamayı kesin."
        ),
        "çilek" to MahsulOnerisi(
            mahsulAdi = "Çilek",
            suIhtiyaci = "Orta-Yüksek",
            sulamaSikligi = 1,
            nemEsik = 550,
            sulamaSaati = "08:00",
            sulamaSuresi = 30,
            aciklama = "Çilek sabah sulanmalı, meyvelerin ıslanmaması önemli.",
            mevsimNotu = "Damla sulama en ideal yöntemdir."
        )
    )

    /**
     * Mahsul adına göre fallback bilgi getir
     */
    fun bilgiGetir(mahsulAdi: String): MahsulOnerisi? {
        val aramaAdi = mahsulAdi.lowercase()
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")

        return bilgiler.entries.find { (key, _) ->
            val normalizedKey = key.lowercase()
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c")
            aramaAdi.contains(normalizedKey) || normalizedKey.contains(aramaAdi)
        }?.value?.copy(mahsulAdi = mahsulAdi)
    }

}
