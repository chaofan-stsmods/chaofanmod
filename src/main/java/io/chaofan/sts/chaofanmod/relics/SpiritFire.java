package io.chaofan.sts.chaofanmod.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import io.chaofan.sts.chaofanmod.monsters.SpiritFireMonster;

import java.util.ArrayList;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class SpiritFire extends CustomRelic {
    public static boolean isInCampfireCombat = false;
    public static final String ID = makeId("relic.SpiritFire");

    private static final Texture COMBAT = ImageMaster.loadImage(getImagePath("ui/combat.png"));

    public SpiritFire() {
        super(ID, "burningBlood.png", RelicTier.SPECIAL, LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    @Override
    public boolean canUseCampfireOption(AbstractCampfireOption option) {
        if (option instanceof CombatOption) {
            return AbstractDungeon.actNum < 4;
        }

        return super.canUseCampfireOption(option);
    }

    @Override
    public void addCampfireOption(ArrayList<AbstractCampfireOption> options) {
        isInCampfireCombat = false;
        options.add(new CombatOption());
    }

    public class CombatOption extends AbstractCampfireOption {
        public CombatOption() {
            this.label = DESCRIPTIONS[1];
            this.description = DESCRIPTIONS[2];
            this.img = COMBAT;
        }

        public void useOption() {
            isInCampfireCombat = true;
            AbstractDungeon.getCurrRoom().monsters = MonsterHelper.getEncounter(SpiritFireMonster.ID);
            AbstractDungeon.lastCombatMetricKey = SpiritFireMonster.ID;
            AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMBAT;
            AbstractDungeon.getCurrRoom().monsters.init();
            AbstractRoom.waitTimer = 0.1F;
            AbstractDungeon.player.preBattlePrep();
        }
    }
}
