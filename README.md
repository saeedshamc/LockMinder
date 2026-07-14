# LockMinder 🔒 — App Blocker & Self-Control Companion

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-M3-green.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0_--_14+-brightgreen.svg?style=flat&logo=android)](https://developer.android.com)

**LockMinder** is an advanced, un-bypassable Android application designed to help you regain focus and curb digital distractions. By integrating modern system-level APIs with an offline-first database, it enforces strict, time-locked lockout commitments. Once a lock session is started, it cannot be aborted, disabled, or uninstalled easily, protecting you from your own impulses.

---

## 🎨 Visual Identity & App Icon

LockMinder features a custom, high-tech **Adaptive App Icon** designed with:
- **Foreground Emblem**: A futuristic, minimalist lock shield emblem rendered with glowing neon electric blue and cyan gradients.
- **Background Slate**: A premium, deep charcoal black linear gradient backdrop, styled with generous negative space to look exceptionally sharp and polished on your home screen.

---

## 🚀 Key Features | ویژگی‌های کلیدی

*   **🔒 Strict Time-Locked Lockouts (قفل‌های زمانی سخت‌گیرانه)**
    Set precise countdown commitments to block selected apps. Once activated, the locking session is strictly non-abortable.
*   **🛠️ Device Admin Protection (محافظت با مدیریت دستگاه)**
    Utilizes Android Device Administration APIs (`LockMinderDeviceAdminReceiver`) to prevent unauthorized uninstallation or sudden force-stops of the blocker.
*   **🧩 Foreground Accessibility Blocker (مسدودکننده دقیق هوشمند)**
    Leverages a background Accessibility Service (`AppBlockAccessibilityService`) to monitor foreground tasks in real-time and display a blocking overlay immediately if a distraction is opened.
*   **⚡ Persistent Monitoring (پایش پایدار و همیشگی)**
    Uses a highly optimized Foreground Service (`LockMonitorForegroundService`) coupled with a boot-up receiver (`BootReceiver`) to ensure active blockouts persist across system restarts and battery optimization cleanups.
*   **📊 Insights & Analytics Dashboard (نمودار آماری و تاریخچه)**
    Visualizes lock patterns using dynamic Material 3 charts showing historical hours locked and active statistics over a 30-day timeline.
*   **🎨 Slate Dark Visual Theme (تم تاریک مدرن)**
    A gorgeous, modern slate-colored visual theme adhering to full Material Design 3 guidelines, delivering an eye-friendly experience during late-night focus sessions.

---

## 🛠️ Technical Stack | ساختار فنی

- **Framework**: Jetpack Compose (Declarative UI with Material 3 components)
- **Architecture**: MVVM (Model-View-ViewModel) with Clean State Management
- **Reactive Streams**: Kotlin Coroutines & `MutableStateFlow` (collectAsStateWithLifecycle)
- **Local Database**: Room SQLite Database for storing persistent lock rules and historical logs safely and offline.
- **System Services**:
  - `AccessibilityService` (for package sniffing & overlay rendering)
  - `DeviceAdminReceiver` (for uninstall/force-stop prevention)
  - `ForegroundService` (for active block status & system persistence)

---

## ⚠️ Samsung & One UI Setup Guide (Android 13/14)
### 📖 راهنمای گام‌به‌گام رفع مشکل «تنظیمات محدود شده» در گوشی‌های سامسونگ

In modern Android versions (Android 13 & 14, including Samsung's One UI 5/6 on devices like **Galaxy S21 FE**), Google and Samsung enforce strict security features for apps installed from developer tools or external sources. When trying to activate the **Accessibility Service** for LockMinder, you might encounter the **"Restricted Setting" (تنظیمات محدود شده)** error, and the toggle will be greyed out.

Here is the exact step-by-step guide to bypass this restriction and fully unlock LockMinder:

در نسخه‌های جدید اندروید (۱۳ و ۱۴، به‌خصوص در رابط کاربری One UI سامسونگ روی گوشی‌هایی مانند **S21 FE**)، ویژگی‌های امنیتی شدیدی برای برنامه‌هایی که از منابع خارجی یا محیط‌های توسعه نصب می‌شوند اعمال می‌شود. هنگام فعال‌سازی **سرویس دسترسی (Accessibility)** با خطای **«تنظیمات محدود شده» (Restricted Setting)** مواجه می‌شوید و دکمه فعال‌سازی غیرفعال (طوسی) است.

**برای حل این مشکل و نمایان کردن دکمه و منوی سه نقطه، دقیقاً مراحل زیر را دنبال کنید:**

---

### 📲 گام اول: باز کردن بخش اطلاعات برنامه (App Info)
1. وارد **تنظیمات گوشی (Settings)** شوید.
2. به بخش **برنامه‌ها (Apps)** بروید.
3. برنامه **LockMinder** را در لیست پیدا کرده و روی آن ضربه بزنید تا صفحه **اطلاعات برنامه (App Info)** باز شود.

*(یا می‌توانید روی آیکون برنامه در صفحه خانگی نگه‌دارید و علامت تعجب کوچک **(i)** را بزنید).*

---

### 📲 گام دوم: فعال کردن مجوزهای محدود شده (بسیار مهم 🌟)
این دقیقاً همان مرحله‌ای است که اکثر کاربران به اشتباه آن را در صفحه Accessibility جستجو می‌کنند! سه نقطه در صفحه اطلاعات خود برنامه در تنظیمات اندروید قرار دارد:

1. در بالای صفحه **App Info (اطلاعات برنامه)** در گوشه بالا سمت راست (یا چپ در زبان فارسی)، به دنبال **آیکون سه نقطه (⋮)** بگردید.
2. روی **سه نقطه (⋮)** ضربه بزنید.
3. گزینه **اجازه به تنظیمات محدود شده (Allow restricted settings)** را انتخاب کنید.
4. سیستم از شما می‌خواهد با اثر انگشت یا رمز گوشی هویت خود را تأیید کنید. تأیید کنید.

> 💡 **نکته بسیار مهم:** اگر با زدن روی سه نقطه گزینه‌ای ندیدید یا سه نقطه وجود نداشت، مطمئن شوید که حداقل یک بار به صفحه Accessibility رفته و تلاش کرده‌اید سرویس را فعال کنید تا خطای Restricted ظاهر شود؛ سیستم عامل تنها پس از اولین تلاش ناموفق، این گزینه را در صفحه اطلاعات برنامه نمایش می‌دهد.

---

### 📲 گام سوم: فعال‌سازی نهایی سرویس دسترسی (Accessibility)
1. حالا به مسیر اصلی فعال‌سازی بازگردید: **تنظیمات گوشی > قابلیت دسترسی > برنامه‌های نصب‌شده** (Settings > Accessibility > Installed apps).
2. روی **LockMinder App Blocker** ضربه بزنید.
3. اکنون خواهید دید که دکمه دیگر طوسی و غیرفعال نیست! سوئیچ را روشن کرده و دسترسی را مجاز کنید.

---

### 📲 گام چهارم: فعال‌سازی مدیریت دستگاه (Device Admin)
برای جلوگیری از حذف ناگهانی برنامه در حین قفل بودن گوشی:
1. برنامه **LockMinder** را باز کنید.
2. به تب **تنظیمات (Settings)** بروید.
3. دکمه **فعال‌سازی مدیریت دستگاه (Activate Device Admin)** را بزنید.
4. در صفحه سیستمی باز شده، گزینه **Activate** یا **فعال‌سازی** را تأیید کنید.

حالا برنامه شما کاملاً مسلح شده و آماده است تا در کمال قدرت به شما در تمرکز و مدیریت زمان کمک کند! 💪

---

## 🏗️ Build and Compile Instructions

To build the APK or run local tests:

1. **Prerequisites**: Ensure you have Android SDK 34 and Gradle Kotlin DSL configured.
2. **Standard Local JVM Unit Tests**:
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. **Compile Debug APK**:
   ```bash
   gradle assembleDebug
   ```

---

## 🔒 Security & Privacy

LockMinder is fully **offline-first**. All list configurations, historical lock durations, and target package schedules are stored locally inside the secure **Room SQLite Database**. No personal data, active packages, or navigation histories are transmitted over network channels.
