package com.harsh.cards;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class JoinGame extends AppCompatActivity {
    String name;
    String myid;
    RecyclerView recycler;
    DatabaseReference db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_join_game);
        recycler=findViewById(R.id.joinfrndlist);
        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        db=FirebaseDatabase.getInstance().getReference().child("users");
        name=user.getDisplayName();
        myid=user.getEmail().replaceAll("\\.",",");

        recycler.setLayoutManager(new LinearLayoutManager(this));
        MainActivity.inviteAdapter=new InviteAdapter();
        recycler.setAdapter(MainActivity.inviteAdapter);
        MainActivity.inviteAdapter.reload();
    }
    class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.InviteViewHolder> {
        ArrayList<String>emails=new ArrayList<>();
        LayoutInflater inflater;
        InviteAdapter(){
            inflater= (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }
        @NonNull
        @Override
        public InviteAdapter.InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view=inflater.inflate(R.layout.single_person,parent,false);
            return new InviteAdapter.InviteViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
            final String id=emails.get(position);
            holder.name.setText(MainActivity.invites.get(id));
            Log.e(id,MainActivity.invites.get(id));
            if (position%2==0){
                holder.layout.setBackground(new ColorDrawable(Color.WHITE));
            }else{
                holder.layout.setBackground(new ColorDrawable(Color.GRAY));
            }
            holder.btn.setText("Join");
            holder.btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    db.child(myid+"/invites/"+id).removeValue();
                    db.child(myid+"/joined").setValue(id);
                    Intent intent=new Intent(JoinGame.this,StartGame.class);
                    intent.putExtra("host",false);
                    intent.putExtra("joinid",id);
                    startActivity(intent);
                    finish();
                }
            });
            holder.btn2.setText("Cancel");
            holder.btn2.setVisibility(View.VISIBLE);
            holder.btn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    db.child(myid+"/invites/"+id).removeValue();
                }
            });
        }
        @Override
        public int getItemCount() {
            return emails.size();
        }
        void reload(){
            emails=new ArrayList<>(MainActivity.invites.keySet());
            notifyDataSetChanged();
        }
        class InviteViewHolder extends RecyclerView.ViewHolder{
            TextView name,btn,btn2;
            ConstraintLayout layout;
            public InviteViewHolder(@NonNull View itemView) {
                super(itemView);
                name=itemView.findViewById(R.id.name);
                btn=itemView.findViewById(R.id.btn);
                btn2=itemView.findViewById(R.id.btn2);
                layout=itemView.findViewById(R.id.single_person_layout);
            }
        }
    }
}
