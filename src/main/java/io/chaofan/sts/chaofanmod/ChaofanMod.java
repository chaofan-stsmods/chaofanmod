package io.chaofan.sts.chaofanmod;

import basemod.*;
import basemod.abstracts.CustomRelic;
import basemod.devcommands.ConsoleCommand;
import basemod.eventUtil.AddEventParams;
import basemod.helpers.RelicType;
import basemod.helpers.ScreenPostProcessorManager;
import basemod.interfaces.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.localization.*;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import io.chaofan.sts.CommonModUtils;
import io.chaofan.sts.chaofanmod.cards.*;
import io.chaofan.sts.chaofanmod.commands.ChaofanModCommand;
import io.chaofan.sts.chaofanmod.events.Gremlin2048;
import io.chaofan.sts.chaofanmod.events.GremlinMiner;
import io.chaofan.sts.chaofanmod.mods.Lonely;
import io.chaofan.sts.chaofanmod.mods.SummarizedMap;
import io.chaofan.sts.chaofanmod.monsters.SpiritFireMonster;
import io.chaofan.sts.chaofanmod.monsters.SpiritFireMonsterAct2;
import io.chaofan.sts.chaofanmod.monsters.SpiritFireMonsterAct3;
import io.chaofan.sts.chaofanmod.patches.ThirdPerspectiveViewPatches;
import io.chaofan.sts.chaofanmod.powers.AddFuelPower;
import io.chaofan.sts.chaofanmod.powers.HeavyHandPower;
import io.chaofan.sts.chaofanmod.relics.*;
import io.chaofan.sts.chaofanmod.rewards.HealReward;
import io.chaofan.sts.chaofanmod.rewards.RubyKeyReward;
import io.chaofan.sts.chaofanmod.ui.WheelSelectScreen;
import io.chaofan.sts.chaofanmod.utils.ChaofanModEnums;
import io.chaofan.sts.chaofanmod.utils.SteamworksHelper;
import io.chaofan.sts.chaofanmod.variables.SecondaryBlock;
import io.chaofan.sts.chaofanmod.variables.SecondaryDamage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.chaofan.sts.CommonModUtils.getLocalizationFilePath;

