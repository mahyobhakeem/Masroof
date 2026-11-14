package com.masroofi.app.data.cloud

/**
 * إعدادات الربط السحابي مع Firebase Authentication و Cloud Firestore
 *
 * تعليمات تفعيل السحابة الحقيقية عبر الإنترنت (خلال دقيقتين مجاناً):
 * 1. افتح منصة Firebase Console عبر الرابط: https://console.firebase.google.com
 * 2. أنشئ مشروعاً جديداً باسم Masroof (أو اختر مشروعاً قائماً).
 * 3. من إعدادات المشروع (Project Settings) انسخ:
 *    - معرف المشروع (Project ID) وضعه في FIREBASE_PROJECT_ID أدناه.
 *    - مفتاح الويب (Web API Key) وضعه في FIREBASE_API_KEY أدناه.
 * 4. من قسم المصادقة (Authentication) -> طريقة تسجيل الدخول (Sign-in method):
 *    - فعّل خيار البريد الإلكتروني وكلمة المرور (Email/Password).
 * 5. من قسم قاعدة البيانات (Firestore Database):
 *    - أنشئ قاعدة بيانات في وضع الإنتاج وانسخ القواعد من ملف firestore.rules المرفق في المشروع.
 */
object CloudConfig {

    /**
     * مفتاح الويب لخدمات Firebase (Web API Key)
     * يمكن تعيينه هنا مباشرة أو تخصيصه لاحقاً من واجهة الإعدادات
     */
    const val DEFAULT_FIREBASE_API_KEY: String = "AIzaSyAXCfSeu46u_pnx9844xes96Ie3TIFIdZA"

    /**
     * معرّف مشروع Firebase السحابي (Project ID)
     * مثال: masroof-app-cloud
     */
    const val DEFAULT_FIREBASE_PROJECT_ID: String = "masroof-bf45e"

    /**
     * الرابط الأساسي لخدمة مصادقة الهوية (Firebase Identity Toolkit REST API)
     */
    const val AUTH_BASE_URL: String = "https://identitytoolkit.googleapis.com/v1"

    /**
     * الرابط الأساسي لتجديد الرموز المميزة للأمان (Secure Token)
     */
    const val TOKEN_REFRESH_BASE_URL: String = "https://securetoken.googleapis.com/v1/token"

    /**
     * توليد الرابط الأساسي لمستندات Firestore بناءً على معرّف المشروع
     */
    fun getFirestoreDocumentsUrl(projectId: String): String {
        return "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
    }

    /**
     * التحقق مما إذا كانت بيانات الاعتماد السحابية مهيأة وصالحة للاتصال بالإنترنت
     */
    fun isConfigured(apiKey: String?, projectId: String?): Boolean {
        val effectiveKey = apiKey?.trim() ?: DEFAULT_FIREBASE_API_KEY.trim()
        val effectiveProject = projectId?.trim() ?: DEFAULT_FIREBASE_PROJECT_ID.trim()
        return effectiveKey.isNotBlank() && effectiveProject.isNotBlank()
    }
}
