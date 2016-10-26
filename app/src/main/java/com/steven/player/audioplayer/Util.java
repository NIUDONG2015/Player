package com.steven.player.audioplayer;

import java.util.Collection;

/**
 * Created by Stevenqiu on 2016/10/26.
 */

public class Util {
    public static boolean isNull(Object aObject) {
        return null == aObject;
    }

    public static boolean isNullOrNil(final String object) {
        if ((object == null) || (object.length() <= 0)) {
            return true;
        }
        return false;
    }

    public static boolean isNullOrNil(final byte[] object) {
        if ((object == null) || (object.length <= 0)) {
            return true;
        }
        return false;
    }

    public static boolean isNullOrNil(Object[] object) {
        if ((object == null) || (object.length <= 0)) {
            return true;
        }
        return false;
    }

    public static boolean isNullOrNil(final Collection object) {
        if ((object == null) || (object.size() <= 0)) {
            return true;
        }
        return false;
    }
}
