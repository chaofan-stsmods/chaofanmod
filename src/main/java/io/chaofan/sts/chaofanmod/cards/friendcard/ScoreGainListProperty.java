package io.chaofan.sts.chaofanmod.cards.friendcard;

import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Arrays;
import java.util.Random;

public abstract class ScoreGainListProperty extends FriendCardProperty {
    private final int[] scoreGain;

    public ScoreGainListProperty(FriendCard card, int[] scoreGain) {
        super(card);
        this.scoreGain = scoreGain;
        this.gainScores = true;
    }

    @Override
    public int getScoreLose() {
        return -scoreGain[(int) value];
    }

    @Override
    public boolean canUpgrade() {
        return value > 1;
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        if (value <= 1) {
            return additionalScore;
        }
        int valueIndex = (int) value;
        int index = valueIndex - 1;
        while (index > 0 && scoreGain[valueIndex] - scoreGain[index] < additionalScore) {
            index--;
        }
        index += 1;
        upgradeValue = index - valueIndex;
        return additionalScore + scoreGain[index] - scoreGain[valueIndex];
    }
}
