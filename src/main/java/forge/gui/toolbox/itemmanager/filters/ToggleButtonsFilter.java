package forge.gui.toolbox.itemmanager.filters;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import forge.Command;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public abstract class ToggleButtonsFilter<T extends InventoryItem> extends ItemFilter<T> {
    private static final Dimension BUTTON_SIZE = new Dimension(60, 24);
    
    private final ArrayList<FLabel> buttons = new ArrayList<FLabel>();

    protected ToggleButtonsFilter(ItemManager<T> itemManager0) {
        super(itemManager0);
    }
    
    protected void addToggleButton(JPanel panel, StatTypes s) {
        addToggleButton(panel, s.toLabelString(), s.img);
    }
    
    protected void addToggleButton(JPanel panel, String filterName, ImageIcon icon) {
        final FLabel button = new FLabel.Builder()
                .icon(icon).iconScaleAuto(false)
                .fontSize(11)
                .tooltip(filterName + " (click to toggle the filter, right-click to show only " + filterName.toLowerCase() + ")")
                .hoverable().selectable(true).selected(true)
                .build();
        
        button.setPreferredSize(BUTTON_SIZE);
        button.setMinimumSize(BUTTON_SIZE);
        
        button.setCommand(new Command() {
            @Override
            public void run() {
                applyChange();
            }
        });

        //hook so right-clicking a button toggles itself on and toggles off all other buttons
        button.setRightClickCommand(new Command() {
            @Override
            public void run() {
                for(FLabel btn : buttons) {
                    btn.setSelected(false);
                }
                button.setSelected(true);
                applyChange();
            }
        });
        
        this.buttons.add(button);
        panel.add(button);
    }
}
