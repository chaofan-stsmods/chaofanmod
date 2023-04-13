package io.chaofan.sts.chaofanmod.cards;

import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.relics.ManualOfMerchant;

import static io.chaofan.sts.chaofanmod.cards.CardBase.getCardStrings;
import static io.chaofan.sts.chaofanmod.cards.CardBase.makeCardId;

@SuppressWarnings("unused")
public class ManualOfMerchantCard extends CustomCard {
    public static final String ID = makeCardId(ManualOfMerchantCard.class.getSimpleName());

    public ManualOfMerchantCard() {
        super(ID, getCardStrings(ID).NAME, new RegionName("colorless/skill/fame_and_fortune"), -2, getCardStrings(ID).DESCRIPTION, CardType.SKILL, CardColor.COLORLESS, CardRarity.SPECIAL, CardTarget.SELF);
        baseMagicNumber = magicNumber = ManualOfMerchant.GOLD_AMOUNT;
    }

    @Override
    public void initializeDescription() {
        super.initializeDescription();
        this.keywords.remove("[W]");
    }

    @Override
    public void upgrade() {
        if (!upgraded) {
            upgradeName();
        }
    }

    @Override
    public void use(AbstractPlayer abstractPlayer, AbstractMonster abstractMonster) {
    }
}
