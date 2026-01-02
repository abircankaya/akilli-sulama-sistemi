package com.ahmetbircan.akillisulama.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmetbircan.akillisulama.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AkilliSulamaApp(viewModel: MainViewModel = viewModel()) {
    
    val bluetoothDurumu by viewModel.bluetoothDurumu.collectAsState()
    val sensorVerileri by viewModel.sensorVerileri.collectAsState()
    val havaDurumu by viewModel.havaDurumu.collectAsState()
    val eslesmisCihazlar by viewModel.eslesmisCihazlar.collectAsState()
    val yukleniyor by viewModel.yukleniyor.collectAsState()
    val hataMesaji by viewModel.hataMesaji.collectAsState()
    val seciliCihaz by viewModel.seciliCihaz.collectAsState()
    
    var cihazSecimDialogAcik by remember { mutableStateOf(false) }
    
    // Hata snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(hataMesaji) {
        hataMesaji?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.hatayiTemizle()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🌱 Akıllı Sulama") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Bluetooth durumu
                    IconButton(onClick = { cihazSecimDialogAcik = true }) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth",
                            tint = when (bluetoothDurumu) {
                                BluetoothDurumu.BAGLI -> Color(0xFF4CAF50)
                                BluetoothDurumu.BAGLANIYOR -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bağlantı Durumu Kartı
            item {
                BaglantiDurumuKarti(
                    bluetoothDurumu = bluetoothDurumu,
                    seciliCihaz = seciliCihaz,
                    onBaglanClick = { cihazSecimDialogAcik = true },
                    onKesClick = { viewModel.baglantiKes() }
                )
            }
            
            // Sensör Verileri Kartı
            item {
                SensorKarti(
                    sensorVerileri = sensorVerileri,
                    bluetoothBagli = bluetoothDurumu == BluetoothDurumu.BAGLI,
                    onYenileClick = { viewModel.durumSorgula() }
                )
            }
            
            // Hava Durumu Kartı
            item {
                HavaDurumuKarti(
                    havaDurumu = havaDurumu,
                    yukleniyor = yukleniyor,
                    onYenileClick = { viewModel.havaDurumuGetir() }
                )
            }
            
            // Veri Gönder Butonu
            if (bluetoothDurumu == BluetoothDurumu.BAGLI && havaDurumu.isNotEmpty()) {
                item {
                    Button(
                        onClick = { viewModel.verileriArduinoyaGonder() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Verileri Arduino'ya Gönder")
                    }
                }
            }
            
            // Haftalık Plan
            if (havaDurumu.isNotEmpty()) {
                item {
                    Text(
                        "📅 Haftalık Sulama Planı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(havaDurumu) { gun ->
                    HaftalikPlanSatiri(gun)
                }
            }
        }
    }
    
    // Cihaz Seçim Dialog
    if (cihazSecimDialogAcik) {
        CihazSecimDialog(
            cihazlar = eslesmisCihazlar,
            onCihazSec = { cihaz ->
                viewModel.cihazaBaglan(cihaz)
                cihazSecimDialogAcik = false
            },
            onKapat = { cihazSecimDialogAcik = false },
            onYenile = { viewModel.cihazlariYenile() }
        )
    }
}

@Composable
fun BaglantiDurumuKarti(
    bluetoothDurumu: BluetoothDurumu,
    seciliCihaz: BluetoothDevice?,
    onBaglanClick: () -> Unit,
    onKesClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (bluetoothDurumu) {
                BluetoothDurumu.BAGLI -> Color(0xFFE8F5E9)
                BluetoothDurumu.BAGLANIYOR -> Color(0xFFFFF8E1)
                else -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Durum ikonu
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (bluetoothDurumu) {
                            BluetoothDurumu.BAGLI -> Color(0xFF4CAF50)
                            BluetoothDurumu.BAGLANIYOR -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Durum metni
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (bluetoothDurumu) {
                        BluetoothDurumu.BAGLI -> "Bağlı"
                        BluetoothDurumu.BAGLANIYOR -> "Bağlanıyor..."
                        BluetoothDurumu.ARANIYOR -> "Aranıyor..."
                        BluetoothDurumu.HATA -> "Hata!"
                        else -> "Bağlı Değil"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                @SuppressLint("MissingPermission")
                if (seciliCihaz != null) {
                    Text(
                        seciliCihaz.name ?: "Bilinmeyen Cihaz",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            
            // Buton
            if (bluetoothDurumu == BluetoothDurumu.BAGLI) {
                TextButton(onClick = onKesClick) {
                    Text("Kes", color = Color.Red)
                }
            } else {
                TextButton(onClick = onBaglanClick) {
                    Text("Bağlan")
                }
            }
        }
    }
}

@Composable
fun SensorKarti(
    sensorVerileri: SensorVerileri,
    bluetoothBagli: Boolean,
    onYenileClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📊 Sensör Verileri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (bluetoothBagli) {
                    IconButton(onClick = onYenileClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (!bluetoothBagli) {
                Text(
                    "Bluetooth bağlantısı gerekli",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Nem
                    SensorDeger(
                        baslik = "💧 Nem",
                        deger = sensorVerileri.nemDegeri.toString(),
                        durum = if (sensorVerileri.nemDegeri > 600) "Kuru" else "Islak",
                        renk = if (sensorVerileri.nemDegeri > 600) Color(0xFFFF9800) else Color(0xFF2196F3)
                    )
                    
                    // Işık
                    SensorDeger(
                        baslik = "☀️ Işık",
                        deger = sensorVerileri.isikDegeri.toString(),
                        durum = if (sensorVerileri.isikDegeri > 600) "Gece" else "Gündüz",
                        renk = if (sensorVerileri.isikDegeri > 600) Color(0xFF673AB7) else Color(0xFFFFC107)
                    )
                    
                    // Pompa
                    SensorDeger(
                        baslik = "💦 Pompa",
                        deger = if (sensorVerileri.pompaDurumu) "AÇIK" else "KAPALI",
                        durum = "",
                        renk = if (sensorVerileri.pompaDurumu) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                    )
                }
            }
        }
    }
}

@Composable
fun SensorDeger(baslik: String, deger: String, durum: String, renk: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(baslik, fontSize = 12.sp, color = Color.Gray)
        Text(
            deger,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = renk
        )
        if (durum.isNotEmpty()) {
            Text(durum, fontSize = 12.sp, color = renk)
        }
    }
}

@Composable
fun HavaDurumuKarti(
    havaDurumu: List<GunlukHavaDurumu>,
    yukleniyor: Boolean,
    onYenileClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🌤️ Hava Durumu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onYenileClick, enabled = !yukleniyor) {
                    if (yukleniyor) {
                        Text("⏳", fontSize = 20.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (havaDurumu.isEmpty()) {
                Text(
                    "Hava durumu verilerini çekmek için yenile butonuna tıklayın",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(havaDurumu) { gun ->
                        HavaDurumuGunKarti(gun)
                    }
                }
            }
        }
    }
}

@Composable
fun HavaDurumuGunKarti(gun: GunlukHavaDurumu) {
    Card(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (gun.yagisOlasiligi > 50) 
                Color(0xFFE3F2FD) else Color(0xFFFFF8E1)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarih
            Text(
                gun.tarih.takeLast(5), // MM-DD
                fontSize = 12.sp,
                color = Color.Gray
            )
            
            // Hava ikonu
            Text(
                if (gun.yagisOlasiligi > 50) "🌧️" else "☀️",
                fontSize = 32.sp
            )
            
            // Sıcaklık
            Text(
                "${gun.sicaklikMax}°",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Yağış olasılığı
            Text(
                "%${gun.yagisOlasiligi}",
                fontSize = 12.sp,
                color = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
fun HaftalikPlanSatiri(gun: GunlukHavaDurumu) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (gun.sulamaOnerisi) 
                Color(0xFFE8F5E9) else Color(0xFFFAFAFA)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tarih
            Text(
                gun.tarih,
                modifier = Modifier.width(100.dp),
                fontWeight = FontWeight.Medium
            )
            
            // Hava
            Text(
                if (gun.yagisOlasiligi > 50) "🌧️" else "☀️",
                modifier = Modifier.width(40.dp)
            )
            
            // Sıcaklık
            Text(
                "${gun.sicaklikMin}°/${gun.sicaklikMax}°",
                modifier = Modifier.width(70.dp)
            )
            
            // Yağış
            Text(
                "%${gun.yagisOlasiligi}",
                modifier = Modifier.width(50.dp),
                color = Color(0xFF2196F3)
            )
            
            Spacer(Modifier.weight(1f))
            
            // Sulama önerisi
            Text(
                if (gun.sulamaOnerisi) "💧 SULA" else "⏸️ BEKLEME",
                fontWeight = FontWeight.Bold,
                color = if (gun.sulamaOnerisi) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun CihazSecimDialog(
    cihazlar: List<BluetoothDevice>,
    onCihazSec: (BluetoothDevice) -> Unit,
    onKapat: () -> Unit,
    onYenile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKapat,
        title = { Text("Bluetooth Cihaz Seç") },
        text = {
            Column {
                if (cihazlar.isEmpty()) {
                    Text("Eşleşmiş cihaz bulunamadı.\nÖnce telefon ayarlarından Bluetooth ile eşleştirin.")
                } else {
                    cihazlar.forEach { cihaz ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCihazSec(cihaz) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    cihaz.name ?: "Bilinmeyen",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    cihaz.address,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Divider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onYenile) {
                Text("Yenile")
            }
        },
        dismissButton = {
            TextButton(onClick = onKapat) {
                Text("Kapat")
            }
        }
    )
}
