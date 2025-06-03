# Add project specific ProGuard rules here.

# Security-focused optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep source file names and line numbers for better crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove Logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Encrypt class names and obfuscate the code
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment

# Keep view classes with their constructors
-keepclassmembers class * extends android.view.View {
    <init>(android.content.Context);
    <init>(android.content.Context, android.util.AttributeSet);
    <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Prevent ProGuard from stripping interface information
-keepattributes InnerClasses,Signature,Exceptions,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

# OkHttp specific rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Keep Retrofit and related libraries
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# Security-specific rules
# Don't remove cryptographic providers
-keep class javax.crypto.** { *; }
-keep class javax.security.** { *; }
-keep class java.security.** { *; }
-keep class org.bouncycastle.** { *; }

# Don't strip security libraries
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }

# Keep SSL classes
-keep class com.android.org.conscrypt.** { *; }
-keep class org.conscrypt.** { *; }

# Remove debug information from private methods
-assumenosideeffects class * {
    private <methods>;
}
