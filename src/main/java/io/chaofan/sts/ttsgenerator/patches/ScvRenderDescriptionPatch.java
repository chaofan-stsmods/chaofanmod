package io.chaofan.sts.ttsgenerator.patches;

import basemod.BaseMod;
import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DescriptionLine;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import io.chaofan.sts.ttsgenerator.TtsGenerator;
import io.chaofan.sts.ttsgenerator.model.TabletopCardDef;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SpirePatch(clz = SingleCardViewPopup.class, method = "renderDescription")
public class ScvRenderDescriptionPatch {
    private static final GlyphLayout gl = new GlyphLayout();
    private static final Texture slot = ImageMaster.loadImage("ttsgenerator/images/slot.png");
    private static final Texture all = ImageMaster.loadImage("ttsgenerator/images/all.png");

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(SingleCardViewPopup instance, SpriteBatch sb, AbstractCard ___card) {
        if (TtsGenerator.isGenerating) {
            renderSlots(sb, ___card);
            renderDescriptionNew(instance, sb, ___card);
            return SpireReturn.Return();
        }

        return SpireReturn.Continue();
    }

    private static void renderSlots(SpriteBatch sb, AbstractCard card) {
        TabletopCardDef cardDef = TtsGenerator.cardMap.get(card.cardID);
        if (cardDef == null || cardDef.slots == null) {
            return;
        }

        float current_x = (float)Settings.WIDTH / 2.0F;
        float current_y = (float)Settings.HEIGHT / 2.0F - 300.0F * Settings.scale;

        List<Point> slots = cardDef.upgradeSlots != null && card.upgraded ? cardDef.upgradeSlots : cardDef.slots;

        for (Point p : slots) {
            sb.draw(slot,
                    current_x + (p.x - slot.getWidth() * 0.909f / 2) * Settings.scale,
                    current_y + (p.y - slot.getHeight() * 0.909f / 2) * Settings.scale,
                    slot.getWidth() * 0.909f * Settings.scale,
                    slot.getHeight() * 0.909f * Settings.scale);
        }
    }

