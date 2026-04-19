# Reglas para que la app sea ligera pero no se cierre
-keepattributes Signature, *Annotation*, InnerClasses
-keep class com.google.firebase.** { *; }
-keep class com.firebase.** { *; }

# Proteger los mensajes y datos de usuario
-keep class com.example.myapplication.ChatMessage { *; }
-keep class com.example.myapplication.UserListActivity$UserData { *; }

-keepclassmembers class com.example.myapplication.** {
    public void set*(***);
    public *** get*();
    public <init>();
}