@SpireInitializer
public class ChaofanMod implements
        EditStringsSubscriber,
        EditRelicsSubscriber,
        EditCardsSubscriber,
        EditKeywordsSubscriber,
        AddAudioSubscriber,
        PostInitializeSubscriber,
        PostExhaustSubscriber,
        StartGameSubscriber,
        PostUpdateSubscriber {

    public static final String MOD_ID = "chaofanmod";
    public static final Logger logger = LogManager.getLogger(ChaofanMod.class.getName());
    public static Map<String, CommonModUtils.Keyword> keywords;
    private static SpireConfig config;

    public static final String USE_OLD_PHONE_V2 = "UseOldPhoneV2";
    public static final String DISABLE_TAUNT_MASK = "DisableTauntMask";
    public static final String DISABLE_MS_WRITHING = "DisableMsWrithing";
    public static boolean useOldPhoneV2 = true;
    public static boolean disableTauntMask = false;
    public static boolean disableMsWrithing = false;
    public static boolean loadoutEnabled = false;
    public static WheelSelectScreen wheelSelectScreen;

    public static SteamworksHelper steamworksHelper;

    public static String getImagePath(String file) {
        return MOD_ID + "/images/" + file;
    }

    public static String getAudioPath(String file) {
        return MOD_ID + "/audio/" + file;
    }

    public static String getShaderPath(String file) {
        return MOD_ID + "/shaders/" + file;
    }

    public static String makeId(String id) {
        return MOD_ID + ":" + id;
    }

    public static String invertId(String id) {
        return id.substring((MOD_ID + ":").length());
    }

    private static final List<ScreenPostProcessor> postProcessors = new ArrayList<>();

    @SuppressWarnings("unused")
    public static void initialize() {
        logger.info("Initializing ChaofanMod");

        ChaofanMod chaofanMod = new ChaofanMod();
        BaseMod.subscribe(chaofanMod);

        config = tryCreateConfig();
        if (config != null) {
            useOldPhoneV2 = !config.has(USE_OLD_PHONE_V2) || config.getBool(USE_OLD_PHONE_V2);
            disableTauntMask = config.has(DISABLE_TAUNT_MASK) && config.getBool(DISABLE_TAUNT_MASK);
            disableMsWrithing = config.has(DISABLE_MS_WRITHING) && config.getBool(DISABLE_MS_WRITHING);

            disableMsWrithing = disableMsWrithing || disableMsWrithingByOtherMod();
        }
    }

    @Override
    public void receivePostInitialize() {
        ModPanel settingsPanel = initSettings();

        Texture badgeTexture = ImageMaster.loadImage(MOD_ID + "/images/badge.png");
        BaseMod.registerModBadge(badgeTexture, "Chaofan Mod", "Chaofan", "", settingsPanel);

        BaseMod.addMonster(SpiritFireMonster.ID,
                CardCrawlGame.languagePack.getMonsterStrings(SpiritFireMonster.ID).NAME,
                () -> {
            if (AbstractDungeon.actNum == 3) {
                return new MonsterGroup(new SpiritFireMonsterAct3());
            } else if (AbstractDungeon.actNum == 2) {
                return new MonsterGroup(new SpiritFireMonsterAct2());
            } else {
                return new MonsterGroup(new SpiritFireMonster());
            }
        });

        BaseMod.registerCustomReward(ChaofanModEnums.CHAOFAN_MOD_HEAL, HealReward::load, HealReward::save);
        BaseMod.registerCustomReward(ChaofanModEnums.CHAOFAN_MOD_RUBY_KEY, RubyKeyReward::load, RubyKeyReward::save);

        HashMap<String, AbstractDailyMod> difficultyMods = ReflectionHacks.getPrivate(null, ModHelper.class, "difficultyMods");
        difficultyMods.put(Lonely.ID, new Lonely());
        difficultyMods.put(SummarizedMap.ID, new SummarizedMap());

        BaseMod.addEvent(Gremlin2048.ID, Gremlin2048.class);
        BaseMod.addEvent(new AddEventParams.Builder(GremlinMiner.ID, GremlinMiner.class)
                .bonusCondition(() -> AbstractDungeon.player.gold >= GremlinMiner.getPrice())
                .create());

        BaseMod.addPower(HeavyHandPower.class, HeavyHandPower.POWER_ID);

        BaseMod.addCustomScreen(new WheelSelectScreen());
        wheelSelectScreen = (WheelSelectScreen) BaseMod.getCustomScreen(WheelSelectScreen.Enum.WHEEL_SELECT_SCREEN);

        steamworksHelper = new SteamworksHelper();
        BaseMod.subscribe(steamworksHelper);
        ConsoleCommand.addCommand("chaofanmod", ChaofanModCommand.class);

        loadoutEnabled = Loader.isModLoadedOrSideloaded("loadout");
    }

    private ModPanel initSettings() {
        if (config == null) {
            config = tryCreateConfig();
        }

        if (config != null) {
            useOldPhoneV2 = !config.has(USE_OLD_PHONE_V2) || config.getBool(USE_OLD_PHONE_V2);
            disableTauntMask = config.has(DISABLE_TAUNT_MASK) && config.getBool(DISABLE_TAUNT_MASK);
            disableMsWrithing = config.has(DISABLE_MS_WRITHING) && config.getBool(DISABLE_MS_WRITHING);
        }

        ModPanel modPanel = new ModPanel();

        Gson gson = new Gson();
        String json = Gdx.files.internal(getLocalizationFilePath(MOD_ID, "config.json")).readString(String.valueOf(StandardCharsets.UTF_8));
        Type configType = (new TypeToken<Map<String, String>>() {}).getType();
        Map<String, String> configStrings = gson.fromJson(json, configType);

        float yPos = 750f;

        ModLabeledToggleButton useOldPhoneV2Button = new ModLabeledToggleButton(
                configStrings.get(USE_OLD_PHONE_V2),
                350.0f,
                yPos,
                Settings.CREAM_COLOR,
                FontHelper.charDescFont,
                useOldPhoneV2,
                modPanel,
                (label) -> {},
                (button) -> {
                    useOldPhoneV2 = button.enabled;
                    if (config != null) {
                        config.setBool(USE_OLD_PHONE_V2, useOldPhoneV2);
                        trySaveConfig(config);
                    }
                });

        yPos -= 50f;
        ModLabeledToggleButton disableTauntMaskButton = new ModLabeledToggleButton(
                configStrings.get(DISABLE_TAUNT_MASK),
                350.0f,
                yPos,
                Settings.CREAM_COLOR,
                FontHelper.charDescFont,
                disableTauntMask,
                modPanel,
                (label) -> {},
                (button) -> {
                    if (config != null) {
                        config.setBool(DISABLE_TAUNT_MASK, button.enabled);
                        trySaveConfig(config);
                    }
                });

        boolean disableMsWrithingByOtherMod = disableMsWrithingByOtherMod();
        yPos -= 50f;
        ModLabeledToggleButton disableMsWrithingButton = new ModLabeledToggleButton(
                configStrings.get(DISABLE_MS_WRITHING),
                disableMsWrithingByOtherMod ? String.format(configStrings.get(DISABLE_MS_WRITHING + ".conflict"), conflictModNames("testmod", "Downfall")) : null,
                350.0f,
                yPos,
                Settings.CREAM_COLOR,
                FontHelper.charDescFont,
                disableMsWrithing || disableMsWrithingByOtherMod,
                modPanel,
                (label) -> {},
                (button) -> {
                    if (disableMsWrithingByOtherMod) {
                        button.enabled = true;
                    } else if (config != null) {
                        config.setBool(DISABLE_MS_WRITHING, button.enabled);
                        trySaveConfig(config);
                    }
                });

        modPanel.addUIElement(useOldPhoneV2Button);
        modPanel.addUIElement(disableTauntMaskButton);
        modPanel.addUIElement(disableMsWrithingButton);

        return modPanel;
    }

    @Override
    public void receiveEditCards() {
        Set<String> excludeCards = new HashSet<>();
        excludeCards.add(AhhMyEyes.class.getName());
        excludeCards.add(CutGlassCard.class.getName());
        excludeCards.add(Huihun.class.getName());
        excludeCards.add(SampleEffectCard.class.getName());
        excludeCards.add(ProgressCard.class.getName());
        excludeCards.add(MsWrithingOptionCard.class.getName());
        new AutoAdd(MOD_ID)
                .packageFilter(AlphaBlue.class)
                .filter((classInfo, classFinder) -> !excludeCards.contains(classInfo.getClassName()))
                .cards();
        BaseMod.addDynamicVariable(new SecondaryBlock());
        BaseMod.addDynamicVariable(new SecondaryDamage());
    }

    @Override
    public void receiveEditRelics() {
        Set<String> excludeRelics = new HashSet<>();
        excludeRelics.add(ManholeCover.class.getName());
        excludeRelics.add(GoldenCube.class.getName());
        if (disableTauntMask) {
            excludeRelics.add(TauntMask.class.getName());
        }
        if (disableMsWrithing) {
            excludeRelics.add(MsWrithing.class.getName());
        }
        new AutoAdd(MOD_ID)
                .packageFilter(Stool.class)
                .filter((classInfo, classFinder) -> !excludeRelics.contains(classInfo.getClassName()))
                .any(CustomRelic.class, (info, relic) -> BaseMod.addRelic(relic, RelicType.SHARED));
    }

    @Override
    public void receiveEditStrings() {
        CommonModUtils.loadCustomStringsFile(MOD_ID, RelicStrings.class, "relics.json");
        CommonModUtils.loadCustomStringsFile(MOD_ID, CardStrings.class, "cards.json");
        CommonModUtils.loadCustomStringsFile(MOD_ID, MonsterStrings.class, "monsters.json");
        CommonModUtils.loadCustomStringsFile(MOD_ID, PowerStrings.class, "powers.json");
        CommonModUtils.loadCustomStringsFile(MOD_ID, OrbStrings.class, "orbs.json");
        CommonModUtils.loadCustomStringsFile(MOD_ID, RunModStrings.class, "run_mods.json");
        CommonModUtils.loadCustomStringsFile(MOD_ID, EventStrings.class, "events.json");
    }

    @Override
    public void receiveEditKeywords() {
        ChaofanMod.keywords = new HashMap<>();
        CommonModUtils.loadKeywordsFile(ChaofanMod.keywords);
    }

    @Override
    public void receiveAddAudio() {
        BaseMod.addAudio("chaofanmod:WoodSmash", getAudioPath("sound/wood_smash.ogg"));
    }

    @Override
    public void receivePostExhaust(AbstractCard abstractCard) {
        AddFuelPower.triggerExhaust(abstractCard);
    }

    @Override
    public void receiveStartGame() {
        clearPostProcessors();

        AbstractPlayer player = AbstractDungeon.player;
        for (AbstractRelic relic : player.relics) {
            if (relic.relicId.equals(OldPhone.ID)) {
                registerPostProcessor(OldPhone.createEffect(false));
            }
            if (relic.relicId.equals(SpotLight.ID)) {
                registerPostProcessor(new SpotLight.SpotLightPostProcessor());
            }
        }

        ThirdPerspectiveViewPatches.setEnable(false, SpiritFire.ID);

        for (AbstractCard card : player.masterDeck.group) {
            if (card instanceof SearingBlowFor2048) {
                SearingBlowFor2048 searingBlowFor2048 = (SearingBlowFor2048) card;
                searingBlowFor2048.setUpgrades(card.misc);
            }
        }
    }

    @Override
    public void receivePostUpdate() {
        if (CardCrawlGame.mode != CardCrawlGame.GameMode.GAMEPLAY && !postProcessors.isEmpty()) {
            clearPostProcessors();
        }
    }

    public static void registerPostProcessor(ScreenPostProcessor postProcessor) {
        postProcessors.add(postProcessor);
        ScreenPostProcessorManager.addPostProcessor(postProcessor);
    }

    public static void removePostProcessor(Class<? extends ScreenPostProcessor> processorClass) {
        for (Iterator<ScreenPostProcessor> iterator = postProcessors.iterator(); iterator.hasNext(); ) {
            ScreenPostProcessor postProcessor = iterator.next();
            if (processorClass.isAssignableFrom(postProcessor.getClass())) {
                ScreenPostProcessorManager.removePostProcessor(postProcessor);
                iterator.remove();
                break;
            }
        }
    }

    private void clearPostProcessors() {
        for (ScreenPostProcessor postProcessor : postProcessors) {
            ScreenPostProcessorManager.removePostProcessor(postProcessor);
        }
        postProcessors.clear();
    }

    public static SpireConfig tryCreateConfig() {
        String configFileName = MOD_ID + "config";
        try {
            return new SpireConfig(MOD_ID, configFileName);
        } catch (IOException e) {
            logger.warn(e);
            return null;
        }
    }

    public static boolean disableMsWrithingByOtherMod() {
        return Loader.isModLoadedOrSideloaded("testmod") || Loader.isModLoadedOrSideloaded("downfall");
    }

    private static String conflictModNames(String... modIds) {
        StringBuilder sb = new StringBuilder();
        for (String modId : modIds) {
            if (Loader.isModLoadedOrSideloaded(modId)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(modId);
            }
        }
        return sb.toString();
    }

    private static void trySaveConfig(SpireConfig config) {
        try {
            config.save();
        } catch (IOException e) {
            logger.warn(e);
        }
    }
}
