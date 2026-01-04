package com.ahmetbircan.akillisulama.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetbircan.akillisulama.api.GeminiApi
import com.ahmetbircan.akillisulama.api.WeatherApiClient
import com.ahmetbircan.akillisulama.bluetooth.BluetoothManager
import com.ahmetbircan.akillisulama.data.*
import com.ahmetbircan.akillisulama.location.Konum
import com.ahmetbircan.akillisulama.location.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothManager(application)
    private val locationHelper = LocationHelper(application)
    private val sharedPrefs = application.getSharedPreferences("sulama_ayarlari", Context.MODE_PRIVATE)

    // Mevcut konum
    private val _mevcutKonum = MutableStateFlow(LocationHelper.VARSAYILAN_KONUM)
    val mevcutKonum: StateFlow<Konum> = _mevcutKonum
    
    // Gemini API
    private val geminiApi: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(GeminiApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }
    
    // Bluetooth durumları
    val bluetoothDurumu = bluetoothManager.durum
    val sensorVerileri = bluetoothManager.sensorVerileri
    val eslesmisCihazlar = bluetoothManager.eslesmisCihazlar
    
    // Hava durumu
    private val _havaDurumu = MutableStateFlow<List<GunlukHavaDurumu>>(emptyList())
    val havaDurumu: StateFlow<List<GunlukHavaDurumu>> = _havaDurumu
    
    // Yükleniyor durumu
    private val _yukleniyor = MutableStateFlow(false)
    val yukleniyor: StateFlow<Boolean> = _yukleniyor
    
    // AI yükleniyor durumu
    private val _aiYukleniyor = MutableStateFlow(false)
    val aiYukleniyor: StateFlow<Boolean> = _aiYukleniyor
    
    // Hata mesajı
    private val _hataMesaji = MutableStateFlow<String?>(null)
    val hataMesaji: StateFlow<String?> = _hataMesaji
    
    // Seçili cihaz
    private val _seciliCihaz = MutableStateFlow<BluetoothDevice?>(null)
    val seciliCihaz: StateFlow<BluetoothDevice?> = _seciliCihaz
    
    // Mahsul önerisi (AI'dan)
    private val _mahsulOnerisi = MutableStateFlow(MahsulOnerisi())
    val mahsulOnerisi: StateFlow<MahsulOnerisi> = _mahsulOnerisi
    
    // Kullanıcı sulama ayarları
    private val _sulamaAyarlari = MutableStateFlow(SulamaAyarlari())
    val sulamaAyarlari: StateFlow<SulamaAyarlari> = _sulamaAyarlari
    
    init {
        // Eşleşmiş cihazları getir
        bluetoothManager.eslesmiscihazlariGetir()
        // Kayıtlı ayarları yükle
        kayitliAyarlariYukle()
    }
    
    /**
     * Kayıtlı sulama ayarlarını yükle
     */
    private fun kayitliAyarlariYukle() {
        val mahsul = sharedPrefs.getString("mahsul", "") ?: ""
        if (mahsul.isNotEmpty()) {
            _sulamaAyarlari.value = SulamaAyarlari(
                mahsulAdi = mahsul,
                sulamaSikligi = sharedPrefs.getInt("siklik", 1),
                sulamaSaati = sharedPrefs.getString("saat", "22:00") ?: "22:00",
                sulamaSuresi = sharedPrefs.getInt("sure", 30),
                nemEsik = sharedPrefs.getInt("nem_esik", 600),
                kullaniciDegistirdi = sharedPrefs.getBoolean("kullanici_degistirdi", false)
            )
        }
    }
    
    /**
     * Sulama ayarlarını kaydet
     */
    fun ayarlariKaydet(ayarlar: SulamaAyarlari) {
        _sulamaAyarlari.value = ayarlar
        
        sharedPrefs.edit().apply {
            putString("mahsul", ayarlar.mahsulAdi)
            putInt("siklik", ayarlar.sulamaSikligi)
            putString("saat", ayarlar.sulamaSaati)
            putInt("sure", ayarlar.sulamaSuresi)
            putInt("nem_esik", ayarlar.nemEsik)
            putBoolean("kullanici_degistirdi", ayarlar.kullaniciDegistirdi)
            apply()
        }
        
        _hataMesaji.value = "Ayarlar kaydedildi!"
    }
    
    /**
     * AI'dan mahsul bilgisi al (fallback destekli)
     */
    fun mahsulBilgisiGetir(mahsulAdi: String) {
        if (mahsulAdi.isBlank()) {
            _hataMesaji.value = "Lütfen mahsul adı girin!"
            return
        }

        viewModelScope.launch {
            _aiYukleniyor.value = true
            _hataMesaji.value = null

            try {
                val mevsim = Mevsim.simdiGetir().isim
                val prompt = GeminiApi.createPrompt(mahsulAdi, mevsim)

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiRequestContent(
                            parts = listOf(GeminiRequestPart(text = prompt))
                        )
                    )
                )

                val response = geminiApi.generateContent(
                    apiKey = GeminiApi.API_KEY,
                    request = request
                )

                val text = response.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text ?: ""

                // JSON parse et
                val oneri = parseAiResponse(mahsulAdi, text)

                if (oneri == null) {
                    // Geçersiz mahsul ismi
                    _hataMesaji.value = "❌ '$mahsulAdi' geçerli bir mahsul ismi değil. Lütfen gerçek bir bitki/sebze/meyve adı girin."
                } else {
                    oneriUygula(oneri, "🤖 AI önerisi alındı")
                }

            } catch (e: HttpException) {
                // API hatası - fallback kullan (sadece bilinen mahsuller için)
                val fallbackOneri = FallbackMahsulBilgileri.bilgiGetir(mahsulAdi)
                if (fallbackOneri != null) {
                    oneriUygula(fallbackOneri.copy(aciklama = fallbackOneri.aciklama + " (Çevrimdışı bilgi)"), "📋 Hazır bilgi kullanıldı")
                } else {
                    _hataMesaji.value = "❌ API bağlantısı başarısız ve '$mahsulAdi' hazır bilgilerde bulunamadı."
                }

            } catch (e: Exception) {
                // Diğer hatalar - fallback kullan (sadece bilinen mahsuller için)
                val fallbackOneri = FallbackMahsulBilgileri.bilgiGetir(mahsulAdi)
                if (fallbackOneri != null) {
                    oneriUygula(fallbackOneri.copy(aciklama = fallbackOneri.aciklama + " (Çevrimdışı bilgi)"), "📋 Hazır bilgi kullanıldı")
                } else {
                    _hataMesaji.value = "❌ Hata oluştu ve '$mahsulAdi' hazır bilgilerde bulunamadı."
                }
            }

            _aiYukleniyor.value = false
        }
    }

    /**
     * Öneriyi uygula
     */
    private fun oneriUygula(oneri: MahsulOnerisi, mesaj: String) {
        _mahsulOnerisi.value = oneri

        if (!_sulamaAyarlari.value.kullaniciDegistirdi) {
            _sulamaAyarlari.value = SulamaAyarlari(
                mahsulAdi = oneri.mahsulAdi,
                sulamaSikligi = oneri.sulamaSikligi,
                sulamaSaati = oneri.sulamaSaati,
                sulamaSuresi = oneri.sulamaSuresi,
                nemEsik = oneri.nemEsik,
                kullaniciDegistirdi = false
            )
        }

        _hataMesaji.value = "$mesaj: ${oneri.mahsulAdi}"
    }
    
    /**
     * AI yanıtını parse et
     * Geçersiz mahsul ismi için null döndürür
     */
    private fun parseAiResponse(mahsulAdi: String, text: String): MahsulOnerisi? {
        return try {
            // JSON kısmını bul
            val jsonStart = text.indexOf('{')
            val jsonEnd = text.lastIndexOf('}') + 1
            val jsonStr = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                text.substring(jsonStart, jsonEnd)
            } else {
                text
            }

            val json = JSONObject(jsonStr)

            // Geçerli mahsul kontrolü
            val gecerli = json.optBoolean("gecerli", true)
            if (!gecerli) {
                return null // Geçersiz mahsul ismi
            }

            MahsulOnerisi(
                mahsulAdi = mahsulAdi,
                suIhtiyaci = json.optString("su_ihtiyaci", "Orta"),
                sulamaSikligi = json.optInt("sulama_sikligi", 1),
                nemEsik = json.optInt("nem_esik", 600),
                sulamaSaati = json.optString("sulama_saati", "22:00"),
                sulamaSuresi = json.optInt("sulama_suresi", 30),
                aciklama = json.optString("aciklama", ""),
                mevsimNotu = json.optString("mevsim_notu", "")
            )
        } catch (e: Exception) {
            // Parse hatası durumunda varsayılan değerler
            MahsulOnerisi(
                mahsulAdi = mahsulAdi,
                suIhtiyaci = "Orta",
                sulamaSikligi = 1,
                nemEsik = 600,
                aciklama = "AI yanıtı işlenemedi, varsayılan değerler kullanılıyor."
            )
        }
    }
    
    /**
     * Hava durumu verilerini çek (konum bazlı)
     */
    fun havaDurumuGetir() {
        viewModelScope.launch {
            _yukleniyor.value = true
            _hataMesaji.value = null

            try {
                // Önce konumu al
                val konum = locationHelper.konumAl()
                _mevcutKonum.value = konum

                // Konum ile hava durumu çek
                val response = WeatherApiClient.api.getHavaDurumu(
                    latitude = konum.latitude,
                    longitude = konum.longitude
                )
                val ayarlar = _sulamaAyarlari.value

                val gunler = response.daily.time.mapIndexed { index, tarih ->
                    val yagisOlasiligi = response.daily.precipitation_probability_max.getOrNull(index) ?: 0
                    val sicaklikMax = response.daily.temperature_2m_max.getOrNull(index)?.toInt() ?: 0

                    // Sulama planı: Sıklığa göre belirle
                    val sulamaGunu = (index % ayarlar.sulamaSikligi) == 0

                    GunlukHavaDurumu(
                        tarih = tarih,
                        sicaklikMax = sicaklikMax,
                        sicaklikMin = response.daily.temperature_2m_min.getOrNull(index)?.toInt() ?: 0,
                        yagisOlasiligi = yagisOlasiligi,
                        yagisMiktari = response.daily.precipitation_sum.getOrNull(index) ?: 0.0,
                        sulamaOnerisi = sulamaKarari(yagisOlasiligi, sicaklikMax, ayarlar.nemEsik),
                        sulamaPlani = sulamaGunu && yagisOlasiligi < 50
                    )
                }

                _havaDurumu.value = gunler

                // Konum bilgisi göster
                val konumMesaj = if (konum == LocationHelper.VARSAYILAN_KONUM) {
                    "📍 Varsayılan konum (Ankara)"
                } else {
                    "📍 Mevcut konum (${String.format("%.2f", konum.latitude)}, ${String.format("%.2f", konum.longitude)})"
                }
                _hataMesaji.value = "$konumMesaj - Hava durumu güncellendi"

            } catch (e: Exception) {
                _hataMesaji.value = "Hava durumu alınamadı: ${e.message}"
            }

            _yukleniyor.value = false
        }
    }
    
    /**
     * ML tabanlı sulama kararı (nem eşiği dahil)
     */
    private fun sulamaKarari(yagisOlasiligi: Int, sicaklik: Int, nemEsik: Int): Boolean {
        // Yağış bekleniyorsa sulama
        if (yagisOlasiligi > 50) return false
        
        // Nem eşiğine göre karar (düşük eşik = daha sık sulama)
        val sulamaAgressifligi = when {
            nemEsik <= 500 -> 30  // Yüksek su ihtiyacı
            nemEsik <= 600 -> 25  // Orta su ihtiyacı
            else -> 15            // Düşük su ihtiyacı
        }
        
        // Yaz aylarında ve yağış düşükse sula
        val mevsim = Mevsim.simdiGetir()
        if (mevsim == Mevsim.YAZ && yagisOlasiligi < sulamaAgressifligi) return true
        
        // Diğer mevsimlerde yağış çok düşükse sula
        if (yagisOlasiligi < sulamaAgressifligi - 10) return true
        
        return false
    }
    
    /**
     * Bluetooth cihazına bağlan
     */
    fun cihazaBaglan(device: BluetoothDevice) {
        viewModelScope.launch {
            _seciliCihaz.value = device
            val basarili = bluetoothManager.baglan(device)
            if (!basarili) {
                _hataMesaji.value = "Bağlantı başarısız!"
            }
        }
    }
    
    /**
     * Bluetooth bağlantısını kes
     */
    fun baglantiKes() {
        bluetoothManager.kapat()
        _seciliCihaz.value = null
    }
    
    /**
     * Hava durumu verilerini Arduino'ya gönder
     */
    fun verileriArduinoyaGonder() {
        viewModelScope.launch {
            val gunler = _havaDurumu.value
            if (gunler.isEmpty()) {
                _hataMesaji.value = "Önce hava durumu verilerini çekin!"
                return@launch
            }
            
            // Yağış olasılıklarını gönder
            val yagislar = gunler.map { it.yagisOlasiligi }
            val yagisBasarili = bluetoothManager.yagisVerileriGonder(yagislar)
            
            // Sıcaklıkları gönder
            val sicakliklar = gunler.map { it.sicaklikMax }
            val sicaklikBasarili = bluetoothManager.sicaklikVerileriGonder(sicakliklar)
            
            // Mevsimi gönder
            val mevsim = Mevsim.simdiGetir()
            val mevsimBasarili = bluetoothManager.mevsimGonder(mevsim.kod)
            
            // Nem eşiğini gönder (yeni!)
            val nemEsik = _sulamaAyarlari.value.nemEsik
            val nemBasarili = bluetoothManager.nemEsikGonder(nemEsik)
            
            if (yagisBasarili && sicaklikBasarili && mevsimBasarili) {
                _hataMesaji.value = "Veriler başarıyla gönderildi!"
            } else {
                _hataMesaji.value = "Veri gönderme hatası!"
            }
        }
    }
    
    /**
     * Sensör durumunu sorgula
     */
    fun durumSorgula() {
        viewModelScope.launch {
            bluetoothManager.durumSorgula()
        }
    }
    
    /**
     * Eşleşmiş cihazları yenile
     */
    fun cihazlariYenile() {
        bluetoothManager.eslesmiscihazlariGetir()
    }
    
    /**
     * Hata mesajını temizle
     */
    fun hatayiTemizle() {
        _hataMesaji.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothManager.kapat()
    }
}
