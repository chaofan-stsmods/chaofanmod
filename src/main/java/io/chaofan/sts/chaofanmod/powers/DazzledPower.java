package io.chaofan.sts.chaofanmod.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class DazzledPower extends AbstractPower {
    public final static String POWER_ID = makeId("power.Dazzled");
    private static final String[] DESCRIPTIONS = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).DESCRIPTIONS;
    private final int initialAmount;

    public DazzledPower(AbstractCreature owner, int amount) {
        this.ID = POWER_ID;
        this.name = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).NAME;
        this.type = PowerType.DEBUFF;
        this.owner = owner;
        this.amount = amount;
        this.initialAmount = amount;
        this.updateDescription();
        this.loadRegion("accuracy");
    }

    public void updateDescription() {
        this.description = this.amount > 0 ?
                String.format(this.amount > 1 ? DESCRIPTIONS[0] : DESCRIPTIONS[1], this.amount, this.initialAmount) :
                String.format(DESCRIPTIONS[2], this.initialAmount);
    }

    @Override
    public void onPlayCard(AbstractCard card, AbstractMonster m) {
        if (this.amount == 0) {
            if (!card.exhaust && card.type != AbstractCard.CardType.POWER) {
                flash();
                card.exhaustOnUseOnce = true;
                this.stackPower(initialAmount);
                updateDescription();
            }
        } else {
            flash();
            this.reducePower(1);
            updateDescription();
        }
    }
}
