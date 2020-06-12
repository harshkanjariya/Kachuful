package com.harsh.cards;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class HostStart extends AppCompatActivity {
    EditText edit;
    TextView decktxt;
    TextView txt;
    String gameid;
    String name;
    String myid;
    DatabaseReference ref;
    ValueEventListener listener,playerlistener,startlistener;
    ArrayList<String>codes=new ArrayList<>();
    ListView playerlistview;
    ArrayList<String>players=new ArrayList<>();
    boolean host,toast=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_host_start);
        final Intent intent=getIntent();
        host=intent.getBooleanExtra("host",false);
        myid=intent.getStringExtra("myid");

        edit=findViewById(R.id.decks);
        decktxt=findViewById(R.id.deckstxt);
        ref=FirebaseDatabase.getInstance().getReference();
        txt=findViewById(R.id.codetxt);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        name=prefs.getString("name","");

        playerlistview=findViewById(R.id.playerslist);
        final ArrayAdapter<String>adapter=new ArrayAdapter<>(this,R.layout.single_player_name,players);
        playerlistview.setAdapter(adapter);
        playerlistener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                players.clear();
                if (dataSnapshot.exists()){
                    for (DataSnapshot snap:dataSnapshot.getChildren())
                        players.add(snap.getValue().toString());
                    adapter.notifyDataSetChanged();
                }else{
                    Intent in=new Intent(HostStart.this,MainActivity.class);
                    startActivity(in);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        if (host){
            decktxt.setVisibility(View.GONE);
            edit.setSelection(edit.getText().length());
            edit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override
                public void afterTextChanged(Editable s) {
                    String x=edit.getText().toString();
                    if (x.isEmpty())x="1";
                    ArrayList<Integer>zz=new ArrayList<>();
                    for (int j=0;j<Integer.parseInt(x);j++)
                        for (int i=0;i<52;i++)
                            zz.add(i+j*52);
                    ref.child("games/"+gameid+"/decks").setValue(Integer.parseInt(x));
                    ref.child("games/"+gameid+"/z").setValue(zz);
                }
            });
            listener=new ValueEventListener(){
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot){
                    codes.clear();
                    Object obj=dataSnapshot.getValue();
                    if (obj instanceof ArrayList)
                    codes=(ArrayList<String>)obj;
                    if (codes==null){
                        codes=new ArrayList<>();
                    }
                    while (codes.contains(gameid))
                        gameid=generateCode(4);
                    int count=codes.size();
                    ref.child("codes/"+count).setValue(gameid);
                    ref.child("codes/"+count).onDisconnect().removeValue();
                    Game game=new Game();
//                    game.host=name;
//                    game.decks=1;
                    game.task="";
                    game.round=0;
                    game.initial=-1;
                    game.sir=-1;
                    ArrayList<Integer>zz=new ArrayList<>();
                    for (int i=0;i<52;i++)
                        zz.add(i);
                    game.z=zz;
                    game.started=false;
                    HashMap<String,String>p=new HashMap<>();
                    myid=""+new Date().getTime();
                    p.put(""+myid,name);
                    game.players=p;
                    game.lastId=myid;
                    ref.child("games/"+gameid).setValue(game);
                    ref.child("games/"+gameid).onDisconnect().removeValue();
                    ref.child("games/"+gameid+"/players").addValueEventListener(playerlistener);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            ref.child("codes").addListenerForSingleValueEvent(listener);
            gameid=generateCode(4);
        }else{
            findViewById(R.id.deckrow).setVisibility(View.GONE);
            findViewById(R.id.start_btn).setVisibility(View.GONE);
            gameid=intent.getStringExtra("code");
            ref.child("games/"+gameid+"/players").addValueEventListener(playerlistener);
            startlistener=new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        Game g=dataSnapshot.getValue(Game.class);
                        if(g!=null)
                        if(g.started && (g.task.isEmpty() || g.task.equals("loadgame"))){
                            ref.child("games/"+gameid).removeEventListener(startlistener);
                            SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(HostStart.this);
                            prefs.edit().putString("gameid",gameid).apply();
                            Intent in=new Intent(HostStart.this,PlayGame.class);
                            in.putExtra("myid",myid);
                            startActivity(in);
                            finish();
                        }else{
                            if (toast) {
                                toast=false;
                                Toast.makeText(HostStart.this, g.started ? "wait until the current round ends!" : "wait until host start!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            };
            ref.child("games/"+gameid).addValueEventListener(startlistener);
        }
        txt.setText(gameid);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!host)
            ref.child("games/"+gameid).removeEventListener(startlistener);
    }
    private String generateCode(int n)
    {
        String AlphaNumericString = "abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }
    public void start(View view){
        ref.child("games/"+gameid+"/started").setValue(true);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("gameid",gameid).apply();
        Intent in=new Intent(this,PlayGame.class);
        in.putExtra("host",true);
        in.putExtra("myid",myid);
        startActivity(in);
        finish();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    public void onBackPressed(){
        ref.child("games/"+gameid+(host?"":"/players/"+myid)).removeValue();
        super.onBackPressed();
    }
}