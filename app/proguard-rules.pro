# R8 全模式 + shrinkResources 已自动剔除未引用的类/方法/资源。
# 这里只补充 AGP 默认规则未覆盖的项。

-allowaccessmodification
-repackageclasses

# 允许不同签名的方法复用同一名字，压缩 dex 的方法名字符串池（对启动期反射无影响，
# 本项目无按方法名反射的代码）。可稳定削减数十字节级字符串与符号开销。
-overloadaggressively

# 注解属性由 AGP 默认规则提供，不重复声明 -keepattributes（会撑大字符串池）。
# 本项目无按名反射，@Composable 均为静态调用，交给 R8 充分做类合并与死代码消除。

# Kotlin When 映射枚举，保留其字段供反射访问
-keepclassmembers class **$WhenMappings { <fields>; }

# 忽略 JVM-only API 缺失类警告
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**

# ---- 体积：剔掉库里的日志调用 ----
# media3 / miuix 里有大量 Log.d/i/v(w) 调用，参数里还带着字符串拼接；
# R8 一旦知道这些调用没有副作用，就会把调用连同拼接一起删掉。
# error 保留：真出问题时还看得到。
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
