/**
 * 
 */
package forge.gui.framework;

import forge.screens.deckeditor.views.*;
import forge.screens.home.gauntlet.VSubmenuGauntletBuild;
import forge.screens.home.gauntlet.VSubmenuGauntletContests;
import forge.screens.home.gauntlet.VSubmenuGauntletLoad;
import forge.screens.home.gauntlet.VSubmenuGauntletQuick;
import forge.screens.home.quest.*;
import forge.screens.home.sanctioned.VSubmenuConstructed;
import forge.screens.home.sanctioned.VSubmenuDraft;
import forge.screens.home.sanctioned.VSubmenuSealed;
import forge.screens.home.sanctioned.VSubmenuWinston;
import forge.screens.home.settings.VSubmenuAchievements;
import forge.screens.home.settings.VSubmenuAvatars;
import forge.screens.home.settings.VSubmenuDownloaders;
import forge.screens.home.settings.VSubmenuPreferences;
import forge.screens.home.settings.VSubmenuReleaseNotes;
import forge.screens.match.views.*;
import forge.screens.workshop.views.VCardDesigner;
import forge.screens.workshop.views.VCardScript;
import forge.screens.workshop.views.VWorkshopCatalog;

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

    EDITOR_ALLDECKS (VAllDecks.SINGLETON_INSTANCE), /** */
    EDITOR_STATISTICS (VStatistics.SINGLETON_INSTANCE), /** */
    EDITOR_PROBABILITIES (VProbabilities.SINGLETON_INSTANCE), /** */
    EDITOR_CATALOG (VCardCatalog.SINGLETON_INSTANCE), /** */
    EDITOR_CURRENTDECK (VCurrentDeck.SINGLETON_INSTANCE), /** */
    EDITOR_DECKGEN (VDeckgen.SINGLETON_INSTANCE), /** */
    
    WORKSHOP_CATALOG (VWorkshopCatalog.SINGLETON_INSTANCE), /** */
    WORKSHOP_CARDDESIGNER (VCardDesigner.SINGLETON_INSTANCE), /** */
    WORKSHOP_CARDSCRIPT (VCardScript.SINGLETON_INSTANCE), /** */

    HOME_QUESTDRAFTS (VSubmenuQuestDraft.SINGLETON_INSTANCE), /** */
    HOME_QUESTCHALLENGES (VSubmenuChallenges.SINGLETON_INSTANCE), /** */
    HOME_QUESTDUELS (VSubmenuDuels.SINGLETON_INSTANCE), /** */
    HOME_QUESTDATA (VSubmenuQuestData.SINGLETON_INSTANCE), /** */
    HOME_QUESTDECKS (VSubmenuQuestDecks.SINGLETON_INSTANCE), /** */
    HOME_QUESTPREFS (VSubmenuQuestPrefs.SINGLETON_INSTANCE), /** */
    HOME_GAUNTLETBUILD (VSubmenuGauntletBuild.SINGLETON_INSTANCE), /** */
    HOME_GAUNTLETLOAD (VSubmenuGauntletLoad.SINGLETON_INSTANCE), /** */
    HOME_GAUNTLETQUICK (VSubmenuGauntletQuick.SINGLETON_INSTANCE), /** */
    HOME_GAUNTLETCONTESTS (VSubmenuGauntletContests.SINGLETON_INSTANCE), /** */
    HOME_PREFERENCES (VSubmenuPreferences.SINGLETON_INSTANCE), /** */
    HOME_ACHIEVEMENTS (VSubmenuAchievements.SINGLETON_INSTANCE), /** */
    HOME_AVATARS (VSubmenuAvatars.SINGLETON_INSTANCE), /** */
    HOME_UTILITIES (VSubmenuDownloaders.SINGLETON_INSTANCE), /** */
    HOME_CONSTRUCTED (VSubmenuConstructed.SINGLETON_INSTANCE), /** */
    HOME_DRAFT (VSubmenuDraft.SINGLETON_INSTANCE), /** */
    HOME_SEALED (VSubmenuSealed.SINGLETON_INSTANCE), /** */
    HOME_WINSTON (VSubmenuWinston.SINGLETON_INSTANCE), /** */
    HOME_RELEASE_NOTES (VSubmenuReleaseNotes.SINGLETON_INSTANCE),

    REPORT_MESSAGE (VPrompt.SINGLETON_INSTANCE), /** */
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
    FIELD_4 (null), /** */
    FIELD_5 (null), /** */
    FIELD_6 (null), /** */
    FIELD_7 (null), /** */

    // Non-user hands (AI or teammate), use setDoc to register.
    HAND_0 (null), /** */
    HAND_1 (null), /** */
    HAND_2 (null), /** */
    HAND_3 (null), /** */
    HAND_4 (null), /** */
    HAND_5 (null), /** */
    HAND_6 (null), /** */
    HAND_7 (null), /** */

    COMMAND_0 (null), /** */
    COMMAND_1 (null), /** */
    COMMAND_2 (null), /** */
    COMMAND_3 (null), /** */
    COMMAND_4 (null), /** */
    COMMAND_5 (null), /** */
    COMMAND_6 (null), /** */
    COMMAND_7 (null); /** */

    public final static EDocID[] Commands = new EDocID[] {COMMAND_0, COMMAND_1, COMMAND_2, COMMAND_3, COMMAND_4, COMMAND_5, COMMAND_6, COMMAND_7};
    public final static EDocID[] Fields = new EDocID[] {FIELD_0, FIELD_1, FIELD_2, FIELD_3, FIELD_4, FIELD_5, FIELD_6, FIELD_7};
    public final static EDocID[] Hands = new EDocID[] {HAND_0, HAND_1, HAND_2, HAND_3, HAND_4, HAND_5, HAND_6, HAND_7};

    // End enum declarations, start enum methods.
    private IVDoc<? extends ICDoc> vDoc;

    /** @param doc0 &emsp; {@link forge.gui.framework.IVDoc} */
    EDocID(final IVDoc<? extends ICDoc> doc0) {
        this.vDoc = doc0;
    }

    /** @param doc0 &emsp; {@link forge.gui.framework.IVDoc} */
    public void setDoc(final IVDoc<? extends ICDoc> doc0) {
        this.vDoc = doc0;
    }

    /** @return {@link forge.gui.framework.IVDoc} */
    public IVDoc<? extends ICDoc> getDoc() {
        if (vDoc == null) { throw new NullPointerException("No document found for " + this.name() + "."); }
        return vDoc;
    }
}
