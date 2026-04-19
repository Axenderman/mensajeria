package com.example.myapplication;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<UserData> userList;
    private DatabaseReference mDatabase;
    private String currentUserRole = "Usuario";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roles_usuario); // Reusing the layout with RecyclerView

        recyclerView = findViewById(R.id.users_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        String currentUid = FirebaseAuth.getInstance().getUid();

        if (currentUid != null) {
            mDatabase.child("users").child(currentUid).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        currentUserRole = snapshot.getValue(String.class);
                    }
                    loadUsers();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void loadUsers() {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String uid = snapshot.getKey();
                    String username = snapshot.child("username").getValue(String.class);
                    String phone = snapshot.child("telefono").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    
                    if (role == null) role = "Usuario";
                    
                    String displayName = "";
                    if (username != null && !username.isEmpty()) {
                        displayName = username;
                    } else {
                        displayName = email;
                    }

                    if (phone != null && !phone.isEmpty()) {
                        displayName += " (" + phone + ")";
                    }
                    
                    if (displayName != null) {
                        userList.add(new UserData(uid, displayName, role));
                    }
                }
                adapter = new UserAdapter(UserListActivity.this, userList, currentUserRole);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public static class UserData {
        public String uid;
        public String displayName;
        public String role;

        public UserData(String uid, String displayName, String role) {
            this.uid = uid;
            this.displayName = displayName;
            this.role = role;
        }
    }
}