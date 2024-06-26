package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import io.chaofan.sts.chaofanmod.actions.common.AnonymousAction;
import io.chaofan.sts.chaofanmod.actions.common.SelectFromRewardAction;
import io.chaofan.sts.chaofanmod.cards.MsWrithingOptionCard;
import io.chaofan.sts.chaofanmod.patches.MsWrithingPatches;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import java.util.*;
import java.util.stream.Collectors;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class MsWrithing extends CustomRelic {
    public static final String ID = makeId("relic.MsWrithing");

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("relics/ms_writhing.png"));
    private static final Texture OUTLINE = TextureLoader.getTexture(getImagePath("relics/outline/ms_writhing.png"));

    public static final Texture DISABLED_RELIC = TextureLoader.getTexture(getImagePath("relics/disabled_relic.png"));

    public static final float DISABLE_RELIC_DURATION = 0.5f;

    public MsWrithing() {
        super(ID, IMG, OUTLINE, RelicTier.BOSS, LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    public void onEquip() {
        ++AbstractDungeon.player.energy.energyMaster;
    }

    public void onUnequip() {
        --AbstractDungeon.player.energy.energyMaster;
    }

    @Override
    public void atTurnStartPostDraw() {
        flash();
        addToBot(new AnonymousAction(() -> {
            List<AbstractRelic> enabledRelics = AbstractDungeon.player.relics.stream()
                    .filter(r -> !MsWrithingPatches.Fields.disabled.get(r))
                    .collect(Collectors.toList());
            if (!enabledRelics.isEmpty()) {
                Collections.shuffle(enabledRelics, new Random(AbstractDungeon.cardRandomRng.randomLong()));
                ArrayList<AbstractCard> cards = enabledRelics.stream()
                        .limit(3)
                        .map(MsWrithingOptionCard::new)
                        .collect(Collectors.toCollection(ArrayList::new));

                if (cards.size() > 1) {
                    addToTop(new SelectFromRewardAction(
                            cards,
                            c -> c.ifPresent(this::afterSelect),
                            DESCRIPTIONS[3],
                            false,
                            AbstractGameAction.ActionType.POWER));
                } else {
                    afterSelect(cards.get(0));
                }
            }
        }));
    }

    private void afterSelect(AbstractCard abstractCard) {
        MsWrithingOptionCard card = (MsWrithingOptionCard) abstractCard;
        AbstractRelic relic = card.relic;
        if (relic instanceof MsWrithingPatches.DisableRelic) {
            ((MsWrithingPatches.DisableRelic) relic).disableByMsWrithing();
        }
        MsWrithingPatches.Fields.disabled.set(relic, true);
        MsWrithingPatches.Fields.disabledProgress.set(relic, DISABLE_RELIC_DURATION);
        PowerTip tip = new PowerTip(DESCRIPTIONS[1], DESCRIPTIONS[2]);
        MsWrithingPatches.Fields.disabledTooltip.set(relic, tip);
        relic.tips.add(tip);
        AbstractDungeon.player.hand.applyPowers();
    }

    @Override
    public boolean canSpawn() {
        return AbstractDungeon.player.relics.size() >= 4;
    }
}
