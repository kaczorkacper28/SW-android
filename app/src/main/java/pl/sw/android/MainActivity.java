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
    private LinearLayout root,content;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private TextView tv(String s,int z,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);t.setTypeface(Typeface.DEFAULT,b?Typeface.BOLD:Typeface.NORMAL);t.setPadding(dp(14),dp(10),dp(14),dp(10));return t;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setAllCaps(false);b.setGravity(Gravity.CENTER_VERTICAL);b.setBackground(bg(Color.rgb(20,43,58),14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(56));p.setMargins(0,dp(4),0,dp(4));b.setLayoutParams(p);return b;}

    @Override public void onCreate(Bundle b){super.onCreate(b);home();}
    private void frame(String title){
        ScrollView sv=new ScrollView(this); root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(22));root.setBackgroundColor(Color.rgb(5,18,28));sv.addView(root);setContentView(sv);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(4),dp(4),dp(4),dp(10));
        TextView icon=tv("🔵",28,true);icon.setPadding(0,0,dp(10),0);top.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(54)));
        TextView brand=tv("BLUEBIRD SW\nSŁUŻBA WIĘZIENNA",18,true);top.addView(brand,new LinearLayout.LayoutParams(0,dp(54),1));root.addView(top);
        TextView line=tv("TERMINAL MOBILNY  •  SYSTEM WEWNĘTRZNY",11,true);line.setTextColor(Color.rgb(110,170,205));root.addView(line);
        TextView h=tv(title,24,true);h.setPadding(dp(2),dp(14),dp(2),dp(10));root.addView(h);
        content=root;
    }
    private void back(){Button b=btn("‹  PANEL GŁÓWNY");b.setOnClickListener(v->home());root.addView(b);}
    private void home(){
        frame("Panel terminala");
        TextView st=tv("●  POŁĄCZENIE AKTYWNE\n   SW-ANDROID  /  DISCORD",14,true);st.setTextColor(Color.rgb(150,220,170));st.setBackground(bg(Color.rgb(10,45,43),16));root.addView(st);
        TextView q=tv("SZYBKI DOSTĘP",12,true);q.setTextColor(Color.rgb(110,170,205));root.addView(q);
        Button f=btn("👮  FUNKCJONARIUSZE SW\n      Kadra, stopnie i status służby");Button s=btn("🕐  SŁUŻBA I GRAFIK\n      Zmiany i obsada");Button r=btn("📋  RAPORTY SŁUŻBOWE\n      Dokumentacja czynności");Button p=btn("🎖️  MÓJ PROFIL\n      Dane funkcjonariusza");root.addView(f);root.addView(s);root.addView(r);root.addView(p);
        TextView op=tv("OPERACJE TERMINALA",12,true);op.setTextColor(Color.rgb(110,170,205));root.addView(op);
        Button b=btn("📢  BROADCASTER SW\n      Komunikaty do Discorda");Button d=btn("📁  DOKUMENTY SW\n      Rozkazy i informacje");root.addView(b);root.addView(d);
        TextView foot=tv("BLUEBIRD SW • WERSJA RP\nSystem przeznaczony do obsługi wewnętrznej serwera Służby Więziennej",11,false);foot.setTextColor(Color.rgb(110,130,140));foot.setGravity(Gravity.CENTER);root.addView(foot);
        f.setOnClickListener(v->officers());s.setOnClickListener(v->simple("Służba i grafik","🕐 GRAFIK SŁUŻBY\n\n🟢 Zmiana dzienna   08:00–16:00\n🟡 Zmiana popołudniowa   16:00–00:00\n🔵 Zmiana nocna   00:00–08:00\n\nModuł może zostać połączony z systemem kadrowym SW."));r.setOnClickListener(v->simple("Raporty","📋 RAPORTY SŁUŻBOWE\n\n• rozpoczęcie służby\n• zakończenie służby\n• interwencja\n• konwój\n• zdarzenie\n• raport przełożonego"));p.setOnClickListener(v->simple("Mój profil","🎖️ PROFIL FUNKCJONARIUSZA\n\nDane, stopień, ID Discord oraz status służby są pobierane z systemu SW."));b.setOnClickListener(v->broadcast());d.setOnClickListener(v->simple("Dokumenty SW","📁 DOKUMENTY SŁUŻBY WIĘZIENNEJ\n\n📄 Rozkazy\n📄 Zarządzenia\n📄 Procedury\n📄 Regulaminy\n📄 Informacje służbowe"));
    }
    private void simple(String title,String text){frame(title);TextView x=tv(text,16,false);x.setBackground(bg(Color.rgb(14,35,49),16));root.addView(x);back();}
    private void officers(){
        frame("Kadra funkcjonariuszy");TextView status=tv("⏳ Łączenie z bazą SW...",15,true);status.setBackground(bg(Color.rgb(12,40,55),14));root.addView(status);back();
        ex.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(API+"/api/officers").openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);int code=c.getResponseCode();BufferedReader r=new BufferedReader(new InputStreamReader(code<400?c.getInputStream():c.getErrorStream(),StandardCharsets.UTF_8));StringBuilder z=new StringBuilder();String l;while((l=r.readLine())!=null)z.append(l);JSONObject j=new JSONObject(z.toString());if(code!=200)throw new Exception(j.optString("message","API error"));JSONArray a=j.optJSONArray("officers");runOnUiThread(()->{status.setText("👮 KADRA SW: "+(a==null?0:a.length())+" funkcjonariuszy");if(a==null)return;for(int i=0;i<a.length();i++)try{JSONObject m=a.getJSONObject(i);String name=m.optString("displayName",m.optString("username","Nieznany"));String rank=m.optString("rank","Brak stopnia");String id=m.optString("id","-");String ss=m.optString("status","Aktywny");LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(10),dp(14),dp(10));card.setBackground(bg(Color.rgb(13,42,57),14));TextView n=tv("👤  "+name,17,true);TextView q=tv("🎖️  "+rank,16,true);q.setTextColor(Color.rgb(150,205,235));TextView x=tv("🆔 "+id+"   •   🟢 "+ss,12,false);card.addView(n);card.addView(q);card.addView(x);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));int idx=Math.min(root.indexOfChild(status)+1+i,root.getChildCount());root.addView(card,idx,p);}catch(Exception ignored){}});}catch(Exception e){runOnUiThread(()->status.setText("❌ Błąd połączenia\n"+e.getMessage()));}});
    }
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setHintTextColor(Color.GRAY);e.setTextColor(Color.WHITE);e.setTextSize(15);e.setBackground(bg(Color.rgb(18,42,57),12));e.setPadding(dp(14),dp(8),dp(14),dp(8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.setMargins(0,dp(4),0,dp(4));e.setLayoutParams(p);return e;}
    private void broadcast(){frame("Broadcaster SW");root.addView(tv("📢 KOMUNIKAT DOWÓDZTWA",13,true));EditText t=input("Tytuł komunikatu"),m=input("Treść komunikatu"),ch=input("ID kanału Discord");root.addView(t);root.addView(m);root.addView(ch);Button send=btn("📤  WYŚLIJ DO DISCORDA");root.addView(send);TextView out=tv("",14,false);root.addView(out);back();send.setOnClickListener(v->{if(m.getText().toString().trim().isEmpty()||ch.getText().toString().trim().isEmpty()){out.setText("❌ Uzupełnij treść i ID kanału.");return;}ex.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(API+"/api/broadcaster/send").openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");JSONObject j=new JSONObject();j.put("channelId",ch.getText().toString().trim());j.put("title",t.getText().toString());j.put("content",m.getText().toString());c.getOutputStream().write(j.toString().getBytes(StandardCharsets.UTF_8));int code=c.getResponseCode();runOnUiThread(()->out.setText(code>=200&&code<300?"✅ Komunikat wysłany.":"❌ Błąd HTTP "+code));}catch(Exception e){runOnUiThread(()->out.setText("❌ "+e.getMessage()));}});});}
    @Override protected void onDestroy(){ex.shutdownNow();super.onDestroy();}
}
