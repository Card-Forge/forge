package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JPanel;

import forge.Command;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class ItemFilter<T extends InventoryItem> {
    private final ItemManager<T> itemManager;
    private JPanel panel;
    
    public static int PANEL_HEIGHT = 30;
    
    public enum FilterTypes {
        CardCMC,
        CardColor,
        CardFormat,
        CardPower,
        CardQuestWorld,
        CardSet,
        CardToughness,
        CardType
    }
    
    protected ItemFilter(ItemManager<T> itemManager0) {
        this.itemManager = itemManager0;
    }
    
    @SuppressWarnings("serial")
    public JPanel getPanel() {
        if (panel == null) {
            panel = new JPanel();
            panel.setOpaque(false);
 
            this.buildPanel(panel);
            
            //add button to remove filter
            panel.add(new FLabel.Builder()
                .text("X")
                .fontSize(10)
                .hoverable(true)
                .tooltip("Remove filter")
                .cmdClick(new Command() {
                    @Override
                    public void run() {
                        itemManager.removeFilter(ItemFilter.this);
                        ItemFilter.this.onRemoved();
                    }
                }).build(), "top");
        }
        return panel;
    }
    
    protected void applyChange() {
        itemManager.buildFilterPredicate();
    }
    
    public abstract FilterTypes getType();
    protected abstract void buildPanel(JPanel panel);
    protected abstract void onRemoved();
}
