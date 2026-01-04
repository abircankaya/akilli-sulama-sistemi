package com.ahmetbircan.akillisulama.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    val aiYukleniyor by viewModel.aiYukleniyor.collectAsState()
    val hataMesaji by viewModel.hataMesaji.collectAsState()
    val seciliCihaz by viewModel.seciliCihaz.collectAsState()
    val mahsulOnerisi by viewModel.mahsulOnerisi.collectAsState()
    val sulamaAyarlari by viewModel.sulamaAyarlari.collectAsState()
    val aiSulamaPlan by viewModel.aiSulamaPlan.collectAsState()

    var cihazSecimDialogAcik by remember { mutableStateOf(false) }
    var ayarlarDialogAcik by remember { mutableStateOf(false) }
    
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
            // Bağlantı Durumu
            item {
                BaglantiDurumuKarti(
                    bluetoothDurumu = bluetoothDurumu,
                    seciliCihaz = seciliCihaz,
                    onBaglanClick = { cihazSecimDialogAcik = true },
                    onKesClick = { viewModel.baglantiKes() }
                )
            }
            
            // 🌱 Mahsul Seçimi (YENİ!)
            item {
                MahsulSecimKarti(
                    mahsulOnerisi = mahsulOnerisi,
                    sulamaAyarlari = sulamaAyarlari,
                    aiYukleniyor = aiYukleniyor,
                    onMahsulAra = { viewModel.mahsulBilgisiGetir(it) },
                    onAyarlarAc = { ayarlarDialogAcik = true }
                )
            }
            
            // Sensör Verileri
            item {
                SensorKarti(
                    sensorVerileri = sensorVerileri,
                    bluetoothBagli = bluetoothDurumu == BluetoothDurumu.BAGLI,
                    onYenileClick = { viewModel.durumSorgula() }
                )
            }
            
            // Hava Durumu
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📅 Haftalık Sulama Planı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { viewModel.aiSulamaPlanGetir() },
                            enabled = !aiYukleniyor && sulamaAyarlari.mahsulAdi.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (aiYukleniyor) "⏳" else "🤖 AI Plan",
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // AI özeti varsa göster
                if (aiSulamaPlan.ozet.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF3E5F5)
                            )
                        ) {
                            Text(
                                "🤖 ${aiSulamaPlan.ozet}",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                color = Color(0xFF6A1B9A)
                            )
                        }
                    }
                }

                itemsIndexed(havaDurumu) { index, gun ->
                    val aiKarar = aiSulamaPlan.gunler.find { it.gun == index }
                    HaftalikPlanSatiri(gun, sulamaAyarlari, index, havaDurumu, aiKarar)
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
    
    // Ayarlar Dialog
    if (ayarlarDialogAcik) {
        SulamaAyarlariDialog(
            ayarlar = sulamaAyarlari,
            onKaydet = { yeniAyarlar ->
                viewModel.ayarlariKaydet(yeniAyarlar)
                ayarlarDialogAcik = false
            },
            onKapat = { ayarlarDialogAcik = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahsulSecimKarti(
    mahsulOnerisi: MahsulOnerisi,
    sulamaAyarlari: SulamaAyarlari,
    aiYukleniyor: Boolean,
    onMahsulAra: (String) -> Unit,
    onAyarlarAc: () -> Unit
) {
    var mahsulText by remember { mutableStateOf(sulamaAyarlari.mahsulAdi) }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF1F8E9)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🌱 Mahsul Seçimi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )

            Spacer(Modifier.height(12.dp))

            // Mahsul giriş alanı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = mahsulText,
                    onValueChange = { mahsulText = it },
                    label = { Text("Mahsul adı girin", color = Color(0xFF424242)) },
                    placeholder = { Text("örn: Domates, Biber, Marul...", color = Color(0xFF757575)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1B5E20),
                        unfocusedTextColor = Color(0xFF2E7D32),
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF81C784),
                        focusedLabelColor = Color(0xFF2E7D32),
                        unfocusedLabelColor = Color(0xFF424242)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onMahsulAra(mahsulText.trim())
                        }
                    )
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onMahsulAra(mahsulText.trim())
                    },
                    enabled = !aiYukleniyor && mahsulText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (aiYukleniyor) "⏳" else "🤖 AI",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // AI Önerisi göster
            if (mahsulOnerisi.mahsulAdi.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🤖 AI Önerisi: ${mahsulOnerisi.mahsulAdi}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20),
                                fontSize = 16.sp
                            )

                            IconButton(onClick = onAyarlarAc) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Ayarlar",
                                    tint = Color(0xFF424242)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AiOneriItem("💧", "Su", mahsulOnerisi.suIhtiyaci)
                            AiOneriItem("📅", "Sıklık", "${mahsulOnerisi.sulamaSikligi} gün")
                            AiOneriItem("⏰", "Saat", mahsulOnerisi.sulamaSaati)
                            AiOneriItem("⏱️", "Süre", "${mahsulOnerisi.sulamaSuresi}s")
                        }

                        if (mahsulOnerisi.aciklama.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                mahsulOnerisi.aciklama,
                                fontSize = 13.sp,
                                color = Color(0xFF424242)
                            )
                        }

                        if (mahsulOnerisi.mevsimNotu.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "📌 ${mahsulOnerisi.mevsimNotu}",
                                fontSize = 13.sp,
                                color = Color(0xFF1565C0),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiOneriItem(emoji: String, baslik: String, deger: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(baslik, fontSize = 11.sp, color = Color(0xFF616161), fontWeight = FontWeight.Medium)
        Text(deger, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SulamaAyarlariDialog(
    ayarlar: SulamaAyarlari,
    onKaydet: (SulamaAyarlari) -> Unit,
    onKapat: () -> Unit
) {
    var siklik by remember { mutableStateOf(ayarlar.sulamaSikligi.toString()) }
    var saat by remember { mutableStateOf(ayarlar.sulamaSaati) }
    var sure by remember { mutableStateOf(ayarlar.sulamaSuresi.toString()) }
    var nemEsik by remember { mutableStateOf(ayarlar.nemEsik.toString()) }
    
    AlertDialog(
        onDismissRequest = onKapat,
        title = { Text("⚙️ Sulama Ayarları") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "AI önerisini değiştirebilirsiniz:",
                    fontSize = 12.sp,
                    color = Color(0xFF616161)
                )
                
                OutlinedTextField(
                    value = siklik,
                    onValueChange = { siklik = it },
                    label = { Text("Sulama sıklığı (gün)") },
                    placeholder = { Text("1 = her gün") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = saat,
                    onValueChange = { saat = it },
                    label = { Text("Sulama saati") },
                    placeholder = { Text("22:00") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = sure,
                    onValueChange = { sure = it },
                    label = { Text("Sulama süresi (saniye)") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = nemEsik,
                    onValueChange = { nemEsik = it },
                    label = { Text("Nem eşiği (0-1023)") },
                    placeholder = { Text("500=ıslak, 700=kuru") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onKaydet(
                        ayarlar.copy(
                            sulamaSikligi = siklik.toIntOrNull() ?: 1,
                            sulamaSaati = saat,
                            sulamaSuresi = sure.toIntOrNull() ?: 30,
                            nemEsik = nemEsik.toIntOrNull() ?: 600,
                            kullaniciDegistirdi = true
                        )
                    )
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onKapat) {
                Text("İptal")
            }
        }
    )
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
                        color = Color(0xFF616161),
                        fontSize = 14.sp
                    )
                }
            }
            
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
                IconButton(onClick = onYenileClick, enabled = bluetoothBagli) {
                    Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (!bluetoothBagli) {
                Text(
                    "Sensör verilerini görmek için Bluetooth bağlantısı gerekli",
                    color = Color(0xFF616161),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SensorDeger(
                        baslik = "🌡️ Nem",
                        deger = "${sensorVerileri.nemDegeri}",
                        durum = if (sensorVerileri.nemDegeri > 600) "Kuru" else "Islak",
                        renk = if (sensorVerileri.nemDegeri > 600) Color(0xFFFF5722) else Color(0xFF4CAF50)
                    )
                    
                    SensorDeger(
                        baslik = "☀️ Işık",
                        deger = "${sensorVerileri.isikDegeri}",
                        durum = if (sensorVerileri.isikDegeri > 600) "Gece" else "Gündüz",
                        renk = if (sensorVerileri.isikDegeri > 600) Color(0xFF673AB7) else Color(0xFFFFC107)
                    )
                    
                    SensorDeger(
                        baslik = "💦 Pompa",
                        deger = if (sensorVerileri.pompaDurumu) "AÇIK" else "KAPALI",
                        durum = "",
                        renk = if (sensorVerileri.pompaDurumu) Color(0xFF4CAF50) else Color(0xFF757575)
                    )
                }
            }
        }
    }
}

@Composable
fun SensorDeger(baslik: String, deger: String, durum: String, renk: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(baslik, fontSize = 12.sp, color = Color(0xFF616161))
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
                    color = Color(0xFF616161),
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
            Text(
                gun.tarih.takeLast(5),
                fontSize = 12.sp,
                color = Color(0xFF616161)
            )
            
            Text(
                if (gun.yagisOlasiligi > 50) "🌧️" else "☀️",
                fontSize = 32.sp
            )
            
            Text(
                "${gun.sicaklikMax}°",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "%${gun.yagisOlasiligi}",
                fontSize = 12.sp,
                color = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
fun HaftalikPlanSatiri(
    gun: GunlukHavaDurumu,
    ayarlar: SulamaAyarlari,
    gunIndex: Int,
    tumGunler: List<GunlukHavaDurumu>,
    aiKarar: AiSulamaKarari? = null
) {
    // AI kararı varsa onu kullan, yoksa kendi mantığımızı uygula
    val sulamaYapilacak = if (aiKarar != null) {
        aiKarar.sula
    } else {
        // Fallback: Kendi mantığımız
        val sulamaGunu = (gunIndex % ayarlar.sulamaSikligi) == 0
        val yarinYagisVar = gunIndex < tumGunler.size - 1 &&
                            tumGunler[gunIndex + 1].yagisOlasiligi > 50
        val dunYagisOldu = gunIndex > 0 &&
                           tumGunler[gunIndex - 1].yagisOlasiligi > 50

        sulamaGunu && gun.yagisOlasiligi < 50 && !yarinYagisVar && !dunYagisOldu
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                gun.yagisOlasiligi > 50 -> Color(0xFFE3F2FD) // Yağış var
                sulamaYapilacak -> Color(0xFFE8F5E9) // Sulama var
                else -> Color(0xFFFAFAFA) // Normal
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    gun.tarih.takeLast(5),
                    modifier = Modifier.width(60.dp),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )

                Text(
                    if (gun.yagisOlasiligi > 50) "🌧️" else "☀️",
                    modifier = Modifier.width(30.dp)
                )

                Text(
                    "${gun.sicaklikMin}°/${gun.sicaklikMax}°",
                    modifier = Modifier.width(65.dp),
                    fontSize = 13.sp
                )

                Text(
                    "%${gun.yagisOlasiligi}",
                    modifier = Modifier.width(45.dp),
                    color = Color(0xFF2196F3),
                    fontSize = 13.sp
                )

                Spacer(Modifier.weight(1f))

                // AI ikonu göster
                if (aiKarar != null) {
                    Text("🤖", modifier = Modifier.padding(end = 4.dp))
                }

                Text(
                    when {
                        gun.yagisOlasiligi > 50 -> "🌧️ YAĞIŞ"
                        sulamaYapilacak -> "💧 SULA"
                        else -> "⏸️ BEKLE"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = when {
                        gun.yagisOlasiligi > 50 -> Color(0xFF1565C0)
                        sulamaYapilacak -> Color(0xFF4CAF50)
                        else -> Color(0xFF757575)
                    }
                )
            }

            // AI sebep açıklaması
            if (aiKarar != null && aiKarar.sebep.isNotEmpty()) {
                Text(
                    aiKarar.sebep,
                    fontSize = 11.sp,
                    color = Color(0xFF6A1B9A),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
                                    color = Color(0xFF616161)
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
