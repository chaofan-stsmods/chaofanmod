package io.chaofan.sts.chaofanmod.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import io.chaofan.sts.chaofanmod.relics.GoldenCube;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public class GoldenCubePatches {
    @SpirePatch(clz = AbstractCard.class, method = "hasEnoughEnergy")
    public static class EnergyCheckPatch {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("freeToPlay") && m.getClassName().equals(AbstractCard.class.getName())) {
                        m.replace(String.format("$_ = $proceed($$) || %s.hasGoldenCube($0);", GoldenCubePatches.class.getName()));
                    }
                }
            };
        }
    }

    @SpirePatch(clz = EnergyPanel.class, method = "useEnergy")
    public static class NegativeEnergyPatch {
        private static int estimatedEnergy;

        @SpirePrefixPatch
        public static void Prefix(int e) {
            estimatedEnergy = EnergyPanel.totalCount - e;
        }

        @SpirePostfixPatch
        public static void Postfix(int e) {
            if (hasGoldenCube(null)) {
                EnergyPanel.totalCount = estimatedEnergy;
            }
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "useCard")
    public static class XCardCostPatch {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (f.getClassName().equals(AbstractCard.class.getName()) && f.getFieldName().equals("energyOnUse") && f.isWriter()) {
                        f.replace("$0.energyOnUse = java.lang.Math.max(0, $1);");
                    }
                }
            };
        }
    }

    public static boolean hasGoldenCube(AbstractCard card) {
        AbstractPlayer player = AbstractDungeon.player;
        return player != null && player.hasRelic(GoldenCube.ID);
    }
}
