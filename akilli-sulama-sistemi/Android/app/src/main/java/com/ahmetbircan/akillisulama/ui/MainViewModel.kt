package com.ahmetbircan.akillisulama.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetbircan.akillisulama.api.WeatherApiClient
import com.ahmetbircan.akillisulama.bluetooth.BluetoothManager
import com.ahmetbircan.akillisulama.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bluetoothManager = BluetoothManager(application)
    
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
    
    // Hata mesajı
    private val _hataMesaji = MutableStateFlow<String?>(null)
    val hataMesaji: StateFlow<String?> = _hataMesaji
    
    // Seçili cihaz
    private val _seciliCihaz = MutableStateFlow<BluetoothDevice?>(null)
    val seciliCihaz: StateFlow<BluetoothDevice?> = _seciliCihaz
    
    init {
        // Eşleşmiş cihazları getir
        bluetoothManager.eslesmiscihazlariGetir()
    }
    
    /**
     * Hava durumu verilerini çek
     */
    fun havaDurumuGetir() {
        viewModelScope.launch {
            _yukleniyor.value = true
            _hataMesaji.value = null
            
            try {
                val response = WeatherApiClient.api.getHavaDurumu()
                
                val gunler = response.daily.time.mapIndexed { index, tarih ->
                    val yagisOlasiligi = response.daily.precipitation_probability_max.getOrNull(index) ?: 0
                    val sicaklikMax = response.daily.temperature_2m_max.getOrNull(index)?.toInt() ?: 0
                    
                    GunlukHavaDurumu(
                        tarih = tarih,
                        sicaklikMax = sicaklikMax,
                        sicaklikMin = response.daily.temperature_2m_min.getOrNull(index)?.toInt() ?: 0,
                        yagisOlasiligi = yagisOlasiligi,
                        yagisMiktari = response.daily.precipitation_sum.getOrNull(index) ?: 0.0,
                        sulamaOnerisi = sulamaKarari(yagisOlasiligi, sicaklikMax)
                    )
                }
                
                _havaDurumu.value = gunler
                
            } catch (e: Exception) {
                _hataMesaji.value = "Hava durumu alınamadı: ${e.message}"
            }
            
            _yukleniyor.value = false
        }
    }
    
    /**
     * ML tabanlı sulama kararı
     */
    private fun sulamaKarari(yagisOlasiligi: Int, sicaklik: Int): Boolean {
        // Yağış bekleniyorsa sulama
        if (yagisOlasiligi > 50) return false
        
        // Yaz aylarında ve yağış yoksa sula
        val mevsim = Mevsim.simdiGetir()
        if (mevsim == Mevsim.YAZ && yagisOlasiligi < 30) return true
        
        // Diğer mevsimlerde yağış çok düşükse sula
        if (yagisOlasiligi < 20) return true
        
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
