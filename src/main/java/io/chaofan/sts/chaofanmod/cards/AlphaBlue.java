package io.chaofan.sts.chaofanmod.cards;

import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.orbs.Alpha;

import static io.chaofan.sts.chaofanmod.cards.CardBase.getCardStrings;
import static io.chaofan.sts.chaofanmod.cards.CardBase.makeCardId;

@SuppressWarnings("unused")
public class AlphaBlue extends CustomCard {
    public static final String ID = makeCardId(AlphaBlue.class.getSimpleName());

    public AlphaBlue() {
        super(ID, getCardStrings(ID).NAME, new RegionName("purple/skill/alpha"), 2, getCardStrings(ID).DESCRIPTION, CardType.SKILL, CardColor.BLUE, CardRarity.UNCOMMON, CardTarget.SELF);
        baseMagicNumber = magicNumber = 1;
    }

    @Override
    public void upgrade() {
        if (!upgraded) {
            upgradeName();
            upgradeBaseCost(1);
            initializeDescription();
        }
    }

    @Override
    public void use(AbstractPlayer abstractPlayer, AbstractMonster abstractMonster) {
        for (int i = 0; i < magicNumber; i++) {
            addToBot(new ChannelAction(new Alpha()));
        }
    }
}
