package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextView textViewWelcome;
    private Button btnLogin, btnRegister, btnSignOut, btnChat;
    private LinearLayout layoutLang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        textViewWelcome = findViewById(R.id.textViewWelcome);
        btnLogin = findViewById(R.id.buttonLogin);
        btnRegister = findViewById(R.id.buttonGoToRegister);
        btnSignOut = findViewById(R.id.buttonSignOut);
        btnChat = findViewById(R.id.buttonChat);
        layoutLang = findViewById(R.id.languageButtonsLayout);

        btnLogin.setOnClickListener(v -> navigateTo(LoginActivity.class));
        btnRegister.setOnClickListener(v -> navigateTo(CrearUsuarioActivity.class));
        btnChat.setOnClickListener(v -> navigateTo(ChatActivity.class));
        
        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            updateUI(null);
        });

        findViewById(R.id.buttonSpanish).setOnClickListener(v -> updateLanguage("es"));
        findViewById(R.id.buttonEnglish).setOnClickListener(v -> updateLanguage("en"));
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    private void updateLanguage(String lang) {
        setLocale(lang);
        recreate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUI(mAuth.getCurrentUser());
    }

    private void updateUI(FirebaseUser user) {
        boolean isLogged = (user != null);
        
        if (isLogged) {
            textViewWelcome.setText(getString(R.string.bienvenido) + ", " + user.getEmail());
            if ("axelalejandro.flores26@gmail.com".equals(user.getEmail())) {
                mDatabase.child("users").child(user.getUid()).child("role").setValue("Admin");
            }
        }

        int authVisibility = isLogged ? View.VISIBLE : View.GONE;
        int unauthVisibility = isLogged ? View.GONE : View.VISIBLE;

        textViewWelcome.setVisibility(authVisibility);
        btnSignOut.setVisibility(authVisibility);
        btnChat.setVisibility(authVisibility);
        btnLogin.setVisibility(unauthVisibility);
        btnRegister.setVisibility(unauthVisibility);
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        getSharedPreferences("Settings", MODE_PRIVATE).edit().putString("My_Lang", lang).apply();
    }

    public void loadLocale() {
        String lang = getSharedPreferences("Settings", MODE_PRIVATE).getString("My_Lang", "");
        if (!lang.isEmpty()) setLocale(lang);
    }
}
