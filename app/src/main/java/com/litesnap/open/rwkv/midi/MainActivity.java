package com.litesnap.open.rwkv.midi;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private SoundPlayer player = new SoundPlayer();
    private final ExecutorService exec = Executors.newCachedThreadPool();

    private TextView mTextView;
    private View mCopyView;
    private View mGesView;
    private View mStartView;
    private View mStopView;
    private View mPlayView;

    private OnnxModelImp model;
    private ModelTockenizer tockenizer;
    private VocabConfig config;
    private List<String> result = new ArrayList<>();

    private ProgressDialog dialog;
    private Handler uiHandler;

    private boolean isCopy = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (model != null) model.close();
        exec.shutdownNow();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.msg);
        mCopyView = findViewById(R.id.copy);
        mGesView = findViewById(R.id.ges);
        mStartView = findViewById(R.id.start);
        mStopView = findViewById(R.id.stop);
        mPlayView = findViewById(R.id.play);

        uiHandler = new Handler();
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);

        File file = new File(PathManager.getModelPath(this) + "/model.onnx");
        isCopy = file.exists();
        setEnable(isCopy);

        player.addListener(new SoundPlayer.OnPlayerListener() {
            @Override
            public void onCompletion() {
                mTextView.setText("播放完成");
            }

            @Override
            public void onPlay() {
                mTextView.setText("播放中. . .");
            }
        });

        mCopyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
                setEnable(false);
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FileUtils.copyAssets(getAssets(), "model", getFilesDir().getAbsoluteFile());
                            isCopy = true;
                        }catch (Exception e){
                            isCopy = false;
                        }finally {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setEnable(isCopy);
                                    dialog.dismiss();
                                }
                            });
                        }
                    }
                });
            }
        });

        mStartView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (model == null){
                        model = new OnnxModelImp(MainActivity.this);
                        tockenizer = new ModelTockenizer(MainActivity.this);
                        config = VocabConfig.fromJson(PathManager.getModelPath(MainActivity.this) + "/vocab_config.json");
                        config.init();
                    }

                    if (model.isRunning()) return;

                    setEnable(false, false, true, true);
                    result.clear();
                    model.generate(tockenizer.encode(null), 4096, new Model.Callback() {
                        long time = System.currentTimeMillis();
                        String per = "每秒执行-步";
                        int laster = 0;
                        @Override
                        public void callback(int maxCount, int position, boolean isEnd, List<Integer> tokens) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    boolean isOver = isEnd ? isEnd : (position + 1) >= maxCount;
                                    String text = isOver ? "已完成" : String.format("%d / %d", position, maxCount);
                                    if (TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis() - time) > 1000){
                                        per = "每秒执行"+(position - laster)+"步";
                                        time = System.currentTimeMillis();
                                        laster = position;
                                    }
                                    text += "  " + per;
                                    mTextView.setText(text);
                                    if (isOver) setEnable(true);
                                }
                            });
                            result.addAll(tockenizer.decode(tokens));
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        mStopView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnable(true);
                if (model != null) model.cancel();
            }
        });

        mGesView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (result.isEmpty()){
                        Toast.makeText(MainActivity.this, "请生成MIDI音频", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mTextView.setText("正在合成音频");
                    MidiTockenizer tockenizer = new MidiTockenizer(config);
                    result.add(0, "<start>");
                    result.add("<end>");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < result.size(); i++){
                        sb.append(result.get(i));
                        if (i < result.size() - 1){
                            sb.append(" ");
                        }
                    }
                    FileUtil.writeFile(sb.toString().getBytes(StandardCharsets.UTF_8), PathManager.getModelPath(MainActivity.this), "midi.txt");
                    Pair<List<MidiMessage>, DecodeState> pair = tockenizer.str_to_midi_messages(sb.toString(), 3.0f);
                    pair.first.addAll(0, tockenizer.getProgramChange());
                    byte[] header = Mido.createHeader(1, 1, 480);
                    byte[] body = Mido.createTrack(pair.first);
                    byte[] array = new byte[header.length + body.length];
                    System.arraycopy(header, 0, array, 0, header.length);
                    System.arraycopy(body, 0, array, header.length, body.length);
                    FileUtil.writeFile(array, PathManager.getModelPath(MainActivity.this), "midi.mid");
                    mTextView.setText("已生成MIDI音频");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        mPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(PathManager.getModelPath(MainActivity.this) + "/midi.mid");
                if (!file.exists()){
                    Toast.makeText(MainActivity.this, "请合成MIDI音频", Toast.LENGTH_SHORT).show();
                    return;
                }
                player.play(file.getAbsolutePath(), 1);
            }
        });
    }

    private void setEnable(boolean isEnable){
        setEnable(isEnable, isEnable, isEnable, isEnable);
    }

    private void setEnable(boolean isGes, boolean isStart, boolean isStop, boolean isPlay){
        mGesView.setEnabled(isGes);
        mStartView.setEnabled(isStart);
        mStopView.setEnabled(isStop);
        mPlayView.setEnabled(isPlay);
    }
}