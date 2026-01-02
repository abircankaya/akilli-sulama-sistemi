"""
Ankara 5 Yıllık Hava Verisi Çekme Scripti
Open-Meteo API - Ücretsiz, API key gerekmez

Kullanım:
1. pip install requests pandas
2. python veri_cek.py
3. ankara_5yil.csv dosyası oluşur
"""

import requests
import pandas as pd
from datetime import datetime

# Ankara koordinatları
LATITUDE = 39.93
LONGITUDE = 32.86

# 5 yıllık tarih aralığı
START_DATE = "2020-01-01"
END_DATE = "2024-12-31"

# API URL
url = f"""https://archive-api.open-meteo.com/v1/archive?
latitude={LATITUDE}&longitude={LONGITUDE}
&start_date={START_DATE}&end_date={END_DATE}
&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean,precipitation_sum,relative_humidity_2m_mean,soil_moisture_0_to_7cm_mean,daylight_duration
&timezone=Europe/Istanbul""".replace("\n", "")

print("Veri çekiliyor...")
print(f"Tarih aralığı: {START_DATE} - {END_DATE}")
print(f"Konum: Ankara ({LATITUDE}, {LONGITUDE})")
print()

try:
    response = requests.get(url)
    response.raise_for_status()
    data = response.json()
    
    # DataFrame oluştur
    df = pd.DataFrame({
        'tarih': data['daily']['time'],
        'sicaklik_max': data['daily']['temperature_2m_max'],
        'sicaklik_min': data['daily']['temperature_2m_min'],
        'sicaklik_ort': data['daily']['temperature_2m_mean'],
        'yagis_mm': data['daily']['precipitation_sum'],
        'nem_ort': data['daily']['relative_humidity_2m_mean'],
        'toprak_nem': data['daily']['soil_moisture_0_to_7cm_mean'],
        'gun_suresi_saat': [d/3600 if d else 0 for d in data['daily']['daylight_duration']]
    })
    
    # Mevsim ekle
    df['tarih'] = pd.to_datetime(df['tarih'])
    df['ay'] = df['tarih'].dt.month
    df['mevsim'] = df['ay'].apply(lambda x: 
        1 if x in [3,4,5] else      # İlkbahar
        2 if x in [6,7,8] else      # Yaz
        3 if x in [9,10,11] else    # Sonbahar
        4                            # Kış
    )
    
    # Kaydet
    df.to_csv('ankara_5yil.csv', index=False)
    
    print(f"✓ Başarılı!")
    print(f"✓ Toplam {len(df)} gün verisi çekildi")
    print(f"✓ Dosya: ankara_5yil.csv")
    print()
    print("İlk 5 satır:")
    print(df.head())
    print()
    print("Veri özeti:")
    print(df.describe())

except requests.exceptions.RequestException as e:
    print(f"Hata: {e}")
except KeyError as e:
    print(f"Veri formatı hatası: {e}")
    print("API yanıtı:", response.text[:500])
