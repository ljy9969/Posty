# kotlinx.serialization — @Serializable 클래스의 serializer 가 리플렉션 없이 유지되도록.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.unboundapex.posty.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.unboundapex.posty.**$$serializer { *; }
