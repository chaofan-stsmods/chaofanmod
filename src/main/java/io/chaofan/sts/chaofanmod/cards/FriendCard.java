package io.chaofan.sts.chaofanmod.cards;

import basemod.ReflectionHacks;
import basemod.abstracts.CustomCard;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.codedisaster.steamworks.SteamID;
import com.evacipated.cardcrawl.modthespire.lib.SpireOverride;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.chaofanmod.ChaofanMod;
import io.chaofan.sts.chaofanmod.cards.friendcard.FriendCardProperty;
import io.chaofan.sts.chaofanmod.utils.TextureLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static io.chaofan.sts.chaofanmod.ChaofanMod.getImagePath;
import static io.chaofan.sts.chaofanmod.ChaofanMod.getShaderPath;
import static io.chaofan.sts.chaofanmod.cards.CardBase.getCardStrings;
import static io.chaofan.sts.chaofanmod.cards.CardBase.makeCardId;

@SuppressWarnings("unused")
public class FriendCard extends CustomCard {
    public static final String ID = makeCardId(FriendCard.class.getSimpleName());

    private static final CardStrings cardStrings = getCardStrings(ID);
    private static final ShaderProgram shader;

    static {
        shader = new ShaderProgram(
                Gdx.files.internal(getShaderPath("common.vs")).readString(),
                Gdx.files.internal(getShaderPath("friendcard.fs")).readString());
        if (!shader.isCompiled()) {
            throw new RuntimeException(shader.getLog());
        }
    }

    public boolean displayingUpgrades;
    public boolean reduceCostOnUpgrade;

    private Texture maskImage;
    private boolean noFriend = false;
    private SteamID friend;
    private long seed = 0;

    public int baseSecondaryDamage;
    public int secondaryDamage;
    public boolean upgradedSecondaryDamage;
    public boolean isSecondaryDamageModified;
    public int[] secondaryMultiDamage;

    public int baseSecondaryBlock;
    public int secondaryBlock;
    public boolean upgradedSecondaryBlock;
    public boolean isSecondaryBlockModified;

    public final List<FriendCardProperty> properties = new ArrayList<>();

    public FriendCard(SteamID friend) {
        this(SteamID.getNativeHandle(friend) * Settings.seed);
        this.friend = friend;
        this.name = String.format(cardStrings.EXTENDED_DESCRIPTION[0], ChaofanMod.steamworksHelper.getFriendName(friend));
        ChaofanMod.steamworksHelper.getFriendAvatar(friend, (texture) -> {
            if (texture != null) {
                this.portrait = new TextureAtlas.AtlasRegion(texture, 0, 0, texture.getWidth(), texture.getHeight());
            }
        });
    }

    public FriendCard(boolean noFriend) {
        this(noFriend, AbstractDungeon.cardRandomRng.randomLong());
    }

    public FriendCard(boolean noFriend, long seed) {
        this(seed);
        this.name = String.format(cardStrings.EXTENDED_DESCRIPTION[0], "Chaofan");
        this.textureImg = getImagePath("cards/chaofanAvatar.jpg");
        this.loadCardImage(this.textureImg);
        this.noFriend = true;
    }

    public FriendCard(long seed) {
        this();
        this.seed = seed;

        Random random = new Random(seed);
        boolean isCost3 = random.nextInt(10) == 0;
        int costRng = random.nextInt(5);
        this.cost = this.costForTurn = isCost3 ? 3 : (costRng == 4 ? 2 : (costRng == 3 ? 0 : 1));
        this.type = random.nextInt(5) < 3 ? CardType.ATTACK : CardType.SKILL;

        FriendCardProperty.addProperties(this, properties, random);
        FriendCardProperty.applyProperties(this, properties);

        this.maskImage = TextureLoader.getTexture(getImagePath("cards/" + this.type.toString().toLowerCase() + ".png"));
    }

    public FriendCard() {
        super(ID, cardStrings.NAME, getImagePath("cards/defaultAvatar.jpg"), -2, cardStrings.DESCRIPTION, CardType.SKILL, CardColor.COLORLESS, CardRarity.SPECIAL, CardTarget.NONE);
        this.maskImage = TextureLoader.getTexture(getImagePath("cards/" + this.type.toString().toLowerCase() + ".png"));
    }

    @Override
    public void initializeDescription() {
        super.initializeDescription();
        keywords.remove("[W]");
    }

    @Override
    public void upgrade() {
        if (!upgraded) {
            upgradeName();
            if (reduceCostOnUpgrade && this.cost > 0) {
                upgradeBaseCost(this.cost - 1);
            }
            FriendCardProperty.upgrade(this, properties);
        }
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        int oldDamage = damage;
        int oldBaseDamage = baseDamage;
        boolean oldIsDamageModified = isDamageModified;
        int[] oldMultiDamage = multiDamage;
        int oldBlock = block;
        int oldBaseBlock = baseBlock;
        boolean oldIsBlockModified = isBlockModified;

        baseDamage = damage = baseSecondaryDamage;
        baseBlock = block = baseSecondaryBlock;
        super.applyPowers();
        secondaryDamage = damage;
        isSecondaryDamageModified = isDamageModified;
        secondaryMultiDamage = multiDamage;
        secondaryBlock = block;
        isSecondaryBlockModified = isBlockModified;

        damage = oldDamage;
        baseDamage = oldBaseDamage;
        isDamageModified = oldIsDamageModified;
        multiDamage = oldMultiDamage;
        block = oldBlock;
        baseBlock = oldBaseBlock;
        isBlockModified = oldIsBlockModified;
    }

