package io.chaofan.sts.chaofanmod.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.ui.WheelButton;
import io.chaofan.sts.chaofanmod.ui.WheelSelectScreen;

import java.util.ArrayList;

public class SelectFromWheelAction extends AbstractGameAction {

    private boolean complete = false;

    public SelectFromWheelAction() {
        duration = startDuration = Settings.ACTION_DUR_FAST;
    }
    @Override
    public void update() {
        AbstractPlayer player = AbstractDungeon.player;
        if (duration == startDuration) {

            ArrayList<WheelButton> buttons = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                WheelButton button = new WheelButton();
                button.startRotation = i * 45 - 22.5f;
                button.endRotation = (i + 1) * 45 - 22.5f;
                button.scale = 0.01f;
                button.targetScale = 1.0f;
                button.color = WheelSelectScreen.WHEEL_COLOR.cpy();
                button.idleColor = WheelSelectScreen.WHEEL_COLOR;
                button.hoverColor = WheelSelectScreen.WHEEL_HOVER_COLOR;
                button.centerX = player.hb.cX;
                button.centerY = player.hb.cY;
                buttons.add(button);
            }

            ChaofanMod.wheelSelectScreen.openScreen(buttons, true);

        } else if (!complete && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE) {
            complete = true;
        }

        tickDuration();
    }
}
