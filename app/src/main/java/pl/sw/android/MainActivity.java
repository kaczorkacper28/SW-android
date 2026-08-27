package pl.sw.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String API = "https://sw-android.onrender.com";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String s,float size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.BLACK);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(dp(8),dp(8),dp(8),dp(8));return t;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(16);b.setAllCaps(false);b.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(58)));return b;}
    private LinearLayout page(String title){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(20),dp(25),dp(20),dp(20));l.setBackgroundColor(Color.WHITE);l.addView(text("SŁUŻBA WIĘZIENNA",24,true));l.addView(text(title,21,true));return l;}
    private void setPage(LinearLayout l){ScrollView s=new ScrollView(this);s.addView(l);setContentView(s);}
    private void back(LinearLayout l){Button b=button("← Powrót");b.setOnClickListener(v->showHome());l.addView(b);}
    @Override public void onCreate(Bundle b){super.onCreate(b);showHome();}
    private void showHome(){LinearLayout l=page("Panel główny");l.addView(text("Panel Służby Więziennej RP",17,false));Button o=button("👮 Funkcjonariusze");Button bc=button("📢 Broadcaster");Button sc=button("📅 Grafik służby");Button r=button("📋 Raporty służbowe");Button d=button("📄 Dokumenty");Button p=button("👤 Mój profil");l.addView(o);l.addView(bc);l.addView(sc);l.addView(r);l.addView(d);l.addView(p);o.setOnClickListener(v->showOfficers());bc.setOnClickListener(v->showBroadcaster());sc.setOnClickListener(v->showSimple("Grafik służby","📅 Grafik służby\n\n08:00 – 16:00 — Służba dzienna\n16:00 – 00:00 — Służba popołudniowa\n00:00 – 08:00 — Służba nocna"));r.setOnClickListener(v->showSimple("Raporty służbowe","📋 Moduł raportów służbowych"));d.setOnClickListener(v->showSimple("Dokumenty","📄 Dokumenty służbowe"));p.setOnClickListener(v->showSimple("Mój profil","👤 Profil funkcjonariusza\n\nDane profilu będą pobierane z Discorda."));setPage(l);}
    private void showSimple(String title,String body){LinearLayout l=page(title);l.addView(text(body,17,false));back(l);setPage(l);}
    private void showOfficers(){LinearLayout l=page("Funkcjonariusze");l.addView(text("👮 Członkowie Twojego Discorda",18,true));TextView status=text("⏳ Pobieranie danych...",16,false);l.addView(status);back(l);setPage(l);executor.execute(()->{try{URL u=new URL(API+"/api/officers");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("GET");c.setConnectTimeout(10000);c.setReadTimeout(15000);int code=c.getResponseCode();BufferedReader br=new BufferedReader(new InputStreamReader(code>=200&&code<300?c.getInputStream():c.getErrorStream()));StringBuilder sb=new StringBuilder();String x;while((x=br.readLine())!=null)sb.append(x);JSONObject root=new JSONObject(sb.toString());if(code!=200)throw new Exception(root.optString("message",root.optString("error","Błąd API")));JSONArray a=root.getJSONArray("officers");runOnUiThread(()->{status.setText("👮 Funkcjonariusze: "+a.length());for(int i=0;i<a.length();i++){try{JSONObject m=a.getJSONObject(i);String name=m.optString("displayName",m.optString("username"));TextView row=text("👤 "+name+"\nDiscord ID: "+m.optString("id")+"\nStatus: 🟢 Aktywny",16,false);l.addView(row, l.indexOfChild(status)+1+i);}catch(Exception ignored){}}});}catch(Exception e){runOnUiThread(()->status.setText("❌ Nie udało się pobrać funkcjonariuszy\n"+e.getMessage()));}});}
    private void showBroadcaster(){LinearLayout l=page("📢 Broadcaster SW");EditText title=new EditText(this);title.setHint("Tytuł komunikatu");EditText content=new EditText(this);content.setHint("Treść komunikatu");content.setMinLines(5);EditText channel=new EditText(this);channel.setHint("ID kanału Discord");EditText color=new EditText(this);color.setHint("Kolor, np. 3447003");Spinner mention=new Spinner(this);mention.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Bez wzmianki","@everyone","@here"}));Button send=button("📤 WYŚLIJ KOMUNIKAT");TextView result=text("",15,false);l.addView(title);l.addView(content);l.addView(channel);l.addView(color);l.addView(mention);l.addView(send);l.addView(result);back(l);send.setOnClickListener(v->sendBroadcast(title.getText().toString(),content.getText().toString(),channel.getText().toString(),color.getText().toString(),mention.getSelectedItemPosition(),result));setPage(l);}
    private void sendBroadcast(String title,String content,String channel,String color,int mi,TextView result){if(content.trim().isEmpty()||channel.trim().isEmpty()){result.setText("❌ Uzupełnij treść i ID kanału.");return;}executor.execute(()->{try{URL u=new URL(API+"/api/broadcaster/send");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");String mention=mi==1?"everyone":mi==2?"here":"";JSONObject body=new JSONObject();body.put("channelId",channel.trim());body.put("title",title);body.put("content",content);body.put("color",color.trim().isEmpty()?3447003:Integer.parseInt(color.trim()));body.put("mention",mention);c.getOutputStream().write(body.toString().getBytes("UTF-8"));int code=c.getResponseCode();runOnUiThread(()->result.setText(code>=200&&code<300?"✅ Komunikat wysłany na Discorda.":"❌ Discord/backend odrzucił komunikat. Kod: "+code));}catch(Exception e){runOnUiThread(()->result.setText("❌ Błąd: "+e.getMessage()));}});}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
