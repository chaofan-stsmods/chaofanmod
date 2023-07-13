package io.chaofan.sts.chaofanmod.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;

public class AnonymousAction extends AbstractGameAction {
    private final Runnable callback;

    public AnonymousAction(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void update() {
        if (this.callback != null) {
            this.callback.run();
        }
        isDone = true;
    }
}
