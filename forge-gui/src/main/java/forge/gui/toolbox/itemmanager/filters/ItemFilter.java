package forge.gui.toolbox.itemmanager.filters;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.Command;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class ItemFilter<T extends InventoryItem> {
    private final ItemManager<T> itemManager;
    private JPanel panel;
    private JLabel lblPanelTitle;
    
    public static int PANEL_HEIGHT = 30;
    
    protected ItemFilter(ItemManager<T> itemManager0) {
        this.itemManager = itemManager0;
    }
    
    @SuppressWarnings("serial")
    public JPanel getPanel() {
        if (this.panel == null) {
            this.panel = new JPanel(new MigLayout("insets 0, gap 2"));
            this.panel.setOpaque(false);
            FSkin.get(this.panel).setMatteBorder(1, 2, 1, 2, FSkin.getColor(FSkin.Colors.CLR_TEXT));
            
            this.lblPanelTitle = new FLabel.Builder().fontSize(10).build();
            this.panel.add(this.lblPanelTitle, "top");
            
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
 
            this.buildPanel(panel);
        }
        return this.panel;
    }
    
    public void updatePanelTitle(int number) {
        this.lblPanelTitle.setText(number + ". " + this.getTitle());
    }
    
    protected abstract String getTitle();
    
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
