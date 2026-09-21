# Upstox API V3 + Supabase Edge Functions Setup Guide

## 🔒 महत्वपूर्ण सुरक्षा नियम (Security First)
**Upstox API Key, API Secret, और Access Token को कभी भी GitHub Repository या Frontend (HTML/JS) में न रखें!**
सभी सीक्रेट्स केवल **Supabase Secrets Vault** में सुरक्षित रहेंगे। 

Frontend केवल Supabase Edge Function के सुरक्षित एंडपॉइंट से डेटा लेगा:
`Frontend (GitHub Pages)` ➔ `Supabase Edge Function` ➔ `Upstox API V3 (Secrets Safe)` ➔ `TradingView Lightweight Charts` ➔ `Analysis Engine`

---

## 🚀 चरण 1: Supabase CLI में Secrets जोड़ें
अपने टर्मिनल (Terminal) में ये कमांड चलाएँ:

```bash
# 1. Supabase CLI लॉगिन करें
supabase login

# 2. अपने प्रोजेक्ट को लिंक करें
supabase link --project-ref your-supabase-project-id

# 3. Upstox API V3 के सीक्रेट्स सुरक्षित सेट करें (Secrets Vault)
supabase secrets set UPSTOX_API_KEY="your_upstox_api_key"
supabase secrets set UPSTOX_API_SECRET="your_upstox_api_secret"
supabase secrets set UPSTOX_ACCESS_TOKEN="your_upstox_access_token"
```

---

## 🚀 चरण 2: Edge Function डिप्लॉय करें
इस प्रोजेक्ट में `supabase/functions/upstox-market-data/index.ts` फ़ाइल पहले से तैयार है। इसे डिप्लॉय करने के लिए चलाएँ:

```bash
supabase functions deploy upstox-market-data --no-verify-jwt
```

डिप्लॉय होने के बाद आपको एक सुरक्षित URL मिलेगा:
`https://<your-project-ref>.supabase.co/functions/v1/upstox-market-data`

---

## 🚀 चरण 3: वेब ऐप (Frontend) में Supabase URL जोड़ें
ऐप के अंदर **"Supabase & Data Bridge Settings"** बटन पर क्लिक करें और अपना:
- **Supabase Edge Function URL**: `https://<your-project-ref>.supabase.co/functions/v1/upstox-market-data`
- **Supabase Anon Key**: (पब्लिक एनॉन की)

यदि आप अभी केवल टेस्ट करना चाहते हैं, तो ऐप का **"Smart Development Relay"** अपने आप वास्तविक रियलिस्टिक कैंडल्स और 60fps लाइव टिक स्ट्रीम लोड करेगा ताकि आपका काम बिना रुके चलता रहे।

---

## 📊 एनालिसिस इंजन में शामिल इंडिकेटर्स:
1. **EMA 20 & SMA 50**: ट्रेंड ट्रैकिंग
2. **RSI 14**: मोमेंटम और ओवरबॉट/ओवरसोल्ड ज़ोन
3. **MACD (12, 26, 9)**: बुलिश/बेयरिश क्रॉसओवर
4. **Bollinger Bands (20, 2)**: वोलैटिलिटी और स्क्वीज़ डिटेक्शन
5. **VWAP**: वॉल्यूम वेटेड एवरेज प्राइस
6. **Support & Resistance (S1, S2, R1, R2)**: स्विंग हाई और पिवट लेवल्स
7. **कैंडलस्टिक पैटर्न्स**: बुलिश एंगल्फिंग, हैमर, डोजी
8. **AI विज़न लेयर**: बिना किसी झूठे लाभ के वादों (No Guaranteed Returns) के साथ तटस्थ तकनीकी मार्गदर्शन।
