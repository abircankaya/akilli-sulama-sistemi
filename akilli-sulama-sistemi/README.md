# 🌱 Tahmine Dayalı Akıllı Sulama Sistemi

[![Arduino](https://img.shields.io/badge/Arduino-UNO-00979D?logo=arduino)](https://www.arduino.cc/)
[![Python](https://img.shields.io/badge/Python-3.9+-3776AB?logo=python)](https://www.python.org/)
[![Android](https://img.shields.io/badge/Android-Kotlin-3DDC84?logo=android)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Makine öğrenmesi destekli, hava durumu tahminlerini kullanarak sulama kararı veren akıllı sulama sistemi.

## 📋 Proje Özeti

Bu proje, meteorolojik tahminleri kullanarak sulama kararı verebilen akıllı bir sistemdir. Sistem; Arduino mikrodenetleyici, toprak nem sensörü, ışık sensörü (LDR), Bluetooth modülü (BC417) ve Android mobil uygulamadan oluşmaktadır.

### ✨ Özellikler

- 🌦️ **Hava Durumu Entegrasyonu**: Open-Meteo API ile 7 günlük hava tahmini
- 🤖 **Makine Öğrenmesi**: Decision Tree modeli ile akıllı sulama kararı
- 📱 **Mobil Uygulama**: Android uygulaması ile uzaktan izleme ve kontrol
- 📡 **Bluetooth Haberleşme**: BC417 modülü ile kablosuz veri aktarımı
- 🔋 **Otonom Çalışma**: İnternet olmadan 7 gün bağımsız çalışabilme
- 💧 **Su Tasarrufu**: Yağış öncesi sulama engelleme ile %30-40 tasarruf

## 🏗️ Sistem Mimarisi

```
┌─────────────────┐     Bluetooth      ┌─────────────────┐
│  Android App    │◄──────────────────►│   Arduino UNO   │
│  - Weather API  │                    │  - Sensörler    │
│  - ML Kuralları │                    │  - Röle/Pompa   │
└─────────────────┘                    └─────────────────┘
        │                                      │
        ▼                                      ▼
  Open-Meteo API                    ┌─────────────────┐
  (Hava Tahmini)                    │  Sulama Ünitesi │
                                    │  - Su Pompası   │
                                    └─────────────────┘
```

## 📁 Proje Yapısı

```
akilli-sulama-sistemi/
├── Arduino/
│   └── akilli_sulama_v2.ino    # Ana Arduino kodu
├── ML/
│   ├── veri_cek.py             # Meteoroloji verisi çekme
│   ├── model_egit.py           # ML model eğitimi
│   └── requirements.txt        # Python bağımlılıkları
├── Android/
│   └── (Kotlin + Jetpack Compose)
├── Hardware/
│   └── README.md               # Donanım bağlantıları
├── Docs/
│   └── images/                 # Proje görselleri
└── README.md
```

## 🔧 Donanım Gereksinimleri

| Bileşen | Açıklama |
|---------|----------|
| Arduino UNO | Ana mikrodenetleyici |
| BC417 Bluetooth | Kablosuz haberleşme modülü |
| Toprak Nem Sensörü | Toprak nem ölçümü |
| LDR | Işık seviyesi ölçümü |
| 5V Röle Modülü | Pompa kontrolü |
| Mini Dalgıç Pompa | Sulama işlemi |

### Bağlantı Şeması

| Bileşen | Arduino Pin |
|---------|-------------|
| Nem Sensörü | A0 |
| LDR | A3 |
| Röle | D7 |
| Bluetooth TX | D10 |
| Bluetooth RX | D11 |

## 🚀 Kurulum

### 1. ML Model Eğitimi

```bash
cd ML

# Bağımlılıkları yükle
pip install -r requirements.txt

# Meteoroloji verilerini çek (5 yıllık Ankara verisi)
python veri_cek.py

# Modeli eğit
python model_egit.py
```

### 2. Arduino

1. Arduino IDE'yi açın
2. `Arduino/akilli_sulama_v2.ino` dosyasını yükleyin
3. Kartı seçin: Tools → Board → Arduino UNO
4. Portu seçin: Tools → Port → (Arduino'nun bağlı olduğu port)
5. Yükle butonuna tıklayın

### 3. Android Uygulaması

```bash
cd Android

# Android Studio ile açın ve derleyin
```

## 📡 Bluetooth Protokolü

### Telefon → Arduino

| Komut | Açıklama | Örnek |
|-------|----------|-------|
| `W:` | 7 günlük yağış % | `W:10,25,30,5,80,0,15` |
| `T:` | 7 günlük sıcaklık | `T:25,26,24,23,22,21,20` |
| `M:` | Mevsim (1-4) | `M:2` |
| `S:` | Durum sorgula | `S:` |

### Arduino → Telefon

| Komut | Açıklama | Örnek |
|-------|----------|-------|
| `S:` | Durum bilgisi | `S:720,450,1,2,30` |

Format: `S:nem,ışık,pompa,günIndex,yağış%`

## 🧠 ML Model

- **Algoritma**: Decision Tree (Karar Ağacı)
- **Veri Seti**: 5 yıllık Ankara meteoroloji verisi (2021-2025)
- **Özellikler**: Sıcaklık, yağış, nem, toprak nemi, mevsim
- **Doğruluk**: %100

### Sulama Kuralları

```
1. Yağış olasılığı > %50 → SULAMA
2. Toprak ıslak (sensör < 500) → SULAMA
3. Gündüz (LDR < 600) → SULAMA
4. Gece + Toprak kuru + Yağış yok → SULA ✓
```

## 📊 Veri Kaynağı

[Open-Meteo API](https://open-meteo.com/) - Ücretsiz hava durumu API'si
- API key gerektirmez
- Günlük 10,000 istek limiti
- Geçmiş ve tahmin verileri

## 🎓 Akademik Bilgi

Bu proje, Selçuk Üniversitesi Teknoloji Fakültesi Bilgisayar Mühendisliği Bölümü **Bilgisayar Mühendisliği Uygulamaları** dersi kapsamında hazırlanmıştır.

- **Öğrenci**: Ahmet Bircan KAYA
- **Danışman**: Öğr. Gör. Mustafa GÖKMEN
- **Yıl**: 2025

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

## 🤝 Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/yeniOzellik`)
3. Commit yapın (`git commit -m 'Yeni özellik eklendi'`)
4. Push yapın (`git push origin feature/yeniOzellik`)
5. Pull Request açın

## 📞 İletişim

- **E-posta**: 183301068@ogr.selcuk.edu.tr
- **GitHub**: [@abircankaya](https://github.com/abircankaya)
