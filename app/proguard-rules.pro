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

# Hilt
-keep class dagger.hilt.internal.aggregatedroot.codegen.* { *; }
-keep class com.astrizhachuk.pianoflow.Hilt_PianoFlowApplication { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep,allowobfuscation @interface dagger.hilt.android.HiltAndroidApp {*;}
-keep,allowobfuscation @interface dagger.hilt.android.AndroidEntryPoint {*;}
-keep,allowobfuscation @interface dagger.hilt.InstallIn {*;}
-keep,allowobfuscation @interface dagger.hilt.components.SingletonComponent {*;}
-keep,allowobfuscation @interface dagger.hilt.DefineComponent {*;}
-keep,allowobfuscation @interface dagger.hilt.DefineComponent.Builder {*;}
-keep @interface javax.inject.Inject {*;}
-keep @interface javax.inject.Qualifier {*;}
-keep @interface javax.inject.Scope {*;}
-keep,allowobfuscation @interface dagger.Module {*;}
-keep,allowobfuscation @interface dagger.Provides {*;}
-keep,allowobfuscation @interface dagger.Binds {*;}
-keep,allowobfuscation @interface dagger.BindsInstance {*;}
-keep,allowobfuscation @interface dagger.BindsOptionalOf {*;}
-keep,allowobfuscation @interface dagger.multibindings.IntoSet {*;}
-keep,allowobfuscation @interface dagger.multibindings.IntoMap {*;}
-keep,allowobfuscation @interface dagger.multibindings.StringKey {*;}
-keep,allowobfuscation @interface dagger.multibindings.ClassKey {*;}

# Generated Hilt code
-keep class **_HiltModules* { *; }
