package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import io.chaofan.sts.chaofanmod.crossover.downfall.DownfallHelper;
import io.chaofan.sts.chaofanmod.mods.SummarizedMap;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class ZMap extends CustomRelic {
    public static final String ID = makeId("relic.ZMap");

    private static final Texture IMG = TextureLoader.getTexture(getImagePath("relics/zmap.png"));
    private static final Texture OUTLINE = TextureLoader.getTexture(getImagePath("relics/outline/zmap.png"));

    public ZMap() {
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
    public boolean canSpawn() {
        return !ModHelper.isModEnabled(SummarizedMap.ID) &&
                (!Loader.isModLoaded("downfall") || !DownfallHelper.isDownfallMap());
    }
}
