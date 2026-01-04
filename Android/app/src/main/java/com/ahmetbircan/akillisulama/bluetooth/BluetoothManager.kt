package com.ahmetbircan.akillisulama.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.ahmetbircan.akillisulama.data.BluetoothDurumu
import com.ahmetbircan.akillisulama.data.SensorVerileri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Bluetooth bağlantı yöneticisi
 * HC-05/BC417 modülü ile iletişim
 */
class BluetoothManager(private val context: Context) {
    
    companion object {
        // HC-05/BC417 için standart SPP UUID
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
    
    private val bluetoothManager: BluetoothManager? = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    private val _durum = MutableStateFlow(BluetoothDurumu.BAGLI_DEGIL)
    val durum: StateFlow<BluetoothDurumu> = _durum
    
    private val _sensorVerileri = MutableStateFlow(SensorVerileri())
    val sensorVerileri: StateFlow<SensorVerileri> = _sensorVerileri
    
    private val _eslesmisCihazlar = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val eslesmisCihazlar: StateFlow<List<BluetoothDevice>> = _eslesmisCihazlar
    
    /**
     * Bluetooth açık mı kontrol et
     */
    fun bluetoothAcikMi(): Boolean = bluetoothAdapter?.isEnabled == true
    
    /**
     * Eşleşmiş cihazları listele
     */
    @SuppressLint("MissingPermission")
    fun eslesmiscihazlariGetir() {
        if (!izinKontrol()) return
        
        val cihazlar = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        _eslesmisCihazlar.value = cihazlar
    }
    
    /**
     * Cihaza bağlan
     */
    @SuppressLint("MissingPermission")
    suspend fun baglan(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        if (!izinKontrol()) {
            _durum.value = BluetoothDurumu.HATA
            return@withContext false
        }
        
        _durum.value = BluetoothDurumu.BAGLANIYOR
        
        try {
            // Mevcut bağlantıyı kapat
            kapat()
            
            // Yeni soket oluştur
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()
            
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            
            _durum.value = BluetoothDurumu.BAGLI
            
            // Veri okumaya başla
            veriOku()
            
            true
        } catch (e: IOException) {
            e.printStackTrace()
            _durum.value = BluetoothDurumu.HATA
            false
        }
    }
    
    /**
     * Bağlantıyı kapat
     */
    fun kapat() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        _durum.value = BluetoothDurumu.BAGLI_DEGIL
    }
    
    /**
     * Arduino'ya veri gönder
     */
    suspend fun veriGonder(mesaj: String): Boolean = withContext(Dispatchers.IO) {
        if (_durum.value != BluetoothDurumu.BAGLI) return@withContext false
        
        try {
            outputStream?.write("$mesaj\n".toByteArray())
            outputStream?.flush()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            _durum.value = BluetoothDurumu.HATA
            false
        }
    }
    
    /**
     * Haftalık yağış verilerini gönder
     * Format: W:10,25,30,5,80,0,15
     */
    suspend fun yagisVerileriGonder(yagislar: List<Int>): Boolean {
        val mesaj = "W:" + yagislar.take(7).joinToString(",")
        return veriGonder(mesaj)
    }
    
    /**
     * Haftalık sıcaklık verilerini gönder
     * Format: T:25,26,24,23,22,21,20
     */
    suspend fun sicaklikVerileriGonder(sicakliklar: List<Int>): Boolean {
        val mesaj = "T:" + sicakliklar.take(7).joinToString(",")
        return veriGonder(mesaj)
    }
    
    /**
     * Mevsim bilgisi gönder
     * Format: M:2 (1=İlkbahar, 2=Yaz, 3=Sonbahar, 4=Kış)
     */
    suspend fun mevsimGonder(mevsim: Int): Boolean {
        return veriGonder("M:$mevsim")
    }
    
    /**
     * Durum sorgula
     */
    suspend fun durumSorgula(): Boolean {
        return veriGonder("S:")
    }
    
    /**
     * Nem eşik değeri gönder
     * Format: N:600
     */
    suspend fun nemEsikGonder(esik: Int): Boolean {
        return veriGonder("N:$esik")
    }
    
    /**
     * Arduino'dan gelen verileri oku
     */
    private suspend fun veriOku() = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        var veri = StringBuilder()
        
        while (_durum.value == BluetoothDurumu.BAGLI) {
            try {
                val bytes = inputStream?.read(buffer) ?: -1
                if (bytes > 0) {
                    val okunan = String(buffer, 0, bytes)
                    veri.append(okunan)
                    
                    // Satır sonu geldi mi kontrol et
                    if (veri.contains("\n")) {
                        val satirlar = veri.toString().split("\n")
                        satirlar.dropLast(1).forEach { satir ->
                            veriIsle(satir.trim())
                        }
                        veri = StringBuilder(satirlar.last())
                    }
                }
            } catch (e: IOException) {
                if (_durum.value == BluetoothDurumu.BAGLI) {
                    _durum.value = BluetoothDurumu.HATA
                }
                break
            }
        }
    }
    
    /**
     * Gelen veriyi işle
     * Format: S:nem,ışık,pompa,günIndex,yağış%
     */
    private fun veriIsle(veri: String) {
        if (veri.startsWith("S:")) {
            try {
                val parcalar = veri.substring(2).split(",")
                if (parcalar.size >= 5) {
                    _sensorVerileri.value = SensorVerileri(
                        nemDegeri = parcalar[0].toIntOrNull() ?: 0,
                        isikDegeri = parcalar[1].toIntOrNull() ?: 0,
                        pompaDurumu = parcalar[2] == "1",
                        gunIndex = parcalar[3].toIntOrNull() ?: 0,
                        yagisOlasiligi = parcalar[4].toIntOrNull() ?: 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * İzin kontrolü
     */
    private fun izinKontrol(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
}
