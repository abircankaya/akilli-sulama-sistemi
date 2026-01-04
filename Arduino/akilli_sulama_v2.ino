/*
 * Tahmine Dayalı Akıllı Sulama Sistemi
 * Arduino UNO + BC417 Bluetooth + ML Kuralları
 *
 * Bağlantılar:
 * - Nem Sensörü: A0
 * - LDR: A3
 * - Röle: D7
 * - Bluetooth TX: D10 (Arduino RX)
 * - Bluetooth RX: D11 (Arduino TX)
 */

#include <SoftwareSerial.h>

// Pin tanımları
const int nemPin = A0;
const int ldrPin = A3;
const int rolePin = 7;
const int btRxPin = 10;  // Bluetooth TX -> Arduino D10
const int btTxPin = 11;  // Bluetooth RX -> Arduino D11

// Bluetooth seri port
SoftwareSerial bluetooth(btRxPin, btTxPin);

// Eşik değerleri (AI tarafından güncellenebilir)
int NEM_ESIK = 600;            // Üstü = kuru toprak (AI değiştirebilir)
const int LDR_ESIK = 600;      // Üstü = karanlık (gece)
const int YAGIS_ESIK = 50;     // Üstü = yağış bekleniyor (%)
int sulamaSikligi = 1;         // Sulama sıklığı (gün cinsinden)

// Haftalık hava durumu verileri (mobil uygulamadan gelecek)
int yagisOlasiliklari[7] = {0, 0, 0, 0, 0, 0, 0};  // 7 günlük yağış %
int sicakliklar[7] = {20, 20, 20, 20, 20, 20, 20}; // 7 günlük sıcaklık
int gunIndex = 0;  // Bugünün indexi
int mevsim = 2;    // 1=İlkbahar, 2=Yaz, 3=Sonbahar, 4=Kış

// Son güncelleme zamanı
unsigned long sonGuncelleme = 0;
const unsigned long GUN_MS = 86400000;  // 24 saat (ms)

// Durum değişkenleri
bool veriAlindi = false;
String gelenVeri = "";

void setup() {
  // Pin modları
  pinMode(rolePin, OUTPUT);
  pinMode(nemPin, INPUT);
  pinMode(ldrPin, INPUT);

  // Pompa başlangıçta kapalı
  digitalWrite(rolePin, HIGH);

  // Seri portlar
  Serial.begin(9600);
  bluetooth.begin(9600);

  Serial.println("================================");
  Serial.println("Akilli Sulama Sistemi v2.1");
  Serial.println("Bluetooth + AI + ML Destekli");
  Serial.println("================================");
  Serial.println("Mobil uygulamadan veri bekleniyor...");
  Serial.println("");
  Serial.println("Komutlar:");
  Serial.println("  W:x,x,x,x,x,x,x  -> 7 gunluk yagis %");
  Serial.println("  T:x,x,x,x,x,x,x  -> 7 gunluk sicaklik");
  Serial.println("  M:x              -> Mevsim (1-4)");
  Serial.println("  N:x              -> Nem esigi (AI)");
  Serial.println("  K:x              -> Sulama sikligi (gun)");
  Serial.println("  S:               -> Durum sorgula");
  Serial.println("================================");
}

void loop() {
  // 1. Bluetooth'dan veri oku
  bluetoothOku();

  // 2. Sensörleri oku
  int nemDegeri = analogRead(nemPin);
  int isikDegeri = analogRead(ldrPin);

  // 3. Bugünün yağış olasılığını al
  int bugunYagis = yagisOlasiliklari[gunIndex];
  int bugunSicaklik = sicakliklar[gunIndex];

  // 4. ML tabanlı sulama kararı
  bool sulamaYap = sulamaKarari(nemDegeri, isikDegeri, bugunYagis, bugunSicaklik, mevsim);

  // 5. Pompayı kontrol et
  if (sulamaYap) {
    digitalWrite(rolePin, LOW);   // Pompa AÇ (aktif LOW)
  } else {
    digitalWrite(rolePin, HIGH);  // Pompa KAPAT
  }

  // 6. Durum bilgisi gönder
  durumGonder(nemDegeri, isikDegeri, sulamaYap);

  // 7. Gün geçişi kontrolü (24 saat)
  if (millis() - sonGuncelleme > GUN_MS) {
    gunIndex = (gunIndex + 1) % 7;
    sonGuncelleme = millis();
    Serial.println(">> Yeni gun, index: " + String(gunIndex));
  }

  delay(2000);  // 2 saniye bekle
}

