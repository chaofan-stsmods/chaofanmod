package io.chaofan.sts.chaofanmod.utils;

import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class SteamworksAdapter {

    private static Boolean isVersion16 = null;

    public static boolean steamUtilsGetImageRGBA(SteamUtils steamUtils, int image, ByteBuffer dest, int length) {
        if (isVersion16 == null || isVersion16) {
            try {
                Method method = steamUtils.getClass().getMethod("getImageRGBA", int.class, ByteBuffer.class, int.class);
                return (boolean) method.invoke(steamUtils, image, dest, length);
            } catch (NoSuchMethodException ignored) {
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                isVersion16 = true;
            }
        }

        isVersion16 = false;

        try {
            Method method = steamUtils.getClass().getMethod("getImageRGBA", int.class, ByteBuffer.class);
            return (boolean) method.invoke(steamUtils, image, dest);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
}