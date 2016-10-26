package com.steven.player.audioplayer.playmodel;


import android.util.Log;

import com.steven.player.audioplayer.Util;
import com.steven.player.audioplayer.bean.AlbumBean;

import static android.content.ContentValues.TAG;

/**
 * 播放模式基类
 * @author Stevenqiu
 *
 */
public abstract class PlayMode {

	/**
	 * 获取下一首歌曲的播放下标
	 * @param albumInfo 专辑信息
	 * @return
     */
	public int getNextSound(AlbumBean albumInfo){
		int currPlayingIndex = checkAlbumInfo(albumInfo);
		if(currPlayingIndex < 0) return currPlayingIndex;
		int nextSoundIndex = nextSound(currPlayingIndex, albumInfo);
		albumInfo.curPlayIndex = nextSoundIndex;
		Log.i(TAG, "currPlayingIndex is "+currPlayingIndex+", next play index is "+nextSoundIndex);
		return albumInfo.curPlayIndex;
	}

	/**
	 * 获取上一首歌曲的播放下标
	 * @param albumInfo 专辑信息
	 * @return
	 */
	public int getPrevSound(AlbumBean albumInfo){
		int currPlayingIndex = checkAlbumInfo(albumInfo);
		if(currPlayingIndex < 0) return currPlayingIndex;
		int preSoundIndex = prevSound(currPlayingIndex, albumInfo);
		albumInfo.curPlayIndex = preSoundIndex;
		Log.i(TAG, "currPlayingIndex is "+currPlayingIndex+", prev play index is "+preSoundIndex);
		return albumInfo.curPlayIndex;
	}

	/**
	 * 获取下一首歌在列表中的下标
	 * @return
	 */
	public abstract int nextSound(int currPlayingIndex, AlbumBean albumInfo);

	/**
	 * 获取上一首歌在列表中的下标
	 * @return
	 */
	public abstract int prevSound(int currPlayingIndex, AlbumBean albumInfo);


	/**
	 * 检测专辑信息
	 * @param albumInfo
	 * @return
     */
	private int checkAlbumInfo(AlbumBean albumInfo){
		if(Util.isNull(albumInfo)){
			Log.e(TAG,"checkAlbumInfo error, albumInfo is null");
			return -1;
		}else if(Util.isNull(albumInfo.mediaList) || albumInfo.mediaList.isEmpty()){
			Log.e(TAG,"checkAlbumInfo error, mediaInfos is null or is empty");
			return -1;
		}
		return albumInfo.curPlayIndex;
	}

	/**
	 * 获取当前播放模式名
	 * @return
     */
	public String getModeName(){
		return this.getClass().getSimpleName();
	}

}
