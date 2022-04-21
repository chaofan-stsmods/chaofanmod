package io.chaofan.sts.chaofanmod.cards;

import basemod.abstracts.CustomCard;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatchExtension;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.ChaofanMod;

import static io.chaofan.sts.chaofanmod.cards.CardBase.getCardStrings;
import static io.chaofan.sts.chaofanmod.cards.CardBase.makeCardId;

public class ProgressCard extends CustomCard {
    public static TextureRegion progressImage = new TextureRegion(ImageMaster.loadImage(ChaofanMod.getImagePath("ui/progress.png")));

    public static final String ID = makeCardId(ProgressCard.class.getSimpleName());

    private float time;

    public ProgressCard() {
        super(ID, getCardStrings(ID).NAME, new RegionName("red/attack/bash"), 0, getCardStrings(ID).DESCRIPTION, CardType.ATTACK, CardColor.COLORLESS, CardRarity.SPECIAL, CardTarget.ALL);
    }

    @Override
    public void render(SpriteBatch sb, boolean selected) {
        time += Gdx.graphics.getDeltaTime() * 30;
        super.render(sb, selected);
        sb.setColor(Color.GREEN);
        SpriteBatchExtension.drawProgress(sb, progressImage, this.current_x - 20f, this.current_y - 20f, 40f, 40f, time * 0.3f, time);
    }

    @Override
    public void upgrade() {

    }

    @Override
    public void use(AbstractPlayer abstractPlayer, AbstractMonster abstractMonster) {

    }
}
