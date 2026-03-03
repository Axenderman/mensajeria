package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder> adapter;
    private RecyclerView listOfMessages;
    private EditText input;
    private Button sendButton;
    private Button buttonVideoLlamada;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userRole = "Usuario";

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView messageUser;
        Button deleteButton;

        public MessageViewHolder(View v) {
            super(v);
            messageText = v.findViewById(R.id.message_text);
            messageUser = v.findViewById(R.id.message_user);
            deleteButton = v.findViewById(R.id.delete_button);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        listOfMessages = findViewById(R.id.list_of_messages);
        input = findViewById(R.id.input);
        sendButton = findViewById(R.id.send_button);
        buttonVideoLlamada = findViewById(R.id.buttonVideoLlamada);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Debe iniciar sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Forzar rol Admin por correo para asegurar el botón de borrar
        if ("axelalejandro.flores26@gmail.com".equals(currentUser.getEmail())) {
            userRole = "Admin";
            Log.d(TAG, "Admin detectado por correo");
        }

        checkUserRoleFromDB(currentUser.getUid());

        sendButton.setOnClickListener(v -> {
            String messageStr = input.getText().toString().trim();
            if (!TextUtils.isEmpty(messageStr)) {
                ChatMessage chatMessage = new ChatMessage(messageStr, currentUser.getEmail());
                mDatabase.child("chats").push().setValue(chatMessage)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Mensaje guardado correctamente"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error al guardar mensaje", e));
                input.setText("");
            }
        });

        buttonVideoLlamada.setOnClickListener(v -> {
            if (checkPermissions()) {
                startActivity(new Intent(ChatActivity.this, VideoLlamadaActivity.class));
            } else {
                requestPermissions();
            }
        });

        displayChatMessages();
    }

    private void checkUserRoleFromDB(String userId) {
        mDatabase.child("users").child(userId).child("role").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userRole = snapshot.getValue(String.class);
                    Log.d(TAG, "Rol actualizado desde DB: " + userRole);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al leer rol", error.toException());
            }
        });
    }

    private void displayChatMessages() {
        Query query = mDatabase.child("chats");
        FirebaseRecyclerOptions<ChatMessage> options =
                new FirebaseRecyclerOptions.Builder<ChatMessage>()
                        .setQuery(query, ChatMessage.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder>(options) {
            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item, parent, false);
                return new MessageViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull ChatMessage model) {
                Log.d(TAG, "Mostrando mensaje de: " + model.getMessageUser());
                holder.messageText.setText(model.getMessageText());
                holder.messageUser.setText(model.getMessageUser());

                if ("Admin".equals(userRole)) {
                    holder.deleteButton.setVisibility(View.VISIBLE);
                    holder.deleteButton.setOnClickListener(v -> {
                        getSnapshots().getSnapshot(holder.getAbsoluteAdapterPosition()).getRef().removeValue();
                    });
                } else {
                    holder.deleteButton.setVisibility(View.GONE);
                }
            }
        };

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        listOfMessages.setLayoutManager(layoutManager);
        listOfMessages.setAdapter(adapter);
    }

    private boolean checkPermissions() {
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int microphonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return cameraPermission == PackageManager.PERMISSION_GRANTED && microphonePermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
