package io.chaofan.sts.chaofanmod.actions.common;

import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SelectFromGridAction extends AbstractGameAction {

    private final BiPredicate<Source, AbstractCard> cardSelector;
    private final BiConsumer<Source[], AbstractCard[]> then;
    private final String uiString;
    private final int numCards;
    private final boolean anyNumber;
    private final BiConsumer<Source, AbstractCard> beforeShow;
    private final BiConsumer<Source, AbstractCard> afterShow;
    private final List<AbstractCard> specialCards;
    private boolean complete = false;

    private List<AbstractCard> selectedCards;

    public SelectFromGridAction(BiPredicate<Source, AbstractCard> cardSelector,
                                BiConsumer<Source[], AbstractCard[]> then,
                                String uiString,
                                ActionType actionType,
                                int numCards,
                                boolean anyNumber) {
        this(cardSelector, then, uiString, actionType, numCards, anyNumber, null, null);
    }

    public SelectFromGridAction(BiPredicate<Source, AbstractCard> cardSelector,
                                BiConsumer<Source[], AbstractCard[]> then,
                                String uiString,
                                ActionType actionType,
                                int numCards,
                                boolean anyNumber,
                                BiConsumer<Source, AbstractCard> beforeShow,
                                BiConsumer<Source, AbstractCard> afterShow) {
        this(cardSelector, then, uiString, actionType, numCards, anyNumber, beforeShow, afterShow, null);
    }

    public SelectFromGridAction(List<AbstractCard> cards,
                                BiConsumer<Source[], AbstractCard[]> then,
                                String uiString,
                                ActionType actionType,
                                int numCards,
                                boolean anyNumber) {
        this((s, c) -> s == Source.NONE, then, uiString, actionType, numCards, anyNumber, null, null, cards);
    }

    public SelectFromGridAction(BiPredicate<Source, AbstractCard> cardSelector,
                                BiConsumer<Source[], AbstractCard[]> then,
                                String uiString,
                                ActionType actionType,
                                int numCards,
                                boolean anyNumber,
                                BiConsumer<Source, AbstractCard> beforeShow,
                                BiConsumer<Source, AbstractCard> afterShow,
                                List<AbstractCard> specialCards) {
        this.anyNumber = anyNumber;
        this.beforeShow = beforeShow;
        this.afterShow = afterShow;
        this.specialCards = specialCards;
        this.actionType = actionType;
        this.cardSelector = cardSelector;
        this.then = then;
        this.uiString = uiString;
        this.numCards = numCards;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        AbstractPlayer player = AbstractDungeon.player;
        if (duration == Settings.ACTION_DUR_FAST) {
            selectedCards = Stream.of(
                    filterCards(player.hand, Source.HAND),
                    filterCards(player.drawPile, Source.DRAW_PILE),
                    filterCards(player.discardPile, Source.DISCARD_PILE),
                    filterSpecialCards(this.specialCards))
                    .flatMap(g -> g)
                    .collect(Collectors.toList());
            if (selectedCards.isEmpty()) {
                then.accept(new Source[0], new AbstractCard[0]);
                isDone = true;
                return;
            }

            CardGroup group = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
            for (AbstractCard card : selectedCards) {
                group.addToTop(card);
                if (beforeShow != null) {
                    beforeShow.accept(getSource(player, card), card);
                }
                card.stopGlowing();
            }

            AbstractDungeon.gridSelectScreen.open(group, Math.min(numCards, selectedCards.size()), anyNumber, uiString);

        } else if (!complete && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE) {
            List<AbstractCard> cards = new ArrayList<>(AbstractDungeon.gridSelectScreen.selectedCards);
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            cards.forEach(AbstractCard::stopGlowing);
            
            then.accept(cards.stream().map(card -> getSource(player, card)).toArray(Source[]::new), cards.toArray(new AbstractCard[0]));
            complete = true;

            for (AbstractCard selectedCard : selectedCards) {
                if (afterShow != null) {
                    afterShow.accept(getSource(player, selectedCard), selectedCard);
                }
            }
        }

        tickDuration();
    }

    public static BiConsumer<Source[], AbstractCard[]> acceptMultipleCards(BiConsumer<Source, AbstractCard> singleThen) {
        return (sources, cards) -> {
            for (int i = 0; i < cards.length; i++) {
                Source source = sources[i];
                AbstractCard card = cards[i];
                singleThen.accept(source, card);
            }
        };
    }

    private Source getSource(AbstractPlayer player, AbstractCard card) {
        return player.hand.contains(card) ? Source.HAND :
                player.drawPile.contains(card) ? Source.DRAW_PILE :
                player.discardPile.contains(card) ? Source.DISCARD_PILE :
                Source.NONE;
    }

    private Stream<AbstractCard> filterCards(CardGroup cardGroup, Source source) {
        return cardGroup.group.stream().filter(c -> cardSelector.test(source, c));
    }

    private Stream<AbstractCard> filterSpecialCards(List<AbstractCard> cards) {
        if (cards == null) {
            return Stream.of();
        }

        return cards.stream().filter(c -> cardSelector.test(Source.NONE, c));
    }

    public enum Source {
        NONE,
        DRAW_PILE,
        DISCARD_PILE,
        HAND,
    }
}
