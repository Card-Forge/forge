package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.model.FModel;
import forge.toolbox.FCardPanel;
import forge.toolbox.FScrollPane;

public class VZoneDisplay extends FScrollPane {
    private ZoneType zoneType;
    private final List<FCardPanel> cards = new ArrayList<FCardPanel>();

    public VZoneDisplay(ZoneType zoneType0) {
        zoneType = zoneType0;

        Card card = Card.getCardForUi(FModel.getMagicDb().getCommonCards().getCard("Forest"));

        for (int i = 0; i < 7; i++) {
            cards.add(add(new FCardPanel(card)));
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = 0;
        float y = 0;
        float cardHeight = height;
        float cardWidth = ((cardHeight - 2 * FCardPanel.PADDING) / FCardPanel.ASPECT_RATIO) + 2 * FCardPanel.PADDING; //ensure aspect ratio maintained after padding applied

        for (FCardPanel cardPanel : cards) {
            cardPanel.setBounds(x, y, cardWidth, cardHeight);
            x += cardWidth;
        }
    }
}
