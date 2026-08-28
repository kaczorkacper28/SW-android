package pl.sw.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
    private static final String API = "https://sw-android.onrender.com";
    private final ExecutorService ex = Executors.newSingleThreadExecutor();
    private LinearLayout root;
    private final int DARK = Color.rgb(8, 18, 25);
    private final int PANEL = Color.rgb(224, 227, 229);
    private final int GREEN = Color.rgb(0, 150, 75);
    private final int PINK = Color.rgb(210, 105, 140);
    private final int BLUE = Color.rgb(35, 80, 115);

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setPadding(dp(10), dp(8), dp(10), dp(8));
        return t;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        home();
    }

    private void frame(String title) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(10), dp(10), dp(18));
        root.setBackgroundColor(Color.WHITE);
        sv.addView(root);
        setContentView(sv);

        TextView brand = text("BLUEBIRD\nSŁUŻBA WIĘZIENNA", 18, Color.rgb(35,35,35), true);
        brand.setBackground(bg(Color.rgb(242,242,242), 2));
        brand.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(brand, new LinearLayout.LayoutParams(-1, dp(66)));

        TextView h = text(title, 20, Color.rgb(30,30,30), true);
        h.setPadding(dp(5), dp(16), dp(5), dp(8));
        root.addView(h);
    }

    private TextView bar(String s, int color) {
        TextView v = text(s, 13, Color.WHITE, true);
        v.setBackground(bg(color, 2));
        v.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(44));
        p.setMargins(0, dp(2), 0, dp(2));
        v.setLayoutParams(p);
        return v;
    }

    private Button tile(String title, String subtitle, int color) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setText(title + "\n" + subtitle);
        b.setTextSize(12);
        b.setTextColor(Color.rgb(35,35,35));
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(color, 2));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(92), 1f);
        p.setMargins(dp(3), dp(3), dp(3), dp(3));
        b.setLayoutParams(p);
        return b;
    }

    private void row(LinearLayout parent, View a, View b) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.addView(a);
        r.addView(b);
        parent.addView(r, new LinearLayout.LayoutParams(-1, dp(98)));
    }

    private void back() {
        Button b = new Button(this);
        b.setText("←  POWRÓT");
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(Color.WHITE);
        b.setBackground(bg(BLUE, 3));
        b.setOnClickListener(v -> home());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(55));
        p.setMargins(0, dp(12), 0, 0);
        root.addView(b, p);
    }

    private void home() {
        frame("CENTRUM OPERACYJNE");
        root.addView(bar("SŁUŻBA WIĘZIENNA • SYSTEM BLUEBIRD SW", GREEN));
        root.addView(bar("Status połączenia: ONLINE", Color.rgb(20, 190, 80)));
        root.addView(bar("Wybierz moduł operacyjny", PINK));

        Button officers = tile("FUNKCJONARIUSZE", "Pełna kadra SW", PANEL);
        Button commands = tile("ROZKAZY", "Rozkazy i polecenia", PANEL);
        Button communication = tile("KOMUNIKATY", "Komunikaty dowództwa", PANEL);
        Button profile = tile("MÓJ PROFIL", "Dane funkcjonariusza", PANEL);
        Button map = tile("MAPA", "Mapa jednostek", PANEL);
        Button reports = tile("RAPORTY", "Raporty służbowe", PINK);
        Button service = tile("SŁUŻBA", "Grafik i obsada", PANEL);
        Button documents = tile("DOKUMENTY", "Dokumentacja SW", PANEL);

        row(root, officers, commands);
        row(root, communication, profile);
        row(root, map, reports);
        row(root, service, documents);

        TextView version = text("BLUEBIRD SW • terminal Służby Więziennej", 10, Color.GRAY, false);
        version.setGravity(Gravity.CENTER);
        root.addView(version);

        officers.setOnClickListener(v -> officers());
        commands.setOnClickListener(v -> simple("ROZKAZY", "📋 ROZKAZY SŁUŻBY WIĘZIENNEJ\n\nTutaj wyświetlane są rozkazy i polecenia przełożonych."));
        communication.setOnClickListener(v -> broadcast());
        profile.setOnClickListener(v -> simple("MÓJ PROFIL", "👤 PROFIL FUNKCJONARIUSZA\n\nDane są pobierane z systemu SW."));
        map.setOnClickListener(v -> simple("MAPA", "🗺️ MAPA JEDNOSTEK\n\nModuł mapy jednostek SW."));
        reports.setOnClickListener(v -> simple("RAPORTY", "📄 RAPORTY SŁUŻBOWE\n\n• rozpoczęcie służby\n• zakończenie służby\n• konwój\n• interwencja\n• zdarzenie"));
        service.setOnClickListener(v -> simple("SŁUŻBA", "🕐 GRAFIK SŁUŻBY\n\nZmiany i aktualna obsada jednostki."));
        documents.setOnClickListener(v -> simple("DOKUMENTY", "📁 DOKUMENTY SW\n\n• rozkazy\n• zarządzenia\n• procedury\n• regulaminy"));
    }

    private void simple(String title, String content) {
        frame(title);
        root.addView(bar("SŁUŻBA WIĘZIENNA", GREEN));
        TextView x = text(content, 15, Color.rgb(30,30,30), false);
        x.setBackground(bg(PANEL, 3));
        root.addView(x, new LinearLayout.LayoutParams(-1, -2));
        back();
    }

    // Obywatel jest wykluczany wyłącznie na podstawie nazwy roli Discord.
    // Nie sprawdzamy nicku, nazwy użytkownika, statusu ani innych pól.
    private boolean isCitizen(JSONObject m) {
        JSONArray roleNames = m.optJSONArray("roleNames");
        if (roleNames != null) {
            for (int i = 0; i < roleNames.length(); i++) {
                String value = String.valueOf(roleNames.opt(i)).toLowerCase(Locale.ROOT).trim();
                if (value.equals("obywatel") || value.startsWith("obywatel ") || value.endsWith(" obywatel") || value.contains(" obywatel ")) {
                    return true;
                }
            }
        }

        JSONArray roles = m.optJSONArray("roles");
        if (roles != null) {
            for (int i = 0; i < roles.length(); i++) {
                Object r = roles.opt(i);
                if (r instanceof JSONObject) {
                    JSONObject ro = (JSONObject) r;
                    String name = first(ro, "name", "roleName").toLowerCase(Locale.ROOT).trim();
                    if (name.equals("obywatel") || name.startsWith("obywatel ") || name.endsWith(" obywatel") || name.contains(" obywatel ")) return true;
                }
            }
        }
        return false;
    }

    private String first(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = o.optString(k, "").trim();
            if (!v.isEmpty() && !v.equalsIgnoreCase("null")) return v;
        }
        return "";
    }

    // Backend zwraca rank/rankRole jako rzeczywistą rolę Discord.
    private String realRank(JSONObject m) {
        String r = first(m, "rankRole", "rank", "stopien", "position", "stanowisko", "grade", "ranga", "roleName", "role");
        if (!r.isEmpty() && !r.equalsIgnoreCase("obywatel")) return r;

        JSONArray roleNames = m.optJSONArray("roleNames");
        if (roleNames != null) {
            for (int i = 0; i < roleNames.length(); i++) {
                String n = String.valueOf(roleNames.opt(i)).trim();
                String low = n.toLowerCase(Locale.ROOT);
                if (!n.isEmpty() && !low.equals("obywatel") && !low.contains("bot") && !low.contains("admin")) return n;
            }
        }
        return "Brak przypisanej rangi SW";
    }

    private String officerName(JSONObject m) {
        return first(m, "displayName", "globalName", "username", "name", "nick");
    }

    private JSONArray getOfficerArray(JSONObject response) {
        JSONArray a = response.optJSONArray("officers");
        if (a != null) return a;
        a = response.optJSONArray("data");
        if (a != null) return a;
        a = response.optJSONArray("members");
        if (a != null) return a;
        return response.optJSONArray("results");
    }

    private void officers() {
        frame("FUNKCJONARIUSZE SW");
        root.addView(bar("WYSZUKIWANIE KADRY SŁUŻBY WIĘZIENNEJ", GREEN));
        TextView status = bar("Pobieranie wszystkich funkcjonariuszy...", Color.rgb(25, 145, 75));
        root.addView(status);
        back();

        ex.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection)new URL(API + "/api/officers").openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(15000);
                c.setReadTimeout(25000);
                c.setRequestProperty("Accept", "application/json");
                int code = c.getResponseCode();
                InputStream stream = code < 400 ? c.getInputStream() : c.getErrorStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                StringBuilder z = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) z.append(line);
                r.close();
                if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + z);
                JSONObject response = new JSONObject(z.toString());
                JSONArray source = getOfficerArray(response);
                if (source == null) throw new Exception("API nie zwróciło tablicy funkcjonariuszy.");
                ArrayList<JSONObject> officers = new ArrayList<>();
                for (int i = 0; i < source.length(); i++) {
                    JSONObject m = source.optJSONObject(i);
                    if (m != null && !isCitizen(m)) officers.add(m);
                }
                runOnUiThread(() -> {
                    status.setText("KADRA SW • " + officers.size() + " FUNKCJONARIUSZY");
                    for (JSONObject m : officers) addOfficerCard(m);
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("BŁĄD POBIERANIA KADRY\n" + e.getMessage()));
            }
        });
    }

    private void addOfficerCard(JSONObject m) {
        String name = officerName(m);
        if (name.isEmpty()) name = "Nieznany funkcjonariusz";
        String rank = realRank(m);
        String id = first(m, "id", "discordId", "userId", "numer", "number");
        String status = first(m, "status", "dutyStatus", "state");
        if (status.isEmpty()) status = "Aktywny";
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.setBackground(bg(PANEL, 2));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(5), 0, 0);
        TextView n = text("👮 " + name, 15, Color.rgb(20,20,20), true);
        TextView q = text("RANGA / STOPIEŃ SW: " + rank, 13, Color.rgb(20,80,45), true);
        TextView x = text("ID: " + (id.isEmpty() ? "-" : id) + "    •    STATUS: " + status, 11, Color.DKGRAY, false);
        card.addView(n);
        card.addView(q);
        card.addView(x);
        root.addView(card, Math.max(0, root.getChildCount() - 1), cp);
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.GRAY);
        e.setTextColor(Color.BLACK);
        e.setTextSize(14);
        e.setSingleLine(false);
        e.setFocusable(true);
        e.setFocusableInTouchMode(true);
        e.setClickable(true);
        e.setLongClickable(true);
        e.setCursorVisible(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setBackground(bg(Color.rgb(245,245,245), 6));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setOnClickListener(v -> {
            e.requestFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(e, InputMethodManager.SHOW_IMPLICIT);
        });
        return e;
    }

    private void broadcast() {
        frame("KOMUNIKATY");
        root.addView(bar("BROADCASTER SŁUŻBY WIĘZIENNEJ", GREEN));
        EditText title = input("Tytuł komunikatu");
        EditText message = input("Treść komunikatu");
        EditText channel = input("ID kanału Discord");
        title.setSingleLine(true);
        channel.setSingleLine(true);
        message.setMinLines(4);
        message.setGravity(Gravity.TOP | Gravity.START);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(58)));
        root.addView(message, new LinearLayout.LayoutParams(-1, dp(120)));
        root.addView(channel, new LinearLayout.LayoutParams(-1, dp(58)));
        Button send = tile("WYŚLIJ KOMUNIKAT", "Broadcaster SW", PINK);
        root.addView(send, new LinearLayout.LayoutParams(-1, dp(85)));
        TextView out = text("", 13, Color.DKGRAY, true);
        root.addView(out);
        back();
        send.setOnClickListener(v -> {
            if (message.getText().toString().trim().isEmpty() || channel.getText().toString().trim().isEmpty()) {
                out.setText("Uzupełnij treść i ID kanału.");
                return;
            }
            ex.execute(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("channelId", channel.getText().toString().trim());
                    body.put("title", title.getText().toString().trim());
                    body.put("content", message.getText().toString().trim());
                    body.put("color", "#2380B5");
                    String response = postJson(API + "/api/broadcaster/send", body.toString());
                    runOnUiThread(() -> out.setText("✅ Komunikat wysłany.\n" + response));
                } catch (Exception e) {
                    runOnUiThread(() -> out.setText("❌ Nie udało się wysłać.\n" + e.getMessage()));
                }
            });
        });
    }

    private String postJson(String address, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(address).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(15000);
        c.setReadTimeout(25000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        try (OutputStream out = c.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        InputStream stream = code < 400 ? c.getInputStream() : c.getErrorStream();
        BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder z = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) z.append(line);
        r.close();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + z);
        return z.toString();
    }
}
