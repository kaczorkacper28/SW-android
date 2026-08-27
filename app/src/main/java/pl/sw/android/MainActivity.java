package pl.sw.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private TextView text(String value, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.BLACK);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(dp(8), dp(8), dp(8), dp(8));
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(16);
        b.setAllCaps(false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(58));
        p.setMargins(0, 0, 0, dp(10));
        b.setLayoutParams(p);
        return b;
    }

    private LinearLayout page(String title) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(30), dp(20), dp(20));
        layout.setBackgroundColor(Color.WHITE);
        layout.addView(text("SŁUŻBA WIĘZIENNA", 24, true));
        layout.addView(text(title, 21, true));
        return layout;
    }

    private void setPage(LinearLayout layout) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private void backButton(LinearLayout layout) {
        Button back = button("← Powrót");
        back.setOnClickListener(v -> showHome());
        layout.addView(back);
    }

    private void showHome() {
        LinearLayout layout = page("Panel główny");
        layout.addView(text("Witaj w aplikacji Służby Więziennej RP", 17, false));

        Button officers = button("👮 Funkcjonariusze");
        Button schedule = button("📅 Grafik służby");
        Button reports = button("📋 Raporty służbowe");
        Button documents = button("📄 Dokumenty");
        Button news = button("📢 Komunikaty");
        Button profile = button("👤 Mój profil");

        layout.addView(officers);
        layout.addView(schedule);
        layout.addView(reports);
        layout.addView(documents);
        layout.addView(news);
        layout.addView(profile);

        officers.setOnClickListener(v -> showOfficers());
        schedule.setOnClickListener(v -> showSchedule());
        reports.setOnClickListener(v -> showReports());
        documents.setOnClickListener(v -> showDocuments());
        news.setOnClickListener(v -> showNews());
        profile.setOnClickListener(v -> showProfile());

        setPage(layout);
    }

    private void showOfficers() {
        LinearLayout layout = page("Funkcjonariusze");
        layout.addView(text("Lista funkcjonariuszy", 18, true));
        layout.addView(text("SW-001\nJan Kowalski\nStopień: Funkcjonariusz\nStatus: 🟢 Aktywny", 16, false));
        layout.addView(text("SW-002\nAdam Nowak\nStopień: Starszy Funkcjonariusz\nStatus: 🟢 Aktywny", 16, false));
        layout.addView(text("SW-003\nPiotr Wiśniewski\nStopień: Dowódca Zmiany\nStatus: 🟢 Aktywny", 16, false));
        backButton(layout);
        setPage(layout);
    }

    private void showSchedule() {
        LinearLayout layout = page("Grafik służby");
        layout.addView(text("📅 DZISIAJ", 19, true));
        layout.addView(text("08:00 – 16:00\nSłużba dzienna", 16, false));
        layout.addView(text("16:00 – 00:00\nSłużba popołudniowa", 16, false));
        layout.addView(text("00:00 – 08:00\nSłużba nocna", 16, false));
        backButton(layout);
        setPage(layout);
    }

    private void showReports() {
        LinearLayout layout = page("Raporty służbowe");
        layout.addView(text("Ostatnie raporty", 18, true));
        layout.addView(text("#001 — Raport służbowy\nFunkcjonariusz: Jan Kowalski\nStatus: Przyjęty", 16, false));
        layout.addView(text("#002 — Raport służbowy\nFunkcjonariusz: Adam Nowak\nStatus: W trakcie rozpatrywania", 16, false));
        backButton(layout);
        setPage(layout);
    }

    private void showDocuments() {
        LinearLayout layout = page("Dokumenty");
        layout.addView(text("📄 Dokumenty służbowe", 18, true));
        layout.addView(text("• Regulamin Służby Więziennej\n• Procedury służbowe\n• Wzory raportów\n• Dokumenty kadrowe", 16, false));
        backButton(layout);
        setPage(layout);
    }

    private void showNews() {
        LinearLayout layout = page("Komunikaty");
        layout.addView(text("📢 Najnowsze informacje", 18, true));
        layout.addView(text("Brak nowych komunikatów.\n\nWszystkie ważne informacje będą wyświetlane w tym miejscu.", 16, false));
        backButton(layout);
        setPage(layout);
    }

    private void showProfile() {
        LinearLayout layout = page("Mój profil");
        layout.addView(text("👤 Profil funkcjonariusza", 18, true));
        layout.addView(text("Numer SW: SW-001\nImię i nazwisko: Jan Kowalski\nStopień: Funkcjonariusz\nStatus: 🟢 Aktywny\nPunkty: +12\nNagany: 0\nPochwały: 2", 16, false));
        backButton(layout);
        setPage(layout);
    }
}
