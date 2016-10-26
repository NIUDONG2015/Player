package com.steven.player.audioplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.steven.player.audioplayer.bean.AlbumBean;
import com.steven.player.audioplayer.playmodel.PlayMode;

import java.util.LinkedList;
import java.util.List;

/**
 * 为唯一与mediaplayService绑定的类，其他需和mediaplayService绑定的类只需要通过在此register一个IPlayerOpToUI实例。
 * 此类还为一个代理类，注册的IPlayerOpToUI实例与service通信也是通过此类
 * Created by Stevenqiu on 2016/7/20.
 */
public class MediaPlayerBindManager implements IPlayerOpToService{

    private final String TAG = MediaPlayerBindManager.class.getSimpleName();
    private IPlayerOpToService mPlayerCaller;
    private final LinkedList<IPlayerOpToUI> listenersList    = new LinkedList<>();
    private boolean                         mHasBind         = false;
    private static MediaPlayerBindManager   manager         = null;
    private AlbumBean mAlbumBean = null;
    private Context context;
    public static synchronized MediaPlayerBindManager getInstance(Context context){
        if(manager == null){
            manager = new MediaPlayerBindManager(context);
        }
        return manager;
    }

    private MediaPlayerBindManager(Context context) {
        this.context = context.getApplicationContext();
    }

    private ServiceConnection mServiceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            MediaPlayService.MediaBinder mb = (MediaPlayService.MediaBinder) service;
            mPlayerCaller = mb.getServicePlayer();
            mb.setPlayOpToUI(mPlayerCallback);
            if (null != mAlbumBean && null != mPlayerCaller) {
                mPlayerCaller.setCurrSoundListInfo(mAlbumBean);
                mPlayerCaller.playSound(mAlbumBean.curPlayIndex);
                mAlbumBean = null;
            }
            mPlayerCallback.onServiceConnected();
            ((MediaPlayService)mPlayerCaller).updateUIByMediaPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mPlayerCaller = null;
            mHasBind = false;
        }
    };

    private void bindMediaPlayService() {
        if (!mHasBind) {
            Log.d(TAG,"bind mediaplayer service");
            Intent i = new Intent(context, MediaPlayService.class);
            context.bindService(i, mServiceConn, Context.BIND_AUTO_CREATE);
            mHasBind = true;
        }
    }

    public void unbindMediaPlayService() {
        if (mHasBind) {
            Log.d(TAG,"unbind mediaplayer service");
            context.unbindService(mServiceConn);
            mPlayerCaller = null;
            //fix bug:解绑后不走onServiceDisconnected
            mPlayerCallback.onServiceDisconnected();
            mHasBind = false;
        }
    }

    private IPlayerOpToUI mPlayerCallback = new IPlayerOpToUI() {

        @Override
        public void onPlayingSound(int currSoundIndex) {
            for (IPlayerOpToUI item : listenersList) {
                item.onPlayingSound(currSoundIndex);
            }
        }

        @Override
        public void onPlayState(PlayState state, int currProgress) {
            for (IPlayerOpToUI item : listenersList) {
                item.onPlayState(state, currProgress);
            }
        }

        @Override
        public void onBufferProgress(int currBuffProgress) {
            for (IPlayerOpToUI item : listenersList) {
                item.onBufferProgress(currBuffProgress);
            }
        }

        @Override
        public void onPlayMode(PlayMode playMode) {
            for (IPlayerOpToUI item : listenersList) {
                item.onPlayMode(playMode);
            }
        }

        @Override
        public void onPlayingList(AlbumBean BMAlbumBean) {
            for (IPlayerOpToUI item : listenersList) {
                item.onPlayingList(BMAlbumBean);
            }
        }

        @Override
        public void onPlayDuration(int duration) {
            for (IPlayerOpToUI item : listenersList) {
                item.onPlayDuration(duration);
            }
        }

        @Override
        public void onPlayError(String errMsg) {
            for (IPlayerOpToUI item : listenersList) {
                item.onPlayError(errMsg);
            }
        }

        @Override
        public void onServiceConnected() {
            Log.d(TAG, "mediaplay service onServiceConnected");
            for (IPlayerOpToUI item : listenersList) {
                item.onServiceConnected();
            }
        }

        @Override
        public void onServiceDisconnected() {
            Log.d(TAG, "mediaplay service onServiceDisconnected");
            for (IPlayerOpToUI item : listenersList) {
                item.onServiceDisconnected();
            }
            listenersList.clear();
        }
    };

    /**
     * 注册一个播放器状态回调
     * @param iPlayerOpToUI
     * @return
     */
    public boolean registerListen(IPlayerOpToUI iPlayerOpToUI){
        if(Util.isNull(iPlayerOpToUI)) return false;
        for (IPlayerOpToUI item : listenersList) {
            if (item == iPlayerOpToUI) {
                return true;
            }
        }
        bindMediaPlayService();
        Log.d(TAG, "register player : " + iPlayerOpToUI);
        boolean result = listenersList.add(iPlayerOpToUI);
        if(result) {
            if(!Util.isNull(mPlayerCaller)) {
                iPlayerOpToUI.onServiceConnected();
                ((MediaPlayService)mPlayerCaller).updateUIByMediaPlayer();
            }
        }
        return result;
    }

    /**
     * 解绑播放器回调监听
     * @param iPlayerOpToUI
     * @return
     */
    public boolean unregisterListen(IPlayerOpToUI iPlayerOpToUI){
        if(Util.isNull(iPlayerOpToUI)) return false;
        for (IPlayerOpToUI item : listenersList) {
            if (item == iPlayerOpToUI) {
                Log.d(TAG, "unregister player : " + iPlayerOpToUI);
                return listenersList.remove(item);
            }
        }
        return false;
    }

    @Override
    public void setCurrSoundListInfo(AlbumBean albumBean) {
        if (null == albumBean) {
            return;
        }
        if (!Util.isNull(mPlayerCaller)) {
            mPlayerCaller.setCurrSoundListInfo(albumBean);
            mPlayerCaller.playSound(albumBean.curPlayIndex);
        } else {
            mAlbumBean = albumBean;
        }
    }

    @Override
    public void playOrPause(PlayState state) {
        if(!Util.isNull(mPlayerCaller)) {
            mPlayerCaller.playOrPause(state);
        }
    }

    @Override
    public void changePlayMode() {
        if(!Util.isNull(mPlayerCaller)) {
            mPlayerCaller.changePlayMode();
        }
    }

    @Override
    public void seekProgress(int progress) {
        if(!Util.isNull(mPlayerCaller)) {
            mPlayerCaller.seekProgress(progress);
        }
    }

    @Override
    public void playSound(int soundIndex) {
        if(!Util.isNull(mPlayerCaller)) {
            mPlayerCaller.playSound(soundIndex);
        }
    }

    @Override
    public void playNext() {
        if(!Util.isNull(mPlayerCaller)) {
            mPlayerCaller.playNext();
        }
    }

    @Override
    public void playPrev() {
        if(!Util.isNull(mPlayerCaller)) {
            mPlayerCaller.playPrev();
        }
    }

    @Override
    public void resetAllPlayModel(List<PlayMode> playModes) {
        if(!Util.isNull(mPlayerCaller)) {
            mPlayerCaller.resetAllPlayModel(playModes);
        }
    }


}
