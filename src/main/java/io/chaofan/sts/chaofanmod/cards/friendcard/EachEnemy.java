package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class EachEnemy extends NoUpgradeProperty {
    protected FriendCardProperty actionProperty;

    public EachEnemy(FriendCard card) {
        super(card);
        isActionableEffect = false;
    }

    @Override
    public boolean canUse(Random random) {
        if (card.properties.size() > 0) {
            FriendCardProperty lastProperty;
            if (card.properties.contains(this)) {
                lastProperty = card.properties.get(card.properties.indexOf(this) - 1);
            } else {
                lastProperty = card.properties.get(card.properties.size() - 1);
            }
            return super.canUse(random) && !lastProperty.gainScores && lastProperty.isActionableEffect && lastProperty.value >= 2;
        }
        return false;
    }

    @Override
    public int getScoreLose() {
        return 1;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        if (score > 0) {
            this.actionProperty = card.properties.get(card.properties.size() - 1);
            return score - 1;
        } else {
            return score;
        }
    }

    @Override
    public void modifyCard() {
        actionProperty.multiplyValues(0.7f);
        super.modifyCard();
        actionProperty.shouldUse = false;
        actionProperty.shouldShowDescription = false;
    }

    @Override
    public String getDescription() {
        return localize("EachEnemy {}").replace("{}", toLowerPrefix(actionProperty.getDescription()));
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster monster) {
        AbstractDungeon.getMonsters().monsters.stream().filter(m -> !m.isDeadOrEscaped()).forEach(m -> actionProperty.use(p, monster));
    }

    @Override
    public FriendCardProperty makeAlternateProperty(Random random) {
        if (random.nextBoolean()) {
            return new EachEnemyDamageOrBlock(card);
        } else {
            return this;
        }
    }

}
