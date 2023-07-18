package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.defect.DecreaseMaxOrbAction;
import com.megacrit.cardcrawl.actions.defect.IncreaseMaxOrbAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.util.Random;

public class GainOrbSlot extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 7, 14, 21 };
    private static final int[] scoreGain = { 0, 7 };

    public GainOrbSlot(FriendCard card) {
        super(card, scoreNeeded);
        canBePower = true;
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && !CharacterAnalyzer.useOrbs.isEmpty();
    }

    @Override
    public boolean canUpgrade() {
        return !isNegative && super.canUpgrade();
    }

    @Override
    public int getScoreLose() {
        if (isNegative) {
            return -scoreGain[(int) value];
        }
        return super.getScoreLose();
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        if (random.nextInt(5) == 0) {
            isNegative = true;
            gainScores = true;
            value = 1;
            return score + scoreGain[(int) value];
        } else {
            return super.tryApplyScore(score, random);
        }
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        if (this.isNegative) {
            return additionalScore;
        } else {
            return super.tryApplyUpgradeScore(additionalScore, random);
        }
    }

    @Override
    public String getDescription() {
        if (this.isNegative) {
            return localize("LoseOrbSlot {}", getValueMayUpgrade());
        } else {
            return localize("GainOrbSlot {}", getValueMayUpgrade());
        }
    }

    @Override
    public int applyPriority() {
        return 80;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (this.isNegative) {
            addToBot(new DecreaseMaxOrbAction(getValueMayUpgrade()));
        } else {
            addToBot(new IncreaseMaxOrbAction(getValueMayUpgrade()));
        }
    }
}
