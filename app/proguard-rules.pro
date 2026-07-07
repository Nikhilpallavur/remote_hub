# RemoteHub release keep rules. Compose, Hilt, OkHttp, Coil and kotlinx.serialization all ship
# their own consumer rules; only libraries doing reflective lookups R8 cannot see need help.

# BouncyCastle (Android TV pairing certificates) resolves algorithm implementations by class
# name at runtime through its provider registry, so R8 must not strip or rename them.
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# JDK-only APIs referenced from OkHttp/BouncyCastle code paths that never run on Android.
-dontwarn javax.naming.**
-dontwarn java.lang.management.**
