"""
Tahmine Dayalı Akıllı Sulama Sistemi - Model Eğitimi
Decision Tree (Karar Ağacı) ile sulama tahmini
"""

import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier, export_text
from sklearn.metrics import accuracy_score, classification_report

print("=" * 50)
print("Akıllı Sulama Sistemi - Model Eğitimi")
print("=" * 50)

# 1. Veriyi yükle
print("\n[1/5] Veri yükleniyor...")
df = pd.read_csv('ankara_5yil.csv')
print(f"      ✓ {len(df)} satır veri yüklendi")

# 2. Eksik verileri temizle
print("\n[2/5] Veri temizleniyor...")
df = df.dropna()
print(f"      ✓ {len(df)} satır temiz veri")

# 3. Sulama kararı oluştur (hedef değişken)
print("\n[3/5] Sulama kararları hesaplanıyor...")

# Sensör simülasyonu
# Toprak nem sensörü: 0-1023 (düşük=ıslak, yüksek=kuru)
df['sensor_nem'] = df['toprak_nem'].apply(
    lambda x: max(100, min(900, int(1000 - (x * 2500))))
)

# Yağış olasılığı (yagis_mm > 1mm ise yağış var kabul et)
df['yagis_var'] = (df['yagis_mm'] > 1).astype(int)

# Sulama kararı kuralları:
# - Yağış varsa veya bekleniyorsa -> SULAMA (0)
# - Toprak ıslaksa (sensor < 500) -> SULAMA (0)  
# - Toprak kuru ve yağış yoksa -> SULA (1)
def sulama_karari(row):
    if row['yagis_var'] == 1:  # Yağış var
        return 0
    elif row['sensor_nem'] < 500:  # Toprak ıslak
        return 0
    else:  # Toprak kuru ve yağış yok
        return 1

df['sulama'] = df.apply(sulama_karari, axis=1)

print(f"      ✓ Sulama gereken gün: {df['sulama'].sum()}")
print(f"      ✓ Sulama gerekmeyen gün: {(df['sulama'] == 0).sum()}")

# 4. Model eğitimi
print("\n[4/5] Model eğitiliyor...")

# Özellikler (features)
ozellikler = ['sicaklik_ort', 'yagis_mm', 'nem_ort', 'toprak_nem', 'mevsim', 'sensor_nem']
X = df[ozellikler]
y = df['sulama']

# Eğitim/test ayırma
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Karar Ağacı modeli (max_depth=5 ile basit tutuyoruz - Arduino için)
model = DecisionTreeClassifier(max_depth=5, random_state=42)
model.fit(X_train, y_train)

# Doğruluk hesapla
y_pred = model.predict(X_test)
accuracy = accuracy_score(y_test, y_pred)
print(f"      ✓ Model doğruluğu: %{accuracy*100:.1f}")

# 5. Model kurallarını çıkar
print("\n[5/5] Arduino için kurallar çıkarılıyor...")

# Karar ağacı kurallarını metin olarak al
tree_rules = export_text(model, feature_names=ozellikler)
print("\n--- KARAR AĞACI KURALLARI ---")
print(tree_rules)

# Arduino için if-else kodu oluştur
print("\n--- ARDUINO KODU ---")
print("""
// Sulama karar fonksiyonu
// sensor_nem: 0-1023 (düşük=ıslak, yüksek=kuru)
// yagis_olasiligi: 0-100 (%)
// sicaklik: Celsius

bool sulamaGerekliMi(int sensor_nem, int yagis_olasiligi, float sicaklik, int mevsim) {
  // Yağış bekleniyorsa sulama
  if (yagis_olasiligi > 50) {
    return false;
  }
  
  // Toprak zaten ıslaksa sulama
  if (sensor_nem < 500) {
    return false;
  }
  
  // Toprak kuru (sensor > 600) ve yağış beklenmiyorsa sula
  if (sensor_nem > 600 && yagis_olasiligi < 30) {
    // Yaz aylarında daha sık sula
    if (mevsim == 2) {  // Yaz
      return true;
    }
    // Diğer mevsimlerde sadece çok kuruysa
    if (sensor_nem > 700) {
      return true;
    }
  }
  
  return false;
}
""")

# Özet bilgileri kaydet
print("\n--- MODEL ÖZETİ ---")
print(f"Toplam veri: {len(df)} gün")
print(f"Eğitim verisi: {len(X_train)} gün")
print(f"Test verisi: {len(X_test)} gün")
print(f"Model doğruluğu: %{accuracy*100:.1f}")
print(f"Kullanılan özellikler: {ozellikler}")

print("\n" + "=" * 50)
print("Model eğitimi tamamlandı!")
print("=" * 50)
