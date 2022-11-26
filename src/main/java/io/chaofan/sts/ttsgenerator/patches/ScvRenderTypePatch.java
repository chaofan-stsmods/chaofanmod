package io.chaofan.sts.ttsgenerator.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import io.chaofan.sts.ttsgenerator.TtsGenerator;
import io.chaofan.sts.ttsgenerator.model.TabletopCardDef;

@SpirePatch(clz = SingleCardViewPopup.class, method = "renderFrame")
public class ScvRenderTypePatch {
    @SpirePrefixPatch
    public static void Prefix(SingleCardViewPopup instance, SpriteBatch sb, AbstractCard ___card) {
        TabletopCardDef cardDef = TtsGenerator.cardMap.get(___card.cardID);
        if (cardDef == null) {
            return;
        }

        if (cardDef.type != null) {
            ___card.type = AbstractCard.CardType.valueOf(cardDef.type);
        }

        if (cardDef.rarity != null) {
            ___card.rarity = AbstractCard.CardRarity.valueOf(cardDef.rarity);
        }
    }
}
