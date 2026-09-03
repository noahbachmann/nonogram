# ---------------------------------------------------------------------------
# kotlinx.serialization
#
# R8 breaking serialization is the classic way a working debug build ships
# broken: the generated `$$serializer` objects and `Companion.serializer()` are
# reached only reflectively, so the shrinker cannot see the edge. The exposed
# surface here is navigation/Routes.kt — eight of the eleven routes are
# `@Serializable object`, which is exactly the case the INSTANCE rule below
# covers — plus the board-state and solution JSON.
#
# These are the upstream R8 rules from the kotlinx.serialization README,
# specialised to this package.
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Companion object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# serializer() on the companion objects of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# INSTANCE.serializer() of serializable objects — the @Serializable object routes.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.trainpaths.nonogram.**$$serializer { *; }
-keepclassmembers class com.trainpaths.nonogram.** {
    *** Companion;
}
-keepclasseswithmembers class com.trainpaths.nonogram.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# SQLDelight
#
# The generated NonogramDb queries are ordinary code and shrink fine; the
# runtime's driver/schema plumbing is the part R8 has trouble tracing.
# ---------------------------------------------------------------------------
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# ---------------------------------------------------------------------------
# Enum entries are read back by name from the database and from Firestore
# (Difficulty, PublishStatus, ColorTheme) — valueOf/entries is reflective.
# ---------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** entries();
}

# ---------------------------------------------------------------------------
# Firebase / Firestore
#
# No blanket keep: every read and write in sync/ goes through explicit
# Map<String, Any> field access, never Firestore's reflective POJO mapping, so
# the SDK's own consumer rules are enough. Keeping com.google.firebase.** would
# just stop Firestore — the largest dependency here — from shrinking at all.
# ---------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn com.google.firebase.**

# ---------------------------------------------------------------------------
# Coroutines
# ---------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------------
# Strip the debug logs from the release binary.
#
# ~38 println sites across shared/ — none log a token, email or credential,
# but they carry symbol names and sync detail that need not ship.
# ---------------------------------------------------------------------------
-assumenosideeffects class java.io.PrintStream {
    public void println(%);
    public void println(**);
    public void print(%);
    public void print(**);
}
