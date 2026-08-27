package pl.sw.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
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
    private LinearLayout root;

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String s, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setPadding(dp(14), dp(10), dp(14), dp(10)); return t;
    }
    private GradientDrawable bg(int color, int radius) { GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private Button menu(String s) { Button b=new Button(this); b.setText(s); b.setTextSize(16); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setGravity(Gravity.CENTER_VERTICAL); b.setPadding(dp(12),0,dp(12),0); b.setBackground(bg(Color.rgb(25,48,70),18)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(62)); p.setMargins(0,0,0,dp(12)); b.setLayoutParams(p); return b; }
    private EditText input(String hint) { EditText e=new EditText(this); e.setHint(hint); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.LTGRAY); e.setPadding(dp(14),0,dp(14),0); e.setBackground(bg(Color.rgb(25,48,70),14)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58)); p.setMargins(0,0,0,dp(12)); e.setLayoutParams(p); return e; }
    private void base(String title) {
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(24),dp(18),dp(24)); root.setBackgroundColor(Color.rgb(9,22,34));
        TextView head=label("🛡️  SŁUŻBA WIĘZIENNA RP",24,true); root.addView(head); TextView sub=label(title,20,true); sub.setPadding(dp(14),dp(2),dp(14),dp(18)); root.addView(sub); scroll.addView(root); setContentView(scroll);
    }
    private void back() { Button b=menu("← Powrót"); b.setOnClickListener(v->home()); root.addView(b); }
    @Override public void onCreate(Bundle b){super.onCreate(b); home();}

    private void home(){
        base("Panel główny");
        TextView welcome=label("👋 Panel służbowy\nPołączono z systemem SW",16,false); welcome.setBackground(bg(Color.rgb(16,39,57),18)); root.addView(welcome);
        Button officers=menu("👮  Funkcjonariusze"); Button broad=menu("📢  Broadcaster"); Button schedule=menu("📅  Grafik służby"); Button reports=menu("📋  Raporty służbowe"); Button docs=menu("📄  Dokumenty"); Button profile=menu("👤  Mój profil");
        root.addView(officers);root.addView(broad);root.addView(schedule);root.addView(reports);root.addView(docs);root.addView(profile);
        officers.setOnClickListener(v->officers()); broad.setOnClickListener(v->broadcaster());
        schedule.setOnClickListener(v->simple("Grafik służby","📅 DZISIAJ\n\n08:00–16:00  Służba dzienna\n16:00–00:00  Służba popołudniowa\n00:00–08:00  Służba nocna"));
        reports.setOnClickListener(v->simple("Raporty","📋 Moduł raportów służbowych")); docs.setOnClickListener(v->simple("Dokumenty","📄 Regulamin\n📄 Procedury\n📄 Wzory dokumentów")); profile.setOnClickListener(v->simple("Mój profil","👤 Dane użytkownika będą pobierane z Discorda."));
    }
    private void simple(String title,String body){base(title); TextView t=label(body,17,false);t.setBackground(bg(Color.rgb(16,39,57),18));root.addView(t);back();}

    private void officers(){
        base("Funkcjonariusze"); TextView status=label("⏳ Pobieram funkcjonariuszy z Discorda...",16,false); root.addView(status); back();
        executor.execute(()->{try{
            HttpURLConnection c=(HttpURLConnection)new URL(API+"/api/officers").openConnection();c.setRequestMethod("GET");c.setConnectTimeout(12000);c.setReadTimeout(20000);int code=c.getResponseCode();BufferedReader r=new BufferedReader(new InputStreamReader(code<400?c.getInputStream():c.getErrorStream()));StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);JSONObject json=new JSONObject(s.toString());if(code!=200)throw new Exception(json.optString("error","API error"));JSONArray a=json.optJSONArray("officers");runOnUiThread(()->{status.setText("👮  Funkcjonariusze: "+(a==null?0:a.length()));if(a==null)return;for(int i=0;i<a.length();i++)try{JSONObject m=a.getJSONObject(i);String name=m.optString("displayName",m.optString("username"));String id=m.optString("id");TextView card=label("👤  "+name+"\n🆔  "+id+"\n🟢  Aktywny",16,true);card.setBackground(bg(Color.rgb(16,39,57),18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(92));p.setMargins(0,0,0,dp(10));root.addView(card,p,root.indexOfChild(status)+1+i);}catch(Exception ignored){} });
        }catch(Exception e){runOnUiThread(()->status.setText("❌ Nie udało się pobrać funkcjonariuszy.\n"+e.getMessage()));}});
    }

    private void broadcaster(){
        base("📢 Broadcaster SW");
        TextView info=label("Wyślij komunikat bezpośrednio na Discorda.",15,false);root.addView(info);
        EditText title=input("Tytuł komunikatu");EditText content=input("Treść komunikatu");content.setMinHeight(dp(120));EditText channel=input("ID kanału Discord");EditText color=input("Kolor embeda, np. 3447003");
        Spinner mention=new Spinner(this);mention.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Bez wzmianki","@everyone","@here"}));root.addView(title);root.addView(content);root.addView(channel);root.addView(color);root.addView(mention);
        Button send=menu("📤  WYŚLIJ KOMUNIKAT");root.addView(send);TextView result=label("",15,false);root.addView(result);Button history=menu("📜  Historia komunikatów");root.addView(history);back();
        send.setOnClickListener(v->send(title.getText().toString(),content.getText().toString(),channel.getText().toString(),color.getText().toString(),mention.getSelectedItemPosition(),result));history.setOnClickListener(v->history());
    }
    private void send(String title,String content,String channel,String color,int mi,TextView result){if(content.trim().isEmpty()||channel.trim().isEmpty()){result.setText("❌ Wpisz treść i ID kanału.");return;}executor.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(API+"/api/broadcaster/send").openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");JSONObject j=new JSONObject();j.put("channelId",channel.trim());j.put("title",title);j.put("content",content);j.put("color",color.trim().isEmpty()?3447003:Integer.parseInt(color.trim()));j.put("mention",mi==1?"everyone":mi==2?"here":"");c.getOutputStream().write(j.toString().getBytes("UTF-8"));int code=c.getResponseCode();runOnUiThread(()->result.setText(code>=200&&code<300?"✅ Wysłano na Discorda.":"❌ Backend zwrócił kod "+code));}catch(Exception e){runOnUiThread(()->result.setText("❌ Błąd wysyłania: "+e.getMessage()));}});}
    private void history(){base("📜 Historia Broadcastera");TextView s=label("⏳ Pobieram historię...",16,false);root.addView(s);back();executor.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(API+"/api/broadcaster/history").openConnection();int code=c.getResponseCode();BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));StringBuilder z=new StringBuilder();String q;while((q=r.readLine())!=null)z.append(q);JSONObject j=new JSONObject(z.toString());JSONArray a=j.optJSONArray("history");runOnUiThread(()->{s.setText("📜 Wysłane: "+(a==null?0:a.length()));if(a!=null)for(int i=0;i<a.length();i++)try{JSONObject x=a.getJSONObject(i);root.addView(label("📢 "+x.optString("title")+"\n👤 "+x.optString("authorName")+"\n🕒 "+x.optString("sentAt"),15,false));}catch(Exception ignored){}});}catch(Exception e){runOnUiThread(()->s.setText("❌ Nie można pobrać historii."));}});}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
