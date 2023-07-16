package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FocusPower;
import com.megacrit.cardcrawl.powers.LoseStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.util.Random;

public class GainFocus extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 10, 20, 30 };
    private static final int[] scoreGain = { 0, 6, 12, 18 };

    public GainFocus(FriendCard card) {
        super(card, scoreNeeded);
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && CharacterAnalyzer.affectedByFocus;
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
            value = Math.max(1, random.nextInt(3) + (random.nextInt(5) == 0 ? 1 : 0));
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
            return localize("LoseFocus {}", getValueMayUpgrade());
        } else {
            return localize("GainFocus {}", getValueMayUpgrade());
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (this.isNegative) {
            addToBot(new ApplyPowerAction(p, p, new FocusPower(p, -getValueMayUpgrade())));
        } else {
            addToBot(new ApplyPowerAction(p, p, new FocusPower(p, getValueMayUpgrade())));
        }
    }
}
