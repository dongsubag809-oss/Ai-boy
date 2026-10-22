plugins { id("com.android.application") }
android {
 namespace = "com.energyblue.avatar"
 compileSdk = 35
 signingConfigs {
   create("stable") {
     storeFile = file("../signing/ai-boy.p12")
     storePassword = System.getenv("AI_BOY_KEYSTORE_PASSWORD") ?: ""
     keyAlias = "aiboy"
     keyPassword = System.getenv("AI_BOY_KEYSTORE_PASSWORD") ?: ""
     storeType = "PKCS12"
   }
 }
 defaultConfig { applicationId = "com.energyblue.avatar.v07"; minSdk = 26; targetSdk = 35; versionCode = 10; versionName = "1.0" }
 buildTypes { getByName("release") { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("stable") } }
}
