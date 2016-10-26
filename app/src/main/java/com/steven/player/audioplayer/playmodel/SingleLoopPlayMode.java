package com.steven.player.audioplayer.playmodel;


import com.steven.player.audioplayer.bean.AlbumBean;

/**
 * 单曲循环播放模式
 * @author Stevenqiu
 *
 */
public class SingleLoopPlayMode extends PlayMode{

	@Override
	public int nextSound(int currPlayingIndex, AlbumBean albumInfo) {
		if(currPlayingIndex < albumInfo.mediaList.size()-1){
			currPlayingIndex ++;
		}else{
			currPlayingIndex = 0;
		}
		return currPlayingIndex;
	}

	@Override
	public int prevSound(int currPlayingIndex, AlbumBean albumInfo) {
		if(currPlayingIndex > 0){
			currPlayingIndex --;
		}else{
			currPlayingIndex = albumInfo.mediaList.size()-1;
		}
		return currPlayingIndex;
	}

}