package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.game.zone.ZoneType;
import forge.toolbox.FCardPanel;
import forge.toolbox.FScrollPane;

public class VZoneDisplay extends FScrollPane {
    private ZoneType zoneType;
    protected final List<FCardPanel> cardPanels = new ArrayList<FCardPanel>();

    public VZoneDisplay(ZoneType zoneType0) {
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
