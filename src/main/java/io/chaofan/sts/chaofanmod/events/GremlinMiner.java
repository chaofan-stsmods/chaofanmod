package io.chaofan.sts.chaofanmod.events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

import java.util.ArrayList;
import java.util.Comparator;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.makeId;

public class GremlinMiner extends AbstractImageEvent {
    public static final String ID = makeId("event.GrimlinMiner");
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString(ID);
    public static final String NAME = eventStrings.NAME;
    public static final String[] DESCRIPTIONS = eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = eventStrings.OPTIONS;

    private static final float ROPE_TIMER_DURATION = 4f;
    private static final float ROPE_START_LENGTH = 100f;

    private Screen screen;
    private final Texture img;
    private final TextureRegion gold;
    private final TextureRegion floor;
    private final TextureRegion rope;
    private final TextureRegion hook;
    private final TextureRegion winch;
    private final TextureRegion handle;
    private final int price = getPrice();
    private float ropeLength = ROPE_START_LENGTH;
    private float ropeRotation = 0f;
    private float ropeTimer;
    private boolean ropeReleasing = false;
    private boolean ropePulling = false;
    private RewardInfo hookHolding;
    private float timer = 60;
    private final TimerEffect timerEffect = new TimerEffect();
    private final ArrayList<RewardInfo> rewards = new ArrayList<>();

    public GremlinMiner() {
        super(NAME, DESCRIPTIONS[0], getImagePath("events/miner.jpg"));
        this.screen = Screen.INTRO;
        this.imageEventText.setDialogOption(OPTIONS[0]);
        this.img = ImageMaster.loadImage(getImagePath("ui/miner.png"));
        this.gold = new TextureRegion(this.img, 1, 1, 147, 137);
        this.floor = new TextureRegion(this.img, 0, 241, 256, 15);
        this.rope = new TextureRegion(this.img, 149, 0, 9, 64);
        this.hook = new TextureRegion(this.img, 159, 1, 67, 58);
        this.winch = new TextureRegion(this.img, 1, 139, 233, 101);
        this.handle = new TextureRegion(this.img, 149, 65, 97, 33);
    }

