package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

public class ApplyVulnerable extends ScoreNeededListProperty {
    private static final int[] scoreNeeded = { 0, 3, 8, 12, 16, 20 };

    public ApplyVulnerable(FriendCard card) {
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
        return localize("ApplyVulnerable {}", "ApplyVulnerableAll {}", getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster monster) {
        forEnemyOrAllEnemies(monster, m -> {
            if (m != null) {
                addToBot(new ApplyPowerAction(m, p, new VulnerablePower(m, getValueMayUpgrade(), false)));
            }
        });
    }
}
