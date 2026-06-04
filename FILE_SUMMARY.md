# ملخص ملفات المشروع

هذا الملف يصف وظيفة كل ملف ومجلد رئيسي في المشروع ليسهل عليك التعديل لاحقًا. الوصف موجز؛ أخبرني إذا أردت تفاصيل أعمق لكل ملف أو كل دالة.

**الجذر (Root)**
- `build.gradle.kts`: إعدادات Gradle للمشروع الجذري.
- `settings.gradle.kts`: تهيئة وحدات المشروع واسم المشروع.
- `gradle.properties`, `local.properties`: إعدادات بيئة البناء وخصائص النظام.
- `gradlew`, `gradlew.bat`: سكربتات تشغيل Gradle المضمنة.
- `build_log.txt`: سجل بناء سابق لمراجعات الأخطاء.
- `manifest.json`, `passenger.html`, `admin.html`, `index.html`, `landing.html`, `sw.js`: ملفات موقع/واجهة وخدمات (مستخدمة في توزيع الويب أو كأصول).

**مجلد `app/` (وحدة التطبيق الرئيسية)**
- `app/build.gradle.kts`: إعدادات Gradle لوحدة التطبيق.
- `app/src/main/AndroidManifest.xml`: تعريف الأنشطة، الأذونات، خدمات الخلفية، والمكونات الرئيسة للتطبيق.
- `app/src/main/res/values/strings.xml`: جميع السلاسل النصية (نصوص الواجهة) والقيم القابلة للترجمة.
- `app/src/main/res/values/themes.xml`, `colors.xml`: إعدادات السمات والألوان للتصميم.
- `app/src/main/res/xml/data_extraction_rules.xml`, `backup_rules.xml`: قواعد استخراج البيانات و/أو إعدادات النسخ الاحتياطي.
- `app/src/main/res/drawable/*`, `mipmap-*/*`: أيقونات ومصادر الصور الخاصة بالتطبيق.

**ملفات Kotlin (`app/src/main/java/com/dolmus/netapp/`)**
- `App.kt`: فئة الـ`Application` — تهيئة عامة عند بدء التطبيق.
- `MainActivity.kt`: النشاط الرئيسي الذي يستضيف واجهة المستخدم (Compose أو Views) ويتولى التوجيه بين الشاشات.
- `LoginScreen.kt`: شاشة تسجيل الدخول — تحتوي واجهة تسجيل الدخول ومنطق التحقق الأولي.
- `RegisterScreen.kt`: شاشة التسجيل — واجهة مستخدم لإنشاء حساب جديد وإرسال بيانات التسجيل.
- `HomeScreen.kt`: الشاشة الرئيسية بعد تسجيل الدخول — تعرض المحتوى الأساسي أو لوحة التحكم.
- `LiveMapScreen.kt`: شاشة الخريطة الحية — تعرض الموقع الحالي وتتبع المركبات/المستخدم.
- `RouteSelectScreen.kt`: واجهة اختيار المسار/الطريق من قائمة أو خريطة.
- `QrScreen.kt`: شاشة عرض/مسح رمز QR (قراءة أو إنشاء رموز QR).
- `LanguageScreen.kt`: واجهة اختيار اللغة وخيارات محلية.
- `CalibrationScreen.kt`: أدوات معايرة (ربما للمستشعرات أو الموقع).
- `LocationForegroundService.kt`: خدمة أمامية لتتبع الموقع تعمل في الخلفية وتضمن استمرار استقبال إحداثيات.
- `SessionManager.kt`: إدارة جلسة المستخدم (تخزين/استرجاع التوكنات، حالة الدخول، انتهاء الجلسة).
- `SupabaseConfig.kt`: إعدادات الاتصال بـ Supabase (نقاط النهاية، مفاتيح، تهيئة SDK).
- `QrScreen.kt`: (مكرر إن وُجد) مسؤول عن واجهة QR.

**مجلد `ui/theme/`**
- `Theme.kt`, `Color.kt`, `Type.kt`: عناصر واجهة المستخدم الموحدة (ألوان، أنماط نص، سمات Compose/Material).

**مجلد `dolmus-net/app/` (نسخة أو وحدة موازية)**
- يحتوي على إعدادات وملفات مشابهة للوحدة الرئيسية: `build.gradle.kts`, `AndroidManifest.xml`, الموارد وملفات Kotlin مكررة أو خاصة بإصدار/فرع آخر من التطبيق.
- ملفات Kotlin المتوفرة هنا: `MainActivity.kt`, `LoginScreen.kt`, `RegisterScreen.kt`, `HomeScreen.kt`, `LiveMapScreen.kt`, `LanguageScreen.kt`, `CalibrationScreen.kt`, `SessionManager.kt`, `SupabaseConfig.kt`, `WorkReportScreen.kt` — تستخدم لعرض تقارير الأعمال/المهام.

**اختبارات ومجلدات إضافية**
- `dolmus-net/app/src/test/.../ExampleUnitTest.kt`: اختبارات وحدة بسيطة.
- `dolmus-net/app/src/androidTest/.../ExampleInstrumentedTest.kt`: اختبارات آلية على الجهاز.

---

نقاط للتحسين أو المراجعة التالية (إن رغبت):
- توضيح واجهة كل ملف: الدوال العامة، نقاط الدخول، وملفات الاعتماد (dependencies).
- استخراج خريطة تبعيات بين الملفات (من يستدعي من، واستخدام `SessionManager`, `SupabaseConfig`).
- توليد ملف README مختصر لكل حزمة (`/app/src/main/java/com/dolmus/netapp/`) أو توثيق لكل شاشة.

إذا كنت ترغب، أستطيع الآن:
- توليد تفصيل أعمق لكل ملف (دوال، فئات، المدخلات/المخرجات).
- إنشاء README لكل حزمة أو ملف `FILE_SUMMARY.md` مفصل أكثر.

أخبرني ماذا تفضّل كخطوة تالية.
