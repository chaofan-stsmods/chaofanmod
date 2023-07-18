package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import io.chaofan.sts.chaofanmod.cards.FriendCard;
import io.chaofan.sts.chaofanmod.utils.CharacterAnalyzer;

import java.util.Random;

public class EachUniqueOrb extends NoUpgradeProperty {
    protected FriendCardProperty actionProperty;

    public EachUniqueOrb(FriendCard card) {
        super(card);
        isActionableEffect = false;
        alternateOf = EachOrb.class;
    }

    @Override
    public boolean canUse(Random random) {
        if (CharacterAnalyzer.useOrbs.isEmpty() || card.properties.stream().anyMatch(p -> p instanceof EachEnemy || p.alternateOf == EachEnemy.class)) {
            return false;
        }
        if (card.properties.size() > 0) {
            FriendCardProperty lastProperty;
            if (card.properties.contains(this)) {
                lastProperty = card.properties.get(card.properties.indexOf(this) - 1);
            } else {
                lastProperty = card.properties.get(card.properties.size() - 1);
            }
            return super.canUse(random) && !lastProperty.gainScores && lastProperty.isActionableEffect;
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
            canBePower = actionProperty.canBePower;
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
        return localize("EachUniqueOrb {}").replace("{}", toLowerPrefix(actionProperty.getDescription()));
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster monster) {
        p.orbs.stream().filter(o -> !(o instanceof EmptyOrbSlot)).map(o -> o.ID).distinct().forEach(o -> actionProperty.use(p, monster));
    }
}
