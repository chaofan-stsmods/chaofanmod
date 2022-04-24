package io.chaofan.sts.chaofanmod;

import basemod.AutoAdd;
import basemod.BaseMod;
import basemod.ModPanel;
import basemod.abstracts.CustomRelic;
import basemod.helpers.RelicType;
import basemod.interfaces.EditCardsSubscriber;
import basemod.interfaces.EditRelicsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import io.chaofan.sts.chaofanmod.cards.AhhMyEyes;
import io.chaofan.sts.chaofanmod.monsters.SpiritFireMonster;
import io.chaofan.sts.chaofanmod.relics.Stool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;

@SpireInitializer
public class ChaofanMod implements
        EditStringsSubscriber,
        EditRelicsSubscriber,
        EditCardsSubscriber,
        PostInitializeSubscriber {

    public static final String MOD_ID = "chaofanmod";
    public static final Logger logger = LogManager.getLogger(ChaofanMod.class.getName());

    public static String getImagePath(String file) {
        return MOD_ID + "/images/" + file;
    }

    public static String getLocalizationPath(String file) {
        return MOD_ID + "/localization/" + file;
    }

    public static String makeId(String id) {
        return MOD_ID + ":" + id;
    }

    public static String invertId(String id) {
        return id.substring((MOD_ID + ":").length());
    }

    @SuppressWarnings("unused")
    public static void initialize() {
        logger.info("Initializing ChaofanMod");

        ChaofanMod bladeGunnerMod = new ChaofanMod();
        BaseMod.subscribe(bladeGunnerMod);
    }

    @Override
    public void receivePostInitialize() {
        ModPanel settingsPanel = initSettings();

        Texture badgeTexture = ImageMaster.loadImage(MOD_ID + "/images/badge.png");
        BaseMod.registerModBadge(badgeTexture, "Better CN Font", "Chaofan", "", settingsPanel);

        BaseMod.addMonster(SpiritFireMonster.ID, () -> new MonsterGroup(new SpiritFireMonster()));
    }

    private ModPanel initSettings() {
        return new ModPanel();
    }

    @Override
    public void receiveEditCards() {
        new AutoAdd(MOD_ID)
                .packageFilter(AhhMyEyes.class)
                .cards();
    }

    @Override
    public void receiveEditRelics() {

        new AutoAdd(MOD_ID)
                .packageFilter(Stool.class)
                .any(CustomRelic.class, (info, relic) -> BaseMod.addRelic(relic, RelicType.SHARED));
    }

    @Override
    public void receiveEditStrings() {
        loadCustomStringsFile(RelicStrings.class, "relics.json");
        loadCustomStringsFile(CardStrings.class, "cards.json");
        loadCustomStringsFile(MonsterStrings.class, "monsters.json");
        loadCustomStringsFile(PowerStrings.class, "powers.json");
    }

    private static String getLocalizationFilePath(String file) {
        String language = Settings.language.toString().toLowerCase();
        logger.info("getLocalizationFilePath - file=" + file + ", language=" + language);

        String path = getLocalizationPath(language + "/" + file);
        URL url = ChaofanMod.class.getResource("/" + path);
        if (url != null) {
            return path;
        } else {
            return getLocalizationPath("eng/" + file);
        }
    }

    private static void loadCustomStringsFile(Class<?> stringType, String file) {
        BaseMod.loadCustomStringsFile(stringType, getLocalizationFilePath(file));
    }
}
