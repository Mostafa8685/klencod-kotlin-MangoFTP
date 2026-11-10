# 🥭 Mango FTP - Local Wi-Fi Router Storage & P2P Family Chat
> تطبيق أندرويد متكامل بلغة **Kotlin** لإدارة ومشاركة الملفات الضخمة عبر فلاشة راوتر الواي فاي المنزلي مع ميزة الدردشة اللحظية P2P بدون إنترنت.

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-orange.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-blue.svg)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blueviolet.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-35-red.svg)
![Clean Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-success.svg)

---

## 🌟 نظرة عامة على المشروع (Project Overview)

يعاني الكثير من المستخدمين في المنازل من صعوبة نقل ومشاركة الملفات الضخمة (مثل الفيديوهات، الأفلام، المسلسلات، والألبومات العائلية) بين أفراد الأسرة دون استهلاك باقة الإنترنت. 

يقوم **Mango FTP** بتحويل راوتر الواي فاي المنزلي العادي إلى **وحدة تخزين سحابية منزلية (Local NAS)** بالاستفادة من منفذ **USB** الموجود في معظم الراوترات الحديثة (TP-Link, Huawei, ZTE, Vodafone, WE, Orange, وغيرها) عبر بروتوكول FTP المحلي، بالإضافة لتوفير شبكة دردشة داخلية فورية (P2P Local Chat) بين هواتف العائلة المتصلة بنفس شبكة الواي فاي دون الحاجة لأي خادم وسيط أو إنترنت خارجي.

---

## ✨ المميزات الرئيسية (Key Features)

### 1. 📁 وحدة إدارة ملفات الراوتر (Router FTP Client Module)
- **الاتصال الذكي بالراوتر:** إمكانية ضبط وتخصيص IP الراوتر (افتراضياً `192.168.1.1`)، منفذ الاتصال (`21`)، وبيانات الدخول مع دعم الدخول كزائر (`Anonymous`).
- **تخزين آمن للإعدادات:** حفظ إعدادات الاتصال عبر `Jetpack DataStore Preferences`.
- **استعراض وتصنيف ذكي للملفات:** قراءة محتويات فلاشة الراوتر باستخدام مكتبة `commons-net:3.10.0` ودعم الوضع السلبي `Passive Mode` مع التصنيف التلقائي:
  - 🎬 **فيديوهات (Videos):** `mp4`, `mkv`, `avi`, `mov`, `webm`, `wmv`...
  - 🖼️ **صور (Images):** `jpg`, `png`, `gif`, `webp`, `svg`...
  - 🎵 **صوتيات (Audio):** `mp3`, `wav`, `m4a`, `flac`, `ogg`...
  - 📄 **مستندات (Documents):** `pdf`, `doc`, `docx`, `xls`, `xlsx`, `ppt`, `txt`...
  - 📦 **ملفات مضغوطة (Archives):** `zip`, `rar`, `7z`, `tar`, `gz`...
  - 📁 **مجلدات (Folders):** تصفح هرمي متكامل مع إمكانية إنشاء مجلدات جديدة والرجوع للخلف بسهولة.
- **🏷️ الرفع الذكي وهوية الناشر (Smart Uploading with Publisher Metadata):**
  - عند رفع أي ملف، يقوم التطبيق تلقائياً بإعادة تسميته برمجياً بالنمط:
    ```text
    [Original_Name]__by_[UserName]__device_[DeviceModel].[ext]
    ```
  - عند عرض قائمة الملفات، يقوم التطبيق بتفكيك الاسم وعرض اسم الملف نظيفاً مع بطاقة أنيقة توضح:
    > **رُفع بواسطة: فلان (اسم الهاتف)**
- **⚡ تنزيل وتشغيل فوري:**
  - نافذة تقدم حية لعمليات الرفع والتنزيل تعرض النسبة المئوية وسرعة النقل بالـ `MB/s` مع إمكانية الإلغاء.
  - تشغيل الملفات المنزلة مباشرة عبر التطبيقات المثبتة في الهاتف باستخدام `FileProvider` و `Intent.ACTION_VIEW`.

---

### 2. 💬 وحدة المحادثة العائلية المباشرة (P2P Local Chat Module)
- **اكتشاف تلقائي للأجهزة (Network Service Discovery - NSD):**
  - يعلن كل جهاز عن نفسه في الشبكة باسم: `MangoChat_[UserName]__device_[DeviceModel]` بنوع خدمة `_mangochat._tcp.`.
  - اكتشاف فوري لهواتف أفراد الأسرة المتصلين بالراوتر بدون الحاجة لإنترنت.
  - تفعيل `WifiManager.createMulticastLock` لضمان استقبال حزم mDNS/Bonjour على كافة أجهزة أندرويد.
- **قنوات اتصال مباشر (Local Sockets):**
  - تشغيل `ServerSocket` محلي على منفذ مخصص لاستقبال الرسائل في الخلفية عبر Coroutines `Dispatchers.IO`.
  - إرسال الرسائل عبر قنوات `Socket` مشفرة ومباشرة مع دعم البث العام لجميع الأجهزة المكتشفة أو المحادثة المباشرة مع جهاز محدد.
- **واجهة دردشة عصرية:**
  - شريط علوي يعرض الأجهزة المتصلة حالياً مع مؤشر حالة أخضر نابض.
  - فقاعات رسائل ملونة تعتمد طابع ألوان ثمرة المانجو (Mango Theme) مع عرض اسم المرسل، نوع الهاتف، وتوقيت الإرسال.

---

