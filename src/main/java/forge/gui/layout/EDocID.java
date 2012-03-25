/**
 * 
 */
package forge.gui.layout;

import forge.gui.match.nonsingleton.VField;
import forge.gui.match.nonsingleton.VHand;
import forge.gui.match.views.VAntes;
import forge.gui.match.views.VCombat;
import forge.gui.match.views.VDetail;
import forge.gui.match.views.VDock;
import forge.gui.match.views.VLog;
import forge.gui.match.views.VMessage;
import forge.gui.match.views.VPicture;
import forge.gui.match.views.VPlayers;
import forge.gui.match.views.VStack;



/**
 * These are the identifiers for tabs found in the Drag3 layout.
 * These IDs are used in the save XML and card layouts.
 * 
 * <br><br><i>(E at beginning of class name denotes an enum.)</i>
 */
public enum EDocID { /** */
    CARD_PICTURE (VPicture.SINGLETON_INSTANCE), /** */
    CARD_DETAIL (VDetail.SINGLETON_INSTANCE), /** */
    CARD_ANTES (VAntes.SINGLETON_INSTANCE), /** */

    REPORT_MESSAGE (VMessage.SINGLETON_INSTANCE), /** */
    REPORT_STACK (VStack.SINGLETON_INSTANCE), /** */
    REPORT_COMBAT (VCombat.SINGLETON_INSTANCE), /** */
    REPORT_LOG (VLog.SINGLETON_INSTANCE), /** */
    REPORT_PLAYERS (VPlayers.SINGLETON_INSTANCE), /** */

    // Current user's panels
    YOUR_HAND (VHand.SINGLETON_INSTANCE), /** */
    YOUR_BATTLEFIELD (VField.SINGLETON_INSTANCE), /** */
    YOUR_DOCK (VDock.SINGLETON_INSTANCE), /** */

    // Non-user battlefields (AI or teammate), use setDoc to activate.
    BATTLEFIELD_1 (null), /** */
    BATTLEFIELD_2 (null), /** */
    BATTLEFIELD_3 (null), /** */

    // Non-user hands (AI or teammate), use setDoc to activate.
    HAND_1 (null), /** */
    HAND_2 (null), /** */
    HAND_3 (null); /** */

    // End enum declarations, start enum methods.
    private IVDoc vDoc;

    /** @param doc0 &emsp; {@link forge.gui.layout.IVDoc} */
    EDocID(final IVDoc doc0) {
        this.vDoc = doc0;
    }

    /** @return {@link forge.gui.layout.IVDoc} */
    public IVDoc getDoc() {
        return vDoc;
    }

    /**
     * Register non-singleton tab instances using this method.
     *
     * @param id0 &emsp; {@link forge.gui.layout.EDocID}
     * @param doc0 &emsp; {@link forge.gui.layout.IVDoc}
     */
    public void setDoc(EDocID id0, IVDoc doc0) {
        id0.vDoc = doc0;
    }
}
