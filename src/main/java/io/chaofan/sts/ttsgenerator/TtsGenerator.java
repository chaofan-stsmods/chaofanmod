package io.chaofan.sts.ttsgenerator;

import basemod.BaseMod;
import basemod.interfaces.PostRenderSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.red.Strike_Red;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import io.chaofan.sts.ttsgenerator.model.CardSetDef;
import io.chaofan.sts.ttsgenerator.model.TabletopCardDef;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TtsGenerator implements PostRenderSubscriber {
    private boolean saved = false;
    public static boolean isGenerating = false;
    public static Map<String, TabletopCardDef> cardMap = new HashMap<>();

    public static void initialize() {
        BaseMod.subscribe(new TtsGenerator());
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (saved) {
            return;
        }

        saved = true;

        CardSetDef csd = new CardSetDef();
        csd.width = 5;
        csd.height = 3;
        csd.list = new ArrayList<>();
        csd.list.add("bladegunner:card.Strike");
        csd.list.add("bladegunner:card.Strike");
        csd.list.add("bladegunner:card.Strike");
        csd.list.add("bladegunner:card.Strike");
        csd.list.add("bladegunner:card.Shoot");
        csd.list.add("bladegunner:card.Shoot");
        csd.list.add("bladegunner:card.Defend");
        csd.list.add("bladegunner:card.Defend");
        csd.list.add("bladegunner:card.Defend");
        csd.list.add("bladegunner:card.Defend");
        csd.list.add("bladegunner:card.RevolverCard");

        TabletopCardDef cardDef = new TabletopCardDef();
        cardDef.description = "1 [Atk]";
        cardDef.upgradeDescription = "2 [Atk]";
        cardMap.put("bladegunner:card.Strike", cardDef);
        cardDef = new TabletopCardDef();
        cardDef.description = "1 [Def]";
        cardDef.upgradeDescription = "2 [Def] to any player.";
        cardMap.put("bladegunner:card.Defend", cardDef);
        cardDef = new TabletopCardDef();
        cardDef.description = "*Shoot.";
        cardDef.upgradeDescription = "*Shoot NL +1 [Atk]";
        cardMap.put("bladegunner:card.Shoot", cardDef);
        cardDef = new TabletopCardDef();
        cardDef.description = "*Shoot = 1 [Atk]";
        cardDef.upgradeDescription = "*Shoot = 2 [Atk]";
        cardMap.put("bladegunner:card.RevolverCard", cardDef);

        try {
            isGenerating = true;
            generateCards(sb, csd);
        } finally {
            isGenerating = false;
        }

        System.exit(0);
    }

    private void generateCards(SpriteBatch sb, CardSetDef csd) {

        SingleCardViewPopup scv = new SingleCardViewPopup();

        int bw = 2112;
        int bh = 1188;
        int w = 744;
        int h = 1039;
        int pw = 744 * csd.width;
        int ph = 1039 * csd.height;

        float factor = Math.max((float)pw / Settings.WIDTH, (float)ph / Settings.HEIGHT);

        FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGB888, bw, bh, false, false);
        TextureRegion textureRegion = new TextureRegion(fb.getColorBufferTexture(), (bw - w) / 2, (bh - h) / 2 + 30, w, h - 30);
        FrameBuffer panel = new FrameBuffer(Pixmap.Format.RGB888, (int) (Settings.WIDTH * factor) + 1, (int) (Settings.HEIGHT * factor) + 1, false, false);

        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sb.end();

        renderBuffer(sb, csd, scv, w, h, pw, ph, factor, fb, textureRegion, panel, false);
        renderBuffer(sb, csd, scv, w, h, pw, ph, factor, fb, textureRegion, panel, true);

        panel.end();

        sb.begin();

        fb.dispose();
        panel.dispose();
    }

    private void renderBuffer(SpriteBatch sb, CardSetDef csd, SingleCardViewPopup scv, int w, int h, int pw, int ph, float factor, FrameBuffer fb, TextureRegion textureRegion, FrameBuffer panel, boolean upgraded) {
        panel.begin();
        Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        panel.end();

        SingleCardViewPopup.enableUpgradeToggle = false;

        outloop:
        for (int y = 0, c = 0; y < csd.height; y++) {
            for (int x = 0; x < csd.width; x++, c++) {
                if (c >= csd.list.size()) {
                    break outloop;
                }

                AbstractCard card = CardLibrary.getCard(csd.list.get(c)).makeCopy();
                SingleCardViewPopup.isViewingUpgrade = upgraded;
                scv.open(card);
                CardCrawlGame.isPopupOpen = false;

                fb.begin();
                Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                sb.begin();
                scv.render(sb);
                sb.end();
                fb.end();

                panel.begin();
                sb.begin();
                sb.setColor(Color.WHITE);
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                float scale = 1f / factor;
                sb.draw(textureRegion, (float) x * w * scale, (y * h) * scale, w * scale, h * scale);
                sb.end();
                panel.end();
            }
        }

        SingleCardViewPopup.enableUpgradeToggle = true;

        panel.begin();
        Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, pw, ph);
        PixmapIO.writePNG(new FileHandle(!upgraded ? "card.png" : "card_upgraded.png"), pixmap);
    }

    private void yFlip(Pixmap pixmap, int w, int h) {
        // Flip the pixmap upside down
        ByteBuffer pixels = pixmap.getPixels();
        int numBytes = w * h * 4;
        byte[] lines = new byte[numBytes];
        int numBytesPerLine = w * 4;
        for (int i = 0; i < h; i++) {
            pixels.position((h - i - 1) * numBytesPerLine);
            pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
        }
        pixels.clear();
        pixels.put(lines);
        pixels.clear();
    }
}
