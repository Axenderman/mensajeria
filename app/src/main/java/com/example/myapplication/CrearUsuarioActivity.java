package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CrearUsuarioActivity extends AppCompatActivity {

    private static final String TAG = "CrearUsuarioActivity";

    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private EditText editTextUsername;
    private EditText editTextTelefono;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_usuario);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextTelefono = findViewById(R.id.editTextTelefono);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);

        Button buttonRegister = findViewById(R.id.buttonRegister);
        buttonRegister.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String username = editTextUsername.getText().toString().trim();
            String telefono = editTextTelefono.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || username.isEmpty() || telefono.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(CrearUsuarioActivity.this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(CrearUsuarioActivity.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            createAccount(email, password, username, telefono);
        });
    }

    private void createAccount(String email, String password, String username, String telefono) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserData(user.getUid(), email, username, telefono);
                        }
                        sendVerificationEmail();
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            signInAndSendVerification(email, password);
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(CrearUsuarioActivity.this, "Error en el registro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserData(String userId, String email, String username, String telefono) {
        String role = "Usuario";
        if (email.equals("axelalejandro.flores26@gmail.com")) {
            role = "Admin";
        }

        mDatabase.child("users").child(userId).child("email").setValue(email);
        mDatabase.child("users").child(userId).child("role").setValue(role);
        mDatabase.child("users").child(userId).child("username").setValue(username);
        mDatabase.child("users").child(userId).child("telefono").setValue(telefono);
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