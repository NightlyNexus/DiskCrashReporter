Disk Crash Reporter
=====================

Get easy persistent crash logs on Android app debug builds. No code necessary. Add one dependency for your debug builds and never lose track of a crash while away from your computer again.

When your app crashes, [`DiskCrashReporter`](diskcrashreporter/src/main/kotlin/com/nightlynexus/diskcrashreporter/DiskCrashReporter.kt) stores the stack trace in its own text file in your app’s external files directory. DiskCrashReporter then posts a notification and propagates the crash. The crash logger installs itself transparently with a ContentProvider.

Download
--------

Gradle:

```groovy
debugImplementation 'com.nightlynexus.diskcrashreporter:diskcrashreporter:0.2.1'
```

License
--------

    Copyright 2023 Eric Cochran

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
