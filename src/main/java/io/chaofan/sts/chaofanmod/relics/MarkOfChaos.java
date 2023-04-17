package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class MarkOfChaos extends CustomRelic {
    public static final String ID = makeId("relic.MarkOfChaos");

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("relics/mark_of_chaos.png"));
    private static final Texture OUTLINE = TextureLoader.getTexture(getImagePath("relics/outline/mark_of_chaos.png"));

    public static String eventOptionPrefix = "[Locked] ";

    public MarkOfChaos() {
        super(ID, IMG, OUTLINE, RelicTier.BOSS, LandingSound.MAGICAL);
        eventOptionPrefix = DESCRIPTIONS[1];
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
}
