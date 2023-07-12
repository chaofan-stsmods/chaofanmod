package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class LoseStrength extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 6, 12, 20, 30, 40 };

    public LoseStrength(FriendCard card) {
        super(card, scoreNeeded);
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
        return localize("EnemyLoseStrength {}", "EnemyLoseStrengthAll {}", getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster monster) {
        forEnemyOrAllEnemies(monster, m -> {
            if (m != null) {
                addToBot(new ApplyPowerAction(m, p, new StrengthPower(m, -getValueMayUpgrade())));
            }
        });
    }

    @Override
    public FriendCardProperty makeAlternateProperty(Random random) {
        if (random.nextBoolean()) {
            return new LoseTempStrength(card);
        } else {
            return this;
        }
    }
}
