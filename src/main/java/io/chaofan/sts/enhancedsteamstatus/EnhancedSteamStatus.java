package io.chaofan.sts.enhancedsteamstatus;

import basemod.BaseMod;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PreStartGameSubscriber;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.*;
import io.chaofan.sts.CommonModUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EnhancedSteamStatus implements
        EditStringsSubscriber,
        PostInitializeSubscriber,
        PreStartGameSubscriber {

    private static final String MOD_ID = "enhancedsteamstatus";
    public static DetailedStatus status = DetailedStatus.STARTING;
    public static final Logger logger = LogManager.getLogger(EnhancedSteamStatus.class.getName());
    public static String[] statusText;

    public static String makeId(String id) {
        return MOD_ID + ":" + id;
    }

    public static void initialize() {
        EnhancedSteamStatus ess = new EnhancedSteamStatus();
        BaseMod.subscribe(ess);
    }

    @Override
    public void receiveEditStrings() {
        CommonModUtils.loadCustomStringsFile(MOD_ID, UIStrings.class, "ui.json");
    }

    @Override
    public void receivePostInitialize() {
        statusText = CardCrawlGame.languagePack.getUIString(makeId("Status")).TEXT;
    }

    @Override
    public void receivePreStartGame() {
        status = DetailedStatus.STARTING;
    }
    
    public static void refreshText() {
        if (AbstractDungeon.player == null) {
            return;
        }
        if (AbstractDungeon.isAscensionMode) {
            CardCrawlGame.publisherIntegration.setRichPresenceDisplayPlaying(AbstractDungeon.floorNum, AbstractDungeon.ascensionLevel, AbstractDungeon.player.getLocalizedCharacterName());
        } else {
            CardCrawlGame.publisherIntegration.setRichPresenceDisplayPlaying(AbstractDungeon.floorNum, AbstractDungeon.player.getLocalizedCharacterName());
        }
    }
}
