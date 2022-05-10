package io.chaofan.sts.chaofanmod.powers;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class SparkPower extends AbstractPower {
    public final static String POWER_ID = makeId("power.Spark");
    private static final String[] DESCRIPTIONS = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).DESCRIPTIONS;

    public SparkPower(AbstractCreature owner, int amount) {
        this.ID = POWER_ID;
        this.name = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).NAME;
        this.type = PowerType.BUFF;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("accuracy");
        this.canGoNegative = true;
    }

    public void updateDescription() {
        this.description = String.format(DESCRIPTIONS[0], -this.amount);
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        AbstractPower strength = owner.getPower(StrengthPower.POWER_ID);
        if (strength != null) {
            flash();
            if (strength.amount > 0) {
                addToBot(new ReducePowerAction(owner, owner, StrengthPower.POWER_ID, -this.amount));
            }
        }
    }
}
