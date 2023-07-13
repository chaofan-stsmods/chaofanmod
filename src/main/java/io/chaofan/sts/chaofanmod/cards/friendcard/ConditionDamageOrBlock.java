package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.cards.AbstractCard;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class ConditionDamageOrBlock extends Condition {
    public ConditionDamageOrBlock(FriendCard card) {
        super(card);
        isActionableEffect = false;
        gainScores = false;
        alternateOf = Condition.class;
    }

    @Override
    public boolean canUpgrade() {
        return true;
    }

    @Override
    public boolean canUse(Random random) {
        return true;
    }

    @Override
    public int getScoreLose() {
        return scoreGain;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        Type[] types = Type.values();
        int tryCount = 100;
        do {
            type = types[random.nextInt(types.length)];
            tryCount--;
            if (tryCount == 0) {
                return score;
            }
        } while (!type.conditionCheck.apply(this));

        isAttack = card.type == AbstractCard.CardType.ATTACK;
        useSecondaryDamage = isAttack;
        useSecondaryBlock = !isAttack;
        actionProperty = isAttack ? new DealDamage(card, true) : new GainBlock(card, true);
        actionProperty.tryApplyScore((int) (score / type.multiplier), random);
        scoreGain = score;
        return 0;
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        return actionProperty.tryApplyUpgradeScore(additionalScore, random);
    }

    @Override
    public void setToAllEnemies() {
        super.setToAllEnemies();
        actionProperty.setToAllEnemies();
    }

    @Override
    public void modifyCard() {
        actionProperty.modifyCard();
        super.modifyCard();
    }

    @Override
    public void upgrade() {
        actionProperty.upgrade();
    }
}
