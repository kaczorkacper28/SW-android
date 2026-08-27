package pl.sw.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        main.setPadding(dp(24), dp(40), dp(24), dp(24));
        main.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("SŁUŻBA WIĘZIENNA");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("Aplikacja SW • RP");
        subtitle.setTextSize(18);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(8), 0, dp(30));

        main.addView(title);
        main.addView(subtitle);

        Button officers = createButton("Funkcjonariusze");
        Button schedule = createButton("Grafik służby");
        Button documents = createButton("Dokumenty");
        Button information = createButton("Informacje");

        main.addView(officers);
        main.addView(schedule);
        main.addView(documents);
        main.addView(information);

        officers.setOnClickListener(v -> toast("Moduł funkcjonariuszy — w przygotowaniu"));
        schedule.setOnClickListener(v -> toast("Grafik służby — w przygotowaniu"));
        documents.setOnClickListener(v -> toast("Dokumenty — w przygotowaniu"));
        information.setOnClickListener(v -> toast("Informacje SW — w przygotowaniu"));

        setContentView(main);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Button createButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        params.setMargins(0, 0, 0, dp(12));
        button.setLayoutParams(params);
        return button;
    }
}
