package forge.gamemodes.limited;

import forge.card.CardRarity;
import forge.card.CardRules;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
        player.setPromptSink((prompt, blocking, onAnswer) -> {
            List<?> cards = prompt.options();
            assertEquals(cards.size(), 3);
            assertTrue(cards.contains(mainCard));
            assertTrue(cards.contains(sideboardCard));
            onAnswer.accept(List.of(cards.indexOf(mainCard)));
        });

        List<PaperCard> chosen = new ArrayList<>();
        player.chooseExchangeCard(null, chosen::add);
        player.chooseExchangeCard(sideboardCard, chosen::add);
        assertEquals(chosen, List.of(mainCard, mainCard));
    }

    @Test
    public void cancellingBrokerDoesNotRequestOffersOrChangeDecks() {
        LimitedPlayer player = new LimitedPlayer(0, mock(IBoosterDraft.class));
        LimitedPlayer opponent = spy(new LimitedPlayer(1, mock(IBoosterDraft.class)));
        PaperCard mainCard = card("Main card");
        PaperCard offer = card("Offer");
        player.getDeck().getMain().add(mainCard);
        opponent.getDeck().getOrCreate(DeckSection.Sideboard).add(offer);
        player.setPromptSink((prompt, blocking, onAnswer) -> onAnswer.accept(List.of()));

        boolean[] done = {false};
        player.activateBrokers(List.of(player, opponent), 1, () -> done[0] = true);

        assertTrue(done[0]);
        verify(opponent, never()).chooseExchangeCard(any(), any());
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
