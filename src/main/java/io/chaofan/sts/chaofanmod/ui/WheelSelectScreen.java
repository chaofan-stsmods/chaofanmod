package io.chaofan.sts.chaofanmod.ui;

import basemod.BaseMod;
import basemod.abstracts.CustomScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import io.chaofan.sts.chaofanmod.ChaofanMod;

import java.util.ArrayList;

public class WheelSelectScreen extends CustomScreen {
    private boolean canCancel;
    public float closeTimer;
    public boolean closing;
    public WheelButton clickedButton;
    public MyCancelButton cancelButton;

    public static class Enum
    {
        @SpireEnum
        public static AbstractDungeon.CurrentScreen WHEEL_SELECT_SCREEN;
    }

    public static Color WHEEL_COLOR = new Color(0.6f, 0.6f, 0.6f, 0.5f);
    public static Color WHEEL_HOVER_COLOR = new Color(0.8f, 0.8f, 0.8f, 0.8f);

    public ArrayList<WheelButton> buttons = new ArrayList<>();

    public WheelSelectScreen() {
        cancelButton = new MyCancelButton(this::onClickCancelButton);
    }

    @Override
    public AbstractDungeon.CurrentScreen curScreen() {
        return Enum.WHEEL_SELECT_SCREEN;
    }

    private void open(ArrayList<WheelButton> buttons, boolean canCancel) {
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.NONE) {
            AbstractDungeon.previousScreen = AbstractDungeon.screen;
        }

        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return;
        }

        this.buttons = buttons;
        this.canCancel = canCancel;

        for (WheelButton button : buttons) {
            button.parent = this;
        }

        reopen();
    }

    public void openScreen(ArrayList<WheelButton> buttons, boolean canCancel) {
        BaseMod.openCustomScreen(Enum.WHEEL_SELECT_SCREEN, buttons, canCancel);
    }

    @Override
    public void reopen() {
        closing = false;
        clickedButton = null;
        if (canCancel) {
            cancelButton.show(CardCrawlGame.languagePack.getUIString("GridCardSelectScreen").TEXT[1]);
        }
        AbstractDungeon.screen = curScreen();
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.overlayMenu.hideBlackScreen();
    }

    @Override
    public void close() {
        genericScreenOverlayReset();
    }

    @Override
    public void update() {
        if (closing) {
            cancelButton.hide();
            closeTimer -= Gdx.graphics.getDeltaTime();
            if (closeTimer <= 0) {
                closing = false;
                AbstractDungeon.closeCurrentScreen();
                return;
            }
        }

        AbstractPlayer player = AbstractDungeon.player;
        for (WheelButton button : buttons) {
            if (player != null) {
                button.centerX = player.hb.cX;
                button.centerY = player.hb.cY;
            }
            button.update();
        }

        cancelButton.update();

        AbstractDungeon.currMapNode.room.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return;
        }

        for (WheelButton button : buttons) {
            button.render(sb);
        }

        cancelButton.render(sb);
    }

    @Override
    public void openingSettings() {
        AbstractDungeon.previousScreen = curScreen();
    }

    @Override
    public void openingDeck() {
        AbstractDungeon.previousScreen = curScreen();
    }

    @Override
    public void openingMap() {
        AbstractDungeon.previousScreen = curScreen();
    }

    @Override
    public boolean allowOpenDeck() {
        return true;
    }

    @Override
    public boolean allowOpenMap() {
        return true;
    }

    private void onClickCancelButton() {
        closing = true;
        closeTimer = 0.3f;
        clickedButton = null;
    }
}