### 3. ⚙️ الملف الشخصي والإعدادات (Profile & Settings)
- **شاشة الإعداد الأولي (Onboarding):** تظهر لمرة واحدة عند أول تشغيل لطلب الاسم المعروض وقراءة موديل الهاتف تلقائياً من النظام عبر `android.os.Build.MODEL`.
- **شاشة الإعدادات:**
  - تعديل بيانات FTP مع زر "اختبار الاتصال" التفاعلي (Ping & Handshake).
  - تعديل الاسم وهوية الهاتف.
  - معلومات حول تطبيق Mango FTP والخصوصية الكاملة.

---

## 🎨 هوية التصميم (Mango Design System)

تم بناء الواجهة بالكامل باستخدام **Jetpack Compose** و **Material Design 3** بالاعتماد على لوحة ألوان دافئة مستوحاة من ثمرة المانجو:
- 🥭 **البرتقالي الأساسي (Mango Orange):** `#FF7A00`
- 🍯 **العنبر / الذهبي (Mango Amber / Gold):** `#FFB300`
- 🍃 **أخضر أوراق المانجو (Mango Leaf Green):** `#388E3C`
- 🥛 **السطح الكريمي الدافئ (Warm Cream Surface):** `#FFFDF8`

---

## 🏗️ هيكلية المشروع (Project Architecture)

يتبع المشروع معمارية **MVVM (Model-View-ViewModel)** مع مبادئ **Clean Architecture**:

```text
MangoFTP/
├── build.gradle.kts                # إعدادات البناء الرئيسية
├── settings.gradle.kts             # إعدادات الموديولات ومستودعات Gradle
├── gradle.properties               # خصائص أداء Gradle و AndroidX
├── gradlew & gradlew.bat           # سكربتات تنفيذ Gradle Wrapper
├── gradle/wrapper/                 # ملفات Gradle Wrapper 8.9
├── app/
│   ├── build.gradle.kts            # إعدادات تطبيق أندرويد والاعتماديات
│   ├── proguard-rules.pro          # قواعد حماية الكود
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml # الصلاحيات وإعدادات التطبيق
│           ├── java/com/mango/ftp/
│           │   ├── MangoApp.kt     # فئة التطبيق (Application Class)
│           │   ├── MainActivity.kt # النشاط الرئيسي والتعامل مع الصلاحيات والـ Intents
│           │   ├── data/
│           │   │   ├── model/      # نماذج البيانات (FtpFileItem, ChatMessage, PeerDevice, etc.)
│           │   │   ├── ftp/        # إدارة FTP (FtpManager, FileParser)
│           │   │   ├── p2p/        # الاتصال المحلي (NsdHelper, ChatSocketManager)
│           │   │   └── local/      # التخزين المحلي (PreferencesManager via DataStore)
│           │   └── ui/
│           │       ├── theme/      # سمات وألوان وخطوط المانجو (Color, Theme, Type)
│           │       ├── navigation/ # مسارات التنقل (Screen)
│           │       ├── components/ # العناصر المشتركة (TopBar, BottomNav, ProgressDialog)
│           │       ├── onboarding/ # شاشة البداية وضبط الاسم
│           │       ├── files/      # شاشة ملفات الراوتر (FileListScreen, FileListViewModel)
│           │       ├── chat/       # شاشة الشات المباشر (ChatScreen, ChatViewModel)
│           │       └── settings/   # شاشة الإعدادات (SettingsScreen, SettingsViewModel)
│           └── res/                # الموارد المرئية، الأيقونات، والألوان
└── README.md
```

---

## 🚀 كيفية تشغيل المشروع وتجميعه (How to Build & Run)

### المتطلبات الأساسية (Prerequisites):
1. **Android Studio** (نسخة Hedgehog 2023.1 أو Ladybug أو Koala أو أحدث).
2. **JDK 17** (مثبت ومختار في `Gradle JDK` داخل Android Studio).
3. هاتف بنظام أندرويد 7.0+ أو محاكي (Emulator).

### خطوات التجميع (Build Steps):
1. قم بفك ضغط ملف المشروع `MangoFTP_Project.zip`.
2. افتح **Android Studio** واختر **Open** ثم حدد مجلد `MangoFTP`.
3. سيبدأ أندرويد ستوديو بمزامنة ملفات Gradle تلقائياً (`Gradle Sync`).
4. اضغط على زر **Run 'app'** (أو `Shift + F10`) لتثبيت التطبيق على هاتفك.
5. لتجميع ملف APK نهائي: من القائمة اختر `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`.

---

## 🛠️ كيفية تفعيل مشاركة USB / FTP على الراوتر المنزلي

معظم راوترات المنازل تدعم مشاركة وحدة تخزين USB بسهولة:
1. قم بتوصيل فلاشة USB أو هارد ديسك خارجي بمنفذ الـ USB في الراوتر.
2. ادخل إلى لوحة تحكم الراوتر عبر المتصفح (غالباً `192.168.1.1`).
3. انتقل إلى قسم **USB Application** أو **Storage Sharing** أو **FTP Server**.
4. فعّل خيار **Enable FTP Server**.
5. حدد صلاحيات القراءة والكتابة (Read/Write) وأنشئ مستخدم أو اسمح بـ Anonymous.
6. افتح تطبيق **Mango FTP**، وانتقل إلى تبويب **الإعدادات** واضغط **اختبار الاتصال**!

---

## 📦 رفع المشروع على GitHub

```bash
cd MangoFTP
git init
git add .
git commit -m "Initial commit: Complete Mango FTP Android application"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/MangoFTP.git
git push -u origin main
```

---

## 📄 الترخيص (License)
هذا المشروع متاح تحت رخصة **MIT License**.
