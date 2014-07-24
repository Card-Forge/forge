package forge.itemmanager.filters;

import forge.assets.FImage;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.toolbox.FLabel;
import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;


public abstract class ToggleButtonsFilter<T extends InventoryItem> extends ItemFilter<T> {
    protected boolean lockFiltering;
    private final ArrayList<FLabel> buttons = new ArrayList<FLabel>();

    protected ToggleButtonsFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }

    protected ToggleButton addToggleButton(Widget widget, FImage icon) {
        final ToggleButton button = new ToggleButton(icon);

        this.buttons.add(button);
        widget.add(button);
        return button;
    }

    @Override
    public float getPreferredWidth(float maxWidth, float height) {
        return Math.min(height * buttons.size(), maxWidth);
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        float buttonHeight = height;
        float buttonWidth = Math.min(buttonHeight, width / buttons.size());

        float x = 0;
        for (FLabel btn : buttons) {
            btn.setBounds(x, 0, buttonWidth, buttonHeight);
            x += buttonWidth - FLabel.BORDER_THICKNESS;
        }
    }

    @Override
    public final boolean isEmpty() {
        for (FLabel button : buttons) { //consider filter empty if any button isn't selected
            if (!button.isSelected()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset() {
        for (FLabel button : buttons) {
            button.setSelected(true);
        }
    }

    public class ToggleButton extends FLabel {
        private FEventHandler longPressHandler;

        private ToggleButton(FImage icon) {
            super(new FLabel.Builder()
                .icon(icon).iconScaleFactor(1f)
                .align(HAlignment.CENTER)
                .selectable(true).selected(true)
                .command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        if (lockFiltering) { return; }
                        applyChange();
                    }
                }));
        }

        public void setLongPressHandler(FEventHandler longPressHandler0) {
            longPressHandler = longPressHandler0;
        }

        @Override
        public boolean longPress(float x, float y) {
            if (longPressHandler != null) {
                longPressHandler.handleEvent(new FEvent(this, FEventType.LONG_PRESS));
                return true;
            }
            return false;
        }
    }
}
