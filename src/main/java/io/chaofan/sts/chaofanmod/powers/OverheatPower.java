package io.chaofan.sts.chaofanmod.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.GainStrengthPower;
import com.megacrit.cardcrawl.powers.LoseStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class OverheatPower extends AbstractPower {
    public final static String POWER_ID = makeId("power.Overheat");
    private static final String[] DESCRIPTIONS = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).DESCRIPTIONS;
    private final int maxAmount;

    public OverheatPower(AbstractCreature owner, int maxAmount) {
        this.ID = POWER_ID;
        this.name = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).NAME;
        this.type = PowerType.DEBUFF;
        this.owner = owner;
        this.amount = maxAmount;
        this.maxAmount = maxAmount;
        this.updateDescription();
        this.loadRegion("accuracy");
    }

    public void updateDescription() {
        this.description = String.format(DESCRIPTIONS[0], amount, this.maxAmount);
    }

    @Override
    public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target) {
        if (target == owner) {
            return;
        }

        fontScale = 8.0F;
        amount -= damageAmount;
        if (amount <= 0) {
            flash();
            int count = (8 - amount) / maxAmount;
            amount += maxAmount * count;
            this.addToBot(new ApplyPowerAction(owner, owner, new StrengthPower(owner, -count), -count));
            this.addToBot(new ApplyPowerAction(owner, owner, new GainStrengthPower(this.owner, count), count));
            this.addToBot(new ApplyPowerAction(target, owner, new StrengthPower(target, count), count));
            this.addToBot(new ApplyPowerAction(target, owner, new LoseStrengthPower(target, count), count));
        }

        updateDescription();
    }
}
