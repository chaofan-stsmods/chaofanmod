package io.chaofan.sts.chaofanmod.patches;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.localization.RunModStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.custom.CustomMod;
import com.megacrit.cardcrawl.screens.custom.CustomModeScreen;
import com.megacrit.cardcrawl.trials.CustomTrial;
import io.chaofan.sts.chaofanmod.mods.Lonely;
import javassist.CtBehavior;

import java.util.ArrayList;

public class LonelyRunModPatches {
    @SpirePatch(clz = CustomModeScreen.class, method = "initializeMods")
    public static class CustomModeScreenInitializePatch {
        @SpirePostfixPatch
        public static void Postfix(CustomModeScreen __instance) {
            ArrayList<CustomMod> modList = ReflectionHacks.getPrivate(__instance, CustomModeScreen.class, "modList");
            addMod(modList, Lonely.ID, "r", false);
        }

        private static CustomMod addMod(ArrayList<CustomMod> modList, String id, String color, boolean isDailyMod) {
            RunModStrings modString = CardCrawlGame.languagePack.getRunModString(id);
            if (modString != null) {
                CustomMod mod = new CustomMod(id, color, isDailyMod);
                modList.add(mod);
                return mod;
            }
            return null;
        }
    }

    @SpirePatch(clz = CustomModeScreen.class, method = "addNonDailyMods")
    public static class CustomModeScreenAddNonDailyModsPatch {
        @SpireInsertPatch(locator = Locator.class, localvars = {"modId"})
        public static void Insert(CustomModeScreen __instance, CustomTrial trial, ArrayList<String> modIds, String modId) {
            if (modId.equals(Lonely.ID)) {
                trial.addDailyMod(Lonely.ID);
                trial.setShouldKeepStarterRelic(false);
            }
        }

        public static class Locator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher.MethodCallMatcher matcher = new Matcher.MethodCallMatcher(String.class, "hashCode");
                return LineFinder.findInOrder(ctBehavior, matcher);
            }
        }
    }

    @SpirePatch(clz = AbstractRelic.class, method = "obtain", paramtypez = {})
    @SpirePatch(clz = AbstractRelic.class, method = "instantObtain", paramtypez = {})
    @SpirePatch(clz = AbstractRelic.class, method = "instantObtain", paramtypez = {AbstractPlayer.class, int.class, boolean.class})
    public static class AbstractRelicInstantObtainPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            if (ModHelper.isModEnabled(Lonely.ID)) {
                return SpireReturn.Return();
            }

            return SpireReturn.Continue();
        }
    }
}
