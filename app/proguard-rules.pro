# Add project specific ProGuard rules here.

# ===== 基础混淆优化 =====
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# ===== 移除所有日志（Release版本） =====
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** println(...);
}

# ===== 保留Android组件（系统通过反射调用） =====
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application
-keep public class * extends android.preference.Preference
-keep public class * extends android.app.admin.DeviceAdminReceiver

# ===== 保留设备管理员接收器 =====
-keep class com.featurephone.launcher.DeviceAdminReceiver { *; }

# ===== 保留ViewBinding生成的类 =====
-keep class * implements androidx.viewbinding.ViewBinding { *; }
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static ** inflate(...);
    public static ** bind(...);
}

# ===== 保留数据类构造函数 =====
-keepclassmembers class com.featurephone.launcher.** {
    public <init>(...);
}

# ===== 保留枚举 =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== 保留序列化相关 =====
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== 保留native方法 =====
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===== 保留View的构造函数和setter =====
-keepclassmembers public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
    public *** get*();
}

# ===== 保留Activity中通过XML引用的方法 =====
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# ===== 混淆保护：防止被轻易反编译 =====
-repackageclasses ''
-allowaccessmodification
-mergeinterfacesaggressively

# ===== 不警告AndroidX相关 =====
-dontwarn androidx.**
-keep class androidx.** { *; }
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }
