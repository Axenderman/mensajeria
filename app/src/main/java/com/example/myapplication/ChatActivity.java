package com.example.myapplication;

import android.Manifest;
import android.content.Context;
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

import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder> adapter;
    private RecyclerView listOfMessages;
    private EditText input;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userRole = "Usuario";
    private String userDisplayName = "";
    private Map<String, String> userRolesMap = new HashMap<>();

    // Solución para el error de Inconsistency detected
    public static class SafeLinearLayoutManager extends LinearLayoutManager {
        public SafeLinearLayoutManager(Context context) { super(context); }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try { super.onLayoutChildren(recycler, state); } catch (IndexOutOfBoundsException e) { Log.e(TAG, "Inconsistencia evitada"); }
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtMsg, txtUser;
        Button btnDel;

        public MessageViewHolder(View v) {
            super(v);
            txtMsg = v.findViewById(R.id.message_text);
            txtUser = v.findViewById(R.id.message_user);
            btnDel = v.findViewById(R.id.delete_button);
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
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) { finish(); return; }

        loadUserAndRoles(user);

        findViewById(R.id.send_button).setOnClickListener(v -> {
            String msg = input.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) {
                String sender = (userDisplayName != null && !userDisplayName.isEmpty()) ? userDisplayName : user.getEmail();
                mDatabase.child("chats").push().setValue(new ChatMessage(msg, sender));
                input.setText("");
            }
        });

        findViewById(R.id.buttonVideoLlamada).setOnClickListener(v -> {
            if (hasPerms()) startActivity(new Intent(this, VideoLlamadaActivity.class));
            else reqPerms();
        });

        findViewById(R.id.buttonViewUsers).setOnClickListener(v -> {
            startActivity(new Intent(this, UserListActivity.class));
        });

        setupChat();
    }

    private void loadUserAndRoles(FirebaseUser user) {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userRolesMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String username = ds.child("username").getValue(String.class);
                    String role = ds.child("role").getValue(String.class);
                    String name = (username != null && !username.isEmpty()) ? username : ds.child("email").getValue(String.class);
                    if (name != null) userRolesMap.put(name, role != null ? role : "Usuario");
                    if (ds.getKey() != null && ds.getKey().equals(user.getUid())) {
                        userRole = role != null ? role : "Usuario";
                        userDisplayName = name;
                    }
                }
                if (adapter != null) adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void setupChat() {
        Query q = mDatabase.child("chats");
        FirebaseRecyclerOptions<ChatMessage> opts = new FirebaseRecyclerOptions.Builder<ChatMessage>().setQuery(q, ChatMessage.class).build();

        adapter = new FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder>(opts) {
            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                return new MessageViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.message_item, p, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull MessageViewHolder h, int pos, @NonNull ChatMessage m) {
                if (m.getMessageText() == null || m.getMessageUser() == null) {
                    h.itemView.setVisibility(View.GONE);
                    h.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                }
                h.itemView.setVisibility(View.VISIBLE);
                h.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                h.txtMsg.setText(m.getMessageText());
                h.txtUser.setText(m.getMessageUser());
                
                String senderName = m.getMessageUser();
                String senderRole = userRolesMap.get(senderName);
                if (senderRole == null) senderRole = "Usuario";

                boolean canDelete = "Admin".equals(userRole) || ("Moderador".equals(userRole) && !"Admin".equals(senderRole)) || (userDisplayName != null && userDisplayName.equals(senderName));
                h.btnDel.setVisibility(canDelete ? View.VISIBLE : View.GONE);
                h.btnDel.setOnClickListener(v -> {
                    int p = h.getBindingAdapterPosition();
                    if (p != RecyclerView.NO_POSITION) getSnapshots().getSnapshot(p).getRef().removeValue();
                });
            }
        };

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int start, int count) {
                listOfMessages.smoothScrollToPosition(adapter.getItemCount());
            }
        });

        listOfMessages.setLayoutManager(new SafeLinearLayoutManager(this));
        listOfMessages.setAdapter(adapter);
    }

    private boolean hasPerms() { return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; }
    private void reqPerms() { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 100); }
    @Override
    protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override
    protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}