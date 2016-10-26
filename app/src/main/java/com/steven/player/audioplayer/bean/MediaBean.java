package com.steven.player.audioplayer.bean;


import java.io.Serializable;

/**
 * 歌曲实例，此bean用于播放器播放
 * Created by vint on 2016/10/21.
 */
public abstract class MediaBean implements Serializable {
    /**歌曲链接**/
    public String mediaUrl;
}
