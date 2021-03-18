package forge.screens.constructed;

import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.util.Utils;

public class SleevesSelector  extends FScreen {
    public static int getRandomSleeves(List<Integer> usedSleeves) {
        int random = 0;
        do {
            random = MyRandom.getRandom().nextInt(FSkin.getSleeves().size());
        } while (usedSleeves.contains(random));
        return random;
    }

    public static void show(final String playerName, final int currentIndex0, final List<Integer> usedSleeves0, final Callback<Integer> callback0) {
        SleevesSelector selector = new SleevesSelector(playerName, currentIndex0, usedSleeves0, callback0);
        Forge.openScreen(selector);
    }

    private static final float PADDING = Utils.scale(5);
    private static final int COLUMNS = 5;

    private final int currentIndex;
    private final List<Integer> usedSleeves;
    private final Callback<Integer> callback;
    private final FScrollPane scroller = new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            int rowCount = 0;
            float x = PADDING;
            float y = PADDING;
            float labelSize = (visibleWidth - (COLUMNS + 1) * PADDING) / COLUMNS;
            for (FDisplayObject lbl : scroller.getChildren()) {
                if (rowCount == COLUMNS) { //wrap to next line
                    x = PADDING;
                    y += labelSize + PADDING;
                    rowCount = 0;
                }
                lbl.setBounds(x, y, labelSize, labelSize);
                x += labelSize + PADDING;
                rowCount++;
            }
            return new ScrollBounds(visibleWidth, y + labelSize + PADDING);
        }
    };

    private SleevesSelector(final String playerName, final int currentIndex0, final List<Integer> usedSleeves0, final Callback<Integer> callback0) {
        super(Localizer.getInstance().getMessage("lblSelectSleevesFroPlayer", playerName));

        currentIndex = currentIndex0;
        usedSleeves = usedSleeves0;
        callback = callback0;

        //add label for selecting random sleeves first
        addSleevesLabel(FSkinImage.UNKNOWN, -1);

        //add label for currently selected sleeves next
        final Map<Integer, TextureRegion> sleeveMap = FSkin.getSleeves();
        addSleevesLabel(new FTextureRegionImage(sleeveMap.get(currentIndex)), currentIndex);

        //add label for remaining sleeves
        for (final Integer i : sleeveMap.keySet()) {
            if (currentIndex != i) {
                addSleevesLabel(new FTextureRegionImage(sleeveMap.get(i)), i);
            }
        }

        add(scroller);
    }

    private void addSleevesLabel(final FImage img, final int index) {
        final FLabel lbl = new FLabel.Builder().icon(img).iconScaleFactor(0.99f).align(Align.center)
                .iconInBackground(true).selectable(true).selected(currentIndex == index)
                .build();

        if (index == -1) {
            lbl.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    callback.run(getRandomSleeves(usedSleeves));
                    Forge.back();
                }
            });
        }
        else {
            lbl.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    callback.run(index);
                    Forge.back();
                }
            });
        }
        scroller.add(lbl);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        scroller.setBounds(0, startY, width, height - startY);
    }
}
