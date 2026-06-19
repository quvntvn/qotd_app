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

# --- Gson : le modèle Quote est désérialisé par réflexion depuis res/raw/quotes.json ---
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Conserver les champs annotés @SerializedName (sinon R8 les renomme/supprime)
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Conserver intégralement le modèle de données
-keep class com.quvntvn.qotd_app.Quote { *; }