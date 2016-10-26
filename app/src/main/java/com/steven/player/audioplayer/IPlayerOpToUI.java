package com.steven.player.audioplayer;


import com.steven.player.audioplayer.bean.AlbumBean;
import com.steven.player.audioplayer.playmodel.PlayMode;

/**
 * 播放器通过此接口回调Activity，将播放器播放状态信息设置到Activity
 *
 */
public interface IPlayerOpToUI {
	/** 当前播放的音乐下表*/
	public void onPlayingSound(int currSoundIndex);
	/** 当前播放状态和进度，进度单位为毫秒，最大值为歌曲时长*/
	public void onPlayState(PlayState state, int currProgress);
	/** 当前的缓冲进度，缓冲进度单位为毫秒，最大值为歌曲时长*/
	public void onBufferProgress(int currBuffProgress);
	/** 当前的播放模式*/
	public void onPlayMode(PlayMode playMode);
	/** 当前播放的专辑信息*/
	public void onPlayingList(AlbumBean bmAlbumBean);
	/** 当前播放歌曲的播放时长*/
	public void onPlayDuration(int duration);
	/** 播放出错*/
	public void onPlayError(String errMsg);
	/** service绑定成功*/
	public void onServiceConnected();
	/** 断开与service连接*/
	public void onServiceDisconnected();
}
