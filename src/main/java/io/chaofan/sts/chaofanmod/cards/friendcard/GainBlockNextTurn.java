package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.NextTurnBlockPower;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class GainBlockNextTurn extends FriendCardProperty {
    private int score;

    public GainBlockNextTurn(FriendCard card) {
        super(card);
        useSecondaryBlock = true;
    }

    @Override
    public boolean canUse(Random random) {
        return super.canUse(random) && card.type == AbstractCard.CardType.SKILL &&
                card.properties.stream().noneMatch(p -> p != this && p.useSecondaryBlock);
    }

    @Override
    public boolean canUpgrade() {
        return true;
    }

    @Override
    public int getScoreLose() {
        return score;
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        this.score = score;
        value = score < 4 ? score + 1 : score * 1.25f;
        return 0;
    }

    @Override
    public int tryApplyUpgradeScore(int additionalScore, Random random) {
        upgradeValue = additionalScore * 1.25f;
        return 0;
    }

    @Override
    public void multiplyValues(float scale) {
        super.multiplyValues(scale);
        card.baseSecondaryBlock = card.secondaryBlock = (int) value;
    }

    @Override
    public void modifyCard() {
        card.baseSecondaryBlock = card.secondaryBlock = (int) value;
        addSelfTarget();
    }

    @Override
    public String getDescription() {
        return localize("GainBlockNextTurn");
    }

    @Override
    public int applyPriority() {
        return 10;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new NextTurnBlockPower(p, card.secondaryBlock)));
    }

    @Override
    public void upgrade() {
        card.upgradeSecondaryBlock((int) upgradeValue);
    }
}
