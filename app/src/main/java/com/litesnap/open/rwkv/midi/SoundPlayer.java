package com.litesnap.open.rwkv.midi;

import android.media.AudioManager;
import android.media.MediaPlayer;

import java.util.HashSet;
import java.util.Set;

public class SoundPlayer {
    private final Set<OnPlayerListener> listeners = new HashSet<>();
    private final MediaPlayer player;

    private int count;
    private int maxLoop;

    public SoundPlayer(){
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    private void play(final String path){
        try {
            count ++;
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (count < maxLoop || isLooping()) {
                        play(path);
                        return;
                    }

                    count ++;
                    call(1);
                }
            });

            player.reset();
            player.setDataSource(path);
            player.prepare();
            play();
        }catch (Exception e){
            call(1);
            e.printStackTrace();
        }
    }

    private void call(int type){
        switch (type){
            case 0:
                for (OnPlayerListener listener : listeners) listener.onPlay();
                break;
            case 1:
                for (OnPlayerListener listener : listeners) listener.onCompletion();
                break;
        }
    }

    public void play(String path, int loop) {
        count = 0;
        setLoop(loop);
        play(path);
    }

    public void setVolume(float volume){
        player.setVolume(volume, volume);
    }

    private void play(){
        player.start();
        call(0);
    }

    public void seekTo(int time){
        player.seekTo(time);
    }

    public void setLoop(int loop){
        maxLoop = loop;
        player.setLooping(loop < 0);
    }

    public void pause(){
        player.pause();
        call(1);
    }

    public boolean isPlaying(){
        return player.isPlaying();
    }

    public boolean isLooping(){
        return player.isLooping();
    }

    public void release(){
        listeners.clear();
        player.release();
    }

    public void addListener(OnPlayerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(OnPlayerListener listener) {
        listeners.remove(listener);
    }

    public interface OnPlayerListener{
        void onCompletion();
        void onPlay();
    }
}
