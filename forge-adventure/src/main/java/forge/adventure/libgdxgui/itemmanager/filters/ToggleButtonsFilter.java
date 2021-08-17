package forge.adventure.libgdxgui.itemmanager.filters;

import com.badlogic.gdx.utils.Align;
import forge.adventure.libgdxgui.assets.FImage;
import forge.item.InventoryItem;
import forge.adventure.libgdxgui.itemmanager.ItemManager;
import forge.adventure.libgdxgui.toolbox.FEvent;
import forge.adventure.libgdxgui.toolbox.FEvent.FEventHandler;
import forge.adventure.libgdxgui.toolbox.FEvent.FEventType;
import forge.adventure.libgdxgui.toolbox.FLabel;

import java.util.ArrayList;
import java.util.List;


public abstract class ToggleButtonsFilter<T extends InventoryItem> extends ItemFilter<T> {
    protected boolean lockFiltering;
    private final List<FLabel> buttons = new ArrayList<>();

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
        return Math.min((height - FLabel.BORDER_THICKNESS) * buttons.size() + FLabel.BORDER_THICKNESS, maxWidth);
    }

    @Override
    protected void doWidgetLayout(float width, float height) {
        float buttonWidth = (width + FLabel.BORDER_THICKNESS * (buttons.size() - 1)) / buttons.size();
        float buttonHeight = height;

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
                .align(Align.center)
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
