package io.chaofan.sts.chaofanmod.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import io.chaofan.sts.chaofanmod.actions.common.AnonymousAction;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class FlammablePower extends AbstractPower {
    public final static String POWER_ID = makeId("power.Flammable");
    private static final String[] DESCRIPTIONS = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).DESCRIPTIONS;

    public FlammablePower(AbstractCreature owner, int amount) {
        this.ID = POWER_ID;
        this.name = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).NAME;
        this.type = PowerType.DEBUFF;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("accuracy");
    }

    public void updateDescription() {
        this.description = String.format(DESCRIPTIONS[0], this.amount);
    }

    @Override
    public void onCardDraw(AbstractCard card) {
        if (card.cardID.equals(Burn.ID)) {
            addToBot(new AnonymousAction(() -> {
                if (!owner.hasPower(VulnerablePower.POWER_ID)) {
                    flash();
                    addToTop(new ApplyPowerAction(owner, owner, new VulnerablePower(owner, amount, false), amount));
                }
            }));
        }
    }
}
