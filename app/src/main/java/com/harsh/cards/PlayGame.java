package com.harsh.cards;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressLint("SetTextI18n")
public class PlayGame extends AppCompatActivity {
    DatabaseReference ref;
    TextView message,mainMsg,myhands;
    String gameid,myid,name;
    Game game;
    ValueEventListener listener;
    Bitmap []cards;
    boolean first=true,host=false;
    Point []points;
    GameView canvas;
    RelativeLayout cardlayout;
    ArrayList<String> keys;
    ImageView[]cardsimg;
    ImageView sirimg;
    int []index;
    int []mycards;
    int presir=2,cw=150,ch=300;
    float w,h;
    DrawerLayout drawerLayout;
    int selected=-1;
    private boolean clickable=false,resumed=false,created=false;
    TableLayout currentTable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_play_game);
        ch= (int) pixel(80,this);
        cw=ch;
        created=true;
        Intent intent=getIntent();
        host=intent.getBooleanExtra("host",false);
        myid=intent.getStringExtra("myid");
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        name= prefs.getString("name","");
        gameid= prefs.getString("gameid","");
        if (gameid.isEmpty()){
            Intent in=new Intent(this,MainActivity.class);
            startActivity(in);
            finish();
        }
        ((TextView)findViewById(R.id.navidtxt)).setText("Game id : "+gameid);
        ConstraintLayout layout=findViewById(R.id.gamelayout);
        message=findViewById(R.id.message_txt);
        mainMsg=findViewById(R.id.main_message_txt);
        myhands=findViewById(R.id.mybids);
        sirimg=findViewById(R.id.sir_img);
        drawerLayout=findViewById(R.id.drawer_layout);
        currentTable=findViewById(R.id.current_table);
        canvas=new GameView(this);
        final ConstraintLayout.LayoutParams params=new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        canvas.setLayoutParams(params);
        layout.addView(canvas);
        cardlayout=findViewById(R.id.cardlayout);
        cardlayout.bringToFront();
        findViewById(R.id.sharelayout).bringToFront();
        findViewById(R.id.hands_layout).bringToFront();
        currentTable.bringToFront();
        findViewById(R.id.leaderboard_btn).bringToFront();
        findViewById(R.id.close_btn).bringToFront();

        ref=FirebaseDatabase.getInstance().getReference().child("games/"+gameid);
        cards=new Bitmap[53];
        getCards();
        if(host){
            mainMsg.setText("Start game");
            ImageView img=findViewById(R.id.spadeimg);
            img.setPadding(15,25,15,25);
            img.setBackground(getDrawable(R.drawable.silected_card));
        }else{
            findViewById(R.id.sharebtn).setVisibility(View.GONE);
            findViewById(R.id.close_btn).setVisibility(View.GONE);
        }
    }
    private void init_listener(){
        listener=new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    game=dataSnapshot.getValue(Game.class);
                    if (created||resumed){
                        resumed=false;
                        created=false;
                        if (game.leaderboard!=null && !game.leaderboard.containsKey(myid) && game.round>0){
                            char []ar=new char[game.round-1];
                            Arrays.fill(ar,',');
                            ref.child("leaderboard/"+myid).setValue(new String(ar));
                        }
                        loadgame();
                        if (game.CurrentCards!=null){
                            distribute();
                            set_my_cards();
                        }
                        if (game.showing!=null)
                        set_showing();
                        ref.child("task").setValue("loadgame");
                    }
                    if(game !=null && game.off!=null){
                        if (game.off.containsKey(myid) && game.off.get(myid)){
                            ref.child("off/"+myid).removeValue();
                            ref.child("task").setValue("loadgame");
                        }
                    }
                    sirimg.setVisibility(View.VISIBLE);
                    switch (game.sir){
                        case 0:
                            sirimg.setImageDrawable(getDrawable(R.drawable.club));
                            break;
                        case 3:
                            sirimg.setImageDrawable(getDrawable(R.drawable.diamond));
                            break;
                        case 1:
                            sirimg.setImageDrawable(getDrawable(R.drawable.heart));
                            break;
                        case 2:
                            sirimg.setImageDrawable(getDrawable(R.drawable.spade));
                            break;
                        default:
                            sirimg.setVisibility(View.GONE);
                            break;
                    }
                    if(game.task.equals("shuffle")) {
                        loadshuffle();
                    }else if(game.task.equals("distribute")){
                        distribute();
                    }else if(game.task.equals("leaderboard")){
                        leaderboard();
                        current_table();
                    }else if(game.task.equals("show")){
                        set_showing();
                    }else if(game.task.equals("end")){
                        ref.removeEventListener(listener);
                        Intent in=new Intent(PlayGame.this,ResultActivity.class);
                        in.putExtra("data",game.leaderboard);
                        in.putExtra("names",game.players);
                        startActivity(in);
                        finish();
                    }else if(game.task.equals("loadgame")){
                        loadgame();
                    }else if(game.task.equals("reset")){
                        message.setText("");
                        mainMsg.setText(host?"Start next round":"wait for host to start next round!");
                        for (int c=0;c<cardsimg.length;c++) {
                            cardsimg[c].animate()
                                    .translationX(0)
                                    .translationY(0)
                                    .rotation(0)
                                    .setDuration(100)
                                    .start();
                            cardsimg[c].setImageBitmap(cards[0]);
                        }
                        findViewById(R.id.sort_btn).setVisibility(View.GONE);
                        leaderboard();
                    }else if(game.task.equals("calculate") && host){
                        ref.child("task").setValue("wait");
                        calculate_hand();
                    }else{
                        if(game.task.indexOf(":")>0){
                            if(game.leaderboard!=null){
                                String handstr=game.leaderboard.get(myid);
                                if (handstr!=null){
                                    String[] hands = handstr.split(",");
                                    if (hands.length>game.round)
                                        myhands.setText(game.players.get(myid)+" ("+game.CurrentHands.get(myid)+"/"+(hands[game.round].contains("=")?hands[game.round].substring(1):hands[game.round])+")");
                                }
                            }
                            canvas.invalidate();
                            clickable=false;
                            String[] s =game.task.split(":");
                            if (s[1].equals("h")){
                                mainMsg.bringToFront();
                                if (game.lastId.equals(myid))
                                    mainMsg.setText("Your first turn");
                                else
                                    mainMsg.setText(game.players.get(game.lastId)+"'s first turn");
                                currentTable.animate().translationY(0).setDuration(500).start();
                                current_table();
                            }else{
                                for (int c:mycards) {
                                    cardsimg[c].bringToFront();
                                    cardsimg[c].setColorFilter(Color.argb(100, 0, 0, 0));
                                }
                                currentTable.animate().translationY(-currentTable.getMeasuredHeight()).setStartDelay(1000).setDuration(500).start();
                            }
                            if (s[1].equals("get")) {
                                give_hand(s[0]);
                                clickable = false;
                            }else if(s[1].equals("won")){
                                message.setText((s[0].equals(myid)?"You":game.players.get(s[0]))+" won!");
                            }else if(s[0].equals(myid)){
                                if (s[1].equals("h")){
                                    findViewById(R.id.hands_layout).setVisibility(View.VISIBLE);
                                    message.setText("");
                                }else if (s[1].equals("t")) {
                                    clickable = true;
                                    mainMsg.setText("");
                                    message.setText("Your turn");
                                    if (game.initial>=0){
                                        boolean found=false;
                                        for (int c:mycards)
                                            if ((c % 52) / 13 == game.initial) {
                                                found = true;
                                                break;
                                            }
                                        if (found){
                                            for (int c:mycards)
                                                if((c%52)/13==game.initial) {
                                                    cardsimg[c].setColorFilter(Color.argb(0, 0, 0, 0));
                                                    cardsimg[c].bringToFront();
                                                }else
                                                    cardsimg[c].setColorFilter(Color.argb(100,0,0,0));
                                        }else
                                            for (int c:mycards)
                                                cardsimg[c].setColorFilter(Color.argb(0, 0, 0, 0));
                                    }else
                                        for (int c:mycards)
                                            cardsimg[c].setColorFilter(Color.argb(0, 0, 0, 0));
                                }
                            }else{
                                clickable=false;
                                if (s[1].equals("h")){
                                    message.setText("waiting for "+game.players.get(s[0]));
                                }else if (s[1].equals("t")) {
                                    message.setText(game.players.get(s[0])+"'s turn");
                                    mainMsg.setText("");
                                }
                            }
                        }
                    }
                }else{
                    finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
    }
    private void give_hand(String s){
        int ix=keys.indexOf(myid);
        int p=0;
        for(int i=0;i<keys.size();i++){
            int pos=(i+ix)%keys.size();
            if (game.off!=null && game.off.containsKey(keys.get(pos)))
                continue;
            if (keys.get(pos).equals(s))
                break;
            p++;
        }
        for (Map.Entry<String,Integer>e:game.showing.entrySet())
            if (e.getValue()>=0)
                cardsimg[e.getValue()].animate()
                        .translationX(points[p].x*2-w)
                        .translationY(points[p].y*2-h)
                        .scaleX(1).scaleY(1)
                        .rotation(0)
                        .setDuration(100)
                        .start();
    }
    private void leaderboard() {
        TableLayout table=findViewById(R.id.leaderboard_table);
        table.removeAllViews();
        TableRow row=new TableRow(this);
        TextView t=new TextView(this);
        t.setText("Round");
        t.setBackground(getDrawable(R.drawable.table_head_cell));
        t.setPadding(20,10,20,10);
        t.setTextColor(Color.WHITE);
        row.addView(t);
        for (String s:keys){
            t=new TextView(this);
            t.setText(game.players.get(s));
            t.setBackground(getDrawable(R.drawable.table_head_cell));
            t.setPadding(20,10,20,10);
            t.setTextColor(Color.WHITE);
            row.addView(t);
        }
        table.addView(row);
        HashMap<String,ArrayList<String>>board=new HashMap<>();
        for(Map.Entry<String,String>e:game.leaderboard.entrySet())
            board.put(e.getKey(),new ArrayList<>(Arrays.asList(e.getValue().split(","))));
        for(int i=0;i<=game.round;i++){
            row=new TableRow(this);
            t=new TextView(this);
            t.setText(""+(i+1));
            t.setTextColor(Color.BLACK);
            t.setBackground(getDrawable(R.drawable.table_cell));
            t.setGravity(Gravity.CENTER);
            row.addView(t);
            for(String s:keys){
                String n="";
                if (board.get(s)!=null && board.get(s).size()>i)
                    n=board.get(s).get(i);
                t=new TextView(this);
                if (n.indexOf("=")==0){
                    t.setText(" "+n.substring(1)+" ");
                    t.setPaintFlags(t.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);
                }else{
                    t.setText(n);
                }
                t.setBackground(getDrawable(R.drawable.table_cell));
                t.setGravity(Gravity.CENTER);
                t.setTextColor(Color.BLACK);
                row.addView(t);
            }
            table.addView(row);
        }
    }
    private void current_table() {
        currentTable.removeAllViews();
        TableRow row=new TableRow(this);
        TextView t=new TextView(this);
        t.setText("Round");
        t.setBackground(getDrawable(R.drawable.table_head_cell));
        t.setPadding(20,10,20,10);
        t.setTextColor(Color.WHITE);
        row.addView(t);
        for (String s:keys){
            t=new TextView(this);
            t.setText(game.players.get(s));
            t.setBackground(getDrawable(R.drawable.table_head_cell));
            t.setPadding(20,10,20,10);
            t.setTextColor(Color.WHITE);
            row.addView(t);
        }
        currentTable.addView(row);
        HashMap<String,ArrayList<String>>board=new HashMap<>();
        if(game.leaderboard==null)return;
        for(Map.Entry<String,String>e:game.leaderboard.entrySet())
            board.put(e.getKey(),new ArrayList<>(Arrays.asList(e.getValue().split(","))));
        row=new TableRow(this);
        t=new TextView(this);
        t.setText(""+(game.round+1));
        t.setTextColor(Color.BLACK);
        t.setBackground(getDrawable(R.drawable.table_cell));
        t.setGravity(Gravity.CENTER);
        row.addView(t);
        for(String s:keys){
            String n="";
            if (board.get(s)!=null && board.get(s).size()>game.round)
                n=board.get(s).get(game.round);
            t=new TextView(this);
            if (n!=null){
                t.setText(n);
            }
            t.setBackground(getDrawable(R.drawable.table_cell));
            t.setGravity(Gravity.CENTER);
            t.setTextColor(Color.BLACK);
            row.addView(t);
        }
        currentTable.addView(row);
    }
    public void shufflecards(){
        if (game==null || game.z==null)
            return;
        Collections.shuffle(game.z);
        ref.child("/z").setValue(game.z);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ref.child("task").setValue("shuffle");
                    Thread.sleep(100);
                    ref.child("task").setValue("nothing");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void loadshuffle(){
        for (int i=game.z.size()-1;i>=0;i--)
            cardsimg[game.z.get(i)].bringToFront();
    }
    public void sharecards(View view){
        if (view.getId()==R.id.sharebtn){
            shufflecards();
            findViewById(R.id.sharelayout).setVisibility(View.VISIBLE);
            findViewById(R.id.sharelayout).bringToFront();
        }else if(view.getId()==R.id.share_ok){
            EditText e=findViewById(R.id.cardcountedit);
            TextView t=findViewById(R.id.share_error_txt);
            if (e.getText().toString().isEmpty()){
                t.setText("Enter number of cards");
                t.setVisibility(View.VISIBLE);
                t.startAnimation(AnimationUtils.loadAnimation(this,R.anim.blink));
            }else{
                int c=Integer.parseInt(e.getText().toString());
                if (c<1){
                    t.setText("Number must be greater than 0");
                    t.setVisibility(View.VISIBLE);
                    t.startAnimation(AnimationUtils.loadAnimation(this,R.anim.blink));
                    return;
                }else if(c*keys.size()>game.z.size()){
                    t.setText("Not enough cards");
                    t.setVisibility(View.VISIBLE);
                    t.startAnimation(AnimationUtils.loadAnimation(this,R.anim.blink));
                    return;
                }else
                    t.setVisibility(View.GONE);
                String k=game.lastId;
                int ps=(keys.indexOf(k)+1)%keys.size();
                k=keys.get(ps);
                if (game.off!=null)
                    while (game.off.containsKey(k)){
                        ps=(ps+1)%keys.size();
                        k=keys.get(ps);
                    }
                ref.child("lastId").setValue(k);
                HashMap<String,String>cards=new HashMap<>();
                int x=0;
                for (int i=0;i<c;i++){
                    for(int p=0;p<keys.size();p++){
                        String s=keys.get((p+ps)%keys.size());
                        if(game.off!=null && game.off.containsKey(s))continue;
                        if (i==0)
                            cards.put(s,game.z.get(x)+"");
                        else
                            cards.put(s,cards.get(s)+","+game.z.get(x));
                        x++;
                    }
                }
                HashMap<String,Integer>hands=new HashMap<>();
                for (String ks:keys)
                    hands.put(ks,0);
                ref.child("initial").setValue(-1);
                ref.child("sir").setValue(presir);
                ref.child("CurrentCards").setValue(cards);
                ref.child("CurrentHands").setValue(hands);
                findViewById(R.id.sharelayout).setVisibility(View.GONE);
                findViewById(R.id.sharebtn).setVisibility(View.GONE);
                final String finalK = k;
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            ref.child("task").setValue("distribute");
                            ref.child("initId").setValue("");
                            ref.child("initial").setValue(-1);
                            Thread.sleep(100);
                            ref.child("lastId").setValue(finalK);
                            ref.child("task").setValue(finalK +":h");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }else{
            findViewById(R.id.sharelayout).setVisibility(View.GONE);
        }
    }
    public void distribute(){
        if (game.CurrentCards==null || !game.CurrentCards.containsKey(myid) || game.CurrentCards.get(myid).isEmpty())return;
        int indx=keys.indexOf(myid);
        canvas.invalidate();
        for(Point p:points){
            if (game.off!=null)
            while (game.off.containsKey(keys.get(indx))){
                indx=(indx+1)%keys.size();
            }
            String[] ar = Objects.requireNonNull(game.CurrentCards.get(keys.get(indx))).split(",");
            for(int l=0;l<ar.length;l++) {
                int a= Integer.parseInt(ar[l]);
                cardsimg[a].animate().translationX(p.x-w/2).translationY(p.y-h/2-60).setDuration(100).start();
            }
            indx=(indx+1)%keys.size();
        }
        findViewById(R.id.sort_btn).setVisibility(View.VISIBLE);
        String[] ar = Objects.requireNonNull(game.CurrentCards.get(myid)).split(",");
        mycards=new int[ar.length];
        for(int l=0;l<ar.length;l++){
            mycards[l]=Integer.parseInt(ar[l]);
            cardsimg[mycards[l]].bringToFront();
            cardsimg[mycards[l]].setImageBitmap(getRoundedCornerBitmap(cards[mycards[l]%52+1],12));
            final int dx = mycards[l];
            cardsimg[dx].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickable){
                        set_my_cards();
                        if (dx==selected){
                            cardsimg[selected].setOnClickListener(null);
                            ref.child("showing/"+myid).setValue(selected);
                            if (game.initId.isEmpty()) {
                                ref.child("initId").setValue(myid);
                                ref.child("initial").setValue((selected % 52) / 13);
                            }else if (game.initial!=(selected%52)/13 && game.sir<0)
                                ref.child("sir").setValue((selected%52)/13);
                            ref.child("task").setValue("show");
                            try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
                            int id=(keys.indexOf(myid)+1)%keys.size();
                            if(keys.get(id).equals(game.initId)){
                                ref.child("task").setValue("calculate");
                            }else{
                                String k=keys.get(id);
                                if (game.off!=null)
                                while (game.off.containsKey(k)){
                                    id=(id+1)%keys.size();
                                    k=keys.get(id);
                                }
                                if(k.equals(game.initId))
                                    ref.child("task").setValue("calculate");
                                else
                                    ref.child("task").setValue(k + ":t");
                            }
                            int cr=0;
                            int[] newar =new int[mycards.length-1];
                            for (int c:mycards)
                                if (c!=selected)
                                    newar[cr++]=c;
                            mycards=newar;
                            StringBuilder sb=new StringBuilder();
                            ArrayUtils.writeArray(sb,mycards);
                            ref.child("CurrentCards/"+myid).setValue(sb.toString());
                            selected=-1;
                            clickable=false;
                        }else{
                            if((dx%52)/13!=game.initial){
                                boolean found=false;
                                for (int c:mycards){
                                    if((c%52)/13==game.initial){
                                        found=true;
                                        break;
                                    }
                                }
                                if (found)return;
                            }
                            selected=dx;
                            cardsimg[dx].animate()
                                    .translationYBy(-50)
                                    .setDuration(100)
                                    .start();
                        }
                    }
                }
            });
        }
        set_my_cards();
        for (int i=0;i<cardsimg.length;i++){
            if (i<mycards.length*(keys.size()-(game.off==null?0:game.off.size())))
                cardsimg[game.z.get(i)].setVisibility(View.VISIBLE);
            else
                cardsimg[game.z.get(i)].setVisibility(View.GONE);
        }
    }
    private void calculate_hand(){
        String big=game.initId;
        boolean neg=false;
        for (int i=0;i<keys.size();i++){
            int c=(i+keys.indexOf(game.initId))%keys.size();
            if (game.off!=null && game.off.containsKey(keys.get(c)))continue;
            Integer card=game.showing.get(keys.get(c));
            if (card!=null)
            if (card<-1)
                neg=true;
            else{
                int type=(card%52)/13;
                if (game.sir>-1 && type==game.sir){
                    if(card%13==0)
                        big=keys.get(c);
                    else{
                        Integer b=game.showing.get(big);
                        if (b!=null)
                        if((b%52)/13==game.sir){
                            if(b%13!=0 && b%52<=card%52)
                                big=keys.get(c);
                        }else{
                            big=keys.get(c);
                        }
                    }
                }else{
                    Integer b=game.showing.get(big);
                    if (b!=null)
                    if((b%52)/13!=game.sir && type==game.initial){
                        if(card%13==0 || (b%13!=0 && b%52<=card%52))
                            big=keys.get(c);
                    }
                }
            }
        }
        if (!neg){
            ref.child("CurrentHands/"+big).setValue(game.CurrentHands.get(big)+1);
            final String finalBig = big;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ref.child("task").setValue(finalBig +":won");
                        ref.child("initId").setValue("");
                        ref.child("initial").setValue(-1);
                        Thread.sleep(4000);
                        ref.child("task").setValue(finalBig+":get");
                        Thread.sleep(1000);
                        reset_turn();
                    } catch (InterruptedException e) {e.printStackTrace();}
                }
            }).start();
        }
    }
    private void reset_turn(){
        for (String k:keys){
            ref.child("showing/"+k).setValue(-1);
        }
        if (mycards.length>0) {
            if (game.task.substring(game.task.lastIndexOf(":")+1).equals("get"))
                ref.child("task").setValue(game.task.split(":")[0] + ":t");
        }else{
            ref.child("task").setValue("reset");
            ref.child("sir").setValue(-1);
            for (String k:keys){
                if (game.off!=null && game.off.containsKey(k))
                    continue;
                String l=game.leaderboard.get(k);
                assert l != null;
                int idx=l.lastIndexOf(",");
                int h=Integer.parseInt(l.substring(idx+1));
                if(h!=game.CurrentHands.get(k)){
                    ref.child("leaderboard/"+k).setValue(l.substring(0,idx+1)+"="+h);
                }else{
                    if(h==0){
                        ref.child("leaderboard/"+k).setValue(l.substring(0,idx+1)+"10");
                    }else{
                        ref.child("leaderboard/"+k).setValue(l.substring(0,idx+1)+h+""+h);
                    }
                }
                ref.child("CurrentHands/" + k).setValue(0);
            }
            ref.child("round").setValue(game.round+1);
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.sharebtn).setVisibility(View.VISIBLE);
                }
            });
            ref.child("task").setValue("");
            presir=(presir+1)%4;
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ImageView img=findViewById(R.id.heartimg);
                    img.setPadding(0,0,0,0);
                    img.setBackground(null);
                    img=findViewById(R.id.spadeimg);
                    img.setPadding(0,0,0,0);
                    img.setBackground(null);
                    img=findViewById(R.id.diamondimg);
                    img.setPadding(0,0,0,0);
                    img.setBackground(null);
                    img=findViewById(R.id.clubimg);
                    img.setPadding(0,0,0,0);
                    img.setBackground(null);
                    img=findViewById(R.id.cutimg);
                    img.setPadding(0,0,0,0);
                    img.setBackground(null);
                }
            });
            int id=0;
            switch (presir){
                case 0:
                    id=R.id.clubimg;
                    break;
                case 3:
                    id=R.id.diamondimg;
                    break;
                case 1:
                    id=R.id.heartimg;
                    break;
                case 2:
                    id=R.id.spadeimg;
                    break;
                case -1:
                    id=R.id.cutimg;
                    break;
            }
            final int finalId = id;
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ImageView img=findViewById(finalId);
                    img.setPadding(15,25,15,25);
                    img.setBackground(getDrawable(R.drawable.silected_card));
                }
            });
        }
    }
    private void set_my_cards(){
        if (mycards!=null)
        for(int c=0;c<mycards.length;c++){
            cardsimg[mycards[c]].animate()
                    .translationX(points[0].x-w/2-mycards.length*30+c*60)
                    .translationY(points[0].y-h/2+40)
                    .setDuration(100)
                    .start();
        }
    }
    private void set_showing(){
        int pos=keys.indexOf(myid);
        for (int p=0;p<points.length;p++,pos=(pos+1)%keys.size()){
            String k=keys.get(pos);
            if (game.off!=null)
                while(game.off.containsKey(k)){
                pos=(pos+1)%keys.size();
                k=keys.get(pos);
            }
            Integer v=game.showing.get(k);
            if (v!=null && v>=0){
                cardsimg[v].animate()
                        .translationX((points[p].x-w/2)*10/24)
                        .translationY((points[p].y-h/2)*10/24)
                        .rotation(0)
                        .setDuration(100)
                        .start();
                cardsimg[v].setImageBitmap(getRoundedCornerBitmap(cards[v%52+1],12));
                cardsimg[v].bringToFront();
            }
        }
        }
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, (float) pixels, (float) pixels, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        canvas.drawRoundRect(rectF, (float) pixels, (float) pixels, paint);
        return output;
    }
    public void handsdone(View view) {
        EditText e=findViewById(R.id.hands_edit);
        TextView t=findViewById(R.id.hands_error);
        if (e.getText().toString().isEmpty()){
            t.setText("Write Your Bids");
            t.setVisibility(View.VISIBLE);
        }else{
            int h= Integer.parseInt(e.getText().toString());
            if (h>mycards.length){
                t.setText("hands limit is "+mycards.length);
                t.setVisibility(View.VISIBLE);
            }else{
                int id=(keys.indexOf(myid)+1)%keys.size();
                boolean last=game.lastId.equals(keys.get(id));
                if (!last && game.off!=null){
                    int pos=id;
                    boolean all=true;
                    while(!keys.get(pos).equals(game.lastId)){
                        all&=game.off.containsKey(keys.get(pos));
                        pos=(pos+1)%keys.size();
                    }
                    last=all;
                }
                if (last){
                    int sum=0;
                    if (game.leaderboard!=null)
                    for(Map.Entry<String, String> ne:game.leaderboard.entrySet()){
                        if(!ne.getKey().equals(myid) && (game.off==null || !game.off.containsKey(ne.getKey()))){
                            if (ne.getValue().split(",").length>game.round)
                            sum += Integer.parseInt(ne.getValue().split(",")[game.round]);
                        }
                    }
                    if (sum+h==mycards.length){
                        t.setText(h+" hands not allowed!");
                        t.setVisibility(View.VISIBLE);
                        return;
                    }
                }
                ref.child("task").setValue("");
                t.setVisibility(View.GONE);
                if (game.round==0)
                    ref.child("leaderboard/"+myid).setValue(""+h);
                else
                    ref.child("leaderboard/"+myid).setValue(game.leaderboard.get(myid)+","+h);
                final int finalId = id;
                findViewById(R.id.hands_layout).setVisibility(View.GONE);
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            ref.child("task").setValue("leaderboard");
                            Thread.sleep(100);
                            int pos= finalId;
                            String k=keys.get(pos);
                            if (game.off!=null){
                                while (game.off.containsKey(k)) {
                                    if (game.round == 0)
                                        ref.child("leaderboard/" + k).setValue("x");
                                    else
                                        ref.child("leaderboard/" + k).setValue(game.leaderboard.get(k) + ",x");
                                    pos = (pos + 1) % keys.size();
                                    k = keys.get(pos);
                                }
                            }
                            if(k.equals(game.lastId))
                                ref.child("task").setValue(k+":t");
                            else
                                ref.child("task").setValue(k+":h");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }
    private void getCards() {
        cards[0] = BitmapFactory.decodeResource(getResources(), R.drawable.cardback);
        Bitmap card = BitmapFactory.decodeResource(getResources(), R.drawable.cards);
        if (card == null){
            recreate();
            return;
        }
        int w=card.getWidth()/13;
        int h=card.getHeight()/4;
        Paint p=new Paint();
        p.setColor(Color.WHITE);
        for(int i=0;i<13;i++){
            for (int j=0;j<4;j++){
                Bitmap bmp=Bitmap.createBitmap(card,i*w,j*h,w,h,null,false);
                Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                Canvas c=new Canvas(bitmap);
                c.drawRect(0,0,c.getWidth(),c.getHeight(),p);
                c.drawBitmap(bmp,0,0,p);
                cards[1+i+j*13]=bitmap;
            }
        }
    }
    private void loadgame(){
        if(game==null || game.players==null)
            return;
        keys=new ArrayList<>(game.players.keySet());
        Collections.sort(keys);
        w=canvas.getWidth();
        h=canvas.getHeight();
        if(cardsimg==null && w>0 && h>0){
//            cardsimg=new ImageView[game.decks*52];
//            index=new int[game.decks*52];
            RelativeLayout.LayoutParams params1=new RelativeLayout.LayoutParams(cw,ch);
            params1.leftMargin= (int) (w/2-cw/2);
            params1.topMargin= (int) (h/2-ch/2);
            for(int j=0;j<index.length;j++){
                index[j]=j;
                cardsimg[j]=new ImageView(PlayGame.this);
                cardsimg[j].setLayoutParams(params1);
                cardsimg[j].setImageBitmap(cards[0]);
                cardlayout.addView(cardsimg[j]);
            }
        }
        first=false;
        if (game.off!=null)
            points=new Point[game.players.size()-game.off.size()];
        else
            points=new Point[game.players.size()];
        int cx= (int) (w/2),cy= (int) (h/2);
        int a=90;
        int d=360/points.length;
        int k=0;
        while(k<points.length){
            double x=cx+(w/2-120)*Math.cos(Math.toRadians(a));
            double y=cy+(w/2-120)*Math.sin(Math.toRadians(a));
            if(x>w-50){
                x=w-50;
                y=cy+(x-cx)*(y-cy)/(x-cx);
            }else if(x<50){
                x=50;
                y=cy+(x-cx)*(y-cy)/(x-cx);
            }
            if(y>h-110){
                y=h-110;
                x=cx+(y-cy)*(x-cx)/(y-cy);
            }else if(y<110){
                y=110;
                x=cx+(y-cy)*(x-cx)/(y-cy);
            }
            points[k]=new Point((int)x,(int)y);
            a+=d;
            if (a<0)a+=360;
            k++;
        }
        canvas.invalidate();
    }
    public void setsir(View view) {
        ImageView img=findViewById(R.id.heartimg);
        img.setPadding(0,0,0,0);
        img.setBackground(null);
        img=findViewById(R.id.spadeimg);
        img.setPadding(0,0,0,0);
        img.setBackground(null);
        img=findViewById(R.id.diamondimg);
        img.setPadding(0,0,0,0);
        img.setBackground(null);
        img=findViewById(R.id.clubimg);
        img.setPadding(0,0,0,0);
        img.setBackground(null);
        img=findViewById(R.id.cutimg);
        img.setPadding(0,0,0,0);
        img.setBackground(null);
        img= (ImageView) view;
        img.setPadding(15,25,15,25);
        img.setBackground(getDrawable(R.drawable.silected_card));
        switch (view.getId()){
            case R.id.clubimg:
                presir=0;
                break;
            case R.id.diamondimg:
                presir=3;
                break;
            case R.id.heartimg:
                presir=1;
                break;
            case R.id.spadeimg:
                presir=2;
                break;
            case R.id.cutimg:
                presir=-1;
                break;
        }
    }
    public void sortCards(View view) {
        int[] sar ={2,3,0,1};
        for(int i=0;i<mycards.length-1;i++){
            for(int j=0;j<mycards.length-1;j++){
                int c1=mycards[j]%52;
                int c2=mycards[j+1]%52;
                if(sar[c1/13]>sar[c2/13]){
                    int t=mycards[j];
                    mycards[j]=mycards[j+1];
                    mycards[j+1]=t;
                }else if (c1/13==c2/13){
                    if(c2%13==0 || (c1%13!=0 && c1<c2)){
                        int t=mycards[j];
                        mycards[j]=mycards[j+1];
                        mycards[j+1]=t;
                    }
                }
            }
        }
        for (int c:mycards)
            cardsimg[c].bringToFront();
        set_my_cards();
    }
    public void openleaderboard(View view) {
        drawerLayout.openDrawer(GravityCompat.START);
    }
    public void closeclicked(View view){
        AlertDialog.Builder builder=new AlertDialog.Builder(this)
                .setMessage("Are You Sure you want to end this game?")
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ref.child("task").setValue("end");
                    }
                })
                .setNegativeButton("no", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }
    class GameView extends View{
        Paint paint;
        public GameView(Context context) {
            super(context);
            paint=new Paint();
        }
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if(points!=null){
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(1);
                paint.setTextSize(40);
                paint.setStyle(Paint.Style.FILL);
                int pos=keys.indexOf(myid);
                pos++;
                if (pos>=points.length)pos=0;
                for (int p=1;p<points.length;p++){
                    String k=keys.get(pos);
                    if (game.off!=null)
                    while(game.off.containsKey(k)){
                        pos=(pos+1)%keys.size();
                        k=keys.get(pos);
                    }
                    String name=game.players.get(k);
                    if (name!=null)
                    if (game.leaderboard!=null){
                        String handstr=game.leaderboard.get(k);
                        if (handstr!=null){
                            String[] hands = handstr.split(",");
                            if (hands.length<game.round+1)
                                canvas.drawText(name, points[p].x - 50, points[p].y + 100, paint);
                            else
                                canvas.drawText(game.players.get(k)+" ("+game.CurrentHands.get(k)+"/"+
                                                (hands[game.round].contains("=") ?hands[game.round].substring(1):hands[game.round]) + ")"
                                        , points[p].x - 70, points[p].y + 100, paint);
                        }else
                            canvas.drawText(name, points[p].x - 50, points[p].y + 100, paint);
                    }else
                        canvas.drawText(name, points[p].x - 50, points[p].y + 100, paint);
                    pos++;
                    if (pos>=points.length)pos=0;
                }
            }
        }
    }
    @Override
    public void onBackPressed(){
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawers();
        }else{
            if(!game.task.isEmpty() && !game.task.equals("loadgame")){
                Toast.makeText(this,"wait until the current round ends!",Toast.LENGTH_SHORT).show();
            }else{
                if(host)
                    ref.removeValue();
                else{
                    ref.removeEventListener(listener);
                    SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putString("gameid",gameid)
                            .putString("myid",myid).apply();
                    ref.child("off/"+myid).setValue(true);
                    ref.child("task").setValue("loadgame");
                }
                super.onBackPressed();
            }
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("gameid",gameid).putString("myid",myid).apply();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (listener==null){
            init_listener();
            resumed=true;
            ref.addValueEventListener(listener);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        ref.removeEventListener(listener);
        listener=null;
    }
    public static float pixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}