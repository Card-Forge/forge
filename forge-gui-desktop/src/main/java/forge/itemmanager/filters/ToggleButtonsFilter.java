package forge.itemmanager.filters;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import forge.UiCommand;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinFont;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.LayoutHelper;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public abstract class ToggleButtonsFilter<T extends InventoryItem> extends ItemFilter<T> {
    protected boolean lockFiltering;
    private final List<FLabel> buttons = new ArrayList<FLabel>();

    protected ToggleButtonsFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }
    
    protected FLabel addToggleButton(JPanel widget, SkinImage icon, String tooltip) {
        final FLabel button = new FLabel.Builder()
                .icon(icon).iconScaleAuto(false)
                .fontSize(11)
                .tooltip(tooltip)
                .hoverable().selectable(true).selected(true)
                .build();

        button.setCommand(new UiCommand() {
            @Override
            public void run() {
                if (lockFiltering) { return; }
                applyChange();
            }
        });

        this.buttons.add(button);
        widget.add(button);
        return button;
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        int availableWidth = helper.getParentWidth() - (buttons.size() - 1) * 2; //account for gaps
        int buttonWidth = availableWidth / buttons.size();
        Graphics g = buttons.get(0).getGraphics();
        if (buttonWidth <= 0 || g == null) {
            return;
        }

        int maxTextWidth = buttonWidth - 8; //account for padding

        for (FLabel btn : buttons) {
            if (btn.getText() != null && !btn.getText().isEmpty()) {
                int max = maxTextWidth;
                Icon icon = btn.getIcon();
                if (icon != null) {
                    max -= icon.getIconWidth() + 4;
                }
                for (int fs = 11; fs > 5; fs--) {
                    SkinFont skinFont = FSkin.getFont(fs);
                    if (skinFont.measureTextWidth(g, btn.getText()) <= max) {
                        btn.setFont(skinFont);
                        break;
                    }
                }
            }
            helper.include(btn, buttonWidth, 25);
            helper.offset(-1, 0); //keep buttons tighter together
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
    
    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    public boolean merge(ItemFilter<?> filter) {
        return true;
    }
}
