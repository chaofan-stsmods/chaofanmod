package io.chaofan.sts.chaofanmod.cards.friendcard;

import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public abstract class NoUpgradeProperty extends FriendCardProperty {
    public NoUpgradeProperty(FriendCard card) {
        super(card);
    }

    @Override
    public boolean canUpgrade() {
        return false;
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        return additionalScore;
    }
}
