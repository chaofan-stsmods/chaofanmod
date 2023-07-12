package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.DiscardAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class Discard extends ScoreGainListProperty {
    private static final int[] scoreGain = { 0, 2, 4, 7, 12 };

    public Discard(FriendCard card) {
        super(card, scoreGain);
        isNegative = true;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        value = Math.max(1, Math.min(scoreGain.length, (int) (1.5 + random.nextGaussian())));
        return score + scoreGain[(int) value];
    }

    @Override
    public String getDescription() {
        return localize("Discard {}", getValueMayUpgrade());
    }

    @Override
    public int applyPriority() {
        return 120; // Larger than draw card
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new DiscardAction(p, p, getValueMayUpgrade(), false));
    }
}
