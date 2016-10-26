package com.steven.player.audiocache;


import com.steven.player.audiocache.file.DiskUsage;
import com.steven.player.audiocache.file.FileNameGenerator;

import java.io.File;

/**
 * Configuration for proxy cache.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class Config {

    public final File cacheRoot;
    public final FileNameGenerator fileNameGenerator;
    public final DiskUsage diskUsage;

    Config(File cacheRoot, FileNameGenerator fileNameGenerator, DiskUsage diskUsage) {
        this.cacheRoot = cacheRoot;
        this.fileNameGenerator = fileNameGenerator;
        this.diskUsage = diskUsage;
    }

    File generateCacheFile(String url) {
        String name = fileNameGenerator.generate(url);
        return new File(cacheRoot, name);
    }

}
