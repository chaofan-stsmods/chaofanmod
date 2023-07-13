package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class Retain extends NoUpgradeProperty {

    public Retain(FriendCard card) {
        super(card);
        isActionableEffect = false;
    }

    @Override
    public int multiplyScore(int score) {
        return (int)(score * 0.9);
    }

    @Override
    public int getScoreLose() {
        return 0;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        return score;
    }

    @Override
    public void modifyCard() {
        card.selfRetain = true;
    }

    @Override
    public String getDescription() {
        return localize("Retain");
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public int descriptionPriority() {
        return -100;
    }
}
