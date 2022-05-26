package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import basemod.helpers.CardModifierManager;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import io.chaofan.sts.chaofanmod.cards.modifiers.StrikeMod;
import io.chaofan.sts.chaofanmod.patches.ScreenFilterPatches;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class OldPhone extends CustomRelic {
    public static final String ID = makeId("relic.OldPhone");

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("relics/old_phone.png"));
    private static final Texture OUTLINE = TextureLoader.getTexture(getImagePath("relics/outline/old_phone.png"));

    public OldPhone() {
        super(ID, IMG, OUTLINE, RelicTier.BOSS, LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    public void onEquip() {
        ++AbstractDungeon.player.energy.energyMaster;
        ScreenFilterPatches.enable = true;
    }

    public void onUnequip() {
        --AbstractDungeon.player.energy.energyMaster;
        ScreenFilterPatches.enable = false;
    }
}
