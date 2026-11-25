package com.example.helloworld;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends AppCompatActivity {
    private final CopyOnWriteArrayList<Peer> peers = new CopyOnWriteArrayList<>();
    private volatile ServerSocket server;
    private volatile boolean running;
    private TextView log;
    private ScrollView scroll;
    private String name;
    private static final int PORT = 8888;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showSetup();
    }
    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 24, 24, 24);
        ViewCompat.setOnApplyWindowInsetsListener(page, (v, insets) -> {
            androidx.core.graphics.Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(24 + b.left, 24 + b.top, 24 + b.right, 24 + b.bottom);
            return insets;
        });
        setContentView(page);
        return page;
    }
    private EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true); return e;
    }
    private void showSetup() {
        LinearLayout p = page();
        EditText n = input("Server 名稱"); p.addView(n);
        Button start = new Button(this); start.setText("啟動 Server（Port 8888）"); p.addView(start);
        start.setOnClickListener(v -> {
            String value = n.getText().toString().trim();
            if (value.isEmpty()) { n.setError("請輸入名稱"); return; }
            name = value;
            start.setEnabled(false);
            new Thread(() -> {
                try {
                    ServerSocket socket = new ServerSocket(PORT);
                    server = socket; running = true;
                    runOnUiThread(() -> { showChat(); append("Server " + name + " 進來了\nIP: " + localIps() + "\nPort: " + PORT); });
                    while (running) {
                        Socket client = socket.accept();
                        Peer peer = new Peer(client);
                        peers.add(peer);
                        new Thread(peer).start();
                    }
                } catch (IOException e) {
                    if (server == null) {
                        runOnUiThread(() -> {
                            start.setEnabled(true);
                            Toast.makeText(this, "無法啟動伺服器：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    } else if (running) {
                        append("伺服器錯誤：" + e.getMessage());
                    }
                }
            }).start();
        });
    }
    private String localIps() {
        StringBuilder result = new StringBuilder();
        try {
            java.util.Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                java.util.Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress a = addresses.nextElement();
                    if (!a.isLoopbackAddress() && a instanceof Inet4Address) {
                        if (result.length() > 0) result.append(", ");
                        result.append(a.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) { }
        return result.length() == 0 ? "無法取得，請檢查 Wi-Fi" : result.toString();
    }
    private void showChat() {
        LinearLayout p = page();
        scroll = new ScrollView(this); log = new TextView(this); log.setTextSize(18);
        scroll.addView(log); p.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        EditText message = input("輸入訊息"); p.addView(message);
        Button send = new Button(this); send.setText("送出"); p.addView(send);
        send.setOnClickListener(v -> {
            String s = message.getText().toString().trim();
            if (!s.isEmpty()) { message.setText(""); new Thread(() -> broadcast(name + "： " + s)).start(); }
        });
        Button close = new Button(this); close.setText("關閉聊天室"); p.addView(close);
        close.setOnClickListener(v -> { shutdown(); showSetup(); });
    }
    private void append(String s) {
        runOnUiThread(() -> { if (log != null) { log.append(s + "\n"); scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN)); } });
    }
    private void broadcast(String line) {
        append(line);
        for (Peer p : peers) p.send(line);
    }
    private class Peer implements Runnable {
        final Socket socket;
        volatile PrintWriter out;
        String clientName;
        Peer(Socket socket) { this.socket = socket; }
        synchronized void send(String line) {
            if (out != null) { out.println(line); if (out.checkError()) try { socket.close(); } catch (IOException ignored) {} }
        }
        public void run() {
            try {
                socket.setSoTimeout(10000);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                String first = in.readLine();
                if (first == null || first.trim().isEmpty()) return;
                clientName = first.trim();
                socket.setSoTimeout(0);
                send("Server " + name + " 已開啟聊天室");
                for (Peer p : peers) if (p != this && p.clientName != null) send(p.clientName + " 已在聊天室");
                broadcast(clientName + " 進來了");
                String line;
                while ((line = in.readLine()) != null) if (!line.trim().isEmpty()) broadcast(clientName + "： " + line);
            } catch (IOException ignored) {
            } finally {
                peers.remove(this);
                try { socket.close(); } catch (IOException ignored) {}
                if (clientName != null && running) broadcast(clientName + " 離開了聊天室");
            }
        }
    }
    private void shutdown() {
        running = false;
        ServerSocket s = server; server = null;
        if (s != null) try { s.close(); } catch (IOException ignored) {}
        for (Peer p : peers) try { p.socket.close(); } catch (IOException ignored) {}
        peers.clear(); log = null; scroll = null;
    }
    @Override protected void onDestroy() { shutdown(); super.onDestroy(); }
}
