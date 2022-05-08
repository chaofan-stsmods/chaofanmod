package io.chaofan.sts.enhancedsteamstatus.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.integrations.steam.SteamIntegration;
import io.chaofan.sts.enhancedsteamstatus.EnhancedSteamStatus;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class InsertAdditionalUiWord {

    @SpirePatch(clz = SteamIntegration.class, method = "setRichPresenceDisplayPlaying", paramtypez = {int.class, int.class, String.class})
    @SpirePatch(clz = SteamIntegration.class, method = "setRichPresenceDisplayPlaying", paramtypez = {int.class, String.class})
    public static class DisplayTextPatch {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getClassName().equals(SteamIntegration.class.getName()) && m.getMethodName().equals("setRichPresenceData")) {
                        m.replace(String.format("$_ = $proceed($0, $1, %s.replaceMessage($1, $2));", DisplayTextPatch.class.getName()));
                    }
                }
            };
        }

        public static String replaceMessage(String target, String msg) {
            if (!target.equals("status")) {
                return msg;
            }

            String statusText = getStatusText();
            if (statusText.length() == 0) {
                return msg;
            }

            String result = msg + " " + statusText;
            EnhancedSteamStatus.logger.info("Replace message to " + result);
            return result;
        }

        private static String getStatusText() {
            if (EnhancedSteamStatus.statusText == null) {
                return "";
            }
            return EnhancedSteamStatus.statusText[EnhancedSteamStatus.status.ordinal()];
        }
    }
}
