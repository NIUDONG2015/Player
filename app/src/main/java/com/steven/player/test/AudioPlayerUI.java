package com.steven.player.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.steven.player.R;
import com.steven.player.audioplayer.PlayState;
import com.steven.player.audioplayer.IPlayerOpToUI;
import com.steven.player.audioplayer.MediaPlayerBindManager;
import com.steven.player.audioplayer.bean.AlbumBean;
import com.steven.player.audioplayer.playmodel.PlayMode;
import com.steven.player.audioplayer.playmodel.SingleLoopPlayMode;

import java.util.ArrayList;

/**
 * Created by vint on 2016/10/21.
 */
public class AudioPlayerUI extends Activity implements IPlayerOpToUI, AdapterView.OnItemClickListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener{

    private final String TAG = AudioPlayerUI.class.getName();
    private SeekBar mProgressBar;
    private TextView mCurTimeTV;
    private TextView mTotalTimeTV;
    private ImageView mPlayModeIV;
    private ImageView mPlayPrevIV;
    private ImageView mPlayNextIV;
    private CheckBox mPlayCB;
    private AlbumInfo mAlbumInfo;
    private AlbumInfo mInputAlbumInfo;
    private ListView mSongList;
    private SongAdapter songAdapter;
    private int currBuffProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_player_ui);
        initView();
        initData();
    }

    private void initView() {
        mProgressBar = (SeekBar) findViewById(R.id.timeline);
        mCurTimeTV = (TextView) findViewById(R.id.cur_time);
        mTotalTimeTV = (TextView) findViewById(R.id.total_time);
        mPlayModeIV = (ImageView) findViewById(R.id.play_mode);
        mPlayPrevIV = (ImageView) findViewById(R.id.play_prev);
        mPlayNextIV = (ImageView) findViewById(R.id.play_next);
        mPlayCB = (CheckBox) findViewById(R.id.play);
        mPlayCB.setOnClickListener(this);
        mSongList = (ListView) findViewById(R.id.song_list);
        songAdapter = new SongAdapter(this, null);
        mSongList.setAdapter(songAdapter);
        mSongList.setOnItemClickListener(this);
        mProgressBar.setOnSeekBarChangeListener(this);
        mPlayPrevIV.setOnClickListener(this);
        mPlayNextIV.setOnClickListener(this);
        mPlayModeIV.setOnClickListener(this);
    }

    private void initData(){
        mInputAlbumInfo = (AlbumInfo) getIntent().getSerializableExtra("bmAlbumBean");
        mInputAlbumInfo = new AlbumInfo();
        mInputAlbumInfo.albumInfoId = 1;
        mInputAlbumInfo.albumName = "专辑名";
        mInputAlbumInfo.albumCoverUrl = "http://img2.niushe.com/upload/201304/19/14-22-31-71-26144.jpg";
        mInputAlbumInfo.curPlayIndex = 0;
        mInputAlbumInfo.mediaList = new ArrayList<>();
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.mediaId = 2;
        mediaInfo.mediaName = "三字经";
        mediaInfo.mediaUrl = "http://s.bemetoy.com/dance/8c/8c5df2b195b020f0fde24550f68dda82.mp3";
        mInputAlbumInfo.mediaList.add(mediaInfo);
        MediaPlayerBindManager.getInstance(this).registerListen(this);
    }

    @Override
    public void onPlayingSound(int currSoundIndex) {
        if (null != mAlbumInfo) {
            songAdapter.updateCurrPlayingIndex(currSoundIndex);
            songAdapter.notifyDataSetChanged();
            Log.d(TAG, "mAlbumInfo: " + mAlbumInfo + ", index:" + currSoundIndex);
        }
    }

    @Override
    public void onPlayState(PlayState state, int currProgress) {
        if (null != state) {
            if (PlayState.playing == state) {
                mPlayCB.setChecked(true);
            } else {
                mPlayCB.setChecked(false);
            }
        }
        mProgressBar.setProgress(currProgress);
        String curTime = getStrDuration(currProgress);
        mCurTimeTV.setText(curTime);
    }

    @Override
    public void onBufferProgress(int currBuffProgress) {
        this.currBuffProgress = currBuffProgress;
        mProgressBar.setSecondaryProgress(currBuffProgress);
    }

    @Override
    public void onPlayMode(PlayMode playMode) {
        if (null != playMode && (playMode instanceof SingleLoopPlayMode)) {
            Toast.makeText(this, "单曲循环", Toast.LENGTH_SHORT).show();
            mPlayModeIV.setImageResource(R.drawable.play_mode_loop_one);
        } else {
            Toast.makeText(this, "列表循环", Toast.LENGTH_SHORT).show();
            mPlayModeIV.setImageResource(R.drawable.play_mode_loop_all);
        }
    }

    @Override
    public void onPlayingList(AlbumBean bmAlbumBean) {
        mAlbumInfo = (AlbumInfo) bmAlbumBean;
        if (null != mAlbumInfo) {
            songAdapter.updateCurrPlayingIndex(bmAlbumBean.curPlayIndex);
            songAdapter.updateCurrPlaySongs(bmAlbumBean.mediaList);
            songAdapter.notifyDataSetChanged();
            Log.d(TAG, "mAlbumInfo: " + mAlbumInfo + ", index:" + bmAlbumBean.curPlayIndex);
        }
    }

    @Override
    public void onPlayDuration(int duration) {
        if(duration > 0) mProgressBar.setMax(duration);
        String totalDuration = getStrDuration(duration);
        mTotalTimeTV.setText(totalDuration);
    }

    @Override
    public void onPlayError(String errMsg) {
        Log.e(TAG, "onPlayError:"+errMsg);
    }

    @Override
    public void onServiceConnected() {
        if (null != mInputAlbumInfo) {
            MediaPlayerBindManager.getInstance(this).setCurrSoundListInfo(mInputAlbumInfo);
            MediaPlayerBindManager.getInstance(this).playSound(mInputAlbumInfo.curPlayIndex);
            mInputAlbumInfo = null;
        }
    }

    @Override
    public void onServiceDisconnected() {
    }

    public static String getStrDuration(long duration) {
        if (0 > duration) {
            return "00:00";
        }
        int min = (int) (duration / 1000 / 60);
        int sec = (int) (duration / 1000 % 60);
        StringBuilder builder = new StringBuilder();
        if (9 >= min) {
            builder.append("0");
        }
        builder.append(min + ":");
        if (9 >= sec) {
            builder.append("0");
        }
        builder.append(sec + "");

        return builder.toString();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MediaPlayerBindManager.getInstance(this).playSound(position);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if(progress > currBuffProgress){
            seekBar.setProgress(currBuffProgress);
            MediaPlayerBindManager.getInstance(this).seekProgress(currBuffProgress);
        }else{
            MediaPlayerBindManager.getInstance(this).seekProgress(progress);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.play_prev:
                MediaPlayerBindManager.getInstance(this).playPrev();
                break;
            case R.id.play_next:
                MediaPlayerBindManager.getInstance(this).playNext();
                break;
            case R.id.play_mode:
                MediaPlayerBindManager.getInstance(this).changePlayMode();
                break;
            case R.id.play:
                if(mPlayCB.isChecked()){
                    MediaPlayerBindManager.getInstance(this).playOrPause(PlayState.playing);
                }else{
                    MediaPlayerBindManager.getInstance(this).playOrPause(PlayState.pause);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaPlayerBindManager.getInstance(this).unregisterListen(this);
        songAdapter = null;
    }
}
