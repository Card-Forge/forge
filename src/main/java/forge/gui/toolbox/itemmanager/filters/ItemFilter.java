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
@SuppressWarnings("serial")
public abstract class ItemFilter<T extends InventoryItem> extends JPanel {
    private final ItemManager<T> itemManager;
    
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
        this.setOpaque(false);
        this.addComponents();
        this.add(new FLabel.Builder().text("X").fontSize(10).hoverable(true)
                .tooltip("Remove filter").cmdClick(new Command() {
                    @Override
                    public void run() {
                        itemManager.removeFilter(ItemFilter.this);
                        ItemFilter.this.onRemoved();
                    }
                }).build(), "top");
    }
    
    protected void applyChange() {
        itemManager.buildFilterPredicate();
    }
    
    public abstract FilterTypes getType();
    protected abstract void addComponents();
    protected abstract void onRemoved();
}
