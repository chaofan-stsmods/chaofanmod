package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DrawCardNextTurnPower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class DrawCardNextTurn extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 1, 4, 6, 10, 14 };

    public DrawCardNextTurn(FriendCard card) {
        super(card, scoreNeeded);
        alternateOf = DrawCard.class;
    }

    @Override
    public String getDescription() {
        return localize("DrawCardNextTurn {}", getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ApplyPowerAction(p, p, new DrawCardNextTurnPower(p, getValueMayUpgrade())));
    }
}
