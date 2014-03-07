package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import forge.Forge.Graphics;
import forge.assets.FSkinTexture;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.toolbox.FScrollPane;

public class VField extends FScrollPane {
    private boolean flipped;

    private final List<FCardPanel> creatures = new ArrayList<FCardPanel>();
    private final List<FCardPanel> lands = new ArrayList<FCardPanel>();
    private final List<FCardPanel> otherPermanents = new ArrayList<FCardPanel>();

    public VField() {
        for (int i = 0; i < 7; i++) {
            creatures.add(add(new FCardPanel()));
            lands.add(add(new FCardPanel()));
            otherPermanents.add(add(new FCardPanel()));
        }
    }

    public boolean isFlipped() {
        return flipped;
    }
    public void setFlipped(boolean flipped0) {
        flipped = flipped0;
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = 0;
        float y = 0;
        float cardSize = height / 3;

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
        x = 0;
        y += cardSize;
        for (FCardPanel cardPanel : otherPermanents) { //TODO: Move to right of lands right-aligned if enough room
            cardPanel.setBounds(x, y, cardSize, cardSize);
            x += cardSize;
        }
    }
}
