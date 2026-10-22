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
 defaultConfig { applicationId = "com.energyblue.avatar.v10"; minSdk = 26; targetSdk = 35; versionCode = 11; versionName = "1.1" }
 buildTypes { getByName("release") { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("stable") } }
}

dependencies { implementation("com.google.mediapipe:tasks-vision:0.10.14") }
