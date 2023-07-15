package io.chaofan.sts.chaofanmod.cards.friendcard;

import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.util.Random;

public class EachOrbDamageOrBlock extends EachOrb {
    public EachOrbDamageOrBlock(FriendCard card) {
        super(card);
        isActionableEffect = false;
        alternateOf = EachOrb.class;
        gainScores = true;
    }

    @Override
    public boolean canUse(Random random) {
        return !CharacterAnalyzer.useOrbs.isEmpty() &&
                card.properties.stream().noneMatch(p -> p instanceof EachEnemy || p.alternateOf == EachEnemy.class);
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
        return localize("EachOrb {}").replace("{}", toLowerPrefix(actionProperty.getDescription()));
    }

    @Override
    public int applyPriority() {
        return 0;
    }
}
