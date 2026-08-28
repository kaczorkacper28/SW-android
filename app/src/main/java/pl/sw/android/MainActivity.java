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
    private final int DARK2 = Color.rgb(16, 28, 37);
    private final int PANEL = Color.rgb(224, 227, 229);
    private final int GREEN = Color.rgb(0, 150, 75);
    private final int PINK = Color.rgb(210, 105, 140);
    private final int BLUE = Color.rgb(35, 80, 115);
    private final int WHITE = Color.WHITE;

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

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

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        home();
    }

    private void frame(String title) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(10), dp(10), dp(18));
        root.setBackgroundColor(DARK);
        sv.addView(root);
        setContentView(sv);

        TextView brand = text("🛡️  BLUEBIRD\nSŁUŻBA WIĘZIENNA", 19, WHITE, true);
        brand.setBackground(bg(DARK2, 12));
        brand.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(brand, new LinearLayout.LayoutParams(-1, dp(76)));

        TextView h = text(title, 22, WHITE, true);
        h.setPadding(dp(6), dp(18), dp(6), dp(10));
        root.addView(h);
    }

    private TextView bar(String s, int color) {
        TextView v = text(s, 13, WHITE, true);
        v.setBackground(bg(color, 8));
        v.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(44));
        p.setMargins(0, dp(3), 0, dp(3));
        root.addView(v, p);
        return v;
    }

    private Button tile(String title, String subtitle, int color) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setText(title + "\n" + subtitle);
        b.setTextSize(12);
        b.setTextColor(Color.rgb(25,25,25));
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(color, 12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(94), 1f);
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setLayoutParams(p);
        return b;
    }

    private void row(LinearLayout parent, View a, View b) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.addView(a);
        r.addView(b);
        parent.addView(r, new LinearLayout.LayoutParams(-1, dp(102)));
    }

    private void back() {
        Button b = new Button(this);
        b.setText("←  POWRÓT");
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(WHITE);
        b.setBackground(bg(BLUE, 10));
        b.setOnClickListener(v -> home());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(55));
        p.setMargins(0, dp(12), 0, 0);
        root.addView(b, p);
    }

    private void home() {
        frame("CENTRUM OPERACYJNE");
        bar("SŁUŻBA WIĘZIENNA • SYSTEM BLUEBIRD SW", GREEN);
        bar("● Status połączenia: ONLINE", Color.rgb(20, 170, 75));
        bar("Wybierz moduł operacyjny", PINK);

        Button officers = tile("FUNKCJONARIUSZE", "Pełna kadra SW", PANEL);
        Button commands = tile("ROZKAZY", "Kanały i wiadomości", PANEL);
        Button communication = tile("KOMUNIKATY", "Broadcaster SW", PANEL);
        Button profile = tile("MÓJ PROFIL", "Dane funkcjonariusza", PANEL);
        Button map = tile("MAPA", "Mapa jednostek", PANEL);
        Button reports = tile("RAPORTY", "Raporty służbowe", PINK);
        Button service = tile("SŁUŻBA", "Grafik i obsada", PANEL);
        Button documents = tile("DOKUMENTY", "Dokumentacja SW", PANEL);

        row(root, officers, commands);
        row(root, communication, profile);
        row(root, map, reports);
        row(root, service, documents);

        TextView version = text("BLUEBIRD SW • terminal Służby Więziennej", 10, Color.LTGRAY, false);
        version.setGravity(Gravity.CENTER);
        root.addView(version);

        officers.setOnClickListener(v -> officers());
        commands.setOnClickListener(v -> channels());
        communication.setOnClickListener(v -> broadcast());
        profile.setOnClickListener(v -> simple("MÓJ PROFIL", "👤 PROFIL FUNKCJONARIUSZA\n\nDane są pobierane z systemu SW."));
        map.setOnClickListener(v -> simple("MAPA", "🗺️ MAPA JEDNOSTEK\n\nModuł mapy jednostek SW."));
        reports.setOnClickListener(v -> simple("RAPORTY", "📄 RAPORTY SŁUŻBOWE\n\n• rozpoczęcie służby\n• zakończenie służby\n• konwój\n• interwencja\n• zdarzenie"));
        service.setOnClickListener(v -> simple("SŁUŻBA", "🕐 GRAFIK SŁUŻBY\n\nZmiany i aktualna obsada jednostki."));
        documents.setOnClickListener(v -> simple("DOKUMENTY", "📁 DOKUMENTY SW\n\n• rozkazy\n• zarządzenia\n• procedury\n• regulaminy"));
    }

    private void simple(String title, String content) {
        frame(title);
        bar("SŁUŻBA WIĘZIENNA", GREEN);
        TextView x = text(content, 15, WHITE, false);
        x.setBackground(bg(DARK2, 10));
        root.addView(x, new LinearLayout.LayoutParams(-1, -2));
        back();
    }

    private boolean isCitizen(JSONObject m) {
        JSONArray roleNames = m.optJSONArray("roleNames");
        if (roleNames != null) {
            for (int i = 0; i < roleNames.length(); i++) {
                String value = String.valueOf(roleNames.opt(i)).toLowerCase(Locale.ROOT).trim();
                if (value.equals("obywatel") || value.startsWith("obywatel ") || value.endsWith(" obywatel") || value.contains(" obywatel ")) return true;
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
        bar("WYSZUKIWANIE KADRY SŁUŻBY WIĘZIENNEJ", GREEN);
        TextView status = bar("Pobieranie wszystkich funkcjonariuszy...", Color.rgb(25,145,75));
        back();

        ex.execute(() -> {
            try {
                JSONObject response = getJson(API + "/api/officers");
                JSONArray source = getOfficerArray(response);
                if (source == null) throw new Exception("API nie zwróciło tablicy funkcjonariuszy.");
                ArrayList<JSONObject> list = new ArrayList<>();
                for (int i = 0; i < source.length(); i++) {
                    JSONObject m = source.optJSONObject(i);
                    if (m != null && !isCitizen(m)) list.add(m);
                }
                runOnUiThread(() -> {
                    status.setText("KADRA SW • " + list.size() + " FUNKCJONARIUSZY");
                    for (JSONObject m : list) addOfficerCard(m);
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("❌ BŁĄD POBIERANIA KADRY\n" + e.getMessage()));
            }
        });
    }

    private void addOfficerCard(JSONObject m) {
        String name = officerName(m);
        if (name.isEmpty()) name = "Nieznany funkcjonariusz";
        String rank = realRank(m);
        String id = first(m, "id", "discordId", "userId", "numer", "number");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setBackground(bg(DARK2, 10));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(5), 0, 0);
        card.addView(text("👮 " + name, 15, WHITE, true));
        card.addView(text("STOPIEŃ SW: " + rank, 13, Color.rgb(130, 220, 165), true));
        card.addView(text("ID: " + (id.isEmpty() ? "-" : id) + "  •  STATUS: Aktywny", 11, Color.LTGRAY, false));
        root.addView(card, Math.max(0, root.getChildCount() - 1), cp);
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(155,165,170));
        e.setTextColor(WHITE);
        e.setTextSize(15);
        e.setFocusable(true);
        e.setFocusableInTouchMode(true);
        e.setClickable(true);
        e.setLongClickable(true);
        e.setCursorVisible(true);
        e.setEnabled(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setBackground(bg(Color.rgb(28, 42, 52), 18));
        e.setPadding(dp(14), dp(10), dp(14), dp(10));
        e.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                e.requestFocus();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(e, InputMethodManager.SHOW_IMPLICIT);
            }
            return false;
        });
        return e;
    }

    private void channels() {
        frame("KANAŁY SW");
        bar("📋 WYBIERZ KANAŁ DISCORD", GREEN);
        TextView status = bar("Pobieranie kanałów...", Color.rgb(25,145,75));
        back();

        ex.execute(() -> {
            try {
                JSONArray channels = getJsonArray(API + "/api/discord/channels", "channels");
                ArrayList<JSONObject> list = new ArrayList<>();
                for (int i = 0; i < channels.length(); i++) {
                    JSONObject c = channels.optJSONObject(i);
                    if (c != null) list.add(c);
                }
                runOnUiThread(() -> {
                    status.setText("DOSTĘPNE KANAŁY • " + list.size());
                    int insertAt = Math.max(0, root.getChildCount() - 1);
                    for (JSONObject c : list) addChannelButton(c, insertAt++);
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("❌ NIE UDAŁO SIĘ POBRAĆ KANAŁÓW\n" + e.getMessage()));
            }
        });
    }

    private void addChannelButton(JSONObject c, int index) {
        String id = c.optString("id", "");
        String name = c.optString("name", "kanał");
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText("#  " + name);
        b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        b.setTextSize(16);
        b.setTextColor(WHITE);
        b.setBackground(bg(DARK2, 10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(58));
        p.setMargins(0, dp(4), 0, dp(4));
        root.addView(b, Math.min(index, root.getChildCount() - 1), p);
        b.setOnClickListener(v -> chat(id, name));
    }

    private void chat(String channelId, String channelName) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.rgb(0,0,0));
        page.setPadding(0, 0, 0, 0);
        setContentView(page);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(8), dp(8), dp(8));
        header.setBackgroundColor(Color.rgb(10,10,12));

        Button backBtn = new Button(this);
        backBtn.setText("←");
        backBtn.setTextColor(WHITE);
        backBtn.setTextSize(24);
        backBtn.setAllCaps(false);
        backBtn.setBackgroundColor(Color.TRANSPARENT);
        header.addView(backBtn, new LinearLayout.LayoutParams(dp(54), dp(58)));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        TextView channelTitle = text("#  " + channelName, 18, WHITE, true);
        TextView channelSub = text("Służba Więzienna • BLUEBIRD SW", 11, Color.LTGRAY, false);
        headerText.addView(channelTitle);
        headerText.addView(channelSub);
        header.addView(headerText, new LinearLayout.LayoutParams(0, -2, 1f));
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(74)));

        ScrollView messagesScroll = new ScrollView(this);
        messagesScroll.setFillViewport(true);
        LinearLayout messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(dp(10), dp(8), dp(10), dp(8));
        messagesScroll.addView(messages);
        page.addView(messagesScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(8), dp(8), dp(8), dp(8));
        composer.setBackgroundColor(Color.rgb(15,15,17));

        EditText messageInput = input("Napisz wiadomość…");
        messageInput.setSingleLine(false);
        messageInput.setMaxLines(5);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, dp(56), 1f);
        ip.setMargins(0, 0, dp(7), 0);
        composer.addView(messageInput, ip);

        Button send = new Button(this);
        send.setAllCaps(false);
        send.setText("➤");
        send.setTextSize(22);
        send.setTextColor(WHITE);
        send.setBackground(bg(BLUE, 18));
        composer.addView(send, new LinearLayout.LayoutParams(dp(58), dp(56)));
        page.addView(composer, new LinearLayout.LayoutParams(-1, dp(74)));

        backBtn.setOnClickListener(v -> channels());

        Runnable load = () -> loadMessages(channelId, messages, messagesScroll);
        load.run();

        send.setOnClickListener(v -> {
            String content = messageInput.getText().toString().trim();
            if (content.isEmpty()) {
                messageInput.requestFocus();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT);
                return;
            }

            send.setEnabled(false);
            ex.execute(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("channelId", channelId);
                    body.put("content", content);
                    postJson(API + "/api/discord/messages", body);
                    runOnUiThread(() -> {
                        messageInput.setText("");
                        send.setEnabled(true);
                        loadMessages(channelId, messages, messagesScroll);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        send.setEnabled(true);
                        Toast.makeText(this, "Nie udało się wysłać: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        messageInput.setOnEditorActionListener((v, actionId, event) -> false);
        messageInput.requestFocus();
    }

    private void loadMessages(String channelId, LinearLayout messages, ScrollView scroll) {
        ex.execute(() -> {
            try {
                JSONArray a = getJsonArray(API + "/api/discord/messages?channelId=" + URLEncoder.encode(channelId, "UTF-8") + "&limit=50", "messages");
                runOnUiThread(() -> {
                    messages.removeAllViews();
                    ArrayList<JSONObject> ordered = new ArrayList<>();
                    for (int i = a.length() - 1; i >= 0; i--) {
                        JSONObject m = a.optJSONObject(i);
                        if (m != null) ordered.add(m);
                    }
                    for (JSONObject m : ordered) addMessage(messages, m);
                    messages.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    messages.removeAllViews();
                    TextView err = text("❌ Nie udało się pobrać wiadomości.\n\n" + e.getMessage(), 14, Color.LTGRAY, false);
                    err.setBackground(bg(Color.rgb(35,20,25), 10));
                    messages.addView(err);
                });
            }
        });
    }

    private void addMessage(LinearLayout parent, JSONObject m) {
        JSONObject author = m.optJSONObject("author");
        String authorName = author == null ? "Użytkownik" : first(author, "global_name", "username");
        String content = m.optString("content", "");
        if (content.isEmpty()) content = "[wiadomość embed / bez tekstu]";

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(7), dp(10), dp(7));
        card.setBackground(bg(Color.rgb(20,20,23), 10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(3), 0, dp(3));

        TextView a = text(authorName, 13, Color.rgb(110, 175, 230), true);
        TextView c = text(content, 15, WHITE, false);
        card.addView(a);
        card.addView(c);
        parent.addView(card, p);
    }

    private void broadcast() {
        frame("KOMUNIKATY");
        bar("BROADCASTER SŁUŻBY WIĘZIENNEJ", GREEN);
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
        root.addView(send, new LinearLayout.LayoutParams(-1, dp(78)));
        TextView result = text("", 13, WHITE, false);
        root.addView(result);
        back();

        send.setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            String msg = message.getText().toString().trim();
            String ch = channel.getText().toString().trim();
            if (msg.isEmpty() || ch.isEmpty()) {
                result.setText("❌ Uzupełnij treść i ID kanału.");
                return;
            }
            send.setEnabled(false);
            ex.execute(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("channelId", ch);
                    body.put("title", t);
                    body.put("content", msg);
                    body.put("color", 3447003);
                    postJson(API + "/api/broadcaster/send", body);
                    runOnUiThread(() -> {
                        result.setText("✅ Komunikat wysłany.");
                        send.setEnabled(true);
                        title.setText("");
                        message.setText("");
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        result.setText("❌ " + e.getMessage());
                        send.setEnabled(true);
                    });
                }
            });
        });
    }

    private JSONObject getJson(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000);
        c.setReadTimeout(25000);
        c.setRequestProperty("Accept", "application/json");
        return readJsonResponse(c);
    }

    private JSONArray getJsonArray(String url, String key) throws Exception {
        JSONObject o = getJson(url);
        JSONArray a = o.optJSONArray(key);
        if (a != null) return a;
        if (o.length() == 1) {
            Iterator<String> it = o.keys();
            if (it.hasNext()) {
                Object v = o.opt(it.next());
                if (v instanceof JSONArray) return (JSONArray)v;
            }
        }
        throw new Exception("API nie zwróciło tablicy " + key + ".");
    }

    private JSONObject postJson(String url, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(15000);
        c.setReadTimeout(25000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        c.setRequestProperty("Accept", "application/json");
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream out = c.getOutputStream();
        out.write(bytes);
        out.flush();
        out.close();
        return readJsonResponse(c);
    }

    private JSONObject readJsonResponse(HttpURLConnection c) throws Exception {
        int code = c.getResponseCode();
        InputStream stream = code < 400 ? c.getInputStream() : c.getErrorStream();
        if (stream == null) throw new Exception("HTTP " + code);
        BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder s = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) s.append(line);
        r.close();
        if (code < 200 || code >= 300) {
            try {
                JSONObject e = new JSONObject(s.toString());
                throw new Exception(e.optString("message", "HTTP " + code));
            } catch (JSONException ignored) {
                throw new Exception("HTTP " + code + ": " + s);
            }
        }
        return new JSONObject(s.toString());
    }

    @Override
    protected void onDestroy() {
        ex.shutdownNow();
        super.onDestroy();
    }
}
