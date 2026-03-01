package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class CrearUsuarioActivity extends AppCompatActivity {

    private static final String TAG = "CrearUsuarioActivity";

    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_usuario);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);

        Button buttonRegister = findViewById(R.id.buttonRegister);
        buttonRegister.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(CrearUsuarioActivity.this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(CrearUsuarioActivity.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            createAccount(email, password);
        });
    }

    private void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        sendVerificationEmail();
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            // If user already exists, try to sign in and send verification email
                            signInAndSendVerification(email, password);
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(CrearUsuarioActivity.this, "Error en el registro.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signInAndSendVerification(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        sendVerificationEmail();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(CrearUsuarioActivity.this, "La contraseña es incorrecta.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(CrearUsuarioActivity.this, "Correo de verificación enviado. Revise su bandeja de entrada.", Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, "sendEmailVerification", task.getException());
                    Toast.makeText(CrearUsuarioActivity.this, "No se pudo enviar el correo de verificación.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
