package io.chaofan.sts.pseudoconfusion.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.ConfusionPower;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SpirePatch(clz = ConfusionPower.class, method = "onCardDraw")
public class ConfusionPatch {
    public static int duplicateCount = 5;
    public static int remainingCount = 3;
    public static List<Integer> pool = new ArrayList<>();

    @SpireInstrumentPatch
    public static ExprEditor instrument() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("random")) {
                    m.replace(String.format("$_ = %s.random();", ConfusionPatch.class.getName()));
                }
            }
        };
    }

    public static int random() {
        if (pool.size() <= remainingCount) {
            pool.clear();
            for (int i = 0; i < duplicateCount; i++) {
                pool.add(0);
                pool.add(1);
                pool.add(2);
                pool.add(3);
            }
            Collections.shuffle(pool, AbstractDungeon.cardRandomRng.random);
        }
        return pool.remove(pool.size() - 1);
    }
}