    private static void renderDescriptionNew(SingleCardViewPopup instance, SpriteBatch sb, AbstractCard card) {
        TabletopCardDef cardDef = TtsGenerator.cardMap.get(card.cardID);
        if (cardDef == null) {
            renderDescription(instance, sb, card);
            return;
        }

        if (!card.upgraded) {
            if (cardDef.cost != Integer.MIN_VALUE) {
                card.cost = card.costForTurn = cardDef.cost;
            }
        } else {
            if (cardDef.cost != Integer.MIN_VALUE) {
                card.cost = card.costForTurn = cardDef.upgradeCost;
            }
            card.isCostModified = card.isCostModifiedForTurn = false;
        }

        float descriptionWidth = 300.0f * Settings.scale * 0.79f * 2;
        BitmapFont font = FontHelper.SCP_cardDescFont;
        float iconWidth = 24.0f * Settings.scale;
        float drawScale = 2.0f;

        gl.reset();

        String description = card.upgraded && cardDef.upgradeDescription != null ? cardDef.upgradeDescription : cardDef.description;
        String[] tokens = description.split("(?=[ .,])");
        List<List<String>> lines = new ArrayList<>();
        List<Float> lineWidths = new ArrayList<>();
        List<String> currentLine = new ArrayList<>();
        float currentWidth = 0;
        for (String token : tokens) {
            String trimmedToken = token.startsWith(" ") ? token.substring(1) : token;
            float tokenWidth;
            if (trimmedToken.startsWith("[") && trimmedToken.endsWith("]")) {
                tokenWidth = iconWidth * drawScale;
            } else if (currentWidth == 0) {
                if (trimmedToken.startsWith("*")) {
                    gl.setText(font, trimmedToken.substring(1));
                } else {
                    gl.setText(font, trimmedToken);
                }
                tokenWidth = gl.width;
            } else {
                if (trimmedToken.startsWith("*")) {
                    gl.setText(font, token.charAt(0) + token.substring(2));
                } else {
                    gl.setText(font, token);
                }
                tokenWidth = gl.width;
            }

            boolean added = false;
            if (currentWidth + tokenWidth > descriptionWidth || trimmedToken.equals("NL")) {
                if (currentWidth == 0 || trimmedToken.matches("[.,]")) {
                    if (trimmedToken.equals("NL")) {
                        continue;
                    }
                    currentWidth += tokenWidth;
                    currentLine.add(trimmedToken);
                    added = true;
                }

                lines.add(currentLine);
                lineWidths.add(currentWidth);

                currentWidth = 0;
                currentLine = new ArrayList<>();
            }

            if (!trimmedToken.equals("NL") && !added) {
                currentWidth += tokenWidth;
                currentLine.add(currentWidth == 0 ? trimmedToken : token);
            }
        }

        if (currentWidth > 0) {
            lines.add(currentLine);
            lineWidths.add(currentWidth);
        }

        float current_x = (float)Settings.WIDTH / 2.0F;
        float current_y = (float)Settings.HEIGHT / 2.0F - 300.0F * Settings.scale;
        float draw_y = current_y + 100.0F * Settings.scale;
        draw_y += (float)lines.size() * font.getCapHeight() * 0.775F - font.getCapHeight() * 0.375F;
        draw_y += cardDef.descriptionYOffset * Settings.scale;

        for (int i = 0; i < lines.size(); i++) {
            currentLine = lines.get(i);
            currentWidth = lineWidths.get(i);
            float start_x = current_x - currentWidth / 2;

            for (String token : currentLine) {
                String trimmedToken = token.startsWith(" ") ? token.substring(1) : token;
                if (trimmedToken.startsWith("[") && trimmedToken.endsWith("]")) {
                    renderIcon(sb, card, trimmedToken.substring(1, trimmedToken.length() - 1), start_x, (i + 0.5f) * 1.53F * -font.getCapHeight() + draw_y + -12.0F, iconWidth / Settings.scale, drawScale * Settings.scale);
                    start_x += iconWidth * drawScale;
                } else {
                    boolean gold = trimmedToken.startsWith("*");
                    if (gold) {
                        if (token.startsWith(" ")) {
                            token = " " + trimmedToken.substring(1);
                        } else {
                            token = trimmedToken.substring(1);
                        }
                    }

                    gl.setText(font, token);
                    FontHelper.renderRotatedText(sb, font, token, current_x, current_y,
                            start_x - current_x + gl.width / 2.0F,
                            i * 1.53F * -font.getCapHeight() + draw_y - current_y + -12.0F,
                            0.0F,
                            true,
                            gold ? Settings.GOLD_COLOR : Settings.CREAM_COLOR);

                    start_x += gl.width;
                }
            }
        }
    }

    private static void renderIcon(SpriteBatch sb, AbstractCard card, String icon, float x, float y, float iconSize, float drawScale) {
        TextureAtlas.AtlasRegion region = null;
        sb.setColor(Color.WHITE);
        switch (icon) {
            case "E":
                region = BaseMod.getCardSmallEnergy(card);
                break;
            case "Atk":
                region = new TextureAtlas.AtlasRegion(ImageMaster.INTENT_ATK_3, 36, 36, 56, 56);
                break;
            case "Def":
                region = new TextureAtlas.AtlasRegion(ImageMaster.BLOCK_ICON, 4, 4, 56, 56);
                sb.setColor(new Color(0.6F, 0.93F, 0.98F, 1.0F));
                break;
            case "Dazed":
                region = new TextureAtlas.AtlasRegion(ImageMaster.INTENT_DEBUFF2, 0, 0, 64, 64);
                break;
            case "Slimed":
                region = new TextureAtlas.AtlasRegion(ImageMaster.INTENT_DEBUFF, 6, 6, 52, 52);
                break;
            case "All":
                region = new TextureAtlas.AtlasRegion(all, 0, 0, all.getWidth(), all.getHeight());
                break;
            case "Burn":
                region = new TextureAtlas.AtlasRegion(AbstractPower.atlas.findRegion("128/flameBarrier"));
                region.offsetX = 0;
                region.offsetY = 0;
                break;
            default:
                region = AbstractPower.atlas.findRegion("128/" + icon.toLowerCase());
                if (region != null) {
                    region = new TextureAtlas.AtlasRegion(region);
                    region.offsetX = 0;
                    region.offsetY = 0;
                }
        }

        if (region != null) {
            TextureRegion textureRegion = new TextureRegion(region);
            float scale = iconSize / Math.max(textureRegion.getRegionWidth(), textureRegion.getRegionHeight());
            sb.draw(textureRegion, x + region.offsetX * drawScale, y + region.offsetY * drawScale, textureRegion.getRegionWidth() * scale * drawScale, textureRegion.getRegionHeight() * scale * drawScale);
            sb.setColor(Color.WHITE);
        }
    }

