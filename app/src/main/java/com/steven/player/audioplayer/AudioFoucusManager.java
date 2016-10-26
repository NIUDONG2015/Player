package com.steven.player.audioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;


/**
 * Created by Stevenqiu on 2016/5/20.
 */
public class AudioFoucusManager {
    private final String TAG = AudioFoucusManager.class.getName();
    private AudioManager mAudioManager;
    private Context context;
    private AudioFocusListener focusListener;

    /**
     * 用于外部监听声音焦点的变化的接口
     *
     */
    public interface AudioFocusListener {
        public void onChange(int focusChange);
    }

    private AudioManager.OnAudioFocusChangeListener audioFocus = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            // TODO Auto-generated method stub
            if (focusListener != null) {
                Log.d(TAG, "jacks change:"+focusChange);
                focusListener.onChange(focusChange);
            }
        }
    };


    public AudioFoucusManager(Context context){
        this.context = context;
    }

    public AudioFoucusManager(Context context, AudioFocusListener focusListener){
        this(context);
        setFocusListener(focusListener);
    }

    public void setFocusListener(AudioFocusListener focusListener) {
        this.focusListener = focusListener;
    }

    public boolean requestAudioFocus(int streamType, int durationHint) {
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1){
            return false;
        }
        if (mAudioManager == null && context != null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        boolean value = false;

        if (this.mAudioManager != null) {
            value = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.requestAudioFocus(audioFocus, streamType, durationHint);
        }

        return value;
    }

    public boolean abandonFocus() {
        if (mAudioManager == null && context != null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        boolean value = false;
        if (this.mAudioManager != null) {
            value = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(audioFocus);
        }
        return value;
    }

}
