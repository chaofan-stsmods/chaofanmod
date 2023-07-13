package io.chaofan.sts.chaofanmod.variables;

import basemod.abstracts.DynamicVariable;
import com.megacrit.cardcrawl.cards.AbstractCard;
import io.chaofan.sts.chaofanmod.cards.FriendCard;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class SecondaryDamage extends DynamicVariable {
    @Override
    public String key() {
        return makeId("var.SecondaryDamage");
    }

    @Override
    public boolean isModified(AbstractCard abstractCard) {
        return abstractCard instanceof FriendCard && ((FriendCard) abstractCard).isSecondaryDamageModified;
    }

    @Override
    public int value(AbstractCard abstractCard) {
        return abstractCard instanceof FriendCard ? ((FriendCard) abstractCard).secondaryDamage : 0;
    }

    @Override
    public int baseValue(AbstractCard abstractCard) {
        return abstractCard instanceof FriendCard ? ((FriendCard) abstractCard).baseSecondaryDamage : 0;
    }

    @Override
    public boolean upgraded(AbstractCard abstractCard) {
        return abstractCard instanceof FriendCard && ((FriendCard) abstractCard).upgradedSecondaryDamage;
    }
}
