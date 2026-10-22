# kotlinx.serialization: згенеровані серіалізатори моделей даних
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class ua.x44to.shiftplanner.**$$serializer { *; }
-keepclassmembers class ua.x44to.shiftplanner.** { *** Companion; }
-keepclasseswithmembers class ua.x44to.shiftplanner.** { kotlinx.serialization.KSerializer serializer(...); }
