package io.chaofan.sts.chaofanmod.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.relics.MarkOfChaos;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MarkOfChaosPatches {
    @SpirePatch(clz = LargeDialogOptionButton.class, method = SpirePatch.CLASS)
    public static class ButtonFields {
        public static SpireField<Boolean> altDisabled = new SpireField<>(() -> false);
    }

    public static boolean justUpdatedEventDialog = false;


    @SpirePatch(clz = EventRoom.class, method = "update")
    public static class EventRoomUpdatePatch {
        @SpirePostfixPatch
        public static void Postfix(EventRoom instance) {
            if (justUpdatedEventDialog) {
                justUpdatedEventDialog = false;
                MarkOfChaosPatches.onEventButtonChanged(instance.event);
            }
        }
    }

    @SpirePatch(clz = GenericEventDialog.class, method = "clear")
    @SpirePatch(clz = GenericEventDialog.class, method = "clearAllDialogs")
    @SpirePatch(clz = GenericEventDialog.class, method = "removeDialogOption")
    @SpirePatch(clz = GenericEventDialog.class, method = "clearRemainingOptions")
    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption", paramtypez = {String.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption", paramtypez = {String.class, AbstractCard.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption", paramtypez = {String.class, AbstractRelic.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption", paramtypez = {String.class, AbstractCard.class, AbstractRelic.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption", paramtypez = {String.class, boolean.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption", paramtypez = {String.class, boolean.class, AbstractCard.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption", paramtypez = {String.class, boolean.class, AbstractRelic.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption", paramtypez = {String.class, boolean.class, AbstractCard.class, AbstractRelic.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "updateDialogOption", paramtypez = {int.class, String.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "updateDialogOption", paramtypez = {int.class, String.class, boolean.class})
    @SpirePatch(clz = GenericEventDialog.class, method = "updateDialogOption", paramtypez = {int.class, String.class, AbstractCard.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "removeDialogOption")
    @SpirePatch(clz = RoomEventDialog.class, method = "clearRemainingOptions")
    @SpirePatch(clz = RoomEventDialog.class, method = "addDialogOption", paramtypez = {String.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "addDialogOption", paramtypez = {String.class, AbstractCard.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "addDialogOption", paramtypez = {String.class, AbstractRelic.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "addDialogOption", paramtypez = {String.class, AbstractCard.class, AbstractRelic.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "addDialogOption", paramtypez = {String.class, boolean.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "addDialogOption", paramtypez = {String.class, boolean.class, AbstractCard.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "addDialogOption", paramtypez = {String.class, boolean.class, AbstractRelic.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "addDialogOption", paramtypez = {String.class, boolean.class, AbstractCard.class, AbstractRelic.class})
    @SpirePatch(clz = RoomEventDialog.class, method = "updateDialogOption")
    public static class EventOptionChangedPatch {
        @SpirePostfixPatch
        public static void Postfix() {
            justUpdatedEventDialog = true;
        }
    }

    @SpirePatch(clz = LargeDialogOptionButton.class, method = "hoverAndClickLogic")
    @SpirePatch(clz = LargeDialogOptionButton.class, method = "render")
    public static class EventButtonDisablePatch {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (f.getFieldName().equals("isDisabled")) {
                        f.replace(String.format("$_ = $proceed($$) || %s.isButtonDisabled(this);", MarkOfChaosPatches.class.getName()));
                    }
                }
            };
        }
    }

    @SpirePatch(clz = LargeDialogOptionButton.class, method = "render")
    public static class EventButtonMsgRenderPatch {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (f.getFieldName().equals("msg")) {
                        f.replace(String.format("$_ = %s.updateButtonMessage(this, $proceed($$));", MarkOfChaosPatches.class.getName()));
                    }
                }
            };
        }
    }

    @SpirePatch(clz = LargeDialogOptionButton.class, method = "renderCardPreview")
    @SpirePatch(clz = LargeDialogOptionButton.class, method = "renderRelicPreview")
    public static class EventButtonHidePreviewPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(LargeDialogOptionButton instance) {
            if (ButtonFields.altDisabled.get(instance)) {
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }


    public static void onEventButtonChanged(AbstractEvent event) {
        if (AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic(MarkOfChaos.ID)) {
            return;
        }

        chooseAvailableOption(RoomEventDialog.optionList);
        chooseAvailableOption(event.imageEventText.optionList);
    }

    public static boolean isButtonDisabled(LargeDialogOptionButton button) {
        if (AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic(MarkOfChaos.ID)) {
            return false;
        }

        if (!(AbstractDungeon.getCurrRoom() instanceof EventRoom)) {
            return false;
        }

        return ButtonFields.altDisabled.get(button);
    }

    public static String updateButtonMessage(LargeDialogOptionButton button, String msg) {
        if (ButtonFields.altDisabled.get(button)) {
            return MarkOfChaos.eventOptionPrefix;
        } else {
            return msg;
        }
    }

    private static String stripColor(String input) {
        input = input.replace("#r", "");
        input = input.replace("#g", "");
        input = input.replace("#b", "");
        input = input.replace("#y", "");
        return input;
    }

    private static void chooseAvailableOption(ArrayList<LargeDialogOptionButton> optionList) {
        if (optionList.isEmpty()) {
            return;
        }

        List<LargeDialogOptionButton> availableOptions = optionList.stream().filter(o -> !o.isDisabled).collect(Collectors.toList());
        if (availableOptions.isEmpty()) {
            optionList.forEach(o -> ButtonFields.altDisabled.set(o, false));
            return;
        }

        int index = AbstractDungeon.cardRandomRng.random(availableOptions.size() - 1);
        availableOptions.forEach(o -> ButtonFields.altDisabled.set(o, true));
        LargeDialogOptionButton button = availableOptions.get(index);
        ButtonFields.altDisabled.set(button, false);
    }
}
