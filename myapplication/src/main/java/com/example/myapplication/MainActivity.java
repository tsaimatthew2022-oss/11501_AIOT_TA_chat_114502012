package com.example.myapplication;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private volatile Socket socket;
    private volatile PrintWriter out;
    private volatile boolean active;
    private ExecutorService sender;
    private TextView log;
    private ScrollView scroll;

    @Override protected void onCreate(Bundle state) { super.onCreate(state); showSetup(); }
    private LinearLayout page() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL);
        ViewCompat.setOnApplyWindowInsetsListener(p, (v, insets) -> {
            androidx.core.graphics.Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(24+b.left, 24+b.top, 24+b.right, 24+b.bottom); return insets;
        });
        setContentView(p); return p;
    }
    private EditText input(String hint) { EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true); return e; }
    private void showSetup() {
        log = null; scroll = null;
        LinearLayout p = page();
        EditText name = input("Client 名稱"); p.addView(name);
        EditText ip = input("Server IP"); p.addView(ip);
        EditText port = input("Port"); port.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); port.setText("8888"); p.addView(port);
        Button connect = new Button(this); connect.setText("連線"); p.addView(connect);
        connect.setOnClickListener(v -> {
            String n = name.getText().toString().trim(), host = ip.getText().toString().trim();
            int number;
            try { number = Integer.parseInt(port.getText().toString()); if (number < 1 || number > 65535) throw new NumberFormatException(); }
            catch (NumberFormatException e) { port.setError("請輸入有效的 Port"); return; }
            if (n.isEmpty()) { name.setError("請輸入名稱"); return; }
            if (host.isEmpty()) { ip.setError("請輸入 IP"); return; }
            connect.setEnabled(false); active = true;
            new Thread(() -> {
                Socket s = new Socket(); socket = s;
                try {
                    s.connect(new InetSocketAddress(host, number), 5000);
                    if (!active) { s.close(); return; }
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
                    out = writer; sender = Executors.newSingleThreadExecutor();
                    writer.println(n);
                    runOnUiThread(() -> { if (active) showChat(); });
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while (active && (line = in.readLine()) != null) append(line);
                    if (active) disconnected("Server 已關閉連線");
                } catch (IOException e) { if (active) disconnected("連線中斷或失敗：" + e.getMessage()); }
                finally { try { s.close(); } catch (IOException ignored) {} }
            }).start();
        });
    }
    private void showChat() {
        LinearLayout p = page();
        scroll = new ScrollView(this); log = new TextView(this); log.setTextSize(18);
        scroll.addView(log); p.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        EditText message = input("輸入訊息"); p.addView(message);
        Button send = new Button(this); send.setText("送出"); p.addView(send);
        send.setOnClickListener(v -> {
            String s = message.getText().toString().trim();
            if (!s.isEmpty() && sender != null) { message.setText(""); sender.execute(() -> { PrintWriter w = out; if (w != null) w.println(s); }); }
        });
        Button leave = new Button(this); leave.setText("離開聊天室"); p.addView(leave);
        leave.setOnClickListener(v -> { disconnect(); showSetup(); });
    }
    private void append(String s) {
        runOnUiThread(() -> { if (active && log != null) { log.append(s + "\n"); scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN)); } });
    }
    private void disconnected(String message) {
        runOnUiThread(() -> { if (active) { disconnect(); showSetup(); Toast.makeText(this, message, Toast.LENGTH_LONG).show(); } });
    }
    private void disconnect() {
        active = false;
        Socket s = socket; socket = null; out = null;
        if (s != null) try { s.close(); } catch (IOException ignored) {}
        if (sender != null) { sender.shutdownNow(); sender = null; }
    }
    @Override protected void onDestroy() { disconnect(); super.onDestroy(); }
}
