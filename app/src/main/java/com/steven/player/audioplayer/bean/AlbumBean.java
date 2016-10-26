package com.steven.player.audioplayer.bean;

import java.io.Serializable;
import java.util.List;

/**
 * 播放器所使用的专辑bean
 */
public abstract class AlbumBean<T extends MediaBean> implements Serializable {
    /**当前播放的歌曲索引**/
    public int curPlayIndex;
    /**歌曲信息**/
    public List<T> mediaList;


}
