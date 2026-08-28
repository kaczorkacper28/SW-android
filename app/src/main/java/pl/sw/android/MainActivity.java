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
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.json.*;

public class MainActivity extends Activity {
    private static final String API = "https://sw-android.onrender.com";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout root;

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private TextView label(String s, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setPadding(dp(16), dp(10), dp(16), dp(10));
        return t;
    }

    private GradientDrawable bg(int c, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(dp(r));
        return g;
    }

    private Button menu(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setPadding(dp(18), 0, dp(18), 0);
        b.setBackground(bg(Color.rgb(19, 48, 70), 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(60));
        p.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(p);
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.LTGRAY);
        e.setTextColor(Color.WHITE);
        e.setTextSize(16);
        e.setPadding(dp(16), dp(8), dp(16), dp(8));
        e.setBackground(bg(Color.rgb(19, 48, 70), 14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(60));
        p.setMargins(0, dp(6), 0, dp(6));
        e.setLayoutParams(p);
        return e;
    }

    private TextView section(String s) {
        TextView t = label(s.toUpperCase(), 13, true);
        t.setTextColor(Color.rgb(165, 195, 215));
        t.setPadding(dp(4), dp(18), dp(4), dp(6));
        return t;
    }

    private void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(Color.rgb(6, 22, 34));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView shield = label("🛡️", 34, false);
        shield.setPadding(0, 0, dp(10), 0);
        TextView brand = label("SŁUŻBA WIĘZIENNA\nCENTRALNY SYSTEM", 20, true);
        header.addView(shield, new LinearLayout.LayoutParams(dp(52), dp(62)));
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(62), 1));
        root.addView(header);

        TextView titleView = label(title, 25, true);
        titleView.setPadding(dp(2), dp(18), dp(2), dp(14));
        root.addView(titleView);

        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.addView(root);
        setContentView(s);
    }

    private void back() {
        Button b = menu("←  Powrót do panelu głównego");
        b.setOnClickListener(v -> home());
        root.addView(b);
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        home();
    }

    private void home() {
        base("Panel służbowy");

        TextView welcome = label("SYSTEM OPERACYJNY SW\nPołączono z systemem kadrowym", 15, false);
        welcome.setBackground(bg(Color.rgb(12, 42, 61), 18));
        root.addView(welcome);

        root.addView(section("Zarządzanie służbą"));

        Button officers = menu("👮  Funkcjonariusze");
        Button duty = menu("🕐  Służba i grafik");
        Button reports = menu("📋  Raporty służbowe");
        Button profile = menu("🎖️  Mój profil służbowy");
        root.addView(officers);
        root.addView(duty);
        root.addView(reports);
        root.addView(profile);

        root.addView(section("Łączność i dokumentacja"));

        Button broadcaster = menu("📢  Komunikaty SW");
        Button documents = menu("📁  Dokumenty służbowe");
        root.addView(broadcaster);
        root.addView(documents);

        TextView footer = label("SŁUŻBA WIĘZIENNA RP • SYSTEM WEWNĘTRZNY\nWersja aplikacji 1.0", 12, false);
        footer.setTextColor(Color.rgb(130, 155, 170));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(22), 0, 0);
        root.addView(footer);

        officers.setOnClickListener(v -> officers());
        broadcaster.setOnClickListener(v -> broadcaster());
        duty.setOnClickListener(v -> simple("Służba i grafik", "🕐 GRAFIK SŁUŻBY\n\n08:00–16:00  Służba dzienna\n16:00–00:00  Służba popołudniowa\n00:00–08:00  Służba nocna\n\n📌 Grafik może zostać podłączony do systemu kadrowego."));
        reports.setOnClickListener(v -> simple("Raporty służbowe", "📋 RAPORTY\n\n• Raport rozpoczęcia służby\n• Raport zakończenia służby\n• Raport interwencji\n• Raport zdarzenia\n• Raport służbowy\n\nModuł przygotowany do rozbudowy."));
        documents.setOnClickListener(v -> simple("Dokumenty służbowe", "📁 DOKUMENTACJA SW\n\n📄 Regulamin Służby Więziennej\n📄 Procedury służbowe\n📄 Rozkazy i zarządzenia\n📄 Wzory dokumentów\n📄 Informacje wewnętrzne"));
        profile.setOnClickListener(v -> profile());
    }

    private void simple(String title, String body) {
        base(title);
        TextView box = label(body, 16, false);
        box.setBackground(bg(Color.rgb(14, 38, 55), 18));
        root.addView(box);
        back();
    }

    private void profile() {
        base("Mój profil służbowy");
        TextView info = label("🎖️  PROFIL FUNKCJONARIUSZA\n\n👤  Dane zostaną pobrane z Discorda.\n🎖️  Stopień służbowy\n🆔  ID Discord\n🟢  Status służby\n📅  Data dołączenia\n⭐  Punkty służbowe", 16, false);
        info.setBackground(bg(Color.rgb(14, 38, 55), 18));
        root.addView(info);
        back();
    }

    private void officers() {
        base("Funkcjonariusze SW");
        TextView status = label("⏳ Pobieram funkcjonariuszy z Discorda...", 16, false);
        status.setBackground(bg(Color.rgb(12, 42, 61), 16));
        root.addView(status);
        back();

        executor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection)new URL(API + "/api/officers").openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(12000);
                c.setReadTimeout(20000);
                int code = c.getResponseCode();
                InputStream stream = code < 400 ? c.getInputStream() : c.getErrorStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                StringBuilder s = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) s.append(line);
                JSONObject json = new JSONObject(s.toString());
                if (code != 200) throw new Exception(json.optString("error", "API error"));
                JSONArray a = json.optJSONArray("officers");

                runOnUiThread(() -> {
                    status.setText("👮  KADRA SW: " + (a == null ? 0 : a.length()) + " funkcjonariuszy");
                    if (a == null) return;

                    for (int i = 0; i < a.length(); i++) {
                        try {
                            JSONObject m = a.getJSONObject(i);
                            String name = m.optString("displayName", m.optString("username", "Nieznany"));
                            String id = m.optString("id", "brak ID");
                            String rank = m.optString("rank", m.optString("role", "Brak stopnia"));
                            String statusText = m.optString("status", "Aktywny");

                            LinearLayout card = new LinearLayout(this);
                            card.setOrientation(LinearLayout.VERTICAL);
                            card.setPadding(dp(16), dp(10), dp(16), dp(10));
                            card.setBackground(bg(Color.rgb(14, 42, 61), 18));

                            TextView nameView = label("👤  " + name, 18, true);
                            TextView rankView = label("🎖️  STOPIEŃ: " + rank, 16, true);
                            TextView idView = label("🆔  " + id + "    •    🟢 " + statusText, 13, false);
                            nameView.setPadding(0, 0, 0, dp(2));
                            rankView.setPadding(0, dp(2), 0, dp(2));
                            idView.setPadding(0, dp(2), 0, 0);
                            card.addView(nameView);
                            card.addView(rankView);
                            card.addView(idView);

                            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
                            p.setMargins(0, 0, 0, dp(10));
                            int index = Math.min(root.indexOfChild(status) + 1 + i, root.getChildCount());
                            root.addView(card, index, p);
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("❌ Nie udało się pobrać funkcjonariuszy.\n" + e.getMessage()));
            }
        });
    }

    private void broadcaster() {
        base("Komunikaty SW");
        root.addView(label("📢 WYŚLIJ KOMUNIKAT NA DISCORDA", 14, true));
        EditText title = input("Tytuł komunikatu");
        EditText content = input("Treść komunikatu");
        EditText channel = input("ID kanału Discord");
        EditText color = input("Kolor embeda, np. 3447003");
        content.setMinHeight(dp(120));
        Spinner mention = new Spinner(this);
        mention.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Bez wzmianki", "@everyone", "@here"}));
        root.addView(title); root.addView(content); root.addView(channel); root.addView(color); root.addView(mention);
        Button send = menu("📤  WYŚLIJ KOMUNIKAT");
        Button history = menu("📜  HISTORIA KOMUNIKATÓW");
        root.addView(send);
        TextView result = label("", 15, false);
        root.addView(result);
        root.addView(history);
        back();
        send.setOnClickListener(v -> send(title.getText().toString(), content.getText().toString(), channel.getText().toString(), color.getText().toString(), mention.getSelectedItemPosition(), result));
        history.setOnClickListener(v -> history());
    }

    private void send(String title, String content, String channel, String color, int mi, TextView result) {
        if (content.trim().isEmpty() || channel.trim().isEmpty()) {
            result.setText("❌ Wpisz treść i ID kanału.");
            return;
        }
        executor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection)new URL(API + "/api/broadcaster/send").openConnection();
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                JSONObject j = new JSONObject();
                j.put("channelId", channel.trim());
                j.put("title", title);
                j.put("content", content);
                j.put("color", color.trim().isEmpty() ? 3447003 : Integer.parseInt(color.trim()));
                j.put("mention", mi == 1 ? "everyone" : mi == 2 ? "here" : "");
                c.getOutputStream().write(j.toString().getBytes(StandardCharsets.UTF_8));
                int code = c.getResponseCode();
                runOnUiThread(() -> result.setText(code >= 200 && code < 300 ? "✅ Komunikat został wysłany na Discorda." : "❌ Backend zwrócił kod " + code));
            } catch (Exception e) {
                runOnUiThread(() -> result.setText("❌ Błąd wysyłania: " + e.getMessage()));
            }
        });
    }

    private void history() {
        base("Historia komunikatów");
        TextView s = label("⏳ Pobieram historię...", 16, false);
        root.addView(s);
        back();
        executor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection)new URL(API + "/api/broadcaster/history").openConnection();
                int code = c.getResponseCode();
                if (code != 200) throw new Exception("HTTP " + code);
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder z = new StringBuilder();
                String q;
                while ((q = r.readLine()) != null) z.append(q);
                JSONObject j = new JSONObject(z.toString());
                JSONArray a = j.optJSONArray("history");
                runOnUiThread(() -> {
                    s.setText("📜 Wysłane komunikaty: " + (a == null ? 0 : a.length()));
                    if (a != null) for (int i = 0; i < a.length(); i++) try {
                        JSONObject x = a.getJSONObject(i);
                        TextView item = label("📢  " + x.optString("title") + "\n👤  " + x.optString("authorName") + "\n🕒  " + x.optString("sentAt"), 15, false);
                        item.setBackground(bg(Color.rgb(14, 42, 61), 18));
                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
                        p.setMargins(0, 0, 0, dp(10));
                        root.addView(item, p);
                    } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                runOnUiThread(() -> s.setText("❌ Nie można pobrać historii.\n" + e.getMessage()));
            }
        });
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
