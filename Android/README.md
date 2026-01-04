# 📱 Akıllı Sulama Android Uygulaması

Tahmine Dayalı Akıllı Sulama Sistemi için Android kontrol uygulaması.

## ✨ Özellikler

- 🌤️ **Hava Durumu**: Open-Meteo API ile 7 günlük tahmin
- 📡 **Bluetooth**: Arduino ile kablosuz haberleşme
- 📊 **Sensör İzleme**: Nem, ışık ve pompa durumu
- 📅 **Haftalık Plan**: Sulama önerileri

## 🛠️ Teknolojiler

- **Kotlin** - Programlama dili
- **Jetpack Compose** - Modern UI toolkit
- **Material 3** - Tasarım sistemi
- **Retrofit** - HTTP client
- **Coroutines** - Asenkron işlemler
- **ViewModel** - MVVM mimarisi

## 📦 Kurulum

### Android Studio ile:

1. Android Studio'yu açın (Arctic Fox veya üstü)
2. "Open an existing project" seçin
3. Bu klasörü seçin
4. Gradle sync tamamlanana kadar bekleyin
5. Run butonuna tıklayın

### Gereksinimler:

- Android Studio Arctic Fox+
- JDK 11+
- Android SDK 26+ (Android 8.0)
- Fiziksel cihaz (Bluetooth için emülatör çalışmaz)

## 📱 Ekran Görüntüleri

```
┌─────────────────────────────┐
│  🌱 Akıllı Sulama      [BT] │
├─────────────────────────────┤
│  ┌─────────────────────┐    │
│  │ 🔵 Bağlı           │    │
│  │ HC-05          [Kes]│    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ 📊 Sensör Verileri  │    │
│  │ 💧720  ☀️450  💦AÇIK│    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ 🌤️ Hava Durumu  [↻] │    │
│  │ ☀️25° ☀️26° 🌧️22°  │    │
│  └─────────────────────┘    │
│                             │
│  [  Verileri Gönder  ]      │
│                             │
│  📅 Haftalık Plan           │
│  ├ 2025-01-02  ☀️  💧SULA   │
│  ├ 2025-01-03  ☀️  💧SULA   │
│  └ 2025-01-04  🌧️  ⏸️BEKLE  │
└─────────────────────────────┘
```

## 📡 Bluetooth Protokolü

### Telefon → Arduino:
| Komut | Açıklama | Örnek |
|-------|----------|-------|
| `W:` | Yağış % | `W:10,25,30,5,80,0,15` |
| `T:` | Sıcaklık | `T:25,26,24,23,22,21,20` |
| `M:` | Mevsim | `M:2` |
| `S:` | Durum sor | `S:` |

### Arduino → Telefon:
| Komut | Format |
|-------|--------|
| `S:` | `S:nem,ışık,pompa,gün,yağış%` |

## 🔧 Yapı

```
app/src/main/java/com/ahmetbircan/akillisulama/
├── MainActivity.kt          # Ana Activity
├── api/
│   └── WeatherApi.kt        # Open-Meteo API
├── bluetooth/
│   └── BluetoothManager.kt  # Bluetooth yönetimi
├── data/
│   └── Models.kt            # Veri modelleri
└── ui/
    ├── AkilliSulamaApp.kt   # Ana UI
    ├── MainViewModel.kt      # ViewModel
    └── theme/
        └── Theme.kt         # Tema
```

## ⚠️ İzinler

Uygulama şu izinleri gerektirir:
- `BLUETOOTH` - Bluetooth kullanımı
- `BLUETOOTH_CONNECT` - Cihaza bağlanma
- `BLUETOOTH_SCAN` - Cihaz tarama
- `INTERNET` - Hava durumu API
- `ACCESS_FINE_LOCATION` - Bluetooth tarama için

## 🐛 Sorun Giderme

### Bluetooth bağlanmıyor:
1. Telefon ayarlarından önce HC-05/BC417 ile eşleştirin
2. PIN genellikle "1234" veya "0000"
3. Konum iznini verin (Android 12+ için gerekli)

### Hava durumu gelmiyor:
1. İnternet bağlantısını kontrol edin
2. Open-Meteo API'nin çalıştığını kontrol edin

## 📄 Lisans

MIT License
