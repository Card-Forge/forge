package forge.gui;

import java.awt.Color;

import javax.swing.JPanel;


/** 
 * <p>SelectablePanel</p>
 * VIEW - Standard selectable JPanel used for many places in the GUI.
 *
 */
@SuppressWarnings("serial")
public abstract class SelectablePanel extends JPanel {
    
    private boolean selected = false;
    protected Color backgroundColor = this.getBackground();
    
    /**
     * <p>Getter for the field <code>selected</code></p>
     *
     * @return boolean.
     */
    public boolean getSelected() {
        return selected;
    }

    /**
     * <p>Setter for the field <code>selected</code></p>
     * Sets selected field and visual effect.
     *
     * @param boolean.
     */
    public void setSelected(boolean selected) {
        if (selected) {
            this.setBackground(backgroundColor.darker());
        } else {
            this.setBackground(backgroundColor);
        }
        
        this.selected = selected;
    }    
}
