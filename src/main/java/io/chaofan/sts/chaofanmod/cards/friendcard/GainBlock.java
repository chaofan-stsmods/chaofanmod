package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class GainBlock extends FriendCardProperty {
    public GainBlock(FriendCard card) {
        super(card);
    }

    @Override
    public boolean canUse(Random random) {
        // Skill card won't use this check.
        return super.canUse(random) && card.type == AbstractCard.CardType.ATTACK;
    }

    @Override
    public boolean canUpgrade() {
        return true;
    }

    @Override
    public int getScoreLose() {
        return (int) value;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        value = score;
        return 0;
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        upgradeValue = additionalScore;
        return 0;
    }

    @Override
    public void multiplyValues(float scale) {
        super.multiplyValues(scale);
        card.baseBlock = card.block = (int) value;
    }

    @Override
    public void modifyCard() {
        card.baseBlock = card.block = (int) value;
        addSelfTarget();
    }

    @Override
    public String getDescription() {
        return localize("GainBlock");
    }

    @Override
    public int applyPriority() {
        return 0;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction(p, card.block));
    }

    @Override
    public void upgrade() {
        card.upgradeBlock((int) upgradeValue);
    }
}
