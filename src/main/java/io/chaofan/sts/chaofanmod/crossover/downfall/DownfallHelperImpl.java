package io.chaofan.sts.chaofanmod.crossover.downfall;

import downfall.downfallMod;
import downfall.patches.EvilModeCharacterSelect;

public class DownfallHelperImpl {
    public static boolean isDownfallMap() {
        return EvilModeCharacterSelect.evilMode && !downfallMod.normalMapLayout;
    }
}
