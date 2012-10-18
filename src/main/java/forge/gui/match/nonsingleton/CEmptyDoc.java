package forge.gui.match.nonsingleton;

import forge.Command;
import forge.gui.framework.ICDoc;

/** 
 * An intentionally empty ICDoc to fill field slots unused
 * by the current layout of a match UI.
 */
public class CEmptyDoc implements ICDoc {

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }
}
