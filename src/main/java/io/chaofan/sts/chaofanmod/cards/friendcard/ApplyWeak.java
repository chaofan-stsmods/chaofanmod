package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.WeakPower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

public class ApplyWeak extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 2, 5, 9, 13, 17 };

    public ApplyWeak(FriendCard card) {
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
        return localize("ApplyWeak {}", "ApplyWeakAll {}", getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster monster) {
        forEnemyOrAllEnemies(monster, m -> {
            if (m != null) {
                addToBot(new ApplyPowerAction(m, p, new WeakPower(m, getValueMayUpgrade(), false)));
            }
        });
    }
}
