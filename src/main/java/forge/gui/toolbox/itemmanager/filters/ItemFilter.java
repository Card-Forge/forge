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
    private int number;
    private JPanel panel;
    
    public static int PANEL_HEIGHT = 30;
    
    protected ItemFilter(ItemManager<T> itemManager0) {
        this.itemManager = itemManager0;
    }
    
    public int getNumber() {
        return this.number;
    }
    
    public void setNumber(int number0) {
        this.number = number0;
    }
    
    @SuppressWarnings("serial")
    public JPanel getPanel() {
        if (this.panel == null) {
            this.panel = new JPanel();
            this.panel.setOpaque(false);
 
            this.buildPanel(panel);
            
            //add button to remove filter
            this.panel.add(new FLabel.Builder()
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
        return this.panel;
    }
    
    protected void applyChange() {
        this.itemManager.buildFilterPredicate();
    }
    
    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @SuppressWarnings("rawtypes")
    public abstract boolean merge(ItemFilter filter);
    
    protected abstract void buildPanel(JPanel panel);
    protected abstract void onRemoved();
}
