package com.steven.player.audioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.steven.player.audioplayer.bean.AlbumBean;
import com.steven.player.audioplayer.bean.MediaBean;
import com.steven.player.audioplayer.playmodel.PlayMode;
import com.steven.player.audioplayer.playmodel.PlayModelManager;
import com.steven.player.audioplayer.playmodel.SingleLoopPlayMode;
import com.steven.player.audiocache.CacheListener;
import com.steven.player.audiocache.HttpProxyCacheServer;
import com.steven.player.audiocache.ProxyCacheUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


/**
 * 封装的MediaPlayer类
 * @author stevenqiu
 *
 */
public class MediaPlayer implements android.media.MediaPlayer.OnCompletionListener,
		android.media.MediaPlayer.OnPreparedListener, android.media.MediaPlayer.OnErrorListener, android.media.MediaPlayer.OnSeekCompleteListener,
		CacheListener, HttpProxyCacheServer.OnProxyErrorListen, OnBufferingUpdateListener {

	private static final String TAG = MediaPlayer.class.getName();
	public android.media.MediaPlayer mediaPlayer;
	private IPlayerOpToUI uiPlayer;
	/** 当前播放的url*/
	private String url;
	/** 当前音频的缓存百分比*/
	private int bufferPercent = 0;
	private boolean isPrepared = false;
	private boolean isProxyPinged = true;
	private boolean playError = false;
	private int seekProgress = 0;
	private PlayModelManager playModelManager;
	private AlbumBean bmAlbumBean = null;
	/**UI的暂停状态，和mediaplayer的播放状态是分离开的*/
	private PlayState uiBMPlayState = PlayState.pause;
	private boolean uiSeek = false;
	private HttpProxyCacheServer proxyCache = null;
	/** 缓存目录，目前缓存目录为app的SD卡缓存目录*/
	private String cacheDir = "";

	Handler updateHander = new Handler(){
		int currProgress = 0;
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what){
				case 0:
					updateUIProgress();
					break;
				case 1:
					if(!Util.isNull(uiPlayer))
						uiPlayer.onPlayError((String) msg.obj);
					playNext();
					break;
				case 2:
					if(!Util.isNull(uiPlayer))
						uiPlayer.onPlayError((String) msg.obj);
					break;
			}
		}

		private int getDelay(){
			int delay = 1000 - (getCurrProgess()%1000);
			return delay > 0 ? delay:1000;
		}

		private void updateUIProgress(){
			if(!Util.isNull(uiPlayer) && mediaPlayer.isPlaying()){
				int currPosition = getCurrProgess()/1000;
				if(currProgress != currPosition){
					currProgress = currPosition;
					uiPlayer.onPlayState(getCurrPlayStatu(), getCurrProgess());
				}
				removeMessages(0);
				sendEmptyMessageDelayed(0, getDelay());
			}
		}
	};

	public MediaPlayer(Context context, HttpProxyCacheServer proxyCache, String cacheDir) {
		this.proxyCache = proxyCache;
		this.cacheDir = cacheDir;
		playModelManager = new PlayModelManager();
		initMediaPlayer(context);
	}

	/**
	 * 初始化MediaPlayer
	 * @return
	 */
	private void initMediaPlayer(Context context){
		mediaPlayer = getMediaPlayer(context);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnBufferingUpdateListener(this);
	}

	private android.media.MediaPlayer getMediaPlayer(Context context){
		android.media.MediaPlayer mediaplayer = new android.media.MediaPlayer();
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
			return mediaplayer;
		}
		try {
			Class<?> cMediaTimeProvider = Class.forName( "android.media.MediaTimeProvider" );
			Class<?> cSubtitleController = Class.forName( "android.media.SubtitleController" );
			Class<?> iSubtitleControllerAnchor = Class.forName( "android.media.SubtitleController$Anchor" );
			Class<?> iSubtitleControllerListener = Class.forName( "android.media.SubtitleController$Listener" );
			Constructor constructor = cSubtitleController.getConstructor(new Class[]{Context.class, cMediaTimeProvider, iSubtitleControllerListener});
			Object subtitleInstance = constructor.newInstance(context, null, null);
			Field f = cSubtitleController.getDeclaredField("mHandler");
			f.setAccessible(true);
			try {
				f.set(subtitleInstance, new Handler());
			} catch (IllegalAccessException e) {
				return mediaplayer;
			} finally {
				f.setAccessible(false);
			}
			Method setsubtitleanchor = mediaplayer.getClass().getMethod("setSubtitleAnchor", cSubtitleController, iSubtitleControllerAnchor);
			setsubtitleanchor.invoke(mediaplayer, subtitleInstance, null);
		} catch (Exception e) {}
		return mediaplayer;
	}

	/**
	 * 根据URL播放在线音频
	 * @param url 音频文件的URL
	 */
	public void playUrl(String url) {
		if(!TextUtils.isEmpty(this.url)) proxyCache.unregisterCacheListener(this, this.url);
		this.url = url;
		//提前记录是否为looping，在reset后looping被重置
		boolean isLooping = mediaPlayer.isLooping();
		proxyCache.registerCacheListener(this, url);
		resetVariable();
		if (mediaPlayer.isPlaying()) mediaPlayer.stop();
		mediaPlayer.reset();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setLooping(isLooping);
		try {
			mediaPlayer.setDataSource(getPath(url));
			mediaPlayer.prepareAsync();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	/**
	 * 根据文件的大小和URL判断是否缓存音频，如果已缓存，则使用本地地址，未缓存则组装URL地址
	 * @return 播放音频的地址
	 */
	private String getPath(String url){
		String path;
		String fileName = ProxyCacheUtils.computeMD5(url);//文件名使用URL的MD5作为文件名
		File file = new File(cacheDir, fileName);
		this.bufferPercent = 0;
		path = proxyCache.getProxyUrl(url);
		if(null != uiPlayer) {
			uiPlayer.onBufferProgress(0);
			uiPlayer.onPlayState(PlayState.playing,0);
			uiPlayer.onPlayDuration(0);
		}
		Log.i(TAG, "file name generator by url: "+fileName+"，local file size: "+(file.exists()?file.length():0)+", the play url is :" +path);
		return path;
	}

	/**
	 * 播放或者暂停
	 * @param state 播放状态
     */
	public void playOrPause(PlayState state) {
		if(playError && state == PlayState.playing){
			//播放出错则继续播放当前歌曲
			if(bmAlbumBean == null){
				Log.e(TAG, "the album is null");
				return;
			}
			playSound(bmAlbumBean.curPlayIndex);
			return;
		}
		if(seekProgress > 0 || !isPrepared){
			//1、进行了拖动；2、还未prepared；此两种情况先记录播放状态，待缓冲达到seekProgress或者prepared后根据记录的播放状态操作
			uiBMPlayState = state;
			return;
		}
		uiBMPlayState = null;
		if(state == PlayState.playing){
			//如果当前状态为非播放状态，则播放
			if(!mediaPlayer.isPlaying()) start();
		}else{
			if(mediaPlayer.isPlaying()) pause();
		}
	}

	private void start(){
		mediaPlayer.start();
		updateHander.sendEmptyMessage(0);
		Log.i(TAG, "player is started");
	}

	private void pause(){
		mediaPlayer.pause();
		if(!Util.isNull(uiPlayer)) uiPlayer.onPlayState(PlayState.pause, getCurrProgess());
		Log.i(TAG, "player is paused");
	}

	public void release() {
		Log.d(TAG, "media player release");
		updateHander.removeMessages(0);
		if(mediaPlayer.isPlaying()) mediaPlayer.stop();
		mediaPlayer.release();
		mediaPlayer = null;
		proxyCache.unregisterCacheListener(this, url);
		if(!Util.isNull(uiPlayer)) uiPlayer.onPlayState(PlayState.stop, getCurrProgess());
	}

	/**
	 * 跳转进度，根据seekbar进度计算mediaplayer进度
	 * @param progress seekbar的进度
	 */
	public void seekTo(int progress){
		if(!isPrepared) return;//未prepared时拖动无效
		if(progress >= getDuration()) {
			playNext();//拖动超出或达到歌曲时长则播放下一曲
			return;
		}
		//此处必须对progress取整，不然seekTo会有问题，能顺利seekComplete，但是实际进度无变化
		progress = (progress/1000)*1000;
		uiBMPlayState = mediaPlayer.isPlaying()? PlayState.playing: PlayState.pause;//记录播放状态，在拖动后恢复
		Log.d(TAG, "start seek progress to "+progress+", duration : "+getDuration()+", buffer progress: "+bufferPercent*getDuration()/100 );
		mediaPlayer.pause();
		uiSeek = true;
		if(bufferPercent*getDuration()/100 > progress){
			uiBMPlayState = PlayState.playing;
			mediaPlayer.seekTo(progress);
		}else{
			seekProgress = progress;
		}
	}

	@Override
	public void onPrepared(android.media.MediaPlayer mediaPlayer) {
		Log.d(TAG, "media player is prepared");
		isPrepared = true;
		if(!Util.isNull(uiPlayer)) uiPlayer.onPlayDuration(getDuration());
		if(!Util.isNull(uiBMPlayState) && uiBMPlayState != PlayState.playing) return;
		uiBMPlayState = null;
		start();
		setBufferPercent(bufferPercent);
	}

	@Override
	public void onCompletion(android.media.MediaPlayer mediaPlayer) {
		//当媒体文件时长正确但是在未到结果就播放结束则暂时判定为网络错误
		int duration = getDuration();
		if(bufferPercent < 100 && duration > 1000 && mediaPlayer.getCurrentPosition() < duration - 2000){
			Log.e(TAG, "error play completion, curr duration: "+duration+", curr position: "+getCurrProgess()+", curr bufferpercent: "+bufferPercent);
			onProxyError();
			return;
		}
		Log.d(TAG, "media player play completed");
		if(!mediaPlayer.isLooping()) playNext();//自动播放下一首
	}

	@Override
	public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
		String errorMsg = String.format("the mediaPlayer play error, error what:%d, error extra:%d", what, extra);
		Log.e(TAG, errorMsg);
		Message message = new Message();
		message.what = 1;
		message.obj = errorMsg;
		if(isPrepared) updateHander.sendMessage(message);
		return true;
	}

	@Override
	public void onSeekComplete(android.media.MediaPlayer mediaPlayer) {
		if(!uiSeek) return;
		uiSeek = false;
		Log.d(TAG, "seek progress complete, update ui seekbar");
		seekProgress = 0;
		if(!Util.isNull(uiBMPlayState) && uiBMPlayState != PlayState.playing) return;
		uiBMPlayState = null;
		start();
	}

	/**
	 * 设置缓冲进度
	 * @param percent 缓冲进度
     */
	private void setBufferPercent(int percent){
		this.bufferPercent = percent;
		if(!Util.isNull(uiPlayer)) uiPlayer.onBufferProgress(percent*getDuration()/100);
		if(getDuration() > seekProgress && seekProgress > 0){
			mediaPlayer.seekTo(seekProgress);
			seekProgress = 0;
		}
	}

	@Override
	public void onCacheAvailable(File cacheFile, String url, int percent) {
		if(isProxyPinged) setBufferPercent(percent);
	}

	@Override
	public void onBufferingUpdate(android.media.MediaPlayer mp, int percent) {
		if(!isProxyPinged && bufferPercent == 100) setBufferPercent(percent);
	}

	@Override
	public void onPingedError() {
		isProxyPinged = false;
	}

	@Override
	public void onProxyError() {
		if(playError) return;
		playError = true;
		String errorMsg = "the audio cache proxy error, please check network";
		Message message = new Message();
		message.what = 2;
		message.obj = errorMsg;
		updateHander.sendMessage(message);
	}



	/**
	 * 设置专辑信息
	 * @param bmAlbumBean 专辑信息
     */
	public void initAlbum(AlbumBean bmAlbumBean){
		if (null == bmAlbumBean || Util.isNullOrNil(bmAlbumBean.mediaList)) {
			Log.e(TAG, "the album or the clone callback is null");
			return;
		}
		this.bmAlbumBean = bmAlbumBean;
	}

	/**
	 * 播放歌曲
	 * @param soundIndex 歌曲的下标
     */
	public void playSound(int soundIndex){
		Log.i(TAG, "play sound , the sound index is "+soundIndex);
		if (bmAlbumBean == null || Util.isNullOrNil(bmAlbumBean.mediaList) || soundIndex > bmAlbumBean.mediaList.size() - 1 || soundIndex < 0) {
			String errorMsg = "play sound error, the soundIndex is "+soundIndex+", and the bmalbumBean is "+(bmAlbumBean == null?"null":"out of soundIndex");
			Log.e(TAG, errorMsg);
			Message message = new Message();
			message.what = 2;
			message.obj = errorMsg;
			updateHander.sendMessage(message);
			return;
		}
		bmAlbumBean.curPlayIndex = soundIndex;
		MediaBean currPlaySound = (MediaBean) bmAlbumBean.mediaList.get(soundIndex);
		playUrl(currPlaySound.mediaUrl);
		if (null != uiPlayer) uiPlayer.onPlayingSound(bmAlbumBean.curPlayIndex);
	}

	/**
	 * 播放下一首
	 */
	public void playNext(){
		//根据播放模式获取下一首歌曲下标
		int soundIndex = playModelManager.getNextSoundIndex(bmAlbumBean);
		Log.i(TAG, "play next , the soundIndex is "+soundIndex);
		if (soundIndex >= 0) {
			playSound(soundIndex);
		} else if (null != uiPlayer){
			uiPlayer.onPlayState(PlayState.stop, getCurrProgess());
		}
	}

	/**
	 * 播放上一首
	 */
	public void playPrev(){
		//根据播放模式获取上一首歌曲下标
		int soundIndex = playModelManager.getPrevSoundIndex(bmAlbumBean);
		Log.i(TAG, "play prev , the soundIndex is "+soundIndex);
		if (soundIndex >= 0) {
			playSound(soundIndex);
		} else if (null != uiPlayer){
			uiPlayer.onPlayState(PlayState.stop, getCurrProgess());
		}
	}

	/**
	 * 切换播放模式，目前主要在单曲模式和列表循环模式之间切换
	 */
	public void changePlayMode() {
		PlayMode currPlayMode = playModelManager.getNextPlayMode();
		if(Util.isNull(currPlayMode)){
			Log.e(TAG, "the currPlayMode is null");
			return;
		}
		Log.d(TAG, "changePlayMode, currPlayMode: "+currPlayMode.getModeName());
		//如果为单曲循环直接将mediaplayer looping设置为true
		mediaPlayer.setLooping((currPlayMode instanceof SingleLoopPlayMode)?true:false);
		if (null != uiPlayer) uiPlayer.onPlayMode(currPlayMode);
	}

	/**
	 * 设置IPlayerOpToUI回调
	 * @param uiPlayer
     */
	public void setUiPlayer(IPlayerOpToUI uiPlayer){
		this.uiPlayer = uiPlayer;
		updateHander.sendEmptyMessage(0);
	}

	private void resetVariable(){
		playError = false;
		isPrepared = false;
		uiBMPlayState = null;
	}

	/**
	 * 根据播放进度计算seekbar进度
	 * @return
	 */
	public int getCurrProgess(){
		return (isPrepared && getDuration() > 0) ? mediaPlayer.getCurrentPosition():0;
	}

	public int getBufferPercent(){
		return bufferPercent;
	}

	public AlbumBean getAlbumInfo(){
		return bmAlbumBean;
	}

	public PlayMode getCurrPlayMode(){
		return  playModelManager.getCurrPlayMode();
	}

	public PlayState getCurrPlayStatu(){
		return mediaPlayer.isPlaying() ? PlayState.playing: PlayState.pause;
	}

	public PlayModelManager getPlayModelManager(){
		return playModelManager;
	}

	public int getDuration(){
		return  isPrepared ? mediaPlayer.getDuration():0;
	}
}
