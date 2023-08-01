package com.litesnap.open.rwkv.midi;

import android.content.Context;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

/**
 * Created by ZTMIDGO 2022/9/15
 */
public class OnnxModelImp implements Model {

    private final String MODEL_NAME = "model.onnx";
    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private final OrtSession.SessionOptions options = new OrtSession.SessionOptions();
    private final Map<String, OnnxTensor> map = new LinkedHashMap<>();
    private final Random random = new Random();
    private final ExecutorService exec = Executors.newCachedThreadPool();

    private final Context context;

    private final int layer = 12;
    private final int embd = 512;
    private final int sequenceLength = 1;
    private final List<String> inputNames = new ArrayList<>();

    private OrtSession.Result ort;
    private OrtSession session;
    private MyRunnable runnable;

    private float presence = 0f;
    private float frequency = 0.5f;
    private boolean isRunnable;

    public OnnxModelImp(Context context){
        this.context = context;
        try {
            String path = PathManager.getModelPath(context) + "/" + MODEL_NAME;
            //options.addConfigEntry("session.load_model_format", "ORT");
            session = environment.createSession(path, options);
            inputNames.addAll(session.getInputNames());
            fillMap();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void generate(List<Integer> arrays, int maxCount, Callback callback) {
        if (runnable != null) runnable.setCancel(true);
        isRunnable = true;
        runnable = new MyRunnable() {
            @Override
            public void run() {
                try {
                    Map<Integer, Float> occurrence = new HashMap<>();

                    fillMap();
                    int nextToken = 0;
                    int size = maxCount + arrays.size();

                    for (int i = 0; i < size; i++) {
                        int[] paddedTokens = new int[sequenceLength];
                        paddedTokens[0] = nextToken;
                        IntBuffer buffer = IntBuffer.wrap(paddedTokens);

                        OnnxTensor idx = OnnxTensor.createTensor(environment, buffer, new long[]{sequenceLength});
                        map.put(inputNames.get(0), idx);

                        ort = session.run(map);
                        float[] outputLogits = (float[]) ort.get(0).getValue();
                        if (isCancel()) return;

                        for (Map.Entry<Integer, Float> entry : occurrence.entrySet()){
                            int x = entry.getKey();
                            outputLogits[x] = outputLogits[x] - (presence + entry.getValue() * frequency);
                        }

                        outputLogits[0] = outputLogits[0] + ((i - 2000f) / 500f);
                        outputLogits[127] = outputLogits[127] - 1f;

                        nextToken = SampleLogits.sample(outputLogits, 1.0f, 0.8f, 8);

                        for (Map.Entry<Integer, Float> entry : occurrence.entrySet()){
                            entry.setValue(entry.getValue() * 0.997f);
                        }
                        if (nextToken >= 128 || nextToken == 127){
                            occurrence.put(nextToken, 1f + (occurrence.containsKey(nextToken) ? occurrence.get(nextToken) : 0));
                        }else {
                            occurrence.put(nextToken, 0.3f + (occurrence.containsKey(nextToken) ? occurrence.get(nextToken) : 0));
                        }

                        boolean isEnd = nextToken == 0;
                        fillMap(ort);
                        if (callback != null){
                            callback.callback(maxCount, i, isEnd, new ArrayList<>(Arrays.asList(nextToken)));
                        }

                        if (isEnd) return;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    isRunnable = false;
                    closeResult();
                }
            }
        };
        exec.execute(runnable);
    }

    @Override
    public int sample(List<Integer> indexes, List<Float> probs){
        int index = randomIndex(probs);
        return indexes.get(index);
    }

    @Override
    public void close() {
        if (runnable != null) runnable.setCancel(true);
        exec.shutdown();
        if (session != null) {
            try {
                closeResult();
                session.close();
                options.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cancel() {
        if (runnable != null) runnable.setCancel(true);
    }

    @Override
    public void setTopK(int value) {

    }

    @Override
    public void setPenalty(float v1, float v2) {
        presence = v1;
        frequency = v2;
    }

    @Override
    public void clean() {
        try {
            fillMap();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isRunning() {
        return isRunnable;
    }

    private void fillMap() throws Exception {
        for (int i = 0; i < inputNames.size(); i++){
            String name = inputNames.get(i);
            float[] buff = new float[embd];
            OnnxTensor inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(buff), new long[]{embd});
            map.put(name, inputTensor);
        }
    }

    /*private void fillMap() throws Exception {
        for (int i = 0; i < inputNames.size(); i++){
            String name = inputNames.get(i);
            float[] buff = new float[layer * embd];
            if (name.equals("pp_att")) Arrays.fill(buff, (float) -1e30);
            OnnxTensor inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(buff), new long[]{layer, embd});
            map.put(name, inputTensor);
        }
    }*/

    private void fillMap(OrtSession.Result result){
        if (result == null) return;
        for (int i = 0; i < inputNames.size(); i++){
            map.put(inputNames.get(i),(OnnxTensor) result.get(i));
        }
    }

    private void closeResult(){
        if (ort != null){
            ort.close();
            ort = null;
        }
    }

    private int randomIndex(List<Float> probs){
        float sun = 0;
        for (float value : probs) sun += value;
        float rnd = sun * random.nextFloat();
        float acc = 0f;
        for (int i = 0; i < probs.size(); i++){
            acc += probs.get(i);
            if (rnd < acc) return i;
        }
        return probs.size() - 1;
    }
}
