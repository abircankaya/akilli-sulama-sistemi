# Akıllı Sulama Sistemi

Tahmine dayalı akıllı sulama sistemi - Arduino UNO + Android + Gemini AI + ML

## Proje Özeti

Bu proje, yapay zeka ve makine öğrenmesi kullanarak tarımda su tasarrufu sağlayan bir IoT sulama sistemidir.

### Özellikler

- **Gemini AI Entegrasyonu**: Mahsul bazlı sulama parametreleri
- **7 Günlük Hava Tahmini**: Open-Meteo API ile yağış tahmini
- **ML Tabanlı Karar**: Arduino üzerinde Decision Tree algoritması
- **Bluetooth İletişim**: Android-Arduino arası veri transferi
- **Konum Bazlı Hava Durumu**: GPS ile bulunduğunuz konumun hava durumu

## Sistem Mimarisi

```
┌─────────────────┐     Bluetooth      ┌─────────────────┐
│   Android App   │◄──────────────────►│    Arduino UNO  │
│                 │      (HC-05)       │                 │
│  • Gemini AI    │                    │  • Nem Sensörü  │
│  • Hava Durumu  │                    │  • LDR (Işık)   │
│  • Kullanıcı UI │                    │  • Röle (Pompa) │
└─────────────────┘                    └─────────────────┘
```

## Donanım Gereksinimleri

- Arduino UNO
- HC-05 Bluetooth Modülü
- Toprak Nem Sensörü
- LDR (Işık Sensörü)
- 5V Röle Modülü
- Mini Su Pompası

## Arduino Bağlantıları

| Pin | Bileşen |
|-----|---------|
| A0 | Nem Sensörü |
| A3 | LDR |
| D7 | Röle |
| D10 | HC-05 TX |
| D11 | HC-05 RX |

## Kurulum

### Android
1. Android Studio ile `Android/` klasörünü aç
2. Build → Build APK
3. APK'yı telefona yükle

### Arduino
1. Arduino IDE ile `Arduino/akilli_sulama_v2.ino` dosyasını aç
2. Board: Arduino UNO seç
3. Upload

## Kullanılan Teknolojiler

- **Android**: Kotlin, Jetpack Compose, MVVM
- **API**: Retrofit, Gemini AI, Open-Meteo
- **Arduino**: C++, SoftwareSerial
- **İletişim**: Bluetooth SPP

## Lisans

Bu proje eğitim amaçlı geliştirilmiştir.