    private static void renderDescription(SingleCardViewPopup instance, SpriteBatch sb, AbstractCard card) {
        float current_x = (float)Settings.WIDTH / 2.0F - 10.0F * Settings.scale;
        float current_y = (float)Settings.HEIGHT / 2.0F - 300.0F * Settings.scale;
        float drawScale = 2.0f;
        float card_energy_w = 24.0f * Settings.scale;
        
        BitmapFont font = FontHelper.SCP_cardDescFont;
        float draw_y = current_y + 100.0F * Settings.scale;
        draw_y += (float)card.description.size() * font.getCapHeight() * 0.775F - font.getCapHeight() * 0.375F;
        float spacing = 1.53F * -font.getCapHeight() / Settings.scale / drawScale;
        GlyphLayout gl = new GlyphLayout();

        for (int i = 0; i < card.description.size(); ++i) {
            DescriptionLine descriptionLine = card.description.get(i);
            float start_x = current_x - descriptionLine.width * drawScale / 2.0F;
            String[] tokens = descriptionLine.getCachedTokenizedText();

            for (String token : tokens) {
                if (token.charAt(0) == '*') {
                    token = token.substring(1);
                    String punctuation = "";
                    if (token.length() > 1 && token.charAt(token.length() - 2) != '+' && !Character.isLetter(token.charAt(token.length() - 2))) {
                        punctuation = punctuation + token.charAt(token.length() - 2);
                        token = token.substring(0, token.length() - 2);
                        punctuation = punctuation + ' ';
                    }

                    gl.setText(font, token);
                    FontHelper.renderRotatedText(sb, font, token, current_x, current_y, start_x - current_x + gl.width / 2.0F, (float) i * 1.53F * -font.getCapHeight() + draw_y - current_y + -12.0F, 0.0F, true, Settings.GOLD_COLOR);
                    start_x = (float) Math.round(start_x + gl.width);
                    gl.setText(font, punctuation);
                    FontHelper.renderRotatedText(sb, font, punctuation, current_x, current_y, start_x - current_x + gl.width / 2.0F, (float) i * 1.53F * -font.getCapHeight() + draw_y - current_y + -12.0F, 0.0F, true, Settings.CREAM_COLOR);
                    gl.setText(font, punctuation);
                    start_x += gl.width;
                } else if (token.charAt(0) == '!') {
                    if (token.length() == 4) {
                        start_x += renderDynamicVariable(instance, token.charAt(1), start_x, draw_y, i, font, sb, null);
                    } else if (token.length() == 5) {
                        start_x += renderDynamicVariable(instance, token.charAt(1), start_x, draw_y, i, font, sb, token.charAt(3));
                    }
                } else if (token.equals("[R] ")) {
                    gl.width = card_energy_w * drawScale;
                    renderSmallEnergy(instance, sb, AbstractCard.orb_red, (start_x - current_x) / Settings.scale / drawScale, -87.0F - (((float) card.description.size() - 4.0F) / 2.0F - (float) i + 1.0F) * spacing);
                    start_x += gl.width;
                } else if (token.equals("[R]. ")) {
                    gl.width = card_energy_w * drawScale / Settings.scale;
                    renderSmallEnergy(instance, sb, AbstractCard.orb_red, (start_x - current_x) / Settings.scale / drawScale, -87.0F - (((float) card.description.size() - 4.0F) / 2.0F - (float) i + 1.0F) * spacing);
                    FontHelper.renderRotatedText(sb, font, LocalizedStrings.PERIOD, current_x, current_y, start_x - current_x + card_energy_w * drawScale, (float) i * 1.53F * -font.getCapHeight() + draw_y - current_y + -12.0F, 0.0F, true, Settings.CREAM_COLOR);
                    start_x += gl.width;
                    gl.setText(font, LocalizedStrings.PERIOD);
                    start_x += gl.width;
                } else if (token.equals("[G] ")) {
                    gl.width = card_energy_w * drawScale;
                    renderSmallEnergy(instance, sb, AbstractCard.orb_green, (start_x - current_x) / Settings.scale / drawScale, -87.0F - (((float) card.description.size() - 4.0F) / 2.0F - (float) i + 1.0F) * spacing);
                    start_x += gl.width;
                } else if (token.equals("[G]. ")) {
                    gl.width = card_energy_w * drawScale;
                    renderSmallEnergy(instance, sb, AbstractCard.orb_green, (start_x - current_x) / Settings.scale / drawScale, -87.0F - (((float) card.description.size() - 4.0F) / 2.0F - (float) i + 1.0F) * spacing);
                    FontHelper.renderRotatedText(sb, font, LocalizedStrings.PERIOD, current_x, current_y, start_x - current_x + card_energy_w * drawScale, (float) i * 1.53F * -font.getCapHeight() + draw_y - current_y + -12.0F, 0.0F, true, Settings.CREAM_COLOR);
                    start_x += gl.width;
                } else if (token.equals("[B] ")) {
                    gl.width = card_energy_w * drawScale;
                    renderSmallEnergy(instance, sb, AbstractCard.orb_blue, (start_x - current_x) / Settings.scale / drawScale, -87.0F - (((float) card.description.size() - 4.0F) / 2.0F - (float) i + 1.0F) * spacing);
                    start_x += gl.width;
                } else if (token.equals("[B]. ")) {
                    gl.width = card_energy_w * drawScale;
                    renderSmallEnergy(instance, sb, AbstractCard.orb_blue, (start_x - current_x) / Settings.scale / drawScale, -87.0F - (((float) card.description.size() - 4.0F) / 2.0F - (float) i + 1.0F) * spacing);
                    FontHelper.renderRotatedText(sb, font, LocalizedStrings.PERIOD, current_x, current_y, start_x - current_x + card_energy_w * drawScale, (float) i * 1.53F * -font.getCapHeight() + draw_y - current_y + -12.0F, 0.0F, true, Settings.CREAM_COLOR);
                    start_x += gl.width;
                } else if (token.equals("[W] ")) {
                    gl.width = card_energy_w * drawScale;
                    renderSmallEnergy(instance, sb, AbstractCard.orb_purple, (start_x - current_x) / Settings.scale / drawScale, -87.0F - (((float) card.description.size() - 4.0F) / 2.0F - (float) i + 1.0F) * spacing);
                    start_x += gl.width;
                } else if (token.equals("[W]. ")) {
                    gl.width = card_energy_w * drawScale;
                    renderSmallEnergy(instance, sb, AbstractCard.orb_purple, (start_x - current_x) / Settings.scale / drawScale, -87.0F - (((float) card.description.size() - 4.0F) / 2.0F - (float) i + 1.0F) * spacing);
                    FontHelper.renderRotatedText(sb, font, LocalizedStrings.PERIOD, current_x, current_y, start_x - current_x + card_energy_w * drawScale, (float) i * 1.53F * -font.getCapHeight() + draw_y - current_y + -12.0F, 0.0F, true, Settings.CREAM_COLOR);
                    start_x += gl.width;
                } else {
                    gl.setText(font, token);
                    FontHelper.renderRotatedText(sb, font, token, current_x, current_y, start_x - current_x + gl.width / 2.0F, (float) i * 1.53F * -font.getCapHeight() + draw_y - current_y + -12.0F, 0.0F, true, Settings.CREAM_COLOR);
                    start_x += gl.width;
                }
            }
        }

        font.getData().setScale(1.0F);
    }

    private static float renderDynamicVariable(SingleCardViewPopup instance, char charAt, float start_x, float draw_y, int i, BitmapFont font, SpriteBatch sb, Character character) {
        Method m = ReflectionHacks.getCachedMethod(SingleCardViewPopup.class, "renderDynamicVariable", char.class, float.class, float.class, int.class, BitmapFont.class, SpriteBatch.class, Character.class);
        try {
            return (float)m.invoke(instance, charAt, start_x, draw_y, i, font, sb, character);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static void renderSmallEnergy(SingleCardViewPopup instance, SpriteBatch sb, TextureAtlas.AtlasRegion region, float x, float y) {
        Method m = ReflectionHacks.getCachedMethod(SingleCardViewPopup.class, "renderSmallEnergy", SpriteBatch.class, TextureAtlas.AtlasRegion.class, float.class, float.class);
        try {
            m.invoke(instance, sb, region, x, y);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
