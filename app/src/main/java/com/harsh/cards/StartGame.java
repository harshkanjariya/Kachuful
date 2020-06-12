package com.harsh.cards;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.view.View.GONE;

public class StartGame extends AppCompatActivity {
    int deck;
    DatabaseReference db;
    ValueEventListener listener,joinedlistener;
    PersonsAdapter adapter;
    RecyclerView recyclerView;
    String myid,name,joinid;
    RelativeLayout invitebox;
    InviteAdapter inviteAdapter;
    Game game;
    boolean host,first;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_start_game);
        deck=1;
        first=true;
        Intent intent=getIntent();
        host=intent.getBooleanExtra("host",false);
        recyclerView=findViewById(R.id.joinedlist);
        invitebox=findViewById(R.id.inviteboxlayout);
        invitebox.animate().translationY(1000).setDuration(1).start();
        RecyclerView inviteview=findViewById(R.id.invitefrndlist);
        inviteview.setLayoutManager(new LinearLayoutManager(this));

        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        myid=user.getEmail().replaceAll("\\.",",");
        name=user.getDisplayName();

        if (host){
            inviteAdapter=new InviteAdapter();
            inviteview.setAdapter(inviteAdapter);
            inviteAdapter.reload();
            String gameid=Game.encrypt(""+new Date().getTime()+","+myid,"kachufulgameplay");
            joinid=myid;
        }else{
            findViewById(R.id.decktext).setVisibility(GONE);
            findViewById(R.id.start_btn).setVisibility(GONE);
            findViewById(R.id.invite_btn).setVisibility(GONE);
            joinid=intent.getStringExtra("joinid");
        }
        adapter=new PersonsAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if (host){
            ItemTouchHelper.Callback callback=new ItemTouchHelper.Callback() {
                @Override
                public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    return makeMovementFlags(ItemTouchHelper.UP|ItemTouchHelper.DOWN,0);
                }
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    adapter.onItemMove(viewHolder.getAdapterPosition(),target.getAdapterPosition());
                    return true;
                }
                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
            };
            ItemTouchHelper itemTouchHelper=new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }

        db=FirebaseDatabase.getInstance().getReference().child("users");

        db.child(joinid+"/game/players/"+myid).setValue(name);
        if (host){
            db.child(myid+"/game/decks").setValue(true);
            db.child(myid+"/game/sequence/0").setValue(myid);
        }
        joinedlistener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String join= (String) dataSnapshot.getValue();
                if (join==null || join.isEmpty()){
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        db.child(myid+"/joined").addValueEventListener(joinedlistener);
        listener=new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Game g=dataSnapshot.getValue(Game.class);
                if (g!=null){
                    game=g;
                    if(g.started){
                        if (host) {
                            if (game.task==null)
                                db.child(myid + "/game/task").setValue("loadgame");
                            if (game.z==null){
                                ArrayList<Integer>zz=new ArrayList<>();
                                for (int i=0;i<52;i++)
                                    zz.add(i);
                                db.child(myid+"/game/z").setValue(zz);
                            }
                        }
                        Intent intent=new Intent(StartGame.this,PlayActivity.class);
                        intent.putExtra("host",host);
                        intent.putExtra("joinid",joinid);
                        startActivity(intent);
                        finish();
                    }
                    if (first){
                        first=false;
                        if (!host && !game.sequence.contains(myid)){
                            db.child(joinid).child("game/sequence/"+game.sequence.size()).setValue(myid);
                        }
                    }
                    adapter.reload();
                    if (inviteAdapter!=null)
                    inviteAdapter.reload();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        db.child(joinid).child("game").addValueEventListener(listener);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.child(joinid).child("game").removeEventListener(listener);
        db.child(myid+"/joined").removeEventListener(joinedlistener);
    }
    public void start(View view){
        db.child(joinid).child("game/started").setValue(true);
    }
    public void deckchange(View view){
        TextView v= (TextView) view;
        String s=v.getText().toString();
        deck= Integer.parseInt(s.substring(s.length()-1));
        deck=3-deck;
        v.setText("Decks : "+deck);
        db.child(myid).child("game/decks").setValue(deck==1);
    }
    public void closeinvitebox(View view){
        invitebox.animate().translationY(1000).setDuration(100).start();
    }
    public void openinvite(View view){
        invitebox.animate().translationY(0).setDuration(100).start();
    }
    class PersonsAdapter extends RecyclerView.Adapter<PersonsAdapter.PersonViewHolder> {
        ArrayList<String> emails=new ArrayList<>();
        LayoutInflater inflater;
        PersonsAdapter(){
            inflater= (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }
        @NonNull
        @Override
        public PersonsAdapter.PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view=inflater.inflate(R.layout.single_person,parent,false);
            return new PersonsAdapter.PersonViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull PersonsAdapter.PersonViewHolder holder, final int position) {
            final String id=emails.get(position);
            holder.name.setText(game.players.get(id));
            if (position%2==0){
                holder.layout.setBackground(new ColorDrawable(Color.WHITE));
            }else{
                holder.layout.setBackground(new ColorDrawable(Color.parseColor("#e0e0e0")));
            }
            if (!host || id.equals(myid))
                holder.btn.setVisibility(GONE);
            else{
                holder.btn.setText("Remove");
                holder.btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder=new AlertDialog.Builder(StartGame.this)
                                .setMessage("Are you sure you want to remove from game ?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which){
                                        db.child(id+"/joined").removeValue();
                                        db.child(myid+"/game/players/"+id).removeValue();
                                    }
                                })
                                .setNegativeButton("No",null);
                        builder.create().show();
                    }
                });
            }
        }
        public boolean onItemMove(int fromPosition, int toPosition) {
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(emails, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(emails, i, i - 1);
                }
            }
            db.child(joinid).child("game/sequence").setValue(emails);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }
        @Override
        public int getItemCount() {
            return emails.size();
        }
        void reload(){
            if (game==null || game.players==null || game.sequence==null)return;
            if(game.players.size()!=game.sequence.size())
                emails=new ArrayList<>(game.players.keySet());
            else
                emails= (ArrayList<String>) game.sequence.clone();
            notifyDataSetChanged();
        }
        class PersonViewHolder extends RecyclerView.ViewHolder{
            TextView name,btn;
            ConstraintLayout layout;
            public PersonViewHolder(@NonNull View itemView) {
                super(itemView);
                name=itemView.findViewById(R.id.name);
                btn=itemView.findViewById(R.id.btn);
                layout=itemView.findViewById(R.id.single_person_layout);
            }
        }
    }
    class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.InviteViewHolder> {
        ArrayList<String>emails=new ArrayList<>();
        LayoutInflater inflater;
        InviteAdapter(){
            inflater= (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }
        @NonNull
        @Override
        public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view=inflater.inflate(R.layout.single_person,parent,false);
            return new InviteViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull InviteViewHolder holder, final int position) {
            final String id=emails.get(position);
            holder.name.setText(MainActivity.friends.get(id));
            String t;
            if(game!=null && game.players !=null && game.players.containsKey(id)){
                char ch=game.players.get(id).charAt(0);
                if (ch=='J')t="Joined";
                else if (ch=='A')t="Accept";
                else t="Invite";
            }else
                t="Invite";
            holder.btn.setText(t);
            holder.btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(game.players.containsKey(id)){
                        char ch=game.players.get(id).charAt(0);
                        if (ch=='A'){
                        }
                    }else{
                        db.child(id+"/invites/"+myid).setValue(name);
                        Toast.makeText(StartGame.this,"Invitation sent!",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        @Override
        public int getItemCount() {
            return emails.size();
        }
        void reload(){
            emails=new ArrayList<>(MainActivity.friends.keySet());
            notifyDataSetChanged();
        }
        class InviteViewHolder extends RecyclerView.ViewHolder{
            TextView name,btn;
            public InviteViewHolder(@NonNull View itemView) {
                super(itemView);
                name=itemView.findViewById(R.id.name);
                btn=itemView.findViewById(R.id.btn);
            }
        }
    }

    @Override
    public void onBackPressed(){
        AlertDialog.Builder builder=new AlertDialog.Builder(StartGame.this)
                .setMessage("Are you sure you want to leave game ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which){
                        if (host){
                            for (String p:game.players.keySet())
                                db.child(p).child("joined").removeValue();
                            db.child(myid).child("game").removeValue();
                        }else{
                            db.child(joinid).child("game/players/"+myid).removeValue();
                            db.child(myid).child("joined").removeValue();
                        }
                        finish();
                    }
                })
                .setNegativeButton("No",null);
        builder.create().show();
    }
}