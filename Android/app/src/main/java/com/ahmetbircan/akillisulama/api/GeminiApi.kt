package com.ahmetbircan.akillisulama.api

import com.ahmetbircan.akillisulama.data.GeminiRequest
import com.ahmetbircan.akillisulama.data.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApi {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
        const val API_KEY = "AIzaSyAi00h1SQicjDCCAW34jXBJrVjLX2vpl88"
        
        fun createPrompt(mahsulAdi: String, mevsim: String): String {
            return """
Sen bir tarım uzmanısın. Verilen ismin gerçek bir bitki/mahsul olup olmadığını kontrol et ve sulama bilgisi ver.

Girilen İsim: $mahsulAdi
Mevcut Mevsim: $mevsim

ÖNEMLİ: Önce "$mahsulAdi" ifadesinin gerçek bir bitki, sebze, meyve veya tarımsal ürün olup olmadığını kontrol et.
- Eğer gerçek bir mahsul DEĞİLSE (örn: rastgele harfler, anlamsız kelimeler, nesne isimleri): gecerli = false
- Eğer gerçek bir mahsul İSE: gecerli = true ve sulama bilgilerini doldur

SADECE aşağıdaki JSON formatında yanıt ver:
{
    "gecerli": true/false,
    "su_ihtiyaci": "Düşük/Orta/Yüksek",
    "sulama_sikligi": <sayı>,
    "nem_esik": <sayı>,
    "sulama_saati": "HH:MM",
    "sulama_suresi": <sayı>,
    "aciklama": "kısa açıklama",
    "mevsim_notu": "mevsime özel not"
}

Açıklamalar:
- gecerli: Girilen isim gerçek bir mahsul mü? (true/false)
- su_ihtiyaci: Düşük, Orta veya Yüksek (geçersizse boş bırak)
- sulama_sikligi: Kaç günde bir sulama (geçersizse 0)
- nem_esik: Arduino nem sensörü eşik değeri 0-1023 (geçersizse 0)
- sulama_saati: En uygun sulama saati (geçersizse boş)
- sulama_suresi: Saniye cinsinden sulama süresi (geçersizse 0)
- aciklama: Geçerliyse sulama bilgisi, geçersizse "Geçerli bir mahsul ismi girin"
- mevsim_notu: Mevsime özel not (geçersizse boş)

SADECE JSON döndür, başka açıklama ekleme.
""".trimIndent()
        }
    }
}
