package io.chaofan.sts.pseudoconfusion;

import basemod.*;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import io.chaofan.sts.pseudoconfusion.patches.ConfusionPatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@SpireInitializer
public class PseudoConfusionMod implements PostInitializeSubscriber, OnStartBattleSubscriber {
    public static void initialize() {
        BaseMod.subscribe(new PseudoConfusionMod());
    }

    public static final String MOD_ID = "pseudoconfusion";
    public static final Logger logger = LogManager.getLogger(PseudoConfusionMod.class.getName());
    private static final String DUPLICATE_COUNT = "duplicateCount";
    private static final String REMAINING_COUNT = "remainingCount";
    private static SpireConfig config;

    @Override
    public void receivePostInitialize() {
        Texture badgeTexture = ImageMaster.loadImage("pseudoconfusion/images/badge.png");
        BaseMod.registerModBadge(badgeTexture, "Pseudo Confusion", "Chaofan", "", initSettings());
    }

    @Override
    public void receiveOnBattleStart(AbstractRoom abstractRoom) {
        ConfusionPatch.pool.clear();
    }

    private ModPanel initSettings() {
        if (config == null) {
            config = tryCreateConfig();
        }

        if (config != null) {
            ConfusionPatch.duplicateCount = config.has(DUPLICATE_COUNT) ? config.getInt(DUPLICATE_COUNT) : 3;
            ConfusionPatch.remainingCount = config.has(REMAINING_COUNT) ? config.getInt(REMAINING_COUNT) : 2;
        }

        ModPanel modPanel = new ModPanel();

        Gson gson = new Gson();
        String json = Gdx.files.internal(getLocalizationFilePath(MOD_ID, "config.json")).readString(String.valueOf(StandardCharsets.UTF_8));
        Type configType = (new TypeToken<Map<String, String>>() {}).getType();
        Map<String, String> configStrings = gson.fromJson(json, configType);

        float yPos = 750f;
        float maxValue = 20;

        ModLabel duplicateCountLabel = new ModLabel(
                configStrings.get(DUPLICATE_COUNT),
                400.0f,
                yPos,
                modPanel,
                (label) -> {
                });
        yPos -= 50f;
        ModSlider duplicateCountSlider = new ModSlider(
                "",
                400.0f,
                yPos,
                maxValue,
                "",
                modPanel,
                (slider) -> {
                    ConfusionPatch.duplicateCount = Math.max(1, Math.round(slider.value * maxValue));
                    slider.value = ConfusionPatch.duplicateCount / maxValue;
                    if (config != null) {
                        config.setInt(DUPLICATE_COUNT, ConfusionPatch.duplicateCount);
                        trySaveConfig(config);
                    }
                });

        float val = ConfusionPatch.duplicateCount / maxValue;
        if (val > 1) val = 1;
        if (val < 0) val = 0;
        duplicateCountSlider.setValue(val);

        yPos -= 50f;
        ModLabel remainingCountLabel = new ModLabel(
                configStrings.get(REMAINING_COUNT),
                400.0f,
                yPos,
                modPanel,
                (label) -> {
                });
        yPos -= 50f;
        ModSlider remainingCountSlider = new ModSlider(
                "",
                400.0f,
                yPos,
                maxValue,
                "",
                modPanel,
                (slider) -> {
                    ConfusionPatch.remainingCount = Math.round(slider.value * maxValue);
                    if (config != null) {
                        config.setInt(REMAINING_COUNT, ConfusionPatch.remainingCount);
                        trySaveConfig(config);
                    }
                });

        val = ConfusionPatch.remainingCount / maxValue;
        if (val > 1) val = 1;
        if (val < 0) val = 0;
        remainingCountSlider.setValue(val);

        modPanel.addUIElement(duplicateCountLabel);
        modPanel.addUIElement(duplicateCountSlider);
        modPanel.addUIElement(remainingCountLabel);
        modPanel.addUIElement(remainingCountSlider);

        return modPanel;
    }

    private static SpireConfig tryCreateConfig() {
        String configFileName = MOD_ID + "config";
        try {
            return new SpireConfig(MOD_ID, configFileName);
        } catch (IOException e) {
            logger.warn(e);
            return null;
        }
    }

    private static void trySaveConfig(SpireConfig config) {
        try {
            config.save();
        } catch (IOException e) {
            logger.warn(e);
        }
    }

    private static String getLocalizationPath(String prefix, String file) {
        return prefix + "/localization/" + file;
    }

    private static String getLocalizationFilePath(String prefix, String file) {
        String language = Settings.language.toString().toLowerCase();
        logger.info("getLocalizationFilePath - file=" + file + ", language=" + language);

        String path = getLocalizationPath(prefix, language + "/" + file);
        URL url = PseudoConfusionMod.class.getResource("/" + path);
        if (url != null) {
            return path;
        } else {
            return getLocalizationPath(prefix, "eng/" + file);
        }
    }
}
