package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class Heal extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    public Heal(FriendCard card) {
        super(card, scoreNeeded);
        canBePower = true;
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && card.properties.stream().anyMatch(p -> p instanceof Exhaust) &&
                card.properties.stream().noneMatch(p -> p instanceof LoseHp);
    }

    @Override
    public String getDescription() {
        return localize("Heal {}", getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new HealAction(p, p, getValueMayUpgrade()));
    }

    @Override
    public FriendCardProperty makeAlternateProperty(Random random) {
        if (random.nextBoolean()) {
            return new HealNonExhaust(card);
        } else {
            return this;
        }
    }
}
