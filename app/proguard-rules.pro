# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   https://developer.android.com/studio/build/shrink-code.html
-keep class com.omarea.vtools.** { *; }
-keep class com.omarea.vtools.core.* { *; }
-dontwarn androidx.**
-dontwarn android.app.**
-dontwarn android.view.**
-dontwarn android.service.**
