# NOVA STORE - Backend PHP & MySQL Integration Guide
# دليل ربط موقع وسيرفر متجر نوفا

يحتوي هذا المجلد على الكود البرمجي الكامل لخلفية النظام (Backend) لمتجر **NOVA STORE** المكتوب بلغة **PHP 7.4 / 8.x** مع قاعدة بيانات **MySQL / MariaDB**.

---

## 1. محتويات المجلد:
1. **`schema.sql`**: ملف تهيئة وإنشاء جداول قاعدة البيانات:
   - `users` (المستخدمين، كلمات المرور المشفرة بـ `password_hash`، الحالات، التواريخ).
   - `user_tokens` (التوكنات المشفرة بـ `sha256`، تاريخ الانتهاء، الإلغاء، اسم الجهاز).
   - `categories` (أقسام المتجر).
   - `products` (المنتجات، الأسعار، الصور، المخزون، التقييم).
   - `orders` و `order_items` (الطلبات وتفاصيل السلة).
2. **`config.php`**: ملف الإعدادات والاتصال الآمن بـ PDO وحماية CORS ورؤوس الأمان.
3. **`auth.php`**: محرك الأمان، التحقق من `X-API-Key`، تشفير الجلسات، التحقق من التوكن، واسترجاع بيانات المستخدم.
4. **`api.php`**: موزع الـ REST API الرئيسي لجميع العمليات.

---

## 2. خطوات التثبيت على الاستضافة (InfinityFree / cPanel):

### الخطوة الأولى: إنشاء قاعدة البيانات واستيراد الجداول
1. ادخل إلى لوحة التحكم (Control Panel) في الاستضافة.
2. توجه إلى **MySQL Databases** وأنشئ قاعدة بيانات جديدة باسم مثلاً `nova_store`.
3. افتح **phpMyAdmin**.
4. اختر قاعدة البيانات الجديدة واضغط على تبويب **Import (استيراد)**.
5. اختر ملف `backend_php/schema.sql` واضغط **Go (تنفيذ)**.

### الخطوة الثانية: ضبط بيانات الاتصال في `config.php`
افتح ملف `config.php` وقم بتعديل الثوابت:
```php
define('DB_HOST', 'sql205.infinityfree.com'); // سيرفر قاعدة البيانات الخاص بك
define('DB_NAME', 'if0_38000000_nova_store'); // اسم قاعدة البيانات
define('DB_USER', 'if0_38000000');            // اسم المستخدم
define('DB_PASS', 'كلمة_مرور_قاعدة_البيانات'); // كلمة مرور قاعدة البيانات
```

### الخطوة الثالثة: رفع الملفات عبر FTP أو File Manager
قم برفع الملفات التالية إلى المجلد `/htdocs/nova/` في موقعك:
```
/htdocs/nova/api.php
/htdocs/nova/config.php
/htdocs/nova/auth.php
```
بحيث تصبح روابط الـ API متاحة على:
`https://nova1.ct.ws/nova/api.php?endpoint=health`

---

## 3. نقاط النهاية المعتمدة (REST API Endpoints):

| الـ Endpoint | الطريقة | الهيدرز المطلوبة | الوظيفة |
| :--- | :--- | :--- | :--- |
| `health` | `GET` | `X-API-Key` | فحص اتصال السيرفر وقاعدة البيانات |
| `categories` | `GET` | `X-API-Key` | استرجاع جميع الأقسام |
| `products` | `GET` | `X-API-Key` | استرجاع المنتجات مع إمكانية الفلترة والبحث |
| `product` | `GET` | `X-API-Key` | تفاصيل منتج محدد (`&id=1`) |
| `register` | `POST` | `X-API-Key` | تسجيل مستخدم جديد وإرجاع التوكن |
| `login` | `POST` | `X-API-Key` | تسجيل الدخول وإرجاع التوكن |
| `me` | `GET` | `X-API-Key`, `Authorization: Bearer <TOKEN>` | استرجاع بيانات الحساب المسجل |
| `profile_update` | `POST` | `X-API-Key`, `Authorization: Bearer <TOKEN>` | تعديل الاسم ورقم الهاتف |
| `change_password` | `POST` | `X-API-Key`, `Authorization: Bearer <TOKEN>` | تغيير كلمة المرور بأمان |
| `delete_account` | `POST` | `X-API-Key`, `Authorization: Bearer <TOKEN>` | حذف الحساب (Soft Delete) وإلغاء الجلسات |
| `logout` | `POST` | `X-API-Key`, `Authorization: Bearer <TOKEN>` | إلغاء التوكن الحالي وتسجيل الخروج |
| `orders` | `GET/POST` | `X-API-Key`, `Authorization: Bearer <TOKEN>` | عرض طلبات المستخدم أو إرسال طلب جديد |

---

## 4. معايير الأمان المطبقة:
- لا يتم حفظ كلمات المرور بنص صريح إطلاقاً، بل باستخدام دالة `password_hash()` بخوارزمية Bcrypt الآمنة.
- الـ Token الخام لا يُحفظ في قاعدة البيانات، بل يتم تشفيره بـ `SHA-256` قبل الحفظ.
- التحقق التام بالـ Prepared Statements ضد هجمات SQL Injection.
- عدم إظهار أي أخطاء حساسة أو Database Credentials في استجابة الـ JSON.
