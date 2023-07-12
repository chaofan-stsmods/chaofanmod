package io.chaofan.sts.chaofanmod.cards.friendcard;

import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Arrays;
import java.util.Random;

public abstract class ScoreNeededListProperty extends FriendCardProperty {
    private final int[] scoreNeeded;

    public ScoreNeededListProperty(FriendCard card, int[] scoreNeeded) {
        super(card);
        this.scoreNeeded = scoreNeeded;
    }

    @Override
    public int getScoreLose() {
        return scoreNeeded[(int) value];
    }

    @Override
    public boolean canUpgrade() {
        return value < scoreNeeded.length - 1;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        int index = Arrays.binarySearch(scoreNeeded, score);
        if (index < 0) {
            index = -index - 2;
        }
        if (index <= 0) {
            return score;
        }
        value = postApplyScore(index, random);
        return score - scoreNeeded[index];
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        int index = Arrays.binarySearch(scoreNeeded, scoreNeeded[(int) value] + additionalScore);
        if (index < 0) {
            index = -index - 2;
        }
        if (index <= 0 || index == value) {
            return additionalScore;
        }

        upgradeValue = index - value;
        return additionalScore - scoreNeeded[index] + scoreNeeded[(int) value];
    }

    protected int postApplyScore(int scoreIndex, Random random) {
        return scoreIndex;
    }
}
