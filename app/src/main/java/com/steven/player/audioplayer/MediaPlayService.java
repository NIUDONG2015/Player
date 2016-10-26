package com.steven.player.audioplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.steven.player.audioplayer.bean.AlbumBean;
import com.steven.player.audioplayer.playmodel.PlayMode;
import com.steven.player.audiocache.HttpProxyCacheServer;

import java.io.File;
import java.util.List;


public class MediaPlayService extends Service implements IPlayerOpToService, AudioFoucusManager.AudioFocusListener  {
    private final static String TAG = MediaPlayService.class.getName();
    private IPlayerOpToUI uiPlayer;
    private MediaPlayer mediaPlayer;
    private HttpProxyCacheServer proxyCache = null;
    /** 缓存目录，目前缓存目录为app的SD卡缓存目录*/
    private String cacheDir = "";
    private AudioFoucusManager mAudioFocusMgr;
    /** 由丢失音频焦点引起的暂停状态*/
    private boolean pauseByLossFocus = false;


    @Override
    public void onCreate() {
        super.onCreate();
        initProxy();
        registerReceiver();
    }


    private void initProxy() {
        this.cacheDir = getApplicationContext().getExternalCacheDir().getAbsolutePath()+"/audio";
        File file = new File(cacheDir);
        if(!file.exists()) file.mkdirs();
        proxyCache = new HttpProxyCacheServer.Builder(this)
                .maxCacheSize(200 * 1024 * 1024)//缓存大小为200M
                .cacheDirectory(new File(cacheDir))
                .build();
        mAudioFocusMgr = new AudioFoucusManager(this, this);
        mAudioFocusMgr.requestAudioFocus(AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mediaPlayer = new MediaPlayer(this, proxyCache, cacheDir);
        proxyCache.setErrorListen(mediaPlayer);
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        registerReceiver(phoneStateReceiver, intentFilter);
    }

    private BroadcastReceiver phoneStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
            if (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE && !Util.isNull(mediaPlayer) && mediaPlayer.getCurrPlayStatu() == PlayState.playing) {
                playOrPause(PlayState.pause);
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MediaBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        uiPlayer = null;
        releasePlayer();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "media play service is ondestory");
        unregisterReceiver(phoneStateReceiver);
        proxyCache.shutdown();
        releasePlayer();
        mAudioFocusMgr.abandonFocus();
    }

    private void releasePlayer(){
        if (null != mediaPlayer) {
            mediaPlayer.release();
            mediaPlayer.setUiPlayer(null);
            mediaPlayer = null;
        }
    }

    @Override
    public void setCurrSoundListInfo(AlbumBean bmAlbumBean) {
        if (null == bmAlbumBean || null == bmAlbumBean.mediaList || bmAlbumBean.mediaList.isEmpty()) return;
        if (!Util.isNull(mediaPlayer)) {
            mediaPlayer.initAlbum(bmAlbumBean);
        }
    }

    @Override
    public void playOrPause(PlayState state) {
        if (!Util.isNull(mediaPlayer)) {
            mediaPlayer.playOrPause(state);
        }
    }

    @Override
    public void changePlayMode() {
        if (!Util.isNull(mediaPlayer)) {
            mediaPlayer.changePlayMode();
        }
    }

    @Override
    public void seekProgress(int progress) {
        if (!Util.isNull(mediaPlayer)) {
            mediaPlayer.seekTo(progress);
        }
    }

    @Override
    public void playSound(int soundIndex) {
        if (!Util.isNull(mediaPlayer)) {
            mediaPlayer.playSound(soundIndex);
        }
    }

    @Override
    public void playNext() {
        if (!Util.isNull(mediaPlayer)) {
            mediaPlayer.playNext();
        }
    }

    @Override
    public void playPrev() {
        if (!Util.isNull(mediaPlayer)) {
            mediaPlayer.playPrev();
        }
    }

