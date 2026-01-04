package com.ahmetbircan.akillisulama.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Konum verisi
 */
data class Konum(
    val latitude: Double,
    val longitude: Double,
    val sehir: String = ""
)

/**
 * Konum yardımcı sınıfı
 */
class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        // Varsayılan konum (Ankara)
        val VARSAYILAN_KONUM = Konum(39.93, 32.86, "Ankara")
    }

    /**
     * Konum izni var mı kontrol et
     */
    fun izinVarMi(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Konum servislerini açık mı kontrol et
     */
    fun konumAcikMi(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Mevcut konumu al
     */
    suspend fun konumAl(): Konum {
        if (!izinVarMi()) {
            return VARSAYILAN_KONUM
        }

        if (!konumAcikMi()) {
            return VARSAYILAN_KONUM
        }

        return try {
            val location = getCurrentLocation()
            if (location != null) {
                Konum(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            } else {
                VARSAYILAN_KONUM
            }
        } catch (e: Exception) {
            e.printStackTrace()
            VARSAYILAN_KONUM
        }
    }

    /**
     * FusedLocationProvider ile konum al
     */
    @Suppress("MissingPermission")
    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            cont.resume(location)
        }.addOnFailureListener {
            cont.resume(null)
        }

        cont.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }
}
