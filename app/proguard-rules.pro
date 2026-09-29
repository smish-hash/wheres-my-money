# Proguard rules for Where's My Money app

# Keep Room entity and DAO classes
-keep class com.smish.wheresmymoney.data.local.entity.** { *; }
-keep class com.smish.wheresmymoney.data.local.dao.** { *; }

# Keep Firestore DTO models
-keep class com.smish.wheresmymoney.data.remote.** { *; }

# Keep Jetpack Glance & Widget classes
-keep class androidx.glance.** { *; }
-keep class com.smish.wheresmymoney.widget.** { *; }

# Keep WorkManager InputMergers
-keep public class * extends androidx.work.InputMerger {
    public <init>();
}
