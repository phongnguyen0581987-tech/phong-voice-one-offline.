package vn.phongvoice.offline;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;

/** Personal offline Vietnamese TTS UI. No voice cloning in this edition. */
public final class VietnameseActivity extends Activity {
    private static final Locale VI = new Locale("vi", "VN");
    private static final int MAX = 3000, SAVE = 39;
    private final int bg = Color.rgb(13,17,27), card = Color.rgb(28,35,49);
    private final int gold = Color.rgb(239,193,93), white = Color.rgb(247,247,242);
    private EditText input;
    private TextView result, counter, voiceLabel;
    private Button create, listen, download;
    private TextToSpeech tts;
    private MediaPlayer player;
    private File audio;
    private boolean ready, wasInSettings, destroyed;
    private String activeId;

    private int dp(int d) {return (int)(d * getResources().getDisplayMetrics().density + 0.5);}
    private GradientDrawable shape(int color) {
        GradientDrawable b = new GradientDrawable();
        b.setColor(color); b.setCornerRadius(dp(14)); return b;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size);
        t.setTextColor(color); if (bold) t.setTypeface(null,Typeface.BOLD);
        return t;
    }
    private Button button(String value, boolean highlight) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false);
        b.setTextColor(highlight ? bg : white);
        b.setBackground(shape(highlight ? gold : Color.rgb(61,69,86)));
        b.setMinHeight(dp(52)); b.setTextSize(15); return b;
    }
    private void insert(LinearLayout layout, View child, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,dp(bottom)); layout.addView(child,p);
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1);
        root.setPadding(dp(18),dp(30),dp(18),dp(30));
        scroll.addView(root); setContentView(scroll);
        insert(root,text("●  PHONG VOICE ONE",14,gold,true),14);
        insert(root,text("Đọc tiếng Việt\nkhông cần Internet",28,white,true),8);
        insert(root,text("Dùng một giọng Việt offline trên Samsung · Xuất WAV",12,white,false),20);

        LinearLayout info = new LinearLayout(this); info.setOrientation(1);
        info.setPadding(dp(15),dp(14),dp(15),dp(14));
        info.setBackground(shape(card));
        insert(info,text("GIỌNG TIẾNG VIỆT",12,gold,true),8);
        voiceLabel = text("Đang kiểm tra giọng offline...",14,white,true);
        insert(info,voiceLabel,8);
        insert(info,text("Bản này KHÔNG nhân bản giọng mẫu Phong. Mục tiêu là đọc đúng tiếng Việt ngay trên điện thoại.",12,white,false),0);
        insert(root,info,20);

        insert(root,text("DÁN VĂN BẢN TIẾNG VIỆT",12,gold,true),8);
        input = new EditText(this);
        input.setTextColor(white); input.setHintTextColor(0xffaeb4be);
        input.setHint("Ví dụ: Chào anh em môi giới nhà phố...");
        input.setTextSize(16); input.setGravity(Gravity.TOP|Gravity.START);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(7); input.setMaxLines(14);
        input.setPadding(dp(14),dp(14),dp(14),dp(14));
        input.setBackground(shape(card)); insert(root,input,5);
        counter = text("0 / 3.000 ký tự",12,white,false);
        counter.setGravity(Gravity.END);insert(root,counter,17);
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count) {
                counter.setText(s.length()+" / 3.000 ký tự");
                counter.setTextColor(s.length()>MAX ? Color.RED : white);
            }
            public void afterTextChanged(Editable e){}
        });

        create=button("TẠO ÂM THANH TIẾNG VIỆT",true);
        create.setEnabled(false);
        create.setOnClickListener(v->generate());
        insert(root,create,12);
        result=text("Đang khởi động bộ máy đọc...",13,white,false);
        insert(root,result,16);
        listen=button("▶  NGHE THỬ",false);listen.setEnabled(false);
        listen.setOnClickListener(v->play());insert(root,listen,9);
        download=button("↓  LƯU FILE WAV",false);download.setEnabled(false);
        download.setOnClickListener(v->save());insert(root,download,22);

        insert(root,text("CÀI ĐẶT TIẾNG VIỆT OFFLINE",12,gold,true),8);
        insert(root,text("Cài sherpa-onnx TTS Engine tiếng Việt ARM64, chọn làm công cụ đọc mặc định trong Cài đặt Samsung, sau đó bấm kiểm tra giọng.",13,white,false),12);
        Button check=button("KIỂM TRA GIỌNG TIẾNG VIỆT",true);
        check.setOnClickListener(v->checkVoice());insert(root,check,9);
        Button settings=button("MỞ CÀI ĐẶT ĐỌC VĂN BẢN",false);
        settings.setOnClickListener(v-> {
            wasInSettings=true;
            try {startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}
            catch(Exception ex){startActivity(new Intent(Settings.ACTION_SETTINGS));}
        });insert(root,settings,9);
        Button install=button("TẢI BỘ MÁY TIẾNG VIỆT",false);
        install.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://k2-fsa.github.io/sherpa/onnx/tts/apk-engine.html"))));
        insert(root,install,12);
        insert(root,text("Không cần CloneTTS, không dùng API và không gửi văn bản lên Internet. Không có giọng Việt offline thì sẽ báo lỗi rõ ràng, không tự đọc bằng giọng online.",11,white,false),0);
        initEngine();
    }

    private void initEngine(){
        ready=false;create.setEnabled(false);
        if(tts!=null){tts.stop();tts.shutdown();tts=null;}
        result.setText("Đang khởi tạo giọng Việt offline...");
        tts=new TextToSpeech(this, code->runOnUiThread(()->{
            if(destroyed)return;
            if(code==TextToSpeech.SUCCESS)checkVoice();
            else result.setText("Lỗi khởi tạo. Cài giọng Việt và thử mở lại.");
        }));
    }
    @Override protected void onResume(){
        super.onResume();
        if(wasInSettings){wasInSettings=false;initEngine();}
    }

    private void checkVoice() {
        if(tts==null){result.setText("Đang khởi tạo, hãy kiểm tra lại.");return;}
        ready=false;create.setEnabled(false);
        int code=tts.setLanguage(VI);
        if(code==TextToSpeech.LANG_MISSING_DATA || code==TextToSpeech.LANG_NOT_SUPPORTED){
            voiceLabel.setText("Chưa có giọng Việt");
            result.setText("Cài bộ máy sherpa-onnx tiếng Việt và đặt làm giọng mặc định trên Samsung.");
            return;
        }
        Voice selected=null; Set<Voice> candidates=tts.getVoices();
        if(candidates!=null)for(Voice v:candidates){
            if(v!=null && v.getLocale()!=null
               && "vi".equals(v.getLocale().getLanguage()) && !v.isNetworkConnectionRequired()){
                if(selected==null || "VN".equalsIgnoreCase(v.getLocale().getCountry()))selected=v;
            }
        }
        if(selected==null){
            Voice v=tts.getVoice();
            if(v!=null && v.getLocale()!=null && "vi".equals(v.getLocale().getLanguage())
               && !v.isNetworkConnectionRequired())selected=v;
        }
        if(selected==null || tts.setVoice(selected)!=TextToSpeech.SUCCESS){
            voiceLabel.setText("Thiếu giọng tiếng Việt ngoại tuyến");
            result.setText("Chọn bộ máy sherpa-onnx tiếng Việt trong Cài đặt Samsung rồi kiểm tra lại.");
            return;
        }
        tts.setSpeechRate(1.0f);
        ready=true;create.setEnabled(true);
        voiceLabel.setText("Sẵn sàng: "+selected.getName());
        result.setText("Có giọng Việt offline. Dán văn bản, sau đó bấm TẠO ÂM THANH.");
    }

    private void generate(){
        String content=input.getText().toString().trim();
        if(content.isEmpty() || content.length()>MAX){
            result.setText("Nhập từ 1 đến 3.000 ký tự.");return;
        }
        if(!ready || tts==null){result.setText("Cần cài giọng tiếng Việt offline.");return;}
        if(player!=null){player.release();player=null;}
        if(audio!=null)audio.delete();
        audio=null;listen.setEnabled(false);download.setEnabled(false);
        create.setEnabled(false);
        result.setText("Đang tạo WAV tiếng Việt...");
        final File dest=new File(getCacheDir(),"PhongVoice_TiengViet.wav");
        final String id="vi-"+System.currentTimeMillis();
        activeId=id;
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
            @Override public void onStart(String key){}
            @Override public void onDone(String key){
                if(!id.equals(key))return;
                runOnUiThread(()->{
                    if(destroyed || !id.equals(activeId))return;
                    create.setEnabled(ready);
                    if(dest.length()>44 && isWav(dest)){
                        audio=dest;listen.setEnabled(true);download.setEnabled(true);
                        result.setText("Đã có WAV tiếng Việt. Bấm NGHE THỬ hoặc LƯU FILE.");
                    }else result.setText("Bộ máy không trả về WAV. Hãy thử giọng Việt sherpa-onnx.");
                });
            }
            @Override public void onError(String key){failed(key,id,-1);}
            @Override public void onError(String key,int code){failed(key,id,code);}
        });
        int code=tts.synthesizeToFile(content,null,dest,id);
        if(code!=TextToSpeech.SUCCESS){
            create.setEnabled(ready);
            result.setText("Không thể tạo file. Hãy chọn bộ máy đọc tiếng Việt khác.");
        }
    }
    private void failed(String key,String expected,int error){
        if(!expected.equals(key))return;
        runOnUiThread(()->{
            if(destroyed)return;
            create.setEnabled(ready);
            result.setText("Tạo giọng lỗi mã "+error+". Kiểm tra bộ máy sherpa-onnx và dữ liệu tiếng Việt.");
        });
    }
    private boolean isWav(File f){
        try(InputStream s=new FileInputStream(f)){
            byte[] h=new byte[12];
            if(s.read(h)!=12)return false;
            return h[0]=='R'&&h[1]=='I'&&h[2]=='F'&&h[3]=='F'
                &&h[8]=='W'&&h[9]=='A'&&h[10]=='V'&&h[11]=='E';
        }catch(IOException e){return false;}
    }
    private void play(){
        if(audio==null || !audio.exists())return;
        try{
            if(player!=null && player.isPlaying()){player.pause();listen.setText("▶  NGHE TIẾP");return;}
            if(player==null){
                player=new MediaPlayer();player.setDataSource(audio.getAbsolutePath());
                player.setOnCompletionListener(p->runOnUiThread(()->listen.setText("▶  NGHE LẠI")));
                player.prepare();
            }
            player.start();listen.setText("Ⅱ  TẠM DỪNG");
        }catch(Exception ex){result.setText("Không nghe được: "+ex.getMessage());}
    }
    private void save(){
        if(audio==null || !audio.exists())return;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/wav");
        intent.putExtra(Intent.EXTRA_TITLE,"PhongVoice_vi_"+System.currentTimeMillis()+".wav");
        startActivityForResult(intent,SAVE);
    }
    @SuppressWarnings("deprecation")
    @Override protected void onActivityResult(int req,int code,Intent data){
        super.onActivityResult(req,code,data);
        if(req!=SAVE || code!=RESULT_OK || data==null || data.getData()==null || audio==null)return;
        try(InputStream in=new FileInputStream(audio);
            OutputStream out=getContentResolver().openOutputStream(data.getData())){
            if(out==null)throw new IOException("Không mở được file.");
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);
            result.setText("Đã lưu file WAV vào điện thoại.");
        }catch(IOException e){result.setText("Không lưu được: "+e.getMessage());}
    }
    @Override protected void onDestroy(){
        destroyed=true;
        if(tts!=null){tts.stop();tts.shutdown();}
        if(player!=null)player.release();
        super.onDestroy();
    }
}