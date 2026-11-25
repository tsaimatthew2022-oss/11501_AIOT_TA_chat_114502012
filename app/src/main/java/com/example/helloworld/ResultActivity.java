package com.example.helloworld;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultActivity extends AppCompatActivity {
    public static final String EXTRA_TEXT = "com.example.helloworld.TEXT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        TextView textView = new TextView(this);
        textView.setText(getIntent().getStringExtra(EXTRA_TEXT));
        textView.setTextSize(24);
        textView.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.addView(textView);

        android.widget.Button changeColor = new android.widget.Button(this);
        changeColor.setText("Change color");
        changeColor.setOnClickListener(v -> setRandomTextColor(textView));
        layout.addView(changeColor);

        android.widget.Button back = new android.widget.Button(this);
        back.setText("Back");
        back.setOnClickListener(v -> finish());
        layout.addView(back);

        setContentView(layout);
        ViewCompat.setOnApplyWindowInsetsListener(layout, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void setRandomTextColor(TextView textView) {
        java.util.Random random = new java.util.Random();
        textView.setTextColor(android.graphics.Color.rgb(
                random.nextInt(256), random.nextInt(256), random.nextInt(256)));
    }
}