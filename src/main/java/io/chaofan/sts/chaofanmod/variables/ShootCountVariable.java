package io.chaofan.sts.chaofanmod.variables;

import basemod.abstracts.DynamicVariable;
import com.megacrit.cardcrawl.cards.AbstractCard;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class ShootCountVariable extends DynamicVariable {
    @Override
    public String key() {
        return makeId("var.ShootCount");
    }

    @Override
    public boolean isModified(AbstractCard card) {
        return true;
    }

    @Override
    public int value(AbstractCard card) {
        return 2;
    }

    @Override
    public int baseValue(AbstractCard card) {
        return 1;
    }

    @Override
    public boolean upgraded(AbstractCard card) {
        return false;
    }
}
