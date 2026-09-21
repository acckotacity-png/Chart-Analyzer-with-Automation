# Indian Stock Screener & AI Assistant 📈

An intelligent Stock Screener with Technical Indicators (SMA, EMA, RSI, MACD), Gemini AI Buy/Hold/Sell insights, and **Supabase User Registration & Admin Rights Approval**.

Available as both:
1. **Android App (Kotlin + Jetpack Compose)**
2. **Web Portal & PWA (HTML5 / JavaScript / Python Server for GitHub Pages & Render.com)**

---

## 🔐 Supabase User Access & Admin Approval System

- **New User Flow**: A new user enters their **Full Name**, **Mobile Number**, and **Email**.
- **Initial Status**: The user's record is created in Supabase `app_users` table with `status = 'pending'`.
- **Gate Screen**: The user cannot access stock charts, screeners, or AI advice until approved. A message prompts them that their access request is awaiting Admin approval.
- **Admin Rights Flow**:
  - Primary Admin: `arjunmalviya166@gmail.com`
  - When the admin logs in, they can view all registered users and tap **"Approve"** (Give Rights) or **"Reject"**.
  - As soon as Admin approves, the user's status updates in Supabase and their access unlocks immediately.

---

## 🌐 Deploy as Free Web App on Render.com

This repo includes full web assets in `/public` and a ready-to-deploy Python server (`server.py`, `Procfile`, `render.yaml`).

1. Create a free account at [Render.com](https://render.com).
2. Click **New +** -> **Web Service**.
3. Connect your GitHub repository.
4. Settings:
   - **Environment**: Python 3
   - **Build Command**: *(leave empty)*
   - **Start Command**: `python3 server.py`
5. Click **Deploy Web Service** (100% Free).
6. Open your Render URL in Chrome/Safari on mobile and tap **"Install App / Add to Home Screen"**!

---

## 📱 Deploy Free on GitHub Pages

1. In your GitHub repository, go to **Settings** -> **Pages**.
2. Under **Build and deployment**, set **Source** to `Deploy from a branch`.
3. Choose branch `main` and folder `/public`.
4. Click **Save**. Your web app is live at `https://YOUR_USERNAME.github.io/YOUR_REPO/`!

---

## 🤖 Free Android APK Build via GitHub Actions

1. Push to your GitHub repo.
2. Go to the **Actions** tab.
3. Once the **"Build Android APK & Release"** workflow completes, download `StockScreener-Debug-APK` from **Artifacts**.
