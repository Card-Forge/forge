package forge.screens.quest;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.assets.ImageCache;
import forge.quest.IQuestEvent;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.util.Utils;

/** 
 * Panels for displaying duels and challenges.<br>
 * Handles radio button selection, event storage, and repainting.<br>
 * Package private!
 */
class QuestEventPanel extends FDisplayObject {
    private static final FSkinFont TITLE_FONT = FSkinFont.get(16);
    private static final FSkinFont DESC_FONT = FSkinFont.get(12);
    private static final FSkinColor TITLE_COLOR = FList.FORE_COLOR;
    private static final FSkinColor DESC_COLOR = SettingsScreen.DESC_COLOR;
    private static final float PADDING = Utils.scale(5);
    private static final FSkinColor GRADIENT_LEFT_COLOR = FSkinColor.get(Colors.CLR_THEME2).alphaColor(200 / 255f);
    private static final Color GRADIENT_RIGHT_COLOR = new Color(1, 1, 0, 0);
    private static final float RADIO_BUTTON_RADIUS = Utils.AVG_FINGER_WIDTH / 4;
    private static final float MIN_HEIGHT = 2 * Utils.AVG_FINGER_HEIGHT;

    private final IQuestEvent event;
    private final FImage img;
    private Container container;

    public QuestEventPanel(final IQuestEvent e0, final Container container0) {
        event = e0;
        img = event.hasImage() ? ImageCache.getIcon(e0) : null;
        container = container0;
        if (container.selectedPanel == null) {
            setSelected(true); //select first panel in container by default
        }
    }

    public IQuestEvent getEvent() {
        return event;
    }

    public boolean isSelected() {
        return container.selectedPanel == this;
    }
    public void setSelected(boolean selected0) {
        if (container.selectedPanel == this) {
            if (!selected0) {
                container.selectedPanel = null;
            }
        }
        else if (selected0) {
            container.selectedPanel = this;
            event.select();
        }
    }

    @Override
    public boolean tap(float x, float y, int count) {
        setSelected(true);
        if (count == 2 && container.activateHandler != null) {
            container.activateHandler.handleEvent(new FEvent(this, FEventType.ACTIVATE));
        }
        return true;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillGradientRect(GRADIENT_LEFT_COLOR, GRADIENT_RIGHT_COLOR, false, 0, 0, w, h);

        float x = PADDING;
        float y = PADDING;

        //draw image if needed
        if (img != null) {
            float imageSize = h - 2 * PADDING;
            float maxImageSize = w / 3;
            if (imageSize > maxImageSize) { //ensure image doesn't take up too much space
                y += (imageSize - maxImageSize) / 2;
                imageSize = maxImageSize;
            }
            g.drawImage(img, x, y, imageSize, imageSize);

            //shift title to the right of and slightly below the top of the icon
            x += imageSize + 2 * PADDING;
            y = 2 * PADDING;
        }

        //draw title
        w -= x + 2 * (RADIO_BUTTON_RADIUS + PADDING);
        String title = event.getFullTitle();
        g.drawText(title, TITLE_FONT, TITLE_COLOR, x, y, w, h, false, HAlignment.LEFT, false);

        //draw description
        y += TITLE_FONT.getCapHeight() + 2 * PADDING;
        g.drawText(event.getDescription(), DESC_FONT, DESC_COLOR, x, y, w, h - PADDING - y, true, HAlignment.LEFT, false);

        //draw radio button
        x = getWidth() - PADDING - RADIO_BUTTON_RADIUS;
        y = h / 2;
        g.drawCircle(Utils.scale(1), SettingsScreen.DESC_COLOR, x, y, RADIO_BUTTON_RADIUS);
        if (isSelected()) {
            g.fillCircle(TITLE_COLOR, x, y, RADIO_BUTTON_RADIUS / 2);
        }
    }

    static class Container extends FScrollPane {
        private QuestEventPanel selectedPanel;
        private FEventHandler activateHandler;

        public QuestEventPanel getSelectedPanel() {
            return selectedPanel;
        }

        public void setActivateHandler(FEventHandler activateHandler0) {
            activateHandler = activateHandler0;
        }

        @Override
        public void clear() {
            super.clear();
            selectedPanel = null;
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            if (getChildCount() == 0) {
                return new ScrollBounds(visibleWidth, visibleHeight);
            }

            float y = 0;
            float panelWidth = visibleWidth;
            float padding = 2 * PADDING; //use twice as much padding between panels
            float panelHeight = (visibleHeight + padding) / 3 - padding;
            if (panelHeight < MIN_HEIGHT) {
                panelHeight = MIN_HEIGHT;
            }
            for (FDisplayObject pnl : getChildren()) {
                pnl.setBounds(0, y, panelWidth, panelHeight);
                y += panelHeight + PADDING;
            }
            return new ScrollBounds(visibleWidth, y);
        }
    }
}
