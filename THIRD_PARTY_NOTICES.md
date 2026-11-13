# Third-party notices

BeamTube's own code is under the MIT License (see [LICENSE](LICENSE)). The app is built on other
people's open-source work, and each piece keeps its own license. This file lists what ships inside
the APK, and the full license texts are in the [licenses](licenses/) folder. The same files are
packed into the APK under `assets/licenses/`.

None of them requires BeamTube's own code to use any particular license.

## The playback engine

| Component | License | Copyright |
| --- | --- | --- |
| [MediaServiceCore](https://github.com/yuliskov/MediaServiceCore) (from SmartTube) | MIT | (c) 2020-present yuliskov |
| [SharedModules](https://github.com/yuliskov/SharedModules) (from SmartTube) | MIT | (c) 2020-present yuliskov |
| [J2V8](https://github.com/eclipsesource/J2V8), as included in SharedModules | EPL 1.0 | EclipseSource and others |
| V8 JavaScript engine, inside the J2V8 native library | BSD 3-Clause | the V8 project authors |
| [Apache Commons IO](https://commons.apache.org/proper/commons-io/) 2.8.0, as included in SharedModules | Apache 2.0 | The Apache Software Foundation |

BeamTube uses MediaServiceCore and SharedModules unchanged, as git submodules. The source code of
J2V8 is available from the link above and from the SharedModules repository.

Apache Commons IO NOTICE:

> Apache Commons IO
> Copyright 2002-2020 The Apache Software Foundation
>
> This product includes software developed at
> The Apache Software Foundation (https://www.apache.org/).

## Libraries

| Component | License |
| --- | --- |
| AndroidX and Jetpack Compose (activity, core, lifecycle, navigation, datastore, Compose UI, foundation, Material 3, Material icons, Compose for TV, Media3 / ExoPlayer, profileinstaller and their dependencies), by The Android Open Source Project | Apache 2.0 |
| Protocol Buffers, inside androidx.datastore | BSD 3-Clause |
| Kotlin standard library, kotlinx.coroutines, kotlinx.serialization, JetBrains annotations, Compose Multiplatform runtime pieces, by JetBrains | Apache 2.0 |
| [Coil](https://github.com/coil-kt/coil) 3, by Coil Contributors | Apache 2.0 |
| [Haze](https://github.com/chrisbanes/haze), by Chris Banes | Apache 2.0 |
| [Accompanist](https://github.com/google/accompanist) drawable painter, by Google | Apache 2.0 |
| [ZXing](https://github.com/zxing/zxing) core, by ZXing authors | Apache 2.0 |
| [OkHttp](https://github.com/square/okhttp) 3 (with logging, DNS over HTTPS and Brotli), [Okio](https://github.com/square/okio), [Retrofit](https://github.com/square/retrofit) and its Gson converter, by Square | Apache 2.0 |
| [Gson](https://github.com/google/gson), [Guava](https://github.com/google/guava), Error Prone annotations, J2ObjC annotations, JSR 305 annotations, by Google and others | Apache 2.0 |
| [Cronet](https://chromium.googlesource.com/chromium/src/+/main/components/cronet/) (org.chromium.net) and cronet-okhttp, by The Chromium Authors | BSD 3-Clause (Cronet), Apache 2.0 (cronet-okhttp); Cronet also contains third-party code listed in the Chromium source |
| [RxJava 2](https://github.com/ReactiveX/RxJava) and RxAndroid, by RxJava Contributors | Apache 2.0 |
| [Reactive Streams](https://www.reactive-streams.org/) | CC0 1.0 (public domain dedication) |
| [JsonPath](https://github.com/json-path/JsonPath), [json-smart and accessors-smart](https://github.com/netplex/json-smart-v2) | Apache 2.0 |
| [nanojson](https://github.com/mmastrac/nanojson), by Matt Mastracci | Apache 2.0 |
| [DiskLruCache](https://github.com/JakeWharton/DiskLruCache), by Jake Wharton | Apache 2.0 |
| [NetCipher](https://github.com/guardianproject/NetCipher), by The Guardian Project | Apache 2.0 |
| OkHttp Profiler (com.localebro), by LocaleBro | Apache 2.0 |
| [JSpecify](https://jspecify.dev/) annotations | Apache 2.0 |
| [dnsjava](https://github.com/dnsjava/dnsjava) 2.1.9, by Brian Wellington | BSD 2-Clause |
| [ASM](https://asm.ow2.io/) 5, by INRIA and France Telecom | BSD 3-Clause |
| [Brotli](https://github.com/google/brotli) decoder, by the Brotli Authors | MIT |
| [Checker Framework](https://checkerframework.org/) qualifiers | MIT |
| [SLF4J](https://www.slf4j.org/) API, by QOS.ch | MIT |
| [com.florianingerl.util.regex](https://github.com/florianingerl/com.florianingerl.util.regex), by Florian Ingerl | MIT (see note) |

Note on com.florianingerl.util.regex: the project's repository is under the MIT License, while the
Maven metadata of version 1.1.11 still says "GPL v2". The library is derived from OpenJDK's
`java.util.regex`, which is licensed as GPL v2 with the Classpath Exception; that exception allows
it to be linked with independent code under any license, so it does not change BeamTube's license.

## Font

| Component | License |
| --- | --- |
| [Outfit](https://github.com/Outfitio/Outfit-Fonts), by The Outfit Project Authors | SIL Open Font License 1.1 |

## License texts

- [Apache-2.0.txt](licenses/Apache-2.0.txt)
- [MIT.txt](licenses/MIT.txt), with the copyright notice of each MIT component
- [BSD-3-Clause.txt](licenses/BSD-3-Clause.txt), with the copyright notice of each BSD 3-Clause component
- [BSD-2-Clause.txt](licenses/BSD-2-Clause.txt) (dnsjava)
- [EPL-1.0.txt](licenses/EPL-1.0.txt) (J2V8)
- [Outfit-OFL.txt](licenses/Outfit-OFL.txt)

## Trademarks

YouTube is a trademark of Google LLC. Android, Android TV and Google TV are trademarks of Google
LLC. BeamTube is not affiliated with, endorsed by, sponsored by or approved by Google LLC or
YouTube. Product names are used only to describe what the app works with.
