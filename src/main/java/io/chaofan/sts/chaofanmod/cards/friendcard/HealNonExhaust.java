package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class HealNonExhaust extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 2, 3, 6, 10, 15, 20 };

    public HealNonExhaust(FriendCard card) {
        super(card, scoreNeeded);
        alternateOf = Heal.class;
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && card.type == AbstractCard.CardType.ATTACK &&
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
}
