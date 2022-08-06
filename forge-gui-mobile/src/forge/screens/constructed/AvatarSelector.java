package forge.screens.constructed;

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
import forge.util.MyRandom;
import forge.util.Utils;

import java.util.List;
import java.util.Map;

public class AvatarSelector extends FScreen {
    public static int getRandomAvatar(List<Integer> usedAvatars) {
        int random = 0;
        do {
            random = MyRandom.getRandom().nextInt(FSkin.getAvatars().size());
        } while (usedAvatars.contains(random));
        return random;
    }

    public static void show(final String playerName, final int currentIndex0, final List<Integer> usedAvatars0, final Callback<Integer> callback0) {
        AvatarSelector selector = new AvatarSelector(playerName, currentIndex0, usedAvatars0, callback0);
        Forge.openScreen(selector);
    }

    private static final float PADDING = Utils.scale(5);
    private static final int COLUMNS = 5;

    private final int currentIndex;
    private final List<Integer> usedAvatars;
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

    private AvatarSelector(final String playerName, final int currentIndex0, final List<Integer> usedAvatars0, final Callback<Integer> callback0) {
        super(Forge.getLocalizer().getMessage("lblSelectAvatarFor").replace("%s",playerName));

        currentIndex = currentIndex0;
        usedAvatars = usedAvatars0;
        callback = callback0;

        //add label for selecting random avatar first
        addAvatarLabel(FSkinImage.UNKNOWN, -1);

        //add label for currently selected avatar next
        final Map<Integer, TextureRegion> avatarMap = FSkin.getAvatars();
        addAvatarLabel(new FTextureRegionImage(avatarMap.get(currentIndex)), currentIndex);

        //add label for remaining avatars
        for (final Integer i : avatarMap.keySet()) {
            if (currentIndex != i) {
                addAvatarLabel(new FTextureRegionImage(avatarMap.get(i)), i);
            }
        }

        add(scroller);
    }

    private void addAvatarLabel(final FImage img, final int index) {
        final FLabel lbl = new FLabel.Builder().icon(img).iconScaleFactor(0.95f).align(Align.center)
                .iconInBackground(true).selectable(true).selected(currentIndex == index)
                .build();

        if (index == -1) {
            lbl.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    callback.run(getRandomAvatar(usedAvatars));
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
