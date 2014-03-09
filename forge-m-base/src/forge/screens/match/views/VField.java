package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FScrollPane;

public class VField extends FScrollPane {
    private boolean flipped;

    private final List<FCardPanel> creatures = new ArrayList<FCardPanel>();
    private final List<FCardPanel> lands = new ArrayList<FCardPanel>();
    private final List<FCardPanel> otherPermanents = new ArrayList<FCardPanel>();

    public VField() {
    }

    public boolean isFlipped() {
        return flipped;
    }
    public void setFlipped(boolean flipped0) {
        flipped = flipped0;
    }

    public void update() {
        
    }

    public void updateSingleCard(Card card) {
        
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = 0;
        float y = 0;
        float cardSize = height / 2;

        for (FCardPanel cardPanel : creatures) {
            cardPanel.setBounds(x, y, cardSize, cardSize);
            x += cardSize;
        }
        x = 0;
        y += cardSize;
        for (FCardPanel cardPanel : lands) {
            cardPanel.setBounds(x, y, cardSize, cardSize);
            x += cardSize;
        }
        for (FCardPanel cardPanel : otherPermanents) {
            cardPanel.setBounds(x, y, cardSize, cardSize);
            x += cardSize;
        }

        if (flipped) { //flip all positions across x-axis if needed
            for (FDisplayObject child : getChildren()) {
                child.setTop(height - child.getBottom());
            }
        }
    }
}