    @Override
    public void update() {
        super.update();

        if (screen == Screen.PLAY) {
            float ropeCenterX = 1920 / 2f;
            float ropeCenterY = 411 + 37;
            if (ropeReleasing) {
                float sinR = MathUtils.sinDeg(ropeRotation);
                float cosR = MathUtils.cosDeg(ropeRotation);
                float oldHookX = ropeCenterX + sinR * (ropeLength + 30);
                float oldHookY = ropeCenterY - cosR * (ropeLength + 30);
                ropeLength += Gdx.graphics.getDeltaTime() * 800;
                float hookX = ropeCenterX + sinR * (ropeLength + 30);
                float hookY = ropeCenterY - cosR * (ropeLength + 30);
                if (hookX <= 0 || hookX >= 1920 || hookY <= -540) {
                    ropeReleasing = false;
                    ropePulling = true;
                } else {
                    for (RewardInfo reward : rewards) {
                        if (reward.collideWith(hookX, hookY, oldHookX, oldHookY)) {
                            hookHolding = reward;
                            ropeReleasing = false;
                            ropePulling = true;
                            break;
                        }
                    }
                }

            } else if (ropePulling) {
                if (hookHolding != null) {
                    float hookX = ropeCenterX + MathUtils.sinDeg(ropeRotation) * (ropeLength + 20 + hookHolding.size / 2);
                    float hookY = ropeCenterY - MathUtils.cosDeg(ropeRotation) * (ropeLength + 20 + hookHolding.size / 2);
                    hookHolding.x = hookX;
                    hookHolding.y = hookY;
                    ropeLength -= Gdx.graphics.getDeltaTime() * Math.min(800, 1000 / hookHolding.weight);
                } else {
                    ropeLength -= Gdx.graphics.getDeltaTime() * 800;
                }

                if (ropeLength <= 0) {
                    ropeLength = 1;
                    ropePulling = false;
                    if (hookHolding != null) {
                        hookHolding.collect();
                        hookHolding = null;
                    }
                }

            } else {
                if (ropeLength < ROPE_START_LENGTH) {
                    ropeLength += Gdx.graphics.getDeltaTime() * 800;
                    if (ropeLength > ROPE_START_LENGTH) {
                        ropeLength = ROPE_START_LENGTH;
                    }
                }

                ropeTimer -= Gdx.graphics.getDeltaTime();
                if (ropeTimer < 0) {
                    ropeTimer = ROPE_TIMER_DURATION;
                }
                ropeRotation = MathUtils.sinDeg(ropeTimer / ROPE_TIMER_DURATION * 360f) * 80;

                if (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed()) {
                    InputHelper.justClickedLeft = false;
                    CInputActionSet.select.unpress();
                    ropeReleasing = true;
                }
            }

            timer -= Gdx.graphics.getDeltaTime();
            if (timer < 0) {
                GenericEventDialog.show();
                screen = Screen.GET_REWARD;
                AbstractDungeon.topLevelEffects.remove(timerEffect);
                this.imageEventText.updateBodyText(DESCRIPTIONS[3]);
                this.imageEventText.removeDialogOption(1);
                this.imageEventText.removeDialogOption(0);
                this.imageEventText.setDialogOption(OPTIONS[0]);
                ArrayList<String> cardsObtained = new ArrayList<>();
                int goldGain = 0;
                for (RewardInfo reward : rewards) {
                    if (reward.collected) {
                        if (reward.card != null) {
                            cardsObtained.add(reward.card.cardID);
                        } else {
                            goldGain += reward.gold;
                        }
                    }
                }
                logMetric("Miner", "Obtain", cardsObtained,
                        null, null, null, null,
                        null, null, 0, 0, 0,
                        0, goldGain, price);
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        super.render(sb);

        if (screen == Screen.PLAY) {
            float scale = Settings.WIDTH / 1920f;
            sb.setColor(Color.WHITE);
            sb.draw(this.winch,
                    (Settings.WIDTH - this.winch.getRegionWidth() * scale) / 2f,
                    Settings.HEIGHT / 2f + 411 * scale,
                    this.winch.getRegionWidth() * scale,
                    this.winch.getRegionHeight() * scale);
            sb.draw(this.handle,
                    (Settings.WIDTH - this.winch.getRegionWidth() * scale) / 2f + 188 * scale,
                    Settings.HEIGHT / 2f + 411 * scale + 37 * scale,
                    this.handle.getRegionWidth() * scale,
                    this.handle.getRegionHeight() * scale);
            sb.draw(this.floor, 0, Settings.HEIGHT / 2f + 400 * scale, Settings.WIDTH, 15 * scale);

            for (RewardInfo reward : rewards) {
                reward.render(sb, scale);
            }

            sb.setColor(Color.WHITE);
            float ropeCenterX = Settings.WIDTH / 2f;
            float ropeCenterY = Settings.HEIGHT / 2f + 411 * scale + 37 * scale;
            sb.draw(this.rope,
                    ropeCenterX - this.rope.getRegionWidth() / 2f,
                    ropeCenterY - this.rope.getRegionHeight(),
                    this.rope.getRegionWidth() / 2f,
                    this.rope.getRegionHeight(),
                    this.rope.getRegionWidth(),
                    this.rope.getRegionHeight(),
                    scale,
                    scale * (ropeLength / 64),
                    ropeRotation);
            sb.draw(this.hook,
                    ropeCenterX + MathUtils.sinDeg(ropeRotation) * ropeLength * scale - this.hook.getRegionWidth() / 2f,
                    ropeCenterY - MathUtils.cosDeg(ropeRotation) * ropeLength * scale - 55f,
                    this.hook.getRegionWidth() / 2f,
                    55f,
                    this.hook.getRegionWidth(),
                    this.hook.getRegionHeight(),
                    scale,
                    scale,
                    ropeRotation);

            if (Settings.isControllerMode) {
                sb.setColor(Color.WHITE);
                TextureRegion texture = new TextureRegion(CInputActionSet.select.getKeyImg());
                sb.draw(texture,
                        (Settings.WIDTH - this.winch.getRegionWidth() * scale) / 2f + 233 * scale - texture.getRegionWidth() / 2f,
                        Settings.HEIGHT / 2f + 411 * scale + 28 * scale - texture.getRegionHeight() / 2f,
                        texture.getRegionWidth() / 2f,
                        texture.getRegionHeight() / 2f,
                        texture.getRegionWidth(),
                        texture.getRegionHeight(),
                        scale,
                        scale,
                        0);
            }
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch(this.screen) {
            case INTRO:
                this.imageEventText.updateBodyText(DESCRIPTIONS[1]);
                this.imageEventText.updateDialogOption(0, String.format(OPTIONS[2], this.price));
                this.imageEventText.setDialogOption(OPTIONS[1]);
                this.screen = Screen.RULE_EXPLANATION;
                return;
            case RULE_EXPLANATION:
                this.imageEventText.removeDialogOption(1);
                this.imageEventText.removeDialogOption(0);
                if (buttonPressed == 1) {
                    this.imageEventText.updateBodyText(DESCRIPTIONS[4]);
                    this.imageEventText.setDialogOption(OPTIONS[1]);
                    this.screen = Screen.COMPLETE;
                } else {
                    GenericEventDialog.hide();
                    AbstractDungeon.player.loseGold(this.price);
                    playBegin();
                    CInputActionSet.select.unpress();
                    this.screen = Screen.PLAY;
                }
                return;
            case GET_REWARD:
                this.imageEventText.updateBodyText(DESCRIPTIONS[2]);
                this.imageEventText.removeDialogOption(0);
                this.imageEventText.setDialogOption(OPTIONS[1]);
                this.screen = Screen.COMPLETE;
                return;
            case COMPLETE:
                this.openMap();
        }
    }

    private void playBegin() {
        // 260 gold - small(10) x 8, medium(20) x 4, large(50) x 2
        // a0: 6 cards - 2 rare, 3 uncommon, 1 common
        // a15: 7 cards - 2 rare, 2 uncommon, 1 common, 2 curses

        AbstractDungeon.topLevelEffects.add(timerEffect);

        RewardInfo center = new RewardInfo();
        center.gold = 1;
        center.x = 1920 / 2f;
        center.y = 400;
        center.scale = 1;
        center.size = 200;
        rewards.add(center);

        for (int i = 0; i < 2; i++) {
            Random r = new Random(AbstractDungeon.miscRng.randomLong());
            float scale = r.random(0.2f, 0.6f);
            float size = scale * 400f;
            RewardInfo reward = new RewardInfo();
            if (!addCard(reward, AbstractCard.CardRarity.RARE, r)) {
                break;
            }
            reward.rotation = r.random(0, 360);
            reward.weight = 8;
            if (applyPosition(reward, scale, size, r, -540 + size / 2, -size / 2)) {
                this.rewards.add(reward);
            }
        }

        int uncommonCards = AbstractDungeon.ascensionLevel >= 15 ? 2 : 3;
        for (int i = 0; i < uncommonCards; i++) {
            Random r = new Random(AbstractDungeon.miscRng.randomLong());
            float scale = r.random(0.24f, 0.6f);
            float size = scale * 400f;
            RewardInfo reward = new RewardInfo();
            if (!addCard(reward, AbstractCard.CardRarity.UNCOMMON, r)) {
                break;
            }
            reward.rotation = r.random(0, 360);
            reward.weight = 10 * scale;
            if (applyPosition(reward, scale, size, r, -270 + size / 2, 270 - size / 2)) {
                this.rewards.add(reward);
            }
        }

        for (int i = 0; i < 1; i++) {
            Random r = new Random(AbstractDungeon.miscRng.randomLong());
            float scale = r.random(0.24f, 0.4f);
            float size = scale * 400f;
            RewardInfo reward = new RewardInfo();
            if (!addCard(reward, AbstractCard.CardRarity.COMMON, r)) {
                break;
            }
            reward.rotation = r.random(0, 360);
            reward.weight = 5 * scale;
            if (applyPosition(reward, scale, size, r, size / 2, 400 - size / 2)) {
                this.rewards.add(reward);
            }
        }

        if (AbstractDungeon.ascensionLevel >= 15) {
            for (int i = 0; i < 2; i++) {
                Random r = new Random(AbstractDungeon.miscRng.randomLong());
                float scale = r.random(0.24f, 0.4f);
                float size = scale * 400f;
                RewardInfo reward = new RewardInfo();
                reward.card = AbstractDungeon.getCard(AbstractCard.CardRarity.CURSE, r);
                reward.rotation = r.random(0, 360);
                reward.weight = 2;
                if (applyPosition(reward, scale, size, r, -540 + size / 2, 400 - size / 2)) {
                    this.rewards.add(reward);
                }
            }
        }

        for (int i = 0; i < 2; i++) {
            Random r = new Random(AbstractDungeon.miscRng.randomLong());
            float scale = 2.0f;
            float size = scale * 145f;
            RewardInfo reward = new RewardInfo();
            reward.gold = 50;
            reward.rotation = r.random(-50, 50);
            reward.weight = 8;
            if (applyPosition(reward, scale, size, r, -540 + size / 2, -size / 2)) {
                this.rewards.add(reward);
            }
        }

        for (int i = 0; i < 4; i++) {
            Random r = new Random(AbstractDungeon.miscRng.randomLong());
            float scale = 0.8f;
            float size = scale * 145f;
            RewardInfo reward = new RewardInfo();
            reward.gold = 20;
            reward.rotation = r.random(-50, 50);
            reward.weight = 4;
            if (applyPosition(reward, scale, size, r, -270 + size / 2, 270 - size / 2)) {
                this.rewards.add(reward);
            }
        }

        for (int i = 0; i < 8; i++) {
            Random r = new Random(AbstractDungeon.miscRng.randomLong());
            float scale = 0.4f;
            float size = scale * 145f;
            RewardInfo reward = new RewardInfo();
            reward.gold = 10;
            reward.rotation = r.random(-50, 50);
            reward.weight = 1;
            if (applyPosition(reward, scale, size, r, -270 + size / 2, 400 - size / 2)) {
                this.rewards.add(reward);
            }
        }

        rewards.remove(center);
        rewards.sort(Comparator.comparing(re -> {
            float dx = re.x - center.x;
            float dy = re.y - center.y;
            return dx * dx + dy * dy;
        }));

        ropeTimer = AbstractDungeon.miscRng.random(ROPE_TIMER_DURATION);
    }

    private boolean addCard(RewardInfo reward, AbstractCard.CardRarity rarity, Random r) {
        int tryCount = 20;
        do {
            reward.card = AbstractDungeon.getCard(rarity, r);
            tryCount--;
        } while (tryCount > 0 && this.rewards.stream().anyMatch(re -> re.isSameCard(reward)));
        return tryCount > 0;
    }

    private boolean applyPosition(RewardInfo reward, float scale, float size, Random r, float yMin, float yMax) {
        reward.scale = scale;
        reward.size = size;
        int tryCount = 20;
        do {
            reward.x = r.random(size / 2, 1920 - size / 2);
            reward.y = r.random(yMin, yMax);
            tryCount--;
        } while (tryCount > 0 && this.rewards.stream().anyMatch(re -> re.collideWith(reward)));
        return tryCount > 0;
    }

    @Override
    public void dispose() {
        super.dispose();
        this.img.dispose();
    }

    public static int getPrice() {
        if (AbstractDungeon.ascensionLevel >= 15) {
            return 75;
        }
        return 50;
    }

    private enum Screen {
        INTRO,
        RULE_EXPLANATION,
        PLAY,
        GET_REWARD,
        COMPLETE,
    }

    private class RewardInfo {
        int gold;
        AbstractCard card;
        float rotation;
        float weight;
        float scale;
        float x;
        float y;
        float size;
        Hitbox hb = new Hitbox(0, 0);
        boolean collected = false;

        public void render(SpriteBatch sb, float scale) {
            if (collected) {
                return;
            }

            hb.resize(size * scale, size * scale);
            hb.move(x * scale, y * scale + Settings.HEIGHT / 2f);
            if (card != null) {
                card.target_x = card.current_x = hb.cX;
                card.target_y = card.current_y = hb.cY;
                card.targetDrawScale = card.drawScale = this.scale;
                card.targetAngle = card.angle = rotation;
                card.render(sb);
            }
            if (gold > 0) {
                sb.setColor(Color.WHITE);
                TextureRegion img = GremlinMiner.this.gold;
                sb.draw(img,
                        hb.cX - img.getRegionWidth() / 2f,
                        hb.cY - img.getRegionHeight() / 2f,
                        img.getRegionWidth() / 2f,
                        img.getRegionHeight() / 2f,
                        img.getRegionWidth(),
                        img.getRegionHeight(),
                        scale * this.scale,
                        scale * this.scale,
                        this.rotation);
            }
            hb.render(sb);
        }

        public boolean collideWith(RewardInfo reward) {
            if (collected) {
                return false;
            }

            float dx = x - reward.x;
            float dy = y - reward.y;
            float s = (size + reward.size) / 2 * 1.2f;
            return dx * dx + dy * dy < s * s;
        }

        public boolean isSameCard(RewardInfo reward) {
            if (collected) {
                return false;
            }

            return this.card != null && reward.card != null && this.card.cardID.equals(reward.card.cardID);
        }

        public boolean collideWith(float hookX, float hookY) {
            if (collected) {
                return false;
            }

            float dx = x - hookX;
            float dy = y - hookY;
            float s = (size + 67) / 2;
            return dx * dx + dy * dy < s * s;
        }

        public boolean collideWith(float x1, float y1, float x2, float y2) {
            if (collected) {
                return false;
            }

            float dx = x2 - x1;
            float dy = y2 - y1;
            float x0;
            float y0;
            if (dx == 0 && dy == 0) {
                return collideWith(x1, y1);
            }
            if (dx == 0) {
                x0 = x1;
                y0 = y;
            }
            else if (dy == 0) {
                x0 = x;
                y0 = y1;
            }
            else {
                float k = dy / dx;
                float k2 = - dx / dy;
                float b = y1 - k * x1;
                float b2 = y - k2 * x;
                x0 = (b2 - b) / (k - k2);
                y0 = k * x0 + b;
            }

            float ds = (x0 - x) * (x0 - x) + (y0 - y) * (y0 - y);
            float s = (size + 67) / 2;
            return (ds <= s * s &&
                    x0 >= Math.min(x1, x2) && x0 <= Math.max(x1, x2) &&
                    y0 >= Math.min(y1, y2) && y0 <= Math.max(y1, y2));
        }

        public void collect() {
            if (collected) {
                return;
            }

            collected = true;
            if (card != null) {
                card.targetAngle = 0;
                AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(card, Settings.WIDTH / 2f, Settings.HEIGHT / 2f));
            } else {
                int roll = MathUtils.random(2);
                switch (roll) {
                    case 0:
                        CardCrawlGame.sound.play("GOLD_GAIN", 0.1F);
                        break;
                    case 1:
                        CardCrawlGame.sound.play("GOLD_GAIN_3", 0.1F);
                        break;
                    default:
                        CardCrawlGame.sound.play("GOLD_GAIN_5", 0.1F);
                }
                AbstractDungeon.player.gainGold(gold);
            }
        }
    }

    private class TimerEffect extends AbstractGameEffect {

        @Override
        public void update() {
            if (timer <= 0) {
                isDone = true;
            }
        }

        @Override
        public void render(SpriteBatch sb) {
            if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) {
                float TIME_X_POS = Settings.WIDTH - (380.0f + 192.0f + 45.0f) * Settings.scale;
                float titleY = Settings.HEIGHT - 28.0F * Settings.scale;
                sb.setColor(Color.WHITE);
                sb.draw(ImageMaster.TIMER_ICON, TIME_X_POS, Settings.HEIGHT - 64.0f * Settings.scale, 64.0f * Settings.scale, 64.0f * Settings.scale);
                Color clockColor = Settings.GOLD_COLOR;
                FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont,
                        String.valueOf((int) timer), TIME_X_POS + 60.0f * Settings.scale, titleY, clockColor);
            }
        }

        @Override
        public void dispose() {

        }
    }
}
