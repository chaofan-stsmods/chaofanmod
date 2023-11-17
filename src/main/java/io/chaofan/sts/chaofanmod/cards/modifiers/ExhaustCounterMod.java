package io.chaofan.sts.chaofanmod.cards.modifiers;

import basemod.abstracts.AbstractCardModifier;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import io.chaofan.sts.chaofanmod.actions.common.AnonymousAction;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class ExhaustCounterMod extends AbstractCardModifier {
    public static final String ID = makeId("ExhaustCounterMod");
    private int amount;

    public ExhaustCounterMod(int amount) {
        this.amount = amount;
    }

    @Override
    public void onInitialApplication(AbstractCard card) {
        if (amount == 0) {
            card.exhaust = true;
        }
    }

    @Override
    public void onUse(AbstractCard card, AbstractCreature target, UseCardAction action) {
        if (card.exhaust) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("chaofanmod:WoodSmash"));
        }
        AbstractDungeon.actionManager.addToBottom(new AnonymousAction(() -> {
            this.amount--;
            if (this.amount <= 0) {
                this.amount = 0;
                card.exhaust = true;
            }
            card.initializeDescription();
        }));
    }

    @Override
    public String modifyDescription(String rawDescription, AbstractCard card) {
        CardStrings exhaustCounterMod = CardCrawlGame.languagePack.getCardStrings(ID);
        if (amount == 0) {
            return rawDescription + exhaustCounterMod.EXTENDED_DESCRIPTION[0];
        } else {
            return rawDescription + String.format(exhaustCounterMod.DESCRIPTION, amount + 1);
        }
    }

    @Override
    public AbstractCardModifier makeCopy() {
        return new ExhaustCounterMod(amount);
    }

    @Override
    public String identifier(AbstractCard card) {
        return ID;
    }
}
