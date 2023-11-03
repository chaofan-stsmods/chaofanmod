package io.chaofan.sts.chaofanmod.crossover.downfall;

public class DownfallHelper {
    public static boolean isDownfallMap() {
        try {
            return DownfallHelperImpl.isDownfallMap();
        } catch (Throwable ex) {
            return true;
        }
    }
}
