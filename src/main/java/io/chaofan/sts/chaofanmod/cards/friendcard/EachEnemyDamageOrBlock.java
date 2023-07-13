package io.chaofan.sts.chaofanmod.cards.friendcard;

import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class EachEnemyDamageOrBlock extends EachEnemy {
    public EachEnemyDamageOrBlock(FriendCard card) {
        super(card);
        isActionableEffect = false;
        alternateOf = EachEnemy.class;
        gainScores = true;
    }

    @Override
    public boolean canUse(Random random) {
        return true;
    }

    @Override
    public int getScoreLose() {
        return -1;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        if (score > 0) {
            return score + 1;
        } else {
            return score;
        }
    }

    @Override
    public void modifyCard() {
        actionProperty = card.properties.stream()
                .filter(p -> (p instanceof DealDamage || p instanceof GainBlock) && !p.useSecondaryBlock && !p.useSecondaryDamage)
                .findFirst()
                .orElse(null);

        if (actionProperty != null) {
            super.modifyCard();
        } else {
            this.shouldShowDescription = false;
            this.shouldUse = false;
        }
    }

    @Override
    public String getDescription() {
        return localize("EachEnemy {}").replace("{}", toLowerPrefix(actionProperty.getDescription()));
    }

    @Override
    public int applyPriority() {
        return 0;
    }
}