    @Override
    public void calculateCardDamage(AbstractMonster mo) {
        super.calculateCardDamage(mo);
        int oldDamage = damage;
        int oldBaseDamage = baseDamage;
        boolean oldIsDamageModified = isDamageModified;
        int[] oldMultiDamage = multiDamage;
        int oldBlock = block;
        int oldBaseBlock = baseBlock;
        boolean oldIsBlockModified = isBlockModified;

        baseDamage = damage = baseSecondaryDamage;
        baseBlock = block = baseSecondaryBlock;
        super.calculateCardDamage(mo);
        secondaryDamage = damage;
        isSecondaryDamageModified = isDamageModified;
        secondaryMultiDamage = multiDamage;
        secondaryBlock = block;
        isSecondaryBlockModified = isBlockModified;

        damage = oldDamage;
        baseDamage = oldBaseDamage;
        isDamageModified = oldIsDamageModified;
        multiDamage = oldMultiDamage;
        block = oldBlock;
        baseBlock = oldBaseBlock;
        isBlockModified = oldIsBlockModified;
    }

    @Override
    public void displayUpgrades() {
        super.displayUpgrades();

        if (this.upgradedSecondaryDamage) {
            this.secondaryDamage = this.baseSecondaryDamage;
            this.isSecondaryDamageModified = true;
        }

        if (this.upgradedSecondaryBlock) {
            this.secondaryBlock = this.baseSecondaryBlock;
            this.isSecondaryBlockModified = true;
        }

        FriendCardProperty.displayUpgrades(this, properties);
    }

    @Override
    public void resetAttributes() {
        super.resetAttributes();
        this.secondaryDamage = this.baseSecondaryDamage;
        this.isSecondaryDamageModified = false;
        this.secondaryBlock = this.baseSecondaryBlock;
        this.isSecondaryBlockModified = false;
    }

    @Override
    public void onMoveToDiscard() {
        super.onMoveToDiscard();
    }

    @Override
    public void use(AbstractPlayer abstractPlayer, AbstractMonster abstractMonster) {
        FriendCardProperty.use(properties, abstractPlayer, abstractMonster);
    }

    @Override
    public void triggerOnGlowCheck() {
        this.glowColor = AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
        if (properties.stream().anyMatch(FriendCardProperty::glowCheck)) {
            this.glowColor = AbstractCard.GOLD_BORDER_GLOW_COLOR.cpy();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        if (this.friend != null) {
            return new FriendCard(friend);
        } else if (this.noFriend) {
            return new FriendCard(true, seed);
        } else {
            return new FriendCard();
        }
    }

    @SpireOverride
    protected void renderPortrait(SpriteBatch sb) {
        Color color = ReflectionHacks.getPrivate(this, AbstractCard.class, "renderColor");
        float drawX = this.current_x - 125.0F;
        float drawY = this.current_y - 95.0F;

        if (!this.isLocked) {
            if (this.portrait != null) {
                drawX = this.current_x - (float)this.portrait.packedWidth / 2.0F;
                drawY = this.current_y - (float)this.portrait.packedHeight / 2.0F;
                float scale = 250f / this.portrait.packedWidth;
                sb.end();
                sb.setShader(shader);
                this.maskImage.bind(1);
                this.portrait.getTexture().bind(0);
                sb.setColor(color);
                sb.begin();
                shader.setUniformi("u_mask", 1);
                sb.draw(this.portrait, drawX, drawY + 72.0F / scale, (float)this.portrait.packedWidth / 2.0F, (float)this.portrait.packedHeight / 2.0F - 72.0F / scale, (float)this.portrait.packedWidth, (float)this.portrait.packedHeight, this.drawScale * Settings.scale * scale, this.drawScale * Settings.scale * scale, this.angle);
                sb.flush();
                sb.end();
                sb.setShader(null);
                sb.begin();
            }
        } else {
            sb.draw(this.portraitImg, drawX, drawY + 72.0F, 125.0F, 23.0F, 250.0F, 190.0F, this.drawScale * Settings.scale, this.drawScale * Settings.scale, this.angle, 0, 0, 250, 190, false, false);
        }
    }

    private FriendCard makeFriendCard() {
        List<SteamID> friends = ChaofanMod.steamworksHelper.getFriends();
        if (friends.size() == 0) {
            return new FriendCard(true);
        }

        SteamID friend = friends.get(AbstractDungeon.cardRandomRng.random(friends.size() - 1));
        return new FriendCard(friend);
    }

    @Override
    public void upgradeDamage(int amount) {
        super.upgradeDamage(amount);
    }

    @Override
    public void upgradeBlock(int amount) {
        super.upgradeBlock(amount);
    }

    @Override
    public void upgradeMagicNumber(int amount) {
        super.upgradeMagicNumber(amount);
    }

    public void upgradeSecondaryDamage(int amount) {
        this.baseSecondaryDamage += amount;
        this.upgradedSecondaryDamage = true;
    }

    public void upgradeSecondaryBlock(int amount) {
        this.baseSecondaryBlock += amount;
        this.upgradedSecondaryBlock = true;
    }

    public void setMultiDamage(boolean value) {
        this.isMultiDamage = value;
    }
}
