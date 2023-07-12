package io.chaofan.sts.chaofanmod.cards.friendcard;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.cards.status.VoidCard;
import com.megacrit.cardcrawl.cards.status.Wound;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import java.util.Random;

public class AddStatus extends NoUpgradeProperty {
    private Type type;

    public AddStatus(FriendCard card) {
        super(card);
        isNegative = true;
        gainScores = true;
    }

    @Override
    public void modifyCard() {
        super.modifyCard();
        card.cardsToPreview = CardLibrary.getCard(type.cardId).makeCopy();
    }

    @Override
    public int getScoreLose() {
        return -type.scoreGain[(int) value];
    }

    @Override
    public int tryApplyScore(int score, Random random) {
        Type[] types = Type.values();
        type = types[random.nextInt(types.length)];
        int[] scoreGain = type.scoreGain;
        value = random.nextInt(20) == 0 ? 3 : (random.nextInt(5) == 0 ? 2 : 1);
        return score + scoreGain[(int) value];
    }

    @Override
    public String getDescription() {
        return localize("AddStatus " + type.toString() + " {}", getValueMayUpgrade());
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (type.toString().endsWith("_HAND")) {
            addToBot(new MakeTempCardInHandAction(CardLibrary.getCard(type.cardId).makeCopy(), (int) value));
        } else if (type.toString().endsWith("_DRAW")) {
            addToBot(new MakeTempCardInDrawPileAction(CardLibrary.getCard(type.cardId).makeCopy(), (int) value, true, true));
        } else {
            addToBot(new MakeTempCardInDiscardAction(CardLibrary.getCard(type.cardId).makeCopy(), (int) value));
        }
    }

    private enum Type {
        WOUND_HAND(new int[] {0, 3, 8, 15}, Wound.ID),
        WOUND_DRAW(new int[] {0, 4, 9, 18}, Wound.ID),
        WOUND_DISCARD(new int[] {0, 3, 7, 13}, Wound.ID),
        BURN_HAND(new int[] {0, 5, 10, 19}, Burn.ID),
        BURN_DRAW(new int[] {0, 5, 12, 22}, Burn.ID),
        BURN_DISCARD(new int[] {0, 4, 9, 17}, Burn.ID),
        DAZED_DRAW(new int[] {0, 2, 7, 13}, Dazed.ID),
        DAZED_DISCARD(new int[] {0, 2, 5, 10}, Dazed.ID),
        VOID_DRAW(new int[] {0, 4, 9, 18}, VoidCard.ID),
        VOID_DISCARD(new int[] {0, 3, 7, 13}, VoidCard.ID);

        public final int[] scoreGain;
        public final String cardId;

        Type(int[] scoreGain, String cardId) {
            this.scoreGain = scoreGain;
            this.cardId = cardId;
        }
    }
}