/*
 * ML Tabanlı Sulama Karar Fonksiyonu
 * Decision Tree modelinden çıkarılan kurallar
 *
 * Girişler:
 *   - sensor_nem: Toprak nem değeri (0-1023, yüksek=kuru)
 *   - sensor_ldr: Işık değeri (0-1023, yüksek=karanlık)
 *   - yagis_olasiligi: Bugünkü yağış tahmini (%)
 *   - sicaklik: Bugünkü sıcaklık (°C)
 *   - mevsim: 1=İlkbahar, 2=Yaz, 3=Sonbahar, 4=Kış
 *
 * Çıkış:
 *   - true: Sulama yap
 *   - false: Sulama yapma
 */
bool sulamaKarari(int sensor_nem, int sensor_ldr, int yagis_olasiligi, int sicaklik, int mevsim) {

  // Kural 1: Yağış bekleniyorsa SULAMA
  if (yagis_olasiligi > YAGIS_ESIK) {
    Serial.println(">> Karar: SULAMA (yagis %" + String(yagis_olasiligi) + " bekleniyor)");
    return false;
  }

  // Kural 2: Toprak zaten ıslaksa SULAMA
  if (sensor_nem < 500) {
    Serial.println(">> Karar: SULAMA (toprak islak, nem:" + String(sensor_nem) + ")");
    return false;
  }

  // Kural 3: Gündüz ise SULAMA (buharlaşma fazla)
  if (sensor_ldr < LDR_ESIK) {
    Serial.println(">> Karar: SULAMA (gunduz, ldr:" + String(sensor_ldr) + ")");
    return false;
  }

  // Kural 4: Toprak kuru + Gece + Yağış yok = SULA
  if (sensor_nem > NEM_ESIK && sensor_ldr > LDR_ESIK && yagis_olasiligi < 30) {

    // Yaz aylarında daha agresif sula
    if (mevsim == 2) {
      Serial.println(">> Karar: SULA (yaz, nem:" + String(sensor_nem) + " > esik:" + String(NEM_ESIK) + ", gece)");
      return true;
    }

    // Diğer mevsimlerde sadece çok kuruysa
    if (sensor_nem > 700) {
      Serial.println(">> Karar: SULA (cok kuru nem:" + String(sensor_nem) + ", gece)");
      return true;
    }
  }

  Serial.println(">> Karar: SULAMA (nem:" + String(sensor_nem) + ", esik:" + String(NEM_ESIK) + ")");
  return false;
}

/*
 * Bluetooth'dan Veri Okuma
 * Desteklenen Komutlar:
 *   W:10,25,30,5,80,0,15  -> 7 günlük yağış %
 *   T:25,26,24,23,22,21,20 -> 7 günlük sıcaklık
 *   M:2                   -> Mevsim (1-4)
 *   D:3                   -> Gün indexi (0-6)
 *   N:550                 -> Nem eşik değeri (AI'dan)
 *   K:2                   -> Sulama sıklığı (gün)
 *   S:                    -> Durum sorgula
 */
void bluetoothOku() {
  while (bluetooth.available()) {
    char c = bluetooth.read();

    if (c == '\n') {
      // Veri tamamlandı, işle
      veriIsle(gelenVeri);
      gelenVeri = "";
    } else {
      gelenVeri += c;
    }
  }
}

