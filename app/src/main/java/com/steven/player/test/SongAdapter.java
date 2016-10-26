package com.steven.player.test;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.steven.player.R;

import java.util.List;

/**
 * Created by Stevenqiu on 2016/10/24.
 */

public class SongAdapter extends BaseAdapter {


    class ViewHolder {
        TextView mNameTV;
        LinearLayout mItemLL;
    }

    private List<MediaInfo> mSongs;
    private Context context;
    private int mCurPlayingIndex;

    public SongAdapter(Context context, List<MediaInfo> songs){
        this.mSongs = songs;
        this.context = context;
    }

    @Override
    public int getCount() {
        return (null != mSongs?mSongs.size():0);
    }

    @Override
    public Object getItem(int position) {
        return mSongs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewholder = null;
        if (null == convertView) {
            convertView = LayoutInflater.from(context).inflate(R.layout.song_item, null);
            viewholder = new ViewHolder();
            viewholder.mNameTV = (TextView) convertView.findViewById(R.id.song_name);
            viewholder.mItemLL = (LinearLayout) convertView.findViewById(R.id.song_item_ll);
            convertView.setTag(viewholder);
        } else {
            viewholder = (ViewHolder) convertView.getTag();
        }

        MediaInfo song = (MediaInfo) getItem(position);
        if (null == song) {
            return convertView;
        }
        viewholder.mNameTV.setText(song.mediaName);
        if (position == mCurPlayingIndex) {
            viewholder.mItemLL.setSelected(true);
        } else {
            viewholder.mItemLL.setSelected(false);
        }

        return convertView;
    }

    public void updateCurrPlayingIndex(int currPlayingIndex){
        this.mCurPlayingIndex = currPlayingIndex;
    }

    public void updateCurrPlaySongs(List<MediaInfo> songs){
        this.mSongs = songs;
    }
}

