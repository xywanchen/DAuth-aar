# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.cyberflow.dauthsdk.login.** { *; }
-keep class com.cyberflow.dauthsdk.wallet.sol.** { *; }
-keep class com.cyberflow.dauthsdk.mpc.entity.** { *; }
-keep class com.cyberflow.dauthsdk.api.** { *; }

# Keep all JNI classes and their methods and fields
-keepclasseswithmembers class * {
    native <methods>;
}

-keep class org.web3j.** { *; }
