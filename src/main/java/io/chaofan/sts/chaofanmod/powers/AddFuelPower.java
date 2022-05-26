package io.chaofan.sts.chaofanmod.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class AddFuelPower extends AbstractPower {
    public final static String POWER_ID = makeId("power.AddFuel");
    private static final String[] DESCRIPTIONS = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).DESCRIPTIONS;

    public AddFuelPower(AbstractCreature owner, int amount) {
        this.ID = POWER_ID;
        this.name = CardCrawlGame.languagePack.getPowerStrings(POWER_ID).NAME;
        this.type = PowerType.BUFF;
        this.owner = owner;
        this.amount = amount;
        this.updateDescription();
        this.loadRegion("accuracy");
    }

    public void updateDescription() {
        this.description = String.format(DESCRIPTIONS[0], this.amount);
    }

    @Override
    public void onExhaust(AbstractCard card) {
        flash();
        addToBot(new ApplyPowerAction(owner, owner, new StrengthPower(owner, amount)));
    }

    public static void triggerExhaust(AbstractCard card) {
        MapRoomNode mapNode = AbstractDungeon.getCurrMapNode();
        if (mapNode != null) {
            AbstractRoom room = mapNode.getRoom();
            if (room != null && room.monsters != null) {
                for (AbstractMonster monster : room.monsters.monsters) {
                    if (!monster.isDeadOrEscaped()) {
                        AbstractPower power = monster.getPower(POWER_ID);
                        if (power != null) {
                            power.onExhaust(card);
                        }
                    }
                }
            }
        }
    }
}
