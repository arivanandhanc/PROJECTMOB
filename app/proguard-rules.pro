# OruBar Pay — R8/ProGuard rules.
# The app has no reflection-heavy code; parser is plain Kotlin.

# Keep ZXing capture classes referenced from XML/intents.
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }

# Keep our parser model (defensive; it is referenced across activities).
-keep class in.arivanandhan.orubar.core.** { *; }

# Standard Android/Kotlin keeps handled by default optimize file.
-dontwarn org.jetbrains.annotations.**
