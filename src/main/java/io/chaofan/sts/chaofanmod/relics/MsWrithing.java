package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import io.chaofan.sts.chaofanmod.actions.common.AnonymousAction;
import io.chaofan.sts.chaofanmod.patches.MsWrithingPatches;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import java.util.List;
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
            AbstractPlayer player = AbstractDungeon.player;
            List<AbstractRelic> enabledRelics = player.relics.stream()
                    .filter(r -> !MsWrithingPatches.Fields.disabled.get(r))
                    .collect(Collectors.toList());
            if (enabledRelics.size() > 0) {
                AbstractRelic relic = enabledRelics.get(AbstractDungeon.cardRandomRng.random(enabledRelics.size() - 1));
                MsWrithingPatches.Fields.disabled.set(relic, true);
                MsWrithingPatches.Fields.disabledProgress.set(relic, DISABLE_RELIC_DURATION);
                PowerTip tip = new PowerTip(DESCRIPTIONS[1], DESCRIPTIONS[2]);
                MsWrithingPatches.Fields.disabledTooltip.set(relic, tip);
                relic.tips.add(tip);
                if (relic instanceof MsWrithingPatches.DisableRelic) {
                    ((MsWrithingPatches.DisableRelic) relic).disableByMsWrithing();
                }
                player.hand.applyPowers();
            }
        }));
    }
}
