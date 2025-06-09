package com.example.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.R;
import com.example.android.model.RFIDUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class RFIDAdapter extends RecyclerView.Adapter<RFIDAdapter.ViewHolder> {
    private List<RFIDUser> list;
    private Context context;
    private DatabaseReference rfidRef;

    public RFIDAdapter(Context context, List<RFIDUser> list) {
        this.context = context;
        this.list = list;
        rfidRef = FirebaseDatabase.getInstance().getReference("rfid");
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUID;
        Button btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvUID = itemView.findViewById(R.id.tvUID);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    @NonNull
    @Override
    public RFIDAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_the, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RFIDAdapter.ViewHolder holder, int position) {
        RFIDUser user = list.get(position);
        holder.tvName.setText("Tên: " + user.name);
        holder.tvUID.setText("UID: " + user.uid);

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận")
                    .setMessage("Xóa thẻ của " + user.name + "?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        rfidRef.child(user.uid).removeValue();
                        Toast.makeText(context, "Đã xóa " + user.name, Toast.LENGTH_SHORT).show();
                        list.remove(position);
                        notifyItemRemoved(position);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
