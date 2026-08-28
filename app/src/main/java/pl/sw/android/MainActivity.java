package pl.sw.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
    private static final String API="https://sw-android.onrender.com";
    private final ExecutorService ex=Executors.newSingleThreadExecutor();
    private LinearLayout root;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private TextView tv(String s,int z,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setTypeface(Typeface.DEFAULT,b?Typeface.BOLD:Typeface.NORMAL);t.setPadding(dp(14),dp(10),dp(14),dp(10));return t;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setGravity(Gravity.CENTER_VERTICAL);b.setBackground(bg(Color.rgb(17,43,61),15));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(60));p.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(p);return b;}

    @Override public void onCreate(Bundle b){super.onCreate(b);home();}
    private void frame(String title){
        ScrollView sv=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(10),dp(14),dp(24));root.setBackgroundColor(Color.rgb(3,15,25));sv.addView(root);setContentView(sv);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(0,dp(3),0,dp(8));
        TextView logo=tv("🛡️",30,true);logo.setPadding(0,0,dp(10),0);head.addView(logo,new LinearLayout.LayoutParams(dp(52),dp(60)));
        TextView brand=tv("BLUEBIRD SW\nSŁUŻBA WIĘZIENNA",17,true);head.addView(brand,new LinearLayout.LayoutParams(0,dp(60),1));root.addView(head);
        TextView online=tv("●  SYSTEM ONLINE   •   TERMINAL SŁUŻBY WIĘZIENNEJ",10,true);online.setTextColor(Color.rgb(130,205,235));online.setBackground(bg(Color.rgb(7,35,49),10));root.addView(online);
        TextView h=tv(title,23,true);h.setPadding(dp(2),dp(17),dp(2),dp(10));root.addView(h);
    }
    private void back(){Button b=btn("‹  PANEL GŁÓWNY");b.setOnClickListener(v->home());root.addView(b);}

    private void home(){
        frame("Centrum dowodzenia");
        TextView welcome=tv("BLUEBIRD SW\nCentrum operacyjne Służby Więziennej\n\n🟢 POŁĄCZENIE AKTYWNE",16,true);welcome.setBackground(bg(Color.rgb(7,42,55),18));root.addView(welcome);
        TextView sec=tv("MODUŁY",12,true);sec.setTextColor(Color.rgb(120,185,220));root.addView(sec);
        Button f=btn("👮  FUNKCJONARIUSZE SW\n      Pełna kadra • stopnie • status");
        Button s=btn("🕐  SŁUŻBA I GRAFIK\n      Zmiany i obsada");
        Button r=btn("📋  RAPORTY SŁUŻBOWE\n      Dokumentacja czynności");
        Button p=btn("🎖️  MÓJ PROFIL\n      Dane funkcjonariusza");
        Button br=btn("📢  BROADCASTER SW\n      Komunikaty dowództwa");
        Button d=btn("📁  DOKUMENTY SW\n      Rozkazy • procedury • regulaminy");
        root.addView(f);root.addView(s);root.addView(r);root.addView(p);root.addView(br);root.addView(d);
        TextView foot=tv("BLUEBIRD SW • SYSTEM RP\nWewnętrzny terminal Służby Więziennej",11,false);foot.setTextColor(Color.rgb(100,120,135));foot.setGravity(Gravity.CENTER);root.addView(foot);
        f.setOnClickListener(v->officers());s.setOnClickListener(v->simple("Służba i grafik","🕐  GRAFIK SŁUŻBY\n\n🟢 ZMIANA I      08:00–16:00\n🟡 ZMIANA II     16:00–00:00\n🔵 ZMIANA III    00:00–08:00"));
        r.setOnClickListener(v->simple("Raporty","📋  RAPORTY SŁUŻBOWE\n\n• rozpoczęcie służby\n• zakończenie służby\n• konwój\n• interwencja\n• zdarzenie\n• raport przełożonego"));
        p.setOnClickListener(v->simple("Mój profil","🎖️  PROFIL FUNKCJONARIUSZA\n\nDane, ID Discord, stopień i status są pobierane z systemu SW."));
        br.setOnClickListener(v->broadcast());d.setOnClickListener(v->simple("Dokumenty SW","📁  DOKUMENTY SŁUŻBY WIĘZIENNEJ\n\n📄 Rozkazy\n📄 Zarządzenia\n📄 Procedury\n📄 Regulaminy\n📄 Informacje służbowe"));
    }

    private void simple(String title,String text){frame(title);TextView x=tv(text,16,false);x.setBackground(bg(Color.rgb(11,35,51),16));root.addView(x);back();}

    private void officers(){
        frame("Funkcjonariusze SW");
        TextView status=tv("⏳  POBIERANIE PEŁNEJ KADRY...",15,true);status.setBackground(bg(Color.rgb(7,37,53),14));root.addView(status);back();
        ex.execute(()->{
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(API+"/api/officers").openConnection();c.setRequestMethod("GET");c.setConnectTimeout(12000);c.setReadTimeout(20000);
                int code=c.getResponseCode();BufferedReader r=new BufferedReader(new InputStreamReader(code<400?c.getInputStream():c.getErrorStream(),StandardCharsets.UTF_8));StringBuilder z=new StringBuilder();String l;while((l=r.readLine())!=null)z.append(l);r.close();
                JSONObject j=new JSONObject(z.toString());if(code!=200)throw new Exception(j.optString("message",j.optString("error","API error")));JSONArray a=j.optJSONArray("officers");
                runOnUiThread(()->{
                    int count=a==null?0:a.length();status.setText("👮  KADRA SW  •  "+count+" FUNKCJONARIUSZY");if(a==null)return;
                    for(int i=0;i<a.length();i++)try{
                        JSONObject m=a.getJSONObject(i);String name=m.optString("displayName",m.optString("username","Nieznany"));String rank=m.optString("rank","Brak stopnia");String id=m.optString("id","-");String ss=m.optString("status","Aktywny");
                        if(rank.trim().isEmpty())rank="Brak stopnia";
                        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(10),dp(14),dp(10));card.setBackground(bg(Color.rgb(10,39,55),16));
                        TextView n=tv("👤  "+name,17,true);TextView q=tv("🎖️  "+rank,16,true);q.setTextColor(Color.rgb(155,215,242));TextView x=tv("🆔  "+id+"   •   🟢 "+ss,12,false);x.setTextColor(Color.rgb(175,190,198));card.addView(n);card.addView(q);card.addView(x);
                        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));root.addView(card,Math.max(1,root.getChildCount()-1),p);
                    }catch(Exception ignored){}
                });
            }catch(Exception e){runOnUiThread(()->status.setText("❌  BŁĄD POŁĄCZENIA\n"+e.getMessage()));}
        });
    }

    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setHintTextColor(Color.GRAY);e.setTextColor(Color.WHITE);e.setTextSize(15);e.setBackground(bg(Color.rgb(17,42,58),12));e.setPadding(dp(14),dp(8),dp(14),dp(8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.setMargins(0,dp(4),0,dp(4));e.setLayoutParams(p);return e;}
    private void broadcast(){
        frame("Broadcaster SW");root.addView(tv("📢  KOMUNIKAT DOWÓDZTWA",13,true));EditText t=input("Tytuł komunikatu"),m=input("Treść komunikatu"),ch=input("ID kanału Discord");root.addView(t);root.addView(m);root.addView(ch);Button send=btn("📤  WYŚLIJ DO DISCORDA");root.addView(send);TextView out=tv("",14,false);root.addView(out);back();
        send.setOnClickListener(v->{if(m.getText().toString().trim().isEmpty()||ch.getText().toString().trim().isEmpty()){out.setText("❌  Uzupełnij treść i ID kanału.");return;}ex.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(API+"/api/broadcaster/send").openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");JSONObject j=new JSONObject();j.put("channelId",ch.getText().toString().trim());j.put("title",t.getText().toString());j.put("content",m.getText().toString());c.getOutputStream().write(j.toString().getBytes(StandardCharsets.UTF_8));int code=c.getResponseCode();runOnUiThread(()->out.setText(code>=200&&code<300?"✅  Komunikat wysłany.":"❌  Błąd HTTP "+code));}catch(Exception e){runOnUiThread(()->out.setText("❌  "+e.getMessage()));}});});
    }
    @Override protected void onDestroy(){ex.shutdownNow();super.onDestroy();}
}
