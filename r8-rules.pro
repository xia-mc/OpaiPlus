-keep class * {
    public static void main(java.lang.String[]);
}
-keep,allowobfuscation,allowoptimization class * extends today.opai.api.Extension
-keep,allowoptimization,allowshrinking class it.unimi.dsi.fastutil.** { *; }
-keepattributes *Annotation*

-repackageclasses __pycache__
-assumenosideeffects class java.lang.AssertionError { <init>(...); }
