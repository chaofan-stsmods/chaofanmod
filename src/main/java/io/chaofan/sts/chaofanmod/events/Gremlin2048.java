package io.chaofan.sts.chaofanmod.events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.red.SearingBlow;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

import java.util.Optional;

import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class Gremlin2048 extends AbstractImageEvent {
    public static final String ID = makeId("event.Grimlin2048");
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString(ID);
    public static final String NAME = eventStrings.NAME;
    public static final String[] DESCRIPTIONS = eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = eventStrings.OPTIONS;
    private Screen screen;
    private final AbstractCard[][] cardsMatrix = new AbstractCard[4][4];
    private final CardGroup cards;
    private final CardGroup cardsToBeRemoved;

    public Gremlin2048() {
        super(NAME, DESCRIPTIONS[0], "images/events/matchAndKeep.jpg");
        this.screen = Screen.INTRO;
        this.cards = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        this.cardsToBeRemoved = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        this.imageEventText.setDialogOption(OPTIONS[0]);
    }

    @Override
    public void update() {
        super.update();
        this.cards.update();
        this.cardsToBeRemoved.update();
        this.cardsToBeRemoved.group.removeIf(card -> Math.abs(card.current_x - card.target_x) < 0.01f && Math.abs(card.current_y - card.target_y) < 0.01f);
        for (AbstractCard card : this.cards.group) {
            card.hb.update();
            if (card.hb.hovered) {
                card.targetDrawScale = .7f;
            } else {
                card.targetDrawScale = .5f;
            }
        }

        if (screen == Screen.PLAY) {
            updateGameLogic();
        }
    }

    private void updateGameLogic() {

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            boolean hasMove = false;
            for (int x = 0; x < 4; x++) {
                AbstractCard currentCard = cardsMatrix[x][0];
                int currentY = 0;
                for (int y = 1; y < 4; y++) {
                    AbstractCard targetCard = cardsMatrix[x][y];
                    if (targetCard == null) {
                        continue;
                    }
                    cardsMatrix[x][y] = null;
                    if (currentCard == null) {
                        currentCard = targetCard;
                        hasMove = true;
                    } else {
                        if (currentCard.timesUpgraded == targetCard.timesUpgraded) {
                            mergeCard(currentCard, targetCard, x, currentY);
                            currentY++;
                            currentCard = null;
                            hasMove = true;
                        } else {
                            moveCard(currentCard, x, currentY);
                            currentY++;
                            currentCard = targetCard;
                            hasMove = hasMove || y != currentY;
                        }
                    }
                }
                if (currentCard != null) {
                    moveCard(currentCard, x, currentY);
                }
            }
            if (hasMove) {
                putNewCard();
                checkValidMoveAndComplete();
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            boolean hasMove = false;
            for (int x = 0; x < 4; x++) {
                AbstractCard currentCard = cardsMatrix[x][3];
                int currentY = 3;
                for (int y = 2; y >= 0; y--) {
                    AbstractCard targetCard = cardsMatrix[x][y];
                    if (targetCard == null) {
                        continue;
                    }
                    cardsMatrix[x][y] = null;
                    if (currentCard == null) {
                        currentCard = targetCard;
                        hasMove = true;
                    } else {
                        if (currentCard.timesUpgraded == targetCard.timesUpgraded) {
                            mergeCard(currentCard, targetCard, x, currentY);
                            currentY--;
                            currentCard = null;
                            hasMove = true;
                        } else {
                            moveCard(currentCard, x, currentY);
                            currentY--;
                            currentCard = targetCard;
                            hasMove = hasMove || y != currentY;
                        }
                    }
                }
                if (currentCard != null) {
                    moveCard(currentCard, x, currentY);
                }
            }
            if (hasMove) {
                putNewCard();
                checkValidMoveAndComplete();
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            boolean hasMove = false;
            for (int y = 0; y < 4; y++) {
                AbstractCard currentCard = cardsMatrix[0][y];
                int currentX = 0;
                for (int x = 1; x < 4; x++) {
                    AbstractCard targetCard = cardsMatrix[x][y];
                    if (targetCard == null) {
                        continue;
                    }
                    cardsMatrix[x][y] = null;
                    if (currentCard == null) {
                        currentCard = targetCard;
                        hasMove = true;
                    } else {
                        if (currentCard.timesUpgraded == targetCard.timesUpgraded) {
                            mergeCard(currentCard, targetCard, currentX, y);
                            currentX++;
                            currentCard = null;
                            hasMove = true;
                        } else {
                            moveCard(currentCard, currentX, y);
                            currentX++;
                            currentCard = targetCard;
                            hasMove = hasMove || x != currentX;
                        }
                    }
                }
                if (currentCard != null) {
                    moveCard(currentCard, currentX, y);
                }
            }
            if (hasMove) {
                putNewCard();
                checkValidMoveAndComplete();
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            boolean hasMove = false;
            for (int y = 0; y < 4; y++) {
                AbstractCard currentCard = cardsMatrix[3][y];
                int currentX = 3;
                for (int x = 2; x >= 0; x--) {
                    AbstractCard targetCard = cardsMatrix[x][y];
                    if (targetCard == null) {
                        continue;
                    }
                    cardsMatrix[x][y] = null;
                    if (currentCard == null) {
                        currentCard = targetCard;
                        hasMove = true;
                    } else {
                        if (currentCard.timesUpgraded == targetCard.timesUpgraded) {
                            mergeCard(currentCard, targetCard, currentX, y);
                            currentX--;
                            currentCard = null;
                            hasMove = true;
                        } else {
                            moveCard(currentCard, currentX, y);
                            currentX--;
                            currentCard = targetCard;
                            hasMove = hasMove || x != currentX;
                        }
                    }
                }
                if (currentCard != null) {
                    moveCard(currentCard, currentX, y);
                }
            }
            if (hasMove) {
                putNewCard();
                checkValidMoveAndComplete();
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        super.render(sb);
        this.cardsToBeRemoved.render(sb);
        this.cards.render(sb);
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch(this.screen) {
            case INTRO:
                this.imageEventText.updateBodyText(DESCRIPTIONS[1]);
                this.imageEventText.updateDialogOption(0, OPTIONS[2]);
                this.screen = Screen.RULE_EXPLANATION;
                return;
            case RULE_EXPLANATION:
                this.imageEventText.removeDialogOption(0);
                GenericEventDialog.hide();
                this.screen = Screen.PLAY;
                this.initializeCards();
                return;
            case COMPLETE:
                this.openMap();
        }
    }

    private void moveCard(AbstractCard card, int x, int y) {
        cardsMatrix[x][y] = card;
        card.target_x = convertX(x);
        card.target_y = convertY(y);
    }

    private void mergeCard(AbstractCard currentCard, AbstractCard targetCard, int x, int y) {
        currentCard.upgrade();
        moveCard(currentCard, x, y);

        cards.removeCard(targetCard);
        cardsToBeRemoved.addToTop(targetCard);
        targetCard.target_x = currentCard.target_x;
        targetCard.target_y = currentCard.target_y;
    }

    private void complete() {
        screen = Screen.COMPLETE;
        GenericEventDialog.show();
        this.imageEventText.updateBodyText(DESCRIPTIONS[2]);
        this.imageEventText.clearRemainingOptions();
        this.imageEventText.setDialogOption(OPTIONS[1]);

        Optional<AbstractCard> optionalCard = cards.group.stream().reduce((c1, c2) -> c1.timesUpgraded >= c2.timesUpgraded ? c1 : c2);
        optionalCard.ifPresent(card ->
            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(card.makeStatEquivalentCopy(), (float) Settings.WIDTH / 2.0F, (float) Settings.HEIGHT / 2.0F))
        );

        cardsToBeRemoved.group.addAll(cards.group);
        for (AbstractCard c : cards.group) {
            c.target_y = -300.0F * Settings.scale;
            c.target_x = (float)Settings.WIDTH / 2.0F;
            c.targetDrawScale = 0.01f;
        }
        cards.clear();
    }

    private void initializeCards() {
        Random rng = AbstractDungeon.cardRandomRng;
        int cardCount = rng.random(2, 3);
        for (int i = 0; i < cardCount; i++) {
            putNewCard();
        }
    }

    private void putNewCard() {
        Random rng = AbstractDungeon.cardRandomRng;
        AbstractCard card = new SearingBlow();
        if (rng.randomBoolean()) {
            card.upgrade();
        }

        while (true) {
            int x = rng.random(3);
            int y = rng.random(3);
            if (cardsMatrix[x][y] != null) {
                continue;
            }

            cards.addToTop(card);
            card.targetDrawScale = 0.5F;
            card.drawScale = 0.001f;
            moveCard(card, x, y);
            card.current_x = card.target_x;
            card.current_y = card.target_y;
            return;
        }
    }

    private boolean hasValidMove() {
        if (cards.size() < 16) {
            return true;
        }

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                if ((x > 0 && cardsMatrix[x][y].timesUpgraded == cardsMatrix[x - 1][y].timesUpgraded) ||
                        (y > 0 && cardsMatrix[x][y].timesUpgraded == cardsMatrix[x][y - 1].timesUpgraded) ||
                        (x < 3 && cardsMatrix[x][y].timesUpgraded == cardsMatrix[x + 1][y].timesUpgraded) ||
                        (y < 3 && cardsMatrix[x][y].timesUpgraded == cardsMatrix[x][y + 1].timesUpgraded)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void checkValidMoveAndComplete() {
        if (!hasValidMove()) {
            complete();
        }
    }

    private float convertX(int x) {
        return x * 235.0F * Settings.scale + 640.0F * Settings.scale;
    }

    private float convertY(int y) {
        return y * -235.0F * Settings.scale + 850.0F * Settings.scale;
    }

    private enum Screen {
        INTRO,
        RULE_EXPLANATION,
        PLAY,
        COMPLETE
    }
}
