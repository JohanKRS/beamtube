# Regras do R8 para o Doorglow.
#
# O R8 otimiza e enxuga o código das bibliotecas (Compose, Media3, Kotlin, Coil) e do app. O motor
# (MediaServiceCore/SharedModules) lê as respostas do YouTube por reflexão (Gson, JsonPath, Retrofit,
# campos anotados) e chama código nativo (V8, Cronet): ele e as bibliotecas dele ficam inteiros, como
# no build sem R8.

# Nomes intactos: os logs e os erros continuam legíveis (tag Doorglow), e nada que use nome por
# reflexão quebra. O ganho do R8 vem de otimizar e tirar o que não é usado, não de encurtar nomes.
-dontobfuscate

-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions,SourceFile,LineNumberTable
-keepattributes *Annotation*,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# O motor e os módulos dele (youtubeapi, mediaserviceinterfaces, sharedutils, j2v8...).
-keep class com.liskovsoft.** { *; }
-keep interface com.liskovsoft.** { *; }
-keep enum com.liskovsoft.** { *; }

# Bibliotecas que o motor usa por reflexão ou por código nativo.
-keep class com.eclipsesource.v8.** { *; }
-keep class org.chromium.** { *; }
-keep class com.google.net.cronet.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.jayway.jsonpath.** { *; }
-keep class net.minidev.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class io.reactivex.** { *; }
-keep class com.grack.nanojson.** { *; }
-keep class com.florianingerl.** { *; }
-keep class org.apache.commons.io.** { *; }

# Também do motor: JNI do Cronet (J.N, gen.*), DNS, Brotli, logs de rede e utilidades.
-keep class J.** { *; }
-keep class gen._base.** { *; }
-keep class gen._net.** { *; }
-keep class org.brotli.** { *; }
-keep class info.guardianproject.** { *; }
-keep class com.localebro.** { *; }
-keep class com.itkacher.** { *; }
-keep class com.jakewharton.** { *; }
-dontwarn org.objectweb.asm.**

# Atalhos do app para dentro do motor (busca com filtros, chamadas que o motor não tem).
-keep class ytslim.data.EngineSearch { *; }
-keep class ytslim.data.EngineHttp { *; }

# O que as bibliotecas do motor referenciam e não existe no Android (ou não está no APK).
-dontwarn javax.annotation.**
-dontwarn javax.lang.model.**
-dontwarn javax.naming.**
-dontwarn javax.script.**
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn kotlin.annotations.jvm.**
-dontwarn okhttp3.internal.platform.**
-dontwarn retrofit2.**
-dontwarn com.jayway.jsonpath.**
-dontwarn net.minidev.**
-dontwarn okhttp3.brotli.**
-dontwarn sun.net.spi.nameservice.**
-keep class okhttp3.brotli.** { *; }
-keep class org.xbill.DNS.** { *; }
