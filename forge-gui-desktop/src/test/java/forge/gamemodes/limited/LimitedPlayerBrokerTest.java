package forge.gamemodes.limited;

import forge.card.CardRarity;
import forge.card.CardRules;
import forge.deck.DeckSection;
import forge.gui.util.SGuiChoose;
import forge.item.PaperCard;
import org.mockito.MockedStatic;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class LimitedPlayerBrokerTest {
    @Test
    public void exchangeChoicesIncludeMainAndSideboard() {
        LimitedPlayer player = new LimitedPlayer(0, mock(IBoosterDraft.class));
        PaperCard mainCard = card("Main card");
        PaperCard sideboardCard = card("Sideboard card");
        player.getDeck().getMain().add(mainCard, 2);
        player.getDeck().getOrCreate(DeckSection.Sideboard).add(sideboardCard);

        try (MockedStatic<SGuiChoose> choices = mockStatic(SGuiChoose.class)) {
            choices.when(() -> SGuiChoose.oneOrNone(anyString(), anyCollection())).thenAnswer(invocation -> {
                Collection<PaperCard> cards = invocation.getArgument(1);
                assertEquals(cards.size(), 3);
                assertTrue(cards.contains(mainCard));
                assertTrue(cards.contains(sideboardCard));
                return mainCard;
            });

            assertSame(player.chooseExchangeCard(null), mainCard);
            assertSame(player.chooseExchangeCard(sideboardCard), mainCard);
        }
    }

    @Test
    public void cancellingBrokerDoesNotRequestOffersOrChangeDecks() {
        LimitedPlayer player = new LimitedPlayer(0, mock(IBoosterDraft.class));
        LimitedPlayer opponent = spy(new LimitedPlayer(1, mock(IBoosterDraft.class)));
        PaperCard mainCard = card("Main card");
        PaperCard offer = card("Offer");
        player.getDeck().getMain().add(mainCard);
        opponent.getDeck().getOrCreate(DeckSection.Sideboard).add(offer);
        player.dealBrokers = 1;
        doReturn(offer).when(opponent).chooseExchangeCard(any());

        try (MockedStatic<SGuiChoose> choices = mockStatic(SGuiChoose.class)) {
            choices.when(() -> SGuiChoose.oneOrNone(anyString(), anyCollection())).thenReturn(null);
            player.activateBrokers(List.of(player, opponent));
        }

        verify(opponent, never()).chooseExchangeCard(any());
        assertFalse(player.hasBrokers());
        assertEquals(player.getDeck().getMain().count(mainCard), 1);
        assertEquals(opponent.getDeck().get(DeckSection.Sideboard).count(offer), 1);
    }

    @DataProvider
    public Object[][] exchangeSections() {
        return new Object[][] {
                {DeckSection.Main, DeckSection.Main},
                {DeckSection.Main, DeckSection.Sideboard},
                {DeckSection.Sideboard, DeckSection.Main},
                {DeckSection.Sideboard, DeckSection.Sideboard}
        };
    }

    @Test(dataProvider = "exchangeSections")
    public void acceptedTradePreservesOtherCardsAndCreatesSideboards(DeckSection source, DeckSection otherSource) {
        LimitedPlayer player = new LimitedPlayer(0, mock(IBoosterDraft.class));
        LimitedPlayer opponent = new LimitedPlayer(1, mock(IBoosterDraft.class));
        PaperCard exchangeCard = card("Exchange card");
        PaperCard offer = card("Offer");
        PaperCard retainedCard = card("Retained card");
        player.getDeck().getOrCreate(source).add(exchangeCard);
        opponent.getDeck().getOrCreate(otherSource).add(offer);
        player.getDeck().getMain().add(retainedCard, 2);
        opponent.getDeck().getMain().add(retainedCard);

        player.exchangeAcceptedOffer(exchangeCard, opponent, offer);

        assertEquals(player.getDeck().getAllCardsInASinglePool().count(exchangeCard), 0);
        assertEquals(opponent.getDeck().getAllCardsInASinglePool().count(offer), 0);
        assertEquals(player.getDeck().get(DeckSection.Sideboard).count(offer), 1);
        assertEquals(opponent.getDeck().get(DeckSection.Sideboard).count(exchangeCard), 1);
        assertEquals(player.getDeck().getMain().count(retainedCard), 2);
        assertEquals(opponent.getDeck().getMain().count(retainedCard), 1);
        assertEquals(player.getDeck().getAllCardsInASinglePool().countAll(), 3);
        assertEquals(opponent.getDeck().getAllCardsInASinglePool().countAll(), 2);
    }

    private static PaperCard card(String name) {
        return new PaperCard(CardRules.getUnsupportedCardNamed(name), "Test", CardRarity.Common);
    }
}
