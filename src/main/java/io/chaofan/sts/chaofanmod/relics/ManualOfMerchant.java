package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;
import io.chaofan.sts.chaofanmod.actions.common.AnonymousAction;
import io.chaofan.sts.chaofanmod.actions.ManualOfMerchantAction;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class ManualOfMerchant extends CustomRelic {
    public static final String ID = makeId("relic.ManualOfMerchant");

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("relics/manual_of_merchant.png"));
    private static final Texture OUTLINE = TextureLoader.getTexture(getImagePath("relics/outline/manual_of_merchant.png"));

    public static final int GOLD_AMOUNT = 20;
    public static String uiDescription = "Choose";

    public ManualOfMerchant() {
        super(ID, IMG, OUTLINE, RelicTier.BOSS, LandingSound.FLAT);
        uiDescription = DESCRIPTIONS[1];
    }

    @Override
    public String getUpdatedDescription() {
        return String.format(DESCRIPTIONS[0], GOLD_AMOUNT);
    }

    @Override
    public void atBattleStart() {
        this.flash();
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));

        if (AbstractDungeon.player.gold < ManualOfMerchant.GOLD_AMOUNT) {
            this.stopPulse();
            this.grayscale = true;
            this.addToBot(new AnonymousAction(() -> AbstractDungeon.effectList.add(new ThoughtBubble(
                    AbstractDungeon.player.dialogX,
                    AbstractDungeon.player.dialogY,
                    3.0F,
                    String.format(DESCRIPTIONS[2], GOLD_AMOUNT),
                    true))));
        } else {
            this.addToBot(new ManualOfMerchantAction());
        }
    }

    @Override
    public void atTurnStartPostDraw() {
        if (this.pulse) {
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new GainEnergyAction(1));
        }
    }

    @Override
    public void onVictory() {
        this.grayscale = false;
        this.stopPulse();
    }
}
