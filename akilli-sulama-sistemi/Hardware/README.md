# 🔌 Donanım Bağlantıları

## Malzeme Listesi

| # | Bileşen | Adet | Açıklama |
|---|---------|------|----------|
| 1 | Arduino UNO | 1 | Ana mikrodenetleyici |
| 2 | BC417 Bluetooth Modülü | 1 | HC-05/HC-06 uyumlu |
| 3 | Toprak Nem Sensörü | 1 | Kapasitif veya dirençli |
| 4 | LDR (Işık Sensörü) | 1 | 10K direnç ile |
| 5 | 5V Röle Modülü | 1 | 1 kanal |
| 6 | Mini Dalgıç Pompa | 1 | 6V DC |
| 7 | Breadboard | 1 | - |
| 8 | Jumper Kablo | ~20 | Erkek-Erkek, Erkek-Dişi |
| 9 | 9V Pil veya Adaptör | 1 | Arduino beslemesi |
| 10 | 6V Pil veya Adaptör | 1 | Pompa beslemesi |

## Bağlantı Şeması

```
                    ┌─────────────────────┐
                    │     ARDUINO UNO     │
                    │                     │
    Nem Sensörü ───►│ A0              D7 │───► Röle IN
                    │                     │
    LDR + 10K ─────►│ A3             D10 │◄─── BT TX
                    │                     │
                    │ 5V             D11 │───► BT RX
                    │                     │
                    │ GND            GND │
                    └─────────────────────┘
```

## Detaylı Bağlantılar

### 1. Toprak Nem Sensörü
```
Nem Sensörü    →    Arduino
-----------         -------
VCC            →    5V
GND            →    GND
A0 (Analog)    →    A0
```

### 2. LDR (Işık Sensörü)
```
        5V
         │
         ┴
        ┌─┐
        │ │ LDR
        └─┘
         │
         ├──────► A3 (Arduino)
         │
        ┌─┐
        │ │ 10KΩ Direnç
        └─┘
         │
        GND
```

### 3. BC417 Bluetooth Modülü
```
BC417          →    Arduino
-----              -------
VCC            →    5V
GND            →    GND
TX             →    D10 (SoftwareSerial RX)
RX             →    D11 (SoftwareSerial TX)
```

⚠️ **Not**: BC417 RX pini 3.3V mantık seviyesi kullanır. Güvenli kullanım için voltage divider eklenebilir:
```
Arduino D11 ──┬── 1KΩ ──┬── BC417 RX
              │         │
              └── 2KΩ ──┴── GND
```

### 4. Röle Modülü
```
Röle Modülü    →    Arduino
-----------         -------
VCC            →    5V
GND            →    GND
IN             →    D7
```

### 5. Su Pompası (Röle üzerinden)
```
Röle           →    Pompa Devresi
----                -------------
COM            →    6V Pil (+)
NO             →    Pompa (+)
                    Pompa (-) → 6V Pil (-)
```

## Devre Şeması (ASCII)

```
    ┌──────────────────────────────────────────────────────────────┐
    │                                                              │
    │   ┌─────────┐                              ┌─────────┐       │
    │   │   9V    │                              │   6V    │       │
    │   │   Pil   │                              │   Pil   │       │
    │   └────┬────┘                              └────┬────┘       │
    │        │                                        │            │
    │        ▼                                        │            │
    │   ┌─────────────────────────────┐              │            │
    │   │                             │              │            │
    │   │        ARDUINO UNO          │         ┌────┴────┐       │
    │   │                             │         │  RÖLE   │       │
    │   │  A0◄── Nem Sensörü          │         │  COM────┤       │
    │   │                             │         │  NO─────┼──┐    │
    │   │  A3◄── LDR + 10K            │    D7──►│  IN     │  │    │
    │   │                             │         │  VCC◄───┤  │    │
    │   │  D10◄── BT TX               │         │  GND◄───┤  │    │
    │   │                             │         └─────────┘  │    │
    │   │  D11──► BT RX               │                      │    │
    │   │                             │              ┌───────┴──┐ │
    │   │  5V───► BT VCC, Sensörler   │              │  POMPA   │ │
    │   │                             │              │          │ │
    │   │  GND──► Ortak GND           │              └──────────┘ │
    │   │                             │                            │
    │   └─────────────────────────────┘                            │
    │                                                              │
    └──────────────────────────────────────────────────────────────┘
```

## Sensör Değer Aralıkları

### Nem Sensörü (0-1023)
| Değer | Durum |
|-------|-------|
| 0-300 | Çok ıslak |
| 300-500 | Islak |
| 500-700 | Normal |
| 700-900 | Kuru |
| 900-1023 | Çok kuru |

**Eşik değer**: 600 (üstü = kuru, sulama gerekli)

### LDR (0-1023)
| Değer | Durum |
|-------|-------|
| 0-200 | Çok aydınlık |
| 200-400 | Aydınlık |
| 400-600 | Alacakaranlık |
| 600-800 | Karanlık |
| 800-1023 | Çok karanlık |

**Eşik değer**: 600 (üstü = gece, sulama için uygun)

## Güç Tüketimi

| Bileşen | Akım |
|---------|------|
| Arduino UNO | ~50mA |
| Bluetooth | ~40mA |
| Sensörler | ~20mA |
| Röle | ~70mA |
| **Toplam (pompa hariç)** | **~180mA** |
| Pompa (çalışırken) | ~200-500mA |

## Sorun Giderme

### Bluetooth bağlanmıyor
- VCC ve GND bağlantılarını kontrol edin
- TX/RX pinlerinin doğru bağlı olduğundan emin olun
- Varsayılan şifre genellikle "1234" veya "0000"

### Sensör değerleri sabit
- Analog pin bağlantısını kontrol edin
- Sensör besleme voltajını kontrol edin
- Nem sensörünü toprağa tam batırın

### Pompa çalışmıyor
- Röle LED'inin yandığını kontrol edin
- Pompa besleme voltajını kontrol edin
- NO/NC terminal bağlantısını kontrol edin
