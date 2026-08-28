package pl.sw.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.json.*;

public class MainActivity extends Activity {
    private static final String API = "https://sw-android.onrender.com";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout root;

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView label(String s, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setPadding(dp(16), dp(12), dp(16), dp(12));
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
        b.setBackground(bg(Color.rgb(22, 52, 74), 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(62));
        p.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(p);
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.LTGRAY);
        e.setTextColor(Color.WHITE);
        e.setPadding(dp(16), dp(8), dp(16), dp(8));
        e.setBackground(bg(Color.rgb(22, 52, 74), 14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(62));
        p.setMargins(0, dp(6), 0, dp(6));
        e.setLayoutParams(p);
        return e;
    }

    private void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(8, 24, 36));

        TextView h = label("🛡️  SŁUŻBA WIĘZIENNA", 24, true);
        root.addView(h, new LinearLayout.LayoutParams(-1, -2));

        TextView t = label(title, 21, true);
        root.addView(t, new LinearLayout.LayoutParams(-1, -2));

        ScrollView s = new ScrollView(this);
        s.addView(root);
        setContentView(s);
    }

    private void back() {
        Button b = menu("←  Powrót");
        b.setOnClickListener(v -> home());
        root.addView(b, new LinearLayout.LayoutParams(-1, dp(62)));
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        home();
    }

    private void home() {
        base("Panel główny");
        root.addView(label("Panel Służby Więziennej RP", 16, false));

        Button o = menu("👮  Funkcjonariusze");
        Button bc = menu("📢  Broadcaster SW");
        Button sc = menu("📅  Grafik służby");
        Button r = menu("📋  Raporty służbowe");
        Button d = menu("📄  Dokumenty");
        Button p = menu("👤  Mój profil");

        root.addView(o);
        root.addView(bc);
        root.addView(sc);
        root.addView(r);
        root.addView(d);
        root.addView(p);

        o.setOnClickListener(v -> officers());
        bc.setOnClickListener(v -> broadcaster());
        sc.setOnClickListener(v -> simple("Grafik służby", "📅 DZISIAJ\n\n08:00–16:00  Służba dzienna\n16:00–00:00  Służba popołudniowa\n00:00–08:00  Służba nocna"));
        r.setOnClickListener(v -> simple("Raporty", "📋 Moduł raportów służbowych"));
        d.setOnClickListener(v -> simple("Dokumenty", "📄 Regulamin\n📄 Procedury\n📄 Wzory dokumentów"));
        p.setOnClickListener(v -> simple("Mój profil", "👤 Dane użytkownika będą pobierane z Discorda."));
    }

    private void simple(String title, String body) {
        base(title);
        root.addView(label(body, 17, false));
        back();
    }

    private void officers() {
        base("Funkcjonariusze");

        TextView status = label("⏳ Pobieram funkcjonariuszy z Discorda...", 16, false);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));
        back();

        executor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(API + "/api/officers").openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(12000);
                c.setReadTimeout(20000);

                int code = c.getResponseCode();
                InputStream stream = code < 400 ? c.getInputStream() : c.getErrorStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                StringBuilder s = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) s.append(line);

                JSONObject json = new JSONObject(s.toString());
                if (code != 200) throw new Exception(json.optString("error", "API error"));

                JSONArray a = json.optJSONArray("officers");

                runOnUiThread(() -> {
                    status.setText("👮  Funkcjonariusze: " + (a == null ? 0 : a.length()));
                    if (a == null) return;

                    for (int i = 0; i < a.length(); i++) {
                        try {
                            JSONObject m = a.getJSONObject(i);
                            String name = m.optString("displayName", m.optString("username", "Nieznany"));
                            String id = m.optString("id", "brak ID");
                            String rank = m.optString("rank", m.optString("role", "Brak stopnia"));
                            String statusText = m.optString("status", "Aktywny");

                            TextView card = label(
                                    "👤  " + name +
                                    "\n🎖️  " + rank +
                                    "\n🆔  " + id +
                                    "\n🟢  " + statusText,
                                    15,
                                    true
                            );
                            card.setBackground(bg(Color.rgb(16, 39, 57), 18));

                            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(120));
                            p.setMargins(0, 0, 0, dp(10));

                            // POPRAWIONE: View + index + LayoutParams.
                            // Wcześniej było View + LayoutParams + index, co powodowało błąd kompilacji.
                            int index = Math.min(root.indexOfChild(status) + 1 + i, root.getChildCount());
                            root.addView(card, index, p);
                        } catch (Exception ignored) {
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("❌ Nie udało się pobrać funkcjonariuszy.\n" + e.getMessage()));
            }
        });
    }

    private void broadcaster() {
        base("📢 Broadcaster SW");
        root.addView(label("Wyślij komunikat bezpośrednio na Discorda.", 15, false));

        EditText title = input("Tytuł komunikatu");
        EditText content = input("Treść komunikatu");
        EditText channel = input("ID kanału Discord");
        EditText color = input("Kolor embeda, np. 3447003");
        content.setMinHeight(dp(120));

        Spinner mention = new Spinner(this);
        mention.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Bez wzmianki", "@everyone", "@here"}));

        root.addView(title);
        root.addView(content);
        root.addView(channel);
        root.addView(color);
        root.addView(mention);

        Button send = menu("📤  WYŚLIJ KOMUNIKAT");
        root.addView(send);

        TextView result = label("", 15, false);
        root.addView(result);

        Button history = menu("📜  Historia komunikatów");
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
                HttpURLConnection c = (HttpURLConnection) new URL(API + "/api/broadcaster/send").openConnection();
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

                runOnUiThread(() -> result.setText(code >= 200 && code < 300 ? "✅ Wysłano na Discorda." : "❌ Backend zwrócił kod " + code));
            } catch (Exception e) {
                runOnUiThread(() -> result.setText("❌ Błąd wysyłania: " + e.getMessage()));
            }
        });
    }

    private void history() {
        base("📜 Historia Broadcastera");
        TextView s = label("⏳ Pobieram historię...", 16, false);
        root.addView(s);
        back();

        executor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(API + "/api/broadcaster/history").openConnection();
                int code = c.getResponseCode();
                if (code != 200) throw new Exception("HTTP " + code);

                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                StringBuilder z = new StringBuilder();
                String q;
                while ((q = r.readLine()) != null) z.append(q);

                JSONObject j = new JSONObject(z.toString());
                JSONArray a = j.optJSONArray("history");

                runOnUiThread(() -> {
                    s.setText("📜 Wysłane: " + (a == null ? 0 : a.length()));
                    if (a != null) {
                        for (int i = 0; i < a.length(); i++) {
                            try {
                                JSONObject x = a.getJSONObject(i);
                                TextView item = label(
                                        "📢 " + x.optString("title") +
                                        "\n👤 " + x.optString("authorName") +
                                        "\n🕒 " + x.optString("sentAt"),
                                        15,
                                        false
                                );
                                item.setBackground(bg(Color.rgb(16, 39, 57), 18));
                                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
                                p.setMargins(0, 0, 0, dp(10));
                                root.addView(item, p);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> s.setText("❌ Nie można pobrać historii.\n" + e.getMessage()));
            }
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
