package vn.phongvoice.offline;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Android client for the on-device CloneTTS engine. No embedded voice or remote API.
 * Requires an alias named "Phong" to be configured privately in CloneTTS.
 */
public final class MainActivity extends Activity {
    private static final int EXPORT_AUDIO = 40;
    private static final int MAX_TEXT = 12000;
    private static final int MAX_AUDIO = 40 * 1024 * 1024;
    private static final String API = "http://127.0.0.1:8080/api/tts";
    private static final String VOICE = "Phong";
    private static final int BG = Color.rgb(13, 17, 27);
    private static final int CARD = Color.rgb(27, 33, 46);
    private static final int ACCENT = Color.rgb(237, 189, 88);
    private static final int WHITE = Color.rgb(245, 242, 238);
    private static final int MUTED = Color.rgb(175, 184, 199);

    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private EditText edit;
    private TextView status;
    private TextView count;
    private Button generate;
    private Button playButton;
    private Button saveButton;
    private ProgressBar progress;
    private File lastAudio;
    private boolean isMp3;
    private MediaPlayer player;

    private int dp(int px) {
        return Math.round(px * getResources().getDisplayMetrics().density);
    }
    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }
    private GradientDrawable background(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return v;
    }
    private void add(LinearLayout root, View child, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(top), 0, dp(bottom));
        root.addView(child, p);
    }
    private Button button(String value, boolean primary) {
        Button v = new Button(this);
        v.setText(value);
        v.setTextSize(15);
        v.setAllCaps(false);
        v.setTextColor(primary ? BG : WHITE);
        v.setBackground(background(primary ? ACCENT : Color.rgb(54, 60, 77), 13));
        v.setMinHeight(dp(52));
        return v;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(19), dp(28), dp(19), dp(36));
        scroll.addView(root);
        setContentView(scroll);

        add(root, label("●  PHONG VOICE ONE", 13, ACCENT, true), 0, 16);
        add(root, label("Giọng Phong.\nDành riêng cho anh.", 29, WHITE, true), 0, 8);
        add(root, label("Ứng dụng Android  •  Xử lý trên điện thoại", 12, MUTED, false), 0, 24);

        LinearLayout voicePanel = column();
        voicePanel.setPadding(dp(17), dp(17), dp(17), dp(17));
        voicePanel.setBackground(background(CARD, 16));
        voicePanel.addView(label("GIỌNG ĐỌC CỐ ĐỊNH", 11, ACCENT, true));
        voicePanel.addView(label("PHONG", 19, WHITE, true));
        voicePanel.addView(label("Cần đăng ký giọng mẫu riêng trong CloneTTS với alias Phong.", 12, MUTED, false));
        add(root, voicePanel, 0, 22);

        add(root, label("DÁN VĂN BẢN CẦN ĐỌC", 12, MUTED, true), 0, 9);
        edit = new EditText(this);
        edit.setGravity(Gravity.TOP | Gravity.START);
        edit.setTextSize(16);
        edit.setTextColor(WHITE);
        edit.setHintTextColor(MUTED);
        edit.setHint("Dán một đoạn văn bản tiếng Việt vào đây...");
        edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edit.setMinLines(7);
        edit.setMaxLines(14);
        edit.setPadding(dp(16), dp(16), dp(16), dp(16));
        edit.setBackground(background(CARD, 16));
        add(root, edit, 0, 5);

        count = label("0 / 12.000 ký tự", 12, MUTED, false);
        count.setGravity(Gravity.END);
        add(root, count, 0, 16);
        edit.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int z) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                count.setText(s.length() + " / 12.000 ký tự");
                count.setTextColor(s.length() > MAX_TEXT ? Color.RED : MUTED);
            }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });

        generate = button("TẠO GIỌNG ĐỌC", true);
        generate.setOnClickListener(v -> generateAudio());
        add(root, generate, 0, 12);

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        progress.setVisibility(View.GONE);
        add(root, progress, 0, 10);

        status = label("Chưa tạo audio. Cần bật CloneTTS trên điện thoại.", 13, MUTED, false);
        add(root, status, 0, 17);

        playButton = button("▶  NGHE THỬ", false);
        playButton.setEnabled(false);
        playButton.setOnClickListener(v -> playAudio());
        add(root, playButton, 0, 9);

        saveButton = button("↓  LƯU ÂM THANH VỀ MÁY", false);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(v -> saveAudio());
        add(root, saveButton, 0, 25);

        add(root, label("CÀI ĐẶT GIỌNG LẦN ĐẦU", 12, ACCENT, true), 0, 9);
        add(root, label(
            "1. Cài CloneTTS và tải mô hình trong ứng dụng.\n\n"
          + "2. CloneTTS → Voices → Add Voice → Select External. "
          + "Chọn file âm thanh giọng mẫu của anh đang lưu trên Samsung, "
          + "dán chính xác lời mẫu, đặt Name / Alias = Phong.\n\n"
          + "3. CloneTTS → Settings → bật Local HTTP API Service (port 8080).\n\n"
          + "4. Trở lại Phong Voice One, kiểm tra kết nối rồi thử 1–2 câu.\n\n"
          + "Chú ý: Bộ máy xử lý trong CloneTTS, không tích hợp vào APK này. "
          + "Khả năng đọc tiếng Việt cần thử thực tế.", 13, MUTED, false), 0, 12);

        Button test = button("BẤM ĐỂ KIỂM TRA KẾT NỐI", true);
        test.setEnabled(true);
        test.setContentDescription("Kiểm tra kết nối cục bộ với CloneTTS");
        test.setOnClickListener(v -> testEngine());
        add(root, test, 0, 9);

        Button install = button("MỞ TRANG TẢI CLONETTS", false);
        install.setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/sipeter/CloneTTS/releases"))));
        add(root, install, 0, 15);

        add(root, label("Kho mã nguồn công khai KHÔNG chứa giọng mẫu. "
           + "Văn bản gửi duy nhất đến 127.0.0.1 trên chính điện thoại.",
           11, MUTED, false), 0, 0);
    }

    private void busy(boolean value) {
        generate.setEnabled(!value);
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    private void testEngine() {
        status.setText("Đang kiểm tra cổng 8080...");
        pool.execute(() -> {
            boolean connected;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", 8080), 2300);
                connected = true;
            } catch (IOException e) {
                connected = false;
            }
            final boolean ok = connected;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                status.setText(ok ? "Đã thấy dịch vụ trên cổng 8080. Hãy thử tạo giọng Phong."
                   : "Không kết nối được. Mở CloneTTS và bật Local HTTP API Service.");
            });
        });
    }

    private void generateAudio() {
        String input = edit.getText().toString().trim();
        if (input.length() == 0) {
            status.setText("Vui lòng dán văn bản trước.");
            return;
        }
        if (input.length() > MAX_TEXT) {
            status.setText("Văn bản dài hơn 12.000 ký tự.");
            return;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        if (lastAudio != null && lastAudio.exists()) lastAudio.delete();
        lastAudio = null;
        playButton.setEnabled(false);
        saveButton.setEnabled(false);
        busy(true);
        status.setText("Đang tạo giọng Phong bằng bộ máy cục bộ...");
        pool.execute(() -> {
            try {
                File result = requestAudio(input);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    lastAudio = result;
                    busy(false);
                    status.setText(isMp3 ? "Đã tạo MP3." : "Đã tạo WAV.");
                    playButton.setEnabled(true);
                    saveButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    busy(false);
                    status.setText("Chưa tạo được audio: " + e.getMessage()
                       + "\nKiểm tra cổng 8080 và alias Phong trong CloneTTS.");
                });
            }
        });
    }

    private File requestAudio(String input) throws IOException {
        URL url = new URL(API);
        if (!"127.0.0.1".equals(url.getHost()) || url.getPort() != 8080)
            throw new IOException("Chỉ cho phép localhost.");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(3200);
        conn.setReadTimeout(120000);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("Accept", "audio/wav, audio/x-wav, audio/mpeg");
        String form = "text=" + URLEncoder.encode(input, "UTF-8")
            + "&voice=" + URLEncoder.encode(VOICE, "UTF-8");
        try {
            try (OutputStream out = conn.getOutputStream()) {
                out.write(form.getBytes(StandardCharsets.UTF_8));
            }
            int http = conn.getResponseCode();
            if (http != 200) throw new IOException("HTTP " + http);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream in = conn.getInputStream()) {
                byte[] part = new byte[16384];
                int n, size = 0;
                while ((n = in.read(part)) != -1) {
                    size += n;
                    if (size > MAX_AUDIO) throw new IOException("Audio vượt 40 MB.");
                    buffer.write(part, 0, n);
                }
            }
            byte[] bytes = buffer.toByteArray();
            boolean wav = bytes.length >= 44
                && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'A'
                && bytes[10] == 'V' && bytes[11] == 'E';
            boolean mp3 = bytes.length >= 4
                && ((bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3')
                    || ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xe0) == 0xe0));
            if (!wav && !mp3) throw new IOException("CloneTTS không trả WAV/MP3.");
            File dest = new File(getCacheDir(), "PhongVoice_" + (mp3 ? "latest.mp3" : "latest.wav"));
            try (FileOutputStream out = new FileOutputStream(dest)) {
                out.write(bytes);
            }
            isMp3 = mp3;
            return dest;
        } finally {
            conn.disconnect();
        }
    }

    private void playAudio() {
        if (lastAudio == null || !lastAudio.exists()) return;
        try {
            if (player != null && player.isPlaying()) {
                player.pause();
                playButton.setText("▶  NGHE TIẾP");
                return;
            }
            if (player == null) {
                player = new MediaPlayer();
                player.setDataSource(lastAudio.getAbsolutePath());
                player.setOnCompletionListener(m -> runOnUiThread(() ->
                    playButton.setText("▶  NGHE LẠI")));
                player.prepare();
            }
            player.start();
            playButton.setText("Ⅱ  TẠM DỪNG");
        } catch (Exception e) {
            status.setText("Không phát được: " + e.getMessage());
        }
    }

    private void saveAudio() {
        if (lastAudio == null || !lastAudio.exists()) return;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(isMp3 ? "audio/mpeg" : "audio/wav");
        i.putExtra(Intent.EXTRA_TITLE,
            "PhongVoice_" + System.currentTimeMillis() + (isMp3 ? ".mp3" : ".wav"));
        startActivityForResult(i, EXPORT_AUDIO);
    }

    @Override @Deprecated
    protected void onActivityResult(int code, int resultCode, Intent data) {
        super.onActivityResult(code, resultCode, data);
        if (code != EXPORT_AUDIO || resultCode != RESULT_OK
            || data == null || data.getData() == null || lastAudio == null) return;
        try (InputStream in = new FileInputStream(lastAudio);
             OutputStream out = getContentResolver().openOutputStream(data.getData())) {
            if (out == null) throw new IOException("Không thể ghi file.");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                if (n > 0) out.write(buf, 0, n);
            }
            status.setText("Đã lưu file vào điện thoại.");
        } catch (IOException e) {
            status.setText("Không lưu được: " + e.getMessage());
        }
    }

    @Override protected void onDestroy() {
        if (player != null) player.release();
        pool.shutdownNow();
        super.onDestroy();
    }
}
