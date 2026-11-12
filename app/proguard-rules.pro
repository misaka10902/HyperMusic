# R8 全模式 + shrinkResources 已自动剔除未引用的类/方法/资源。
# 这里只补充 AGP 默认规则未覆盖的项。

-allowaccessmodification
-repackageclasses

# 注解属性由 AGP 默认规则提供，不重复声明 -keepattributes（会撑大字符串池）。
# 本项目无按名反射，@Composable 均为静态调用，交给 R8 充分做类合并与死代码消除。

# Kotlin When 映射枚举，保留其字段供反射访问
-keepclassmembers class **$WhenMappings { <fields>; }

# 忽略 JVM-only API 缺失类警告
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**
