package io.chaofan.sts.chaofanmod.mods;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.daily.mods.AbstractDailyMod;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.RunModStrings;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class Lonely extends AbstractDailyMod {
    public static final String ID = makeId("Lonely");
    private static final RunModStrings modStrings = CardCrawlGame.languagePack.getRunModString(ID);
    public static final String NAME = modStrings.NAME;
    public static final String DESC = modStrings.DESCRIPTION;

    public Lonely() {
        super(ID, NAME, DESC, null, false);
        this.img = ImageMaster.loadImage(getImagePath("ui/run_mods/lonely.png"));
    }
}
