package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class LoseHp extends ScoreGainListProperty {
    private static final int[] scoreGain = { 0, 3, 5, 10, 16, 22 };

    public LoseHp(FriendCard card) {
        super(card, scoreGain);
        isNegative = true;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        value = Math.max(1, Math.min(scoreGain.length, (int) (2.5 + random.nextGaussian())));
        return score + scoreGain[(int) value];
    }

    @Override
    public String getDescription() {
        return localize("LoseHp {}", getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new LoseHPAction(p, p, getValueMayUpgrade()));
    }
}
