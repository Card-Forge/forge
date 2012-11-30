/**
 * 
 */
package forge.gui.framework;

import forge.gui.deckeditor.views.VAllDecks;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.deckeditor.views.VDeckgen;
import forge.gui.deckeditor.views.VEditorPreferences;
import forge.gui.deckeditor.views.VFilters;
import forge.gui.deckeditor.views.VProbabilities;
import forge.gui.deckeditor.views.VStatistics;
import forge.gui.home.gauntlet.VSubmenuGauntletBuild;
import forge.gui.home.gauntlet.VSubmenuGauntletContests;
import forge.gui.home.gauntlet.VSubmenuGauntletLoad;
import forge.gui.home.gauntlet.VSubmenuGauntletQuick;
import forge.gui.home.quest.VSubmenuChallenges;
import forge.gui.home.quest.VSubmenuDuels;
import forge.gui.home.quest.VSubmenuQuestData;
import forge.gui.home.quest.VSubmenuQuestDecks;
import forge.gui.home.quest.VSubmenuQuestPrefs;
import forge.gui.home.sanctioned.VSubmenuConstructed;
import forge.gui.home.sanctioned.VSubmenuDraft;
import forge.gui.home.sanctioned.VSubmenuSealed;
import forge.gui.home.settings.VSubmenuAvatars;
import forge.gui.home.settings.VSubmenuDownloaders;
import forge.gui.home.settings.VSubmenuPreferences;
import forge.gui.home.variant.VSubmenuArchenemy;
import forge.gui.home.variant.VSubmenuVanguard;
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

    EDITOR_FILTERS (VFilters.SINGLETON_INSTANCE), /** */
    EDITOR_PREFERENCES (VEditorPreferences.SINGLETON_INSTANCE), /** */
    EDITOR_ALLDECKS (VAllDecks.SINGLETON_INSTANCE), /** */
    EDITOR_STATISTICS (VStatistics.SINGLETON_INSTANCE), /** */
    EDITOR_PROBABILITIES (VProbabilities.SINGLETON_INSTANCE), /** */
    EDITOR_CATALOG (VCardCatalog.SINGLETON_INSTANCE), /** */
    EDITOR_CURRENTDECK (VCurrentDeck.SINGLETON_INSTANCE), /** */
    EDITOR_DECKGEN (VDeckgen.SINGLETON_INSTANCE), /** */

    HOME_QUESTCHALLENGES (VSubmenuChallenges.SINGLETON_INSTANCE), /** */
    HOME_QUESTDUELS (VSubmenuDuels.SINGLETON_INSTANCE), /** */
    HOME_QUESTDATA (VSubmenuQuestData.SINGLETON_INSTANCE), /** */
    HOME_QUESTDECKS (VSubmenuQuestDecks.SINGLETON_INSTANCE), /** */
    HOME_QUESTPREFS (VSubmenuQuestPrefs.SINGLETON_INSTANCE), /** */
    HOME_GAUNTLETBUILD (VSubmenuGauntletBuild.SINGLETON_INSTANCE), /** */
    HOME_GAUNTLETLOAD (VSubmenuGauntletLoad.SINGLETON_INSTANCE), /** */
    HOME_GAUNTLETQUICK (VSubmenuGauntletQuick.SINGLETON_INSTANCE), /** */
    HOME_GAUNTLETCONTESTS (VSubmenuGauntletContests.SINGLETON_INSTANCE), /** */
    HOME_MULTITEST (VSubmenuArchenemy.SINGLETON_INSTANCE), /** */
    HOME_PREFERENCES (VSubmenuPreferences.SINGLETON_INSTANCE), /** */
    HOME_AVATARS (VSubmenuAvatars.SINGLETON_INSTANCE), /** */
    HOME_UTILITIES (VSubmenuDownloaders.SINGLETON_INSTANCE), /** */
    HOME_CONSTRUCTED (VSubmenuConstructed.SINGLETON_INSTANCE), /** */
    HOME_DRAFT (VSubmenuDraft.SINGLETON_INSTANCE), /** */
    HOME_SEALED (VSubmenuSealed.SINGLETON_INSTANCE), /** */
    HOME_VANGUARD (VSubmenuVanguard.SINGLETON_INSTANCE), /** */

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
    FIELD_4 (null), /** */
    FIELD_5 (null), /** */
    FIELD_6 (null), /** */
    FIELD_7 (null), /** */

    // Non-user hands (AI or teammate), use setDoc to register.
    HAND_0 (null), /** */
    HAND_1 (null), /** */
    HAND_2 (null), /** */
    HAND_3 (null), /** */

    COMMAND_0 (null), /** */
    COMMAND_1 (null), /** */
    COMMAND_2 (null), /** */
    COMMAND_3 (null), /** */
    COMMAND_4 (null), /** */
    COMMAND_5 (null), /** */
    COMMAND_6 (null), /** */
    COMMAND_7 (null); /** */

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
