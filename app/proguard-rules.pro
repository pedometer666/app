# -----------------------------------------------------------------------------
# Pedometer release-build keep rules
# -----------------------------------------------------------------------------
# Room's generated code and Kotlin metadata are consumed via reflection during
# builds, so keep the metadata attributes for tooling like KSP/Room.
-keepattributes *Annotation*, Signature, EnclosingMethod, InnerClasses

# Keep Room database and entity classes (fields are accessed reflectively in
# some edge cases, e.g. auto-migration).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }

# Keep data classes used as DAO query projections.
-keep class com.pedometer.app.data.local.MonthlyTotals { *; }

# Coroutines / DataStore rely on standard Kotlin metadata.
-dontwarn kotlinx.coroutines.**
