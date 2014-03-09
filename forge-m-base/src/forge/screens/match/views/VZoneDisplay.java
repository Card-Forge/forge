package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.FThreads;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.toolbox.FCardPanel;
import forge.toolbox.FScrollPane;

public class VZoneDisplay extends FScrollPane {
    private final Player player;
    private final ZoneType zoneType;
    protected final List<FCardPanel> cardPanels = new ArrayList<FCardPanel>();

    public VZoneDisplay(Player player0, ZoneType zoneType0) {
        player = player0;
        zoneType = zoneType0;
    }

    public ZoneType getZoneType() {
        return zoneType;
    }

    public Iterable<FCardPanel> getCardPanels() {
        return cardPanels;
    }

    public int getCount() {
        return cardPanels.size();
    }

    public void update() {
        FThreads.invokeInEdtNowOrLater(updateRoutine);
    }

    private final Runnable updateRoutine = new Runnable() {
        @Override
        public void run() {
            clear();
            cardPanels.clear();
            for (Card card : player.getZone(zoneType).getCards()) {
                cardPanels.add(add(new FCardPanel(card)));
            }
            revalidate();
        }
    };

    public void updateSingleCard(Card card) {
        
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = 0;
        float y = 0;
        float cardHeight = height;
        float cardWidth = ((cardHeight - 2 * FCardPanel.PADDING) / FCardPanel.ASPECT_RATIO) + 2 * FCardPanel.PADDING; //ensure aspect ratio maintained after padding applied

        for (FCardPanel cardPanel : cardPanels) {
            cardPanel.setBounds(x, y, cardWidth, cardHeight);
            x += cardWidth;
        }
    }
}
