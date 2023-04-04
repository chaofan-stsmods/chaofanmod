package io.chaofan.sts.chaofanmod.cards;

import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.SearingBlowEffect;

import static io.chaofan.sts.chaofanmod.cards.CardBase.getCardStrings;
import static io.chaofan.sts.chaofanmod.cards.CardBase.makeCardId;

public class SearingBlowFor2048 extends CustomCard {
    public static final String ID = makeCardId(SearingBlowFor2048.class.getSimpleName());

    public SearingBlowFor2048() {
        this(0);
    }

    public SearingBlowFor2048(int upgrades) {
        super(ID, getCardStrings(ID).NAME, new RegionName("red/attack/searing_blow"), 2, getCardStrings(ID).DESCRIPTION, CardType.ATTACK, CardColor.COLORLESS, CardRarity.SPECIAL, CardTarget.ENEMY);
        setUpgrades(upgrades);
    }

    public void setUpgrades(int upgrades) {
        this.baseDamage = this.damage = upgrades * (upgrades + 7) / 2 + 12;
        this.misc = upgrades;
        this.name = getCardStrings(ID).NAME + "+" + upgrades;
        this.initializeTitle();
        if (baseDamage < 35) {
            this.rarity = CardRarity.SPECIAL;
        } else if (baseDamage < 70) {
            this.rarity = CardRarity.UNCOMMON;
        } else {
            this.rarity = CardRarity.RARE;
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            this.addToBot(new VFXAction(new SearingBlowEffect(m.hb.cX, m.hb.cY, this.timesUpgraded), 0.2F));
        }

        this.addToBot(new DamageAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
    }

    @Override
    public void upgrade() {
    }

    @Override
    public boolean canUpgrade() {
        return false;
    }

    @Override
    public AbstractCard makeCopy() {
        return new SearingBlowFor2048(this.misc);
    }
}
