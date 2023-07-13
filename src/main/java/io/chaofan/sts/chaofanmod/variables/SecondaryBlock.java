package io.chaofan.sts.chaofanmod.variables;

import basemod.abstracts.DynamicVariable;
import com.megacrit.cardcrawl.cards.AbstractCard;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class SecondaryBlock extends DynamicVariable {
    @Override
    public String key() {
        return makeId("var.SecondaryBlock");
    }

    @Override
    public boolean isModified(AbstractCard abstractCard) {
        return abstractCard instanceof FriendCard && ((FriendCard) abstractCard).isSecondaryBlockModified;
    }

    @Override
    public int value(AbstractCard abstractCard) {
        return abstractCard instanceof FriendCard ? ((FriendCard) abstractCard).secondaryBlock : 0;
    }

    @Override
    public int baseValue(AbstractCard abstractCard) {
        return abstractCard instanceof FriendCard ? ((FriendCard) abstractCard).baseSecondaryBlock : 0;
    }

    @Override
    public boolean upgraded(AbstractCard abstractCard) {
        return abstractCard instanceof FriendCard && ((FriendCard) abstractCard).upgradedSecondaryBlock;
    }
}
