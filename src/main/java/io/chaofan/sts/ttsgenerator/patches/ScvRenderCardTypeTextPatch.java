package io.chaofan.sts.ttsgenerator.patches;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import io.chaofan.sts.ttsgenerator.TtsGenerator;
import io.chaofan.sts.ttsgenerator.cards.GoldenTicket;
import io.chaofan.sts.ttsgenerator.model.TabletopCardDef;

@SpirePatch(clz = SingleCardViewPopup.class, method = "renderCardTypeText")
public class ScvRenderCardTypeTextPatch {
    private static final Color CARD_TYPE_COLOR = new Color(0.35F, 0.35F, 0.35F, 1.0F);

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(SingleCardViewPopup instance, SpriteBatch sb, AbstractCard ___card) {
        if (___card instanceof GoldenTicket) {
            FontHelper.renderFontCentered(
                    sb,
                    FontHelper.panelNameFont,
                    "Ticket",
                    Settings.WIDTH / 2.0F + 3.0F * Settings.scale,
                    Settings.HEIGHT / 2.0F - 40.0F * Settings.scale,
                    CARD_TYPE_COLOR);
            return SpireReturn.Return();
        }

        return SpireReturn.Continue();
    }
}
