package com.steven.player.audiocache.file;


import com.steven.player.audiocache.ProxyCacheUtils;

/**
 * Implementation of {@link FileNameGenerator} that uses MD5 of url as file name
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class Md5FileNameGenerator implements FileNameGenerator {

    @Override
    public String generate(String url) {
        return ProxyCacheUtils.computeMD5(url);
    }
}