void veriIsle(String veri) {
  veri.trim();

  if (veri.length() < 2) return;

  char tip = veri.charAt(0);
  String degerler = veri.substring(2);  // "W:" veya "T:" kısmını atla

  if (tip == 'W') {
    // Haftalık yağış olasılıkları
    int idx = 0;
    int baslangic = 0;

    for (int i = 0; i <= degerler.length(); i++) {
      if (i == degerler.length() || degerler.charAt(i) == ',') {
        if (idx < 7) {
          yagisOlasiliklari[idx] = degerler.substring(baslangic, i).toInt();
          idx++;
        }
        baslangic = i + 1;
      }
    }

    Serial.print(">> Yagis verileri: ");
    for (int i = 0; i < 7; i++) {
      Serial.print(yagisOlasiliklari[i]);
      if (i < 6) Serial.print(",");
    }
    Serial.println("");

    veriAlindi = true;
    sonGuncelleme = millis();
    gunIndex = 0;
  }
  else if (tip == 'T') {
    // Haftalık sıcaklıklar
    int idx = 0;
    int baslangic = 0;

    for (int i = 0; i <= degerler.length(); i++) {
      if (i == degerler.length() || degerler.charAt(i) == ',') {
        if (idx < 7) {
          sicakliklar[idx] = degerler.substring(baslangic, i).toInt();
          idx++;
        }
        baslangic = i + 1;
      }
    }

    Serial.print(">> Sicaklik verileri: ");
    for (int i = 0; i < 7; i++) {
      Serial.print(sicakliklar[i]);
      if (i < 6) Serial.print(",");
    }
    Serial.println(" C");
  }
  else if (tip == 'M') {
    // Mevsim
    mevsim = degerler.toInt();
    String mevsimAdi;
    switch(mevsim) {
      case 1: mevsimAdi = "Ilkbahar"; break;
      case 2: mevsimAdi = "Yaz"; break;
      case 3: mevsimAdi = "Sonbahar"; break;
      case 4: mevsimAdi = "Kis"; break;
      default: mevsimAdi = "Bilinmiyor";
    }
    Serial.println(">> Mevsim: " + mevsimAdi + " (" + String(mevsim) + ")");
  }
  else if (tip == 'D') {
    // Gün indexi
    gunIndex = degerler.toInt() % 7;
    Serial.println(">> Gun indexi: " + String(gunIndex));
  }
  else if (tip == 'S') {
    // Durum sorgulama
    durumSorgulamaYanit();
  }
  else if (tip == 'N') {
    // Nem eşik değeri (AI'dan gelen)
    int yeniEsik = degerler.toInt();
    if (yeniEsik >= 0 && yeniEsik <= 1023) {
      int eskiEsik = NEM_ESIK;
      NEM_ESIK = yeniEsik;
      Serial.println(">> Nem esigi guncellendi: " + String(eskiEsik) + " -> " + String(NEM_ESIK));

      // Eşik yorumu
      if (NEM_ESIK < 550) {
        Serial.println("   (Yuksek su ihtiyacli bitki - sik sulama)");
      } else if (NEM_ESIK < 650) {
        Serial.println("   (Orta su ihtiyacli bitki)");
      } else {
        Serial.println("   (Dusuk su ihtiyacli bitki - seyrek sulama)");
      }
    } else {
      Serial.println(">> HATA: Gecersiz nem esigi: " + String(yeniEsik));
    }
  }
  else if (tip == 'K') {
    // Sulama sıklığı (gün cinsinden)
    int yeniSiklik = degerler.toInt();
    if (yeniSiklik >= 1 && yeniSiklik <= 7) {
      sulamaSikligi = yeniSiklik;
      Serial.println(">> Sulama sikligi: Her " + String(sulamaSikligi) + " gunde bir");
    } else {
      Serial.println(">> HATA: Gecersiz sulama sikligi: " + String(yeniSiklik));
    }
  }
  else {
    Serial.println(">> UYARI: Bilinmeyen komut: " + veri);
  }
}

/*
 * Mobil Uygulamaya Durum Bilgisi Gönder
 * Her 20 saniyede bir otomatik gönderim
 */
void durumGonder(int nem, int isik, bool pompa) {
  // Her 10 döngüde bir gönder (20 saniyede bir)
  static int sayac = 0;
  sayac++;

  if (sayac >= 10) {
    sayac = 0;

    // Format: "S:nem,isik,pompa,gunIndex,yagisOlasiligi"
    String mesaj = "S:";
    mesaj += String(nem) + ",";
    mesaj += String(isik) + ",";
    mesaj += String(pompa ? 1 : 0) + ",";
    mesaj += String(gunIndex) + ",";
    mesaj += String(yagisOlasiliklari[gunIndex]);

    bluetooth.println(mesaj);

    // Debug için seri porta da yaz
    Serial.println("----------------------------------------");
    Serial.println("| Nem: " + String(nem) + " | Isik: " + String(isik) +
                   " | Pompa: " + String(pompa ? "ACIK" : "KAPALI") + " |");
    Serial.println("| Gun: " + String(gunIndex) + " | Yagis%: " + String(yagisOlasiliklari[gunIndex]) +
                   " | Esik: " + String(NEM_ESIK) + " |");
    Serial.println("----------------------------------------");
  }
}

/*
 * Durum Sorgulama Yanıtı
 * Mobil uygulama "S:" gönderdiğinde çağrılır
 */
void durumSorgulamaYanit() {
  int nem = analogRead(nemPin);
  int isik = analogRead(ldrPin);
  bool pompa = (digitalRead(rolePin) == LOW);

  String mesaj = "S:";
  mesaj += String(nem) + ",";
  mesaj += String(isik) + ",";
  mesaj += String(pompa ? 1 : 0) + ",";
  mesaj += String(gunIndex) + ",";
  mesaj += String(yagisOlasiliklari[gunIndex]);

  bluetooth.println(mesaj);
  Serial.println(">> Durum sorgusu yaniti: " + mesaj);
}
