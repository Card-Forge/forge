/**
 * 
 */
package forge.gui.framework;

import forge.gui.match.views.VAntes;
import forge.gui.match.views.VCombat;
import forge.gui.match.views.VDetail;
import forge.gui.match.views.VDev;
import forge.gui.match.views.VDock;
import forge.gui.match.views.VLog;
import forge.gui.match.views.VMessage;
import forge.gui.match.views.VPicture;
import forge.gui.match.views.VPlayers;
import forge.gui.match.views.VStack;

/**
 * These are the identifiers for tabs found in the drag layout.
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

    DEV_MODE (VDev.SINGLETON_INSTANCE), /** */
    BUTTON_DOCK (VDock.SINGLETON_INSTANCE), /** */

    // Non-user battlefields (AI or teammate), use setDoc to register.
    FIELD_0 (null), /** */
    FIELD_1 (null), /** */
    FIELD_2 (null), /** */
    FIELD_3 (null), /** */

    // Non-user hands (AI or teammate), use setDoc to register.
    HAND_0 (null), /** */
    HAND_1 (null), /** */
    HAND_2 (null), /** */
    HAND_3 (null); /** */

    // End enum declarations, start enum methods.
    private IVDoc vDoc;

    /** @param doc0 &emsp; {@link forge.gui.framework.IVDoc} */
    EDocID(final IVDoc doc0) {
        this.vDoc = doc0;
    }

    /** @param doc0 &emsp; {@link forge.gui.framework.IVDoc} */
    public void setDoc(final IVDoc doc0) {
        this.vDoc = doc0;
    }

    /** @return {@link forge.gui.framework.IVDoc} */
    public IVDoc getDoc() {
        if (vDoc == null) { throw new NullPointerException("No document found!"); }
        return vDoc;
    }
}