    @Override
    public void resetAllPlayModel(List<PlayMode> playModes) {
        if (!Util.isNull(mediaPlayer)) {
            mediaPlayer.getPlayModelManager().resetPlayMods(playModes);
        }
    }

    public void updateUIByMediaPlayer() {
        if(Util.isNull(uiPlayer)) return;
        if(!Util.isNull(mediaPlayer)) {
            Log.i(TAG, " update ui now, callback the mediaPlayer state");
            uiPlayer.onBufferProgress(mediaPlayer.getBufferPercent());
            uiPlayer.onPlayingList(mediaPlayer.getAlbumInfo());
            uiPlayer.onPlayMode(mediaPlayer.getCurrPlayMode());
            uiPlayer.onPlayDuration(mediaPlayer.getDuration());
            uiPlayer.onPlayState(mediaPlayer.getCurrPlayStatu(), mediaPlayer.getCurrProgess());
        }else{
            Log.i(TAG, "update ui error, the mediaPlayer is null");
        }
    }

    public void setPlayOpToUI(IPlayerOpToUI playerOpToUI) {
        this.uiPlayer = playerOpToUI;
        if(mediaPlayer != null) mediaPlayer.setUiPlayer(uiPlayer);
    }

    public class MediaBinder extends Binder {
        public IPlayerOpToService getServicePlayer() {
            return MediaPlayService.this;
        }

        public void setPlayOpToUI(IPlayerOpToUI playerOpToUI){
            MediaPlayService.this.setPlayOpToUI(playerOpToUI);
        }
    }


    /**
     * 设置由音频焦点得失而改变的暂停状态
     * @param isPause 是否为暂停，此暂停状态是根据音频焦点得到和失去设置
     * @param log 需要打印的日志，根据日志查找哪里设置的问题
     */
    private void setPauseByLossAudioFocus(boolean isPause, String log){
        pauseByLossFocus = isPause;
        Log.i(TAG,"update pause state:"+log+", afater update, pauseByLossFocus: "+pauseByLossFocus);
    }

    @Override
    public void onChange(int focusChange) {
        if(focusChange == AudioManager.AUDIOFOCUS_LOSS){
            //你已经丢失了音频焦点比较长的时间了．你必须停止所有的音频播放．因为预料到你可能很长时间也不能再获音频焦点，所以这里是清理你的资源的好地方．
            if(!Util.isNull(mediaPlayer) && mediaPlayer.getCurrPlayStatu() == PlayState.playing){
                setPauseByLossAudioFocus(true, "focusChange == AudioManager.AUDIOFOCUS_LOSS");
                mediaPlayer.playOrPause(PlayState.pause);
            }
        }else if(focusChange == AudioManager.AUDIOFOCUS_GAIN){
            //你已获得了音频焦点．
            if(!Util.isNull(mediaPlayer) && pauseByLossFocus && mediaPlayer.getCurrPlayStatu() != PlayState.playing){
                mediaPlayer.playOrPause(PlayState.playing);
            }
            setPauseByLossAudioFocus(false, "focusChange == AudioManager.AUDIOFOCUS_GAIN");
        }else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
            //你临时性的丢掉了音频焦点，很快就会重新获得．你必须停止所有的音频播放，但是可以保留你的资源，因为你可能很快就能重新获得焦点．
            if(!Util.isNull(mediaPlayer) && mediaPlayer.getCurrPlayStatu() == PlayState.playing){
                setPauseByLossAudioFocus(true, "focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT");
                mediaPlayer.playOrPause(PlayState.pause);
            }
        }else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
            //你临时性的丢掉了音频焦点，但是你被允许继续以低音量播放，而不是完全停止．
            // 此处临时性丢掉音频也暂停是因为有可能是语音消息那边抢占焦点，减低音量会影响播放语音消息音量
            if(!Util.isNull(mediaPlayer) && mediaPlayer.getCurrPlayStatu() == PlayState.playing){
                setPauseByLossAudioFocus(true, "focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                mediaPlayer.playOrPause(PlayState.pause);
            }
        }
    }


}
