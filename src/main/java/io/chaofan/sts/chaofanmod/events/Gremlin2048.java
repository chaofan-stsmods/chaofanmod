package io.chaofan.sts.chaofanmod.events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.red.SearingBlow;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

import java.util.Optional;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class Gremlin2048 extends AbstractImageEvent {
    public static final String ID = makeId("event.Grimlin2048");
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString(ID);
    public static final String NAME = eventStrings.NAME;
    public static final String[] DESCRIPTIONS = eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = eventStrings.OPTIONS;
    private final Texture pressKey1;
    private final Texture pressKey2;
    private Screen screen;
    private final AbstractCard[][] cardsMatrix = new AbstractCard[4][4];
    private final CardGroup cards;
    private final CardGroup cardsToBeRemoved;
    private AbstractCard hoverCard;
    private float timer = 0;

    private final Hitbox left;
    private final Hitbox right;
    private final Hitbox up;
    private final Hitbox down;

    public Gremlin2048() {
        super(NAME, DESCRIPTIONS[0], "images/events/matchAndKeep.jpg");
        this.pressKey1 = ImageMaster.loadImage(getImagePath("ui/press_key_1.png"));
        this.pressKey2 = ImageMaster.loadImage(getImagePath("ui/press_key_2.png"));
        this.screen = Screen.INTRO;
        this.cards = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        this.cardsToBeRemoved = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        this.imageEventText.setDialogOption(OPTIONS[0]);

        left = new Hitbox(120, 120);
        right = new Hitbox(120, 120);
        up = new Hitbox(120, 120);
        down = new Hitbox(120, 120);
        left.move(convertX(-1.5f), convertY(1.5f));
        right.move(convertX(-0.5f), convertY(1.5f));
        up.move(convertX(-1f), convertY(1f));
        down.move(convertX(-1f), convertY(2f));
    }

    @Override
    public void update() {
        timer += Gdx.graphics.getDeltaTime();
        super.update();
        this.cards.update();
        this.cardsToBeRemoved.update();
        this.cardsToBeRemoved.group.removeIf(card -> Math.abs(card.current_x - card.target_x) < 0.01f && Math.abs(card.current_y - card.target_y) < 0.01f);
        hoverCard = null;
        for (AbstractCard card : this.cards.group) {
            card.hb.update();
            if (card.hb.hovered) {
                card.targetDrawScale = .7f;
                hoverCard = card;
            } else {
                card.targetDrawScale = .5f;
            }
        }

        if (screen == Screen.PLAY) {
            updateHitbox(left);
            updateHitbox(right);
            updateHitbox(up);
            updateHitbox(down);
            updateGameLogic();

            if (InputHelper.justReleasedClickLeft) {
                left.clicked = false;
                right.clicked = false;
                up.clicked = false;
                down.clicked = false;
            }
        }
    }

    private void updateHitbox(Hitbox hitbox) {
        hitbox.update();
        if (InputHelper.justClickedLeft && hitbox.hovered) {
            hitbox.clickStarted = true;
        }
    }

    private void updateGameLogic() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed() || up.clicked) {
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
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed() || down.clicked) {
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
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed() || left.clicked) {
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
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed() || right.clicked) {
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

        if (screen == Screen.PLAY) {
            this.cardsToBeRemoved.render(sb);
            this.cards.render(sb);
            if (hoverCard != null) {
                hoverCard.render(sb);
            }

            Texture img = pressKey1;
            if (MathUtils.floor(timer) % 2 == 0) {
                img = pressKey2;
            }

            sb.draw(img,
                    convertX(-1) - img.getWidth() / 2f * Settings.scale,
                    convertY(1.5f) - img.getHeight() / 2f * Settings.scale,
                    img.getWidth() * Settings.scale,
                    img.getHeight() * Settings.scale);

            renderArrow(sb, left, 0, false);
            left.render(sb);
            renderArrow(sb, right, 0, true);
            right.render(sb);
            renderArrow(sb, up, 90, true);
            up.render(sb);
            renderArrow(sb, down, 90, false);
            down.render(sb);
        }
    }

    private void renderArrow(SpriteBatch sb, Hitbox hitbox, float rotation, boolean flipX) {
        Texture popupArrow = ImageMaster.POPUP_ARROW;
        sb.setColor(1, 1, 1, 1);
        sb.draw(popupArrow,
                hitbox.cX - popupArrow.getWidth() / 2f,
                hitbox.cY - popupArrow.getHeight() / 2f,
                popupArrow.getWidth() / 2f,
                popupArrow.getHeight() / 2f,
                popupArrow.getWidth(),
                popupArrow.getHeight(),
                Settings.scale * 0.8f,
                Settings.scale * 0.8f,
                rotation,
                0,
                0,
                popupArrow.getWidth(),
                popupArrow.getHeight(),
                flipX,
                false);

        if (hitbox.hovered) {
            sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            sb.setColor(1, 1, 1, 0.5f);
            sb.draw(popupArrow,
                    hitbox.cX - popupArrow.getWidth() / 2f,
                    hitbox.cY - popupArrow.getHeight() / 2f,
                    popupArrow.getWidth() / 2f,
                    popupArrow.getHeight() / 2f,
                    popupArrow.getWidth(),
                    popupArrow.getHeight(),
                    Settings.scale * 0.8f,
                    Settings.scale * 0.8f,
                    rotation,
                    0,
                    0,
                    popupArrow.getWidth(),
                    popupArrow.getHeight(),
                    flipX,
                    false);
            sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
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

    private float convertX(float x) {
        return x * 235.0F * Settings.scale + 640.0F * Settings.scale;
    }

    private float convertY(float y) {
        return y * -235.0F * Settings.scale + 850.0F * Settings.scale;
    }

    private enum Screen {
        INTRO,
        RULE_EXPLANATION,
        PLAY,
        COMPLETE
    }
}
