package forge.deck;

import forge.Forge;
import forge.screens.FScreen;

/** Full screen deck statistics, opened from the deck viewer menu. */
public class FDeckStatsScreen extends FScreen {
    private final FDeckStatsView statsView;

    public static void show(final Deck deck) {
        if (deck == null) { return; }
        Forge.openScreen(new FDeckStatsScreen(deck));
    }

    private FDeckStatsScreen(final Deck deck) {
        super(Forge.getLocalizer().getMessage("lblStatistics") + " - " + deck.getName());
        statsView = add(new FDeckStatsView());
        statsView.setDeck(deck);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        statsView.setBounds(0, startY, width, height - startY);
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null;
    }
}
