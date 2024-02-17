package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.powers.GainStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class EnemyLoseTempStrength extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 1, 2, 4, 6, 8, 10, 12, 15, 18, 22 };

    public EnemyLoseTempStrength(FriendCard card) {
        super(card, scoreNeeded);
        alternateOf = EnemyLoseStrength.class;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        int result = super.tryApplyScore(score, random);
        if (value < 3) {
            return score;
        }
        return result;
    }

    @Override
    public void setToAllEnemies() {
        super.setToAllEnemies();
        multiplyValues(0.8f);
    }

    @Override
    public void modifyCard() {
        super.modifyCard();
        addEnemyTarget();
    }

    @Override
    public String getDescription() {
        return localize("EnemyLoseTempStrength {}", "EnemyLoseTempStrengthAll {}", getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster monster) {
        forEnemyOrAllEnemies(monster, m -> {
            if (m != null) {
                addToBot(new ApplyPowerAction(m, p, new StrengthPower(m, -getValueMayUpgrade())));
                if (!m.hasPower(ArtifactPower.POWER_ID)) {
                    addToBot(new ApplyPowerAction(m, p, new GainStrengthPower(m, getValueMayUpgrade())));
                }
            }
        });
    }
}
