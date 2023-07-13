package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class DrawCard extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 2, 5, 9, 12, 16, 20, 24, 28, 32, 36 };
    public boolean upgradeOnly;

    public DrawCard(FriendCard card) {
        super(card, scoreNeeded);
    }

    @Override
    protected int postApplyScore(int scoreIndex, Random random) {
        int limit = card.cost > 1 ? 2 : 5;
        while (scoreIndex > limit) {
            if (random.nextInt(7) < scoreIndex - limit) {
                scoreIndex--;
            } else {
                break;
            }
        }

        return scoreIndex;
    }

    @Override
    public String getDescription() {
        return localize("DrawCard {}", getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new DrawCardAction(getValueMayUpgrade()));
    }

    @Override
    public void upgrade() {
        if (upgradeOnly) {
            shouldShowDescription = true;
            shouldUse = true;
        }
    }

    @Override
    public FriendCardProperty makeAlternateProperty(Random random) {
        if (random.nextInt(3) == 0) {
            return new DrawCardNextTurn(card);
        } else {
            return this;
        }
    }
}
