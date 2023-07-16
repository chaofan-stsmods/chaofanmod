package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.LoseDexterityPower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class GainDexterity extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 6, 12, 18, 30 };
    private static final int[] scoreGain = { 0, 10, 20, 30 };

    protected boolean temp = false;

    public GainDexterity(FriendCard card) {
        super(card, scoreNeeded);
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
            int result = super.tryApplyScore(score, random);
            if (random.nextBoolean()) {
                this.temp = true;
            }
            return result;
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
        if (this.temp) {
            return localize("GainDexterityTemp {}", 2 * getValueMayUpgrade());
        } else if (this.isNegative) {
            return localize("LoseDexterity {}", getValueMayUpgrade());
        } else {
            return localize("GainDexterity {}", getValueMayUpgrade());
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (this.temp) {
            addToBot(new ApplyPowerAction(p, p, new DexterityPower(p, 2 * getValueMayUpgrade())));
            addToBot(new ApplyPowerAction(p, p, new LoseDexterityPower(p, 2 * getValueMayUpgrade())));
        } else if (this.isNegative) {
            addToBot(new ApplyPowerAction(p, p, new DexterityPower(p, -getValueMayUpgrade())));
        } else {
            addToBot(new ApplyPowerAction(p, p, new DexterityPower(p, getValueMayUpgrade())));
        }
    }
}
