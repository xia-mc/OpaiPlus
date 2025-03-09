-keepclasseswithmembers class * {
    public static void main(java.lang.String[]);
}
-keep,allowobfuscation,allowoptimization class * extends today.opai.api.Extension
-keepattributes RuntimeVisibleAnnotations

-repackageclasses __pycache__
-assumenosideeffects class java.lang.AssertionError { <init>(...); }

# R8的静态分析不是无敌的😭
-dontoptimize
