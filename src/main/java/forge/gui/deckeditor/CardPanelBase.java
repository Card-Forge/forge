package forge.gui.deckeditor;

import javax.swing.JPanel;
import forge.card.InventoryItem;

/** 
 * Base class for any cardView panel
 *
 */
public abstract class CardPanelBase extends JPanel {
    private static final long serialVersionUID = -2230733670423143126L;

    public abstract void showCard(InventoryItem card);

}
