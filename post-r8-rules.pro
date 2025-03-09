-keepclasseswithmembers class * {
    public static void main(java.lang.String[]);
}
-keep,allowobfuscation,allowoptimization class * extends today.opai.api.Extension
-keepattributes RuntimeVisibleAnnotations

-repackageclasses __pycache__
-assumenosideeffects class java.lang.AssertionError { <init>(...); }
-renamesourcefileattribute
