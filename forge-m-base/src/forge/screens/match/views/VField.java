package forge.screens.match.views;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.toolbox.FCardPanel;

public class VField extends VZoneDisplay {
    private boolean flipped;

    public VField(Player player0) {
        super(player0, ZoneType.Battlefield);
    }

    public boolean isFlipped() {
        return flipped;
    }
    public void setFlipped(boolean flipped0) {
        flipped = flipped0;
    }

    @Override
    protected void doLayout(float width, float height) {
        float x, y;
        float x1 = 0;
        float x2 = 0;
        float y1 = 0;
        float cardSize = height / 2;
        float y2 = cardSize;
        if (flipped) {
            y1 = y2;
            y2 = 0;
        }

        for (FCardPanel cardPanel : cardPanels) {
            if (cardPanel.getCard().isCreature()) {
                x = x1;
                y = y1;
                x1 += cardSize;
            }
            else {
                x = x2;
                y = y2;
                x2 += cardSize;
            }
            cardPanel.setBounds(x, y, cardSize, cardSize);
        }
    }
}
