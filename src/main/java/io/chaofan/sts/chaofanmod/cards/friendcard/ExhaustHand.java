package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ExhaustAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class ExhaustHand extends NoUpgradeProperty {

    public ExhaustHand(FriendCard card) {
        super(card);
    }

    @Override
    public int getScoreLose() {
        return 2;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        if (score > 2) {
            return score - 2;
        } else {
            return score;
        }
    }

    @Override
    public String getDescription() {
        return localize("ExhaustHand");
    }

    @Override
    public int applyPriority() {
        return 120;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ExhaustAction(1, false));
    }
}
