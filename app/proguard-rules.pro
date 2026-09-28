# Proguard rules for Where's My Money app

# Keep Room entity and DAO classes
-keep class com.smish.wheresmymoney.data.local.entity.** { *; }
-keep class com.smish.wheresmymoney.data.local.dao.** { *; }

# Keep Firestore DTO models
-keep class com.smish.wheresmymoney.data.remote.** { *; }
