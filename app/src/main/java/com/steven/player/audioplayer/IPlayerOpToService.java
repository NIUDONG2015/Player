package com.steven.player.audioplayer;


import com.steven.player.audioplayer.bean.AlbumBean;
import com.steven.player.audioplayer.playmodel.PlayMode;

import java.util.List;

/**
 * Activity通过此接口调用播放器service，对播放器进行操作
 * @author Stevenqiu
 *
 */
public interface IPlayerOpToService {

	/** 设置专辑信息给播放器*/
	public void setCurrSoundListInfo(AlbumBean bmAlbumBean);
	/** 设置播放器状态*/
	public void playOrPause(PlayState state);
	/** 改变播放器播放模式，默认为列表循环模式和单曲循环模式*/
	public void changePlayMode();
	/** 拖动播放器进度， progress为拖动到的具体时间，单位毫秒，不能超出歌曲时长*/
	public void seekProgress(int progress);
	/** 播放专辑里面具体某首歌，soundIndex为歌曲在列表中下标*/
	public void playSound(int soundIndex);
	/** 播放下一首*/
	public void playNext();
	/** 播放上一首*/
	public void playPrev();
	/** 第三方自定义播放模式，默认的包含列表循环模式和单曲循环，自定义播放模式继承PlayMode*/
	public void resetAllPlayModel(List<PlayMode> playModes);
}