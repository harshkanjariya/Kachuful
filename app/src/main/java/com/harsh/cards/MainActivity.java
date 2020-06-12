package com.harsh.cards;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    static HashMap<String,String>friends=new HashMap<>();
    static HashMap<String,String>invites=new HashMap<>();
    static JoinGame.InviteAdapter inviteAdapter;

    EditText nam;
    EditText search;
    DrawerLayout layout;
    RecyclerView recyclerView;
    DatabaseReference db;
    ValueEventListener friendlistener,allistener,invitelistener;
    HashMap<String,String>allperson=new HashMap<>();
    HashMap<String,String>filtered=new HashMap<>();
    HashMap<String,String>requested=new HashMap<>();
    HashMap<String,String>accepts=new HashMap<>();
    String myid,name;
    PersonsAdapter adapter;
    boolean friendshowing=true;
    TextView invitecount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        layout=findViewById(R.id.drawerlayout);
        nam=findViewById(R.id.person_name);
        recyclerView=findViewById(R.id.recyclerview);
        search=findViewById(R.id.searchedit);
        invitecount=findViewById(R.id.invitecount);



        Intent intent=getIntent();
        Uri uri=intent.getData();
        db=FirebaseDatabase.getInstance().getReference().child("users");
        final FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        final TextView nametxt=findViewById(R.id.nametxt);
        if (user==null)
            startActivity(new Intent(this,LoginActivity.class));
        if (uri!=null){
            String s=uri.getQueryParameter("id");
            intent=new Intent(this,StartGame.class);
            intent.putExtra("myid",myid);
            intent.putExtra("id",s);
            startActivity(intent);
        }
        name=user.getDisplayName();
        myid=user.getEmail().replaceAll("\\.",",");
        ImageView img=findViewById(R.id.profileimage);

        Glide.with(this)
                .load(user.getPhotoUrl())
                .into(img);
        nametxt.setText(user.getDisplayName());
        nametxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nam.setVisibility(View.VISIBLE);
                nametxt.setVisibility(View.GONE);
            }
        });
        nam.setText(name);
        nam.setSelection(nam.getText().length());
        db.child(myid+"/name/").setValue(name);

        nam.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==EditorInfo.IME_ACTION_DONE){
                    name=nam.getText().toString();
                    nametxt.setText(name);
                    UserProfileChangeRequest request=new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build();
                    user.updateProfile(request);
                    nam.setVisibility(View.GONE);
                    nametxt.setVisibility(View.VISIBLE);
                    db.child(myid+"/name").setValue(name);
                    for (String s:friends.keySet())
                        db.child(s+"/frnds/"+myid).setValue("F"+name);
                    for (String s:accepts.keySet())
                        db.child(s+"/frnds/"+myid).setValue("R"+name);
                    for (String s:requested.keySet())
                        db.child(s+"/frnds/"+myid).setValue("A"+name);
                }
                return true;
            }
        });
        friendlistener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                friends.clear();
                requested.clear();
                accepts.clear();
                for (DataSnapshot snapshot:dataSnapshot.getChildren()){
                    String val= (String) snapshot.getValue();
                    char c=val.charAt(0);
                    val=val.substring(1);
                    if (c=='F')
                        friends.put(snapshot.getKey(),val);
                    else if (c=='R')
                        requested.put(snapshot.getKey(),val);
                    else if (c=='A')
                        accepts.put(snapshot.getKey(),val);
                }
                adapter.reload();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        db.child(myid+"/frnds").addValueEventListener(friendlistener);

        allistener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allperson.clear();
                for (DataSnapshot snapshot:dataSnapshot.getChildren()){
                    String email=snapshot.getKey();
                    if (email.equals(myid))continue;
                    HashMap<String,String>map= (HashMap<String, String>) snapshot.getValue();
                    allperson.put(email,map.get("name"));
                }
                adapter.reload();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        db.addValueEventListener(allistener);
        invitelistener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long count=dataSnapshot.getChildrenCount();
                if (count>0){
                    invitecount.setVisibility(View.VISIBLE);
                    invitecount.setText(""+(count<99?count:"99+"));
                }else{
                    invitecount.setVisibility(View.GONE);
                }
                Object obj=dataSnapshot.getValue();
                if (obj instanceof Map){
                    invites= (HashMap<String, String>) obj;
                    if (inviteAdapter!=null)
                        inviteAdapter.reload();
                }else{
                    invites=new HashMap<>();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        db.child(myid+"/invites").addValueEventListener(invitelistener);
        ValueEventListener joinedlistener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String joined= (String) dataSnapshot.getValue();
                if (joined!=null){
                    Intent intent1=new Intent(MainActivity.this,StartGame.class);
                    intent1.putExtra("host",joined.equals(myid));
                    intent1.putExtra("joinid",joined);
                    startActivity(intent1);
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        db.child(myid+"/joined").addListenerForSingleValueEvent(joinedlistener);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration decoration=new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(decoration);
        adapter=new PersonsAdapter();
        recyclerView.setAdapter(adapter);
        adapter.reload();

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String str=search.getText().toString();
                if (str.isEmpty()){
                    filtered= (HashMap<String, String>) allperson.clone();
                }else{
                    filtered.clear();
                    for (Map.Entry<String,String>e:allperson.entrySet()){
                        if (e.getValue().contains(str) || e.getKey().contains(str))
                            filtered.put(e.getKey(),e.getValue());
                    }
                }
                adapter.reload();
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        final SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("name",nam.getText().toString()).apply();
        db.removeEventListener(allistener);
        db.child(myid+"/frnds").removeEventListener(friendlistener);
        db.child(myid+"/invites").removeEventListener(invitelistener);
    }
    private String generateCode()
    {
        String AlphaNumericString = "0123456789";
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }
    public void start(View view){
        db.child(myid).child("joined").setValue(myid);
        Intent in=new Intent(this,StartGame.class);
        in.putExtra("host",true);
        in.putExtra("myid",myid);
        startActivity(in);
    }
    public void join(View view){
        Intent in=new Intent(this,JoinGame.class);
        startActivity(in);
    }
    public void instructions(View view) {
        Intent intent=new Intent(this,Instructs.class);
        startActivity(intent);
    }
    public void openinfo(View view) {
        Intent intent=new Intent(this,About.class);
        startActivity(intent);
    }
    public void openfrndlist(View view) {
        layout.openDrawer(GravityCompat.START);
    }
    public void addfriend(View view) {
        findViewById(R.id.addfrndbtn).setVisibility(View.GONE);
        findViewById(R.id.friendstitle).setVisibility(View.GONE);
        findViewById(R.id.frndbackbtn).setVisibility(View.VISIBLE);
        findViewById(R.id.searchedit).setVisibility(View.VISIBLE);
        friendshowing=false;
        adapter.reload();
    }
    public void frndbackclick(View view) {
        findViewById(R.id.addfrndbtn).setVisibility(View.VISIBLE);
        findViewById(R.id.friendstitle).setVisibility(View.VISIBLE);
        findViewById(R.id.frndbackbtn).setVisibility(View.GONE);
        findViewById(R.id.searchedit).setVisibility(View.GONE);
        friendshowing=true;
        adapter.reload();
    }
    private void signout(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient client= GoogleSignIn.getClient(this,gso);
        client.revokeAccess()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            FirebaseAuth.getInstance().signOut();
                            Intent intent=new Intent(MainActivity.this,LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }else{
                            task.getException().printStackTrace();
                        }
                    }
                });
    }
    public void logout(View view) {
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        signout();
                    }
                })
                .setNegativeButton("No",null);
        builder.create().show();
    }
    class PersonsAdapter extends RecyclerView.Adapter<PersonsAdapter.PersonViewHolder> {
        ArrayList<String>emails=new ArrayList<>();
        HashMap<String,String>map=new HashMap<>();
        LayoutInflater inflater;
        PersonsAdapter(){
            inflater= (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        }
        @NonNull
        @Override
        public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view=inflater.inflate(R.layout.single_person,parent,false);
            return new PersonViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull PersonViewHolder holder, final int position) {
            final String id=emails.get(position);
            holder.name.setText(map.get(id));
            String t;
            if (friends.containsKey(id))
                t="Friends";
            else if (requested.containsKey(id))
                t="Requested";
            else if (accepts.containsKey(id))
                t="Accept";
            else
                t="+ Add Friend";
            holder.btn.setText(t);
            holder.btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (friends.containsKey(id)){
                        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this)
                                .setMessage("Are you sure you want to remove from friend ?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which){
                                        db.child("users/"+myid+"/frnds/"+id).removeValue();
                                        db.child("users/"+id+"/frnds/"+myid).removeValue();
                                    }
                                })
                                .setNegativeButton("No",null);
                        builder.create().show();
                    }else if (requested.containsKey(id)){
                        db.child(myid+"/frnds/"+id).removeValue();
                        db.child(id+"/frnds/"+myid).removeValue();
                    }else if (accepts.containsKey(id)){
                        String s=accepts.get(id);
                        db.child(myid+"/frnds/"+id).setValue("F"+s);
                        db.child(id+"/frnds/"+myid).setValue("F"+name);
                    }else{
                        String s=map.get(id);
                        db.child(myid+"/frnds/"+id).setValue("R"+s);
                        db.child(id+"/frnds/"+myid).setValue("A"+name);
                    }
                }
            });
        }
        @Override
        public int getItemCount() {
            return emails.size();
        }
        void reload(){
            if (friendshowing){
                emails=new ArrayList<>(accepts.keySet());
                emails.addAll(friends.keySet());
                map.clear();
                map.putAll(accepts);
                map.putAll(friends);
            }else{
                map=filtered;
                emails=new ArrayList<>(map.keySet());
            }
            notifyDataSetChanged();
        }
        class PersonViewHolder extends RecyclerView.ViewHolder{
            TextView name,btn;
            public PersonViewHolder(@NonNull View itemView) {
                super(itemView);
                name=itemView.findViewById(R.id.name);
                btn=itemView.findViewById(R.id.btn);
            }
        }
    }
}