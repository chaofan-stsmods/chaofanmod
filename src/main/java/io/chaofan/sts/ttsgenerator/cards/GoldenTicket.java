package io.chaofan.sts.ttsgenerator.cards;

import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class GoldenTicket extends CustomCard {
    public GoldenTicket(CardColor color) {
        super(
                "ttsgen:GoldenTicket",
                "Golden Ticket",
                "ttsgenerator/images/GoldenTicket.png",
                -2,
                "Golden Ticket can't be added to your deck. NL When you reveal Golden Ticket, reveal a card from your rare deck.",
                CardType.POWER,
                color,
                CardRarity.RARE,
                CardTarget.NONE);
    }

    @Override
    public void upgrade() {

    }

    @Override
    public void use(AbstractPlayer abstractPlayer, AbstractMonster abstractMonster) {

    }

    @Override
    public AbstractCard makeCopy() {
        return new GoldenTicket(color);
    }
}
