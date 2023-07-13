package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class Ethereal extends NoUpgradeProperty {

    public Ethereal(FriendCard card) {
        super(card);
        isActionableEffect = false;
    }

    @Override
    public int multiplyScore(int score) {
        return (int)(score * 1.15);
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
        card.isEthereal = true;
    }

    @Override
    public String getDescription() {
        return localize("Ethereal");
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public int descriptionPriority() {
        return Integer.MAX_VALUE;
    }
}
