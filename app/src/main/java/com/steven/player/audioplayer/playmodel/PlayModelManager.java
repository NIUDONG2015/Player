package com.steven.player.audioplayer.playmodel;


import android.util.Log;

import com.steven.player.audioplayer.Util;
import com.steven.player.audioplayer.bean.AlbumBean;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * 播放模式管理类
 */
public class PlayModelManager {
    private List<PlayMode> playModes = new ArrayList<>();
    private int currPlayModeIndex = 0;

    public PlayModelManager(){
        //设置默认的播放模式
        playModes.add(new OrderLoopPlayMode());
        playModes.add(new SingleLoopPlayMode());
    }

    /**
     * 重置所有的播放模式
     * @param playModes
     * @return
     */
    public boolean resetPlayMods(List<PlayMode> playModes){
        if(Util.isNullOrNil(playModes)){
            Log.e(TAG, "can not set the null or empty playMode list.");
            return false;
        }
        this.playModes.clear();
        boolean result = this.playModes.addAll(playModes);
        currPlayModeIndex = 0;//重置之后默认为第一种播放模式
        return result;
    }

    /**
     * 获取下一种播放模式
     * @return
     */
    public PlayMode getNextPlayMode(){
        if(Util.isNullOrNil(playModes)){
            Log.e(TAG, "get next play mode error, the playModes is null or empty");
            return null;
        }
        if(currPlayModeIndex < playModes.size()-1){
            currPlayModeIndex ++;
        }else{
            currPlayModeIndex = 0;
        }
        return playModes.get(currPlayModeIndex);
    }

    /**
     * 获取当前播放模式
     * @return
     */
    public PlayMode getCurrPlayMode(){
        if(Util.isNullOrNil(playModes)){
            Log.e(TAG, "get next play mode error, the playModes is null or empty");
            return null;
        }
        return playModes.get(currPlayModeIndex);
    }

    /**
     * 根据当前播放模式获取下一首歌曲下标
     * @param albumInfo 专辑信息
     * @return
     */
    public int getNextSoundIndex(AlbumBean albumInfo){
        PlayMode currPlayMode = getCurrPlayMode();
        if(Util.isNull(currPlayMode)) return -1;
        return currPlayMode.getNextSound(albumInfo);
    }

    /**
     * 根据当前播放模式获取上一首歌曲下标
     * @param albumInfo
     * @return
     */
    public int getPrevSoundIndex(AlbumBean albumInfo){
        PlayMode currPlayMode = getCurrPlayMode();
        if(Util.isNull(currPlayMode)) return -1;
        return currPlayMode.getPrevSound(albumInfo);
    }

    /**
     * 获取所有的播放模式
     * @return
     */
    public List<PlayMode> getPlayModes(){
        return playModes;
    }
}
