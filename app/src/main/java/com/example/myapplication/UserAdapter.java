package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<UserListActivity.UserData> userList;
    private Context context;
    private String currentUserRole;

    public UserAdapter(Context context, List<UserListActivity.UserData> userList, String currentUserRole) {
        this.context = context;
        this.userList = userList;
        this.currentUserRole = currentUserRole;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_user_list, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserListActivity.UserData user = userList.get(position);
        holder.nameTextView.setText(user.displayName);
        holder.roleTextView.setText("Rol: " + user.role);

        holder.itemView.setOnClickListener(v -> {
            if ("Admin".equals(currentUserRole)) {
                showRoleDialog(user);
            } else {
                Toast.makeText(context, "Solo el Administrador puede asignar roles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRoleDialog(UserListActivity.UserData user) {
        String[] roles = {"Admin", "Moderador", "Usuario"};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Asignar rol a " + user.displayName);
        builder.setItems(roles, (dialog, which) -> {
            String selectedRole = roles[which];
            FirebaseDatabase.getInstance().getReference().child("users").child(user.uid).child("role").setValue(selectedRole)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Rol actualizado", Toast.LENGTH_SHORT).show());
        });
        builder.show();
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView roleTextView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.user_email_text_view);
            roleTextView = itemView.findViewById(R.id.user_role_text_view);
        }
    }
}