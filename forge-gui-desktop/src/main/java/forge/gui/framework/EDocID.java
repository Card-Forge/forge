/**
 * 
 */
package forge.gui.framework;

import com.google.common.collect.ObjectArrays;

import forge.screens.deckeditor.views.*;
import forge.screens.home.gauntlet.*;
import forge.screens.home.online.VSubmenuOnlineLobby;
import forge.screens.home.puzzle.VSubmenuPuzzleCreate;
import forge.screens.home.puzzle.VSubmenuPuzzleSolve;
import forge.screens.home.puzzle.VSubmenuTutorial;
import forge.screens.home.quest.VSubmenuChallenges;
import forge.screens.home.quest.VSubmenuDuels;
import forge.screens.home.quest.VSubmenuQuestDecks;
import forge.screens.home.quest.VSubmenuQuestDraft;
import forge.screens.home.quest.VSubmenuQuestLoadData;
import forge.screens.home.quest.VSubmenuQuestPrefs;
import forge.screens.home.quest.VSubmenuQuestStart;
import forge.screens.home.sanctioned.VSubmenuConstructed;
import forge.screens.home.sanctioned.VSubmenuDraft;
import forge.screens.home.sanctioned.VSubmenuSealed;
import forge.screens.home.sanctioned.VSubmenuWinston;
import forge.screens.home.settings.VSubmenuAchievements;
import forge.screens.home.settings.VSubmenuAvatars;
import forge.screens.home.settings.VSubmenuDownloaders;
import forge.screens.home.settings.VSubmenuPreferences;
import forge.screens.home.settings.VSubmenuReleaseNotes;
import forge.screens.workshop.views.VCardDesigner;
import forge.screens.workshop.views.VCardScript;
import forge.screens.workshop.views.VWorkshopCatalog;

/**
 * These are the identifiers for tabs found in the drag layout.
 * These IDs are used in the save XML and card layouts.
 * 
 * <br><br><i>(E at beginning of class name denotes an enum.)</i>
 */
public enum EDocID {
    CARD_PICTURE (),
    CARD_DETAIL (),
    CARD_ANTES (),

    EDITOR_ALLDECKS (VAllDecks.SINGLETON_INSTANCE),
    EDITOR_STATISTICS (VStatistics.SINGLETON_INSTANCE),
    EDITOR_PROBABILITIES (VProbabilities.SINGLETON_INSTANCE),
    EDITOR_CATALOG (VCardCatalog.SINGLETON_INSTANCE),
    EDITOR_CURRENTDECK (VCurrentDeck.SINGLETON_INSTANCE),
    EDITOR_DECKGEN (VDeckgen.SINGLETON_INSTANCE),
    EDITOR_COMMANDER (VCommanderDecks.SINGLETON_INSTANCE),
    EDITOR_BRAWL (VBrawlDecks.SINGLETON_INSTANCE),
    EDITOR_TINY_LEADERS (VTinyLeadersDecks.SINGLETON_INSTANCE),
    EDITOR_OATHBREAKER (VOathbreakerDecks.SINGLETON_INSTANCE),
    EDITOR_LOG(VEditorLog.SINGLETON_INSTANCE),

    WORKSHOP_CATALOG (VWorkshopCatalog.SINGLETON_INSTANCE),
    WORKSHOP_CARDDESIGNER (VCardDesigner.SINGLETON_INSTANCE),
    WORKSHOP_CARDSCRIPT (VCardScript.SINGLETON_INSTANCE),

    HOME_QUESTSTART (VSubmenuQuestStart.SINGLETON_INSTANCE),
    HOME_QUESTLOADDATA(VSubmenuQuestLoadData.SINGLETON_INSTANCE),
    HOME_QUESTDRAFTS (VSubmenuQuestDraft.SINGLETON_INSTANCE),
    HOME_QUESTCHALLENGES (VSubmenuChallenges.SINGLETON_INSTANCE),
    HOME_QUESTDUELS (VSubmenuDuels.SINGLETON_INSTANCE),
    HOME_QUESTDECKS (VSubmenuQuestDecks.SINGLETON_INSTANCE),
    HOME_QUESTPREFS (VSubmenuQuestPrefs.SINGLETON_INSTANCE),
    HOME_GAUNTLETBUILD (VSubmenuGauntletBuild.SINGLETON_INSTANCE),
    HOME_GAUNTLETLOAD (VSubmenuGauntletLoad.SINGLETON_INSTANCE),
    HOME_GAUNTLETQUICK (VSubmenuGauntletQuick.SINGLETON_INSTANCE),
    HOME_GAUNTLETCOMMANDERQUICK (VSubmenuGauntletCommanderQuick.SINGLETON_INSTANCE),
    HOME_GAUNTLETCOMMANDERBUILD (VSubmenuGauntletCommanderBuild.SINGLETON_INSTANCE),
    HOME_GAUNTLETCONTESTS (VSubmenuGauntletContests.SINGLETON_INSTANCE),
    HOME_PREFERENCES (VSubmenuPreferences.SINGLETON_INSTANCE),
    HOME_ACHIEVEMENTS (VSubmenuAchievements.SINGLETON_INSTANCE),
    HOME_AVATARS (VSubmenuAvatars.SINGLETON_INSTANCE),
    HOME_UTILITIES (VSubmenuDownloaders.SINGLETON_INSTANCE),
    HOME_TUTORIAL(VSubmenuTutorial.SINGLETON_INSTANCE),
    HOME_PUZZLE_CREATE(VSubmenuPuzzleCreate.SINGLETON_INSTANCE),
    HOME_PUZZLE_SOLVE(VSubmenuPuzzleSolve.SINGLETON_INSTANCE),
    HOME_CONSTRUCTED (VSubmenuConstructed.SINGLETON_INSTANCE),
    HOME_DRAFT (VSubmenuDraft.SINGLETON_INSTANCE),
    HOME_SEALED (VSubmenuSealed.SINGLETON_INSTANCE),
    HOME_WINSTON (VSubmenuWinston.SINGLETON_INSTANCE),
    HOME_NETWORK (VSubmenuOnlineLobby.SINGLETON_INSTANCE),
    HOME_RELEASE_NOTES (VSubmenuReleaseNotes.SINGLETON_INSTANCE),

    REPORT_MESSAGE (),
    REPORT_STACK (),
    REPORT_COMBAT (),
    REPORT_DEPENDENCIES (),
    REPORT_LOG (),
    REPORT_YIELD (),

    DEV_MODE (),
    BUTTON_DOCK (),

    // Battlefields, use setDoc to register.
    FIELD_0 (),
    FIELD_1 (),
    FIELD_2 (),
    FIELD_3 (),
    FIELD_4 (),
    FIELD_5 (),
    FIELD_6 (),
    FIELD_7 (),

    // Hands, use setDoc to register.
    HAND_0 (),
    HAND_1 (),
    HAND_2 (),
    HAND_3 (),
    HAND_4 (),
    HAND_5 (),
    HAND_6 (),
    HAND_7 ();

    public final static EDocID[] Fields = new EDocID[] {FIELD_0, FIELD_1, FIELD_2, FIELD_3, FIELD_4, FIELD_5, FIELD_6, FIELD_7};
    public final static EDocID[] Hands = new EDocID[] {HAND_0, HAND_1, HAND_2, HAND_3, HAND_4, HAND_5, HAND_6, HAND_7};
    static {
        for (int i = 0; i < 8; i++) EDocID.Fields[i].setDoc(new VEmptyDoc(EDocID.Fields[i]));
        for (int i = 0; i < 8; i++) EDocID.Hands[i].setDoc(new VEmptyDoc(EDocID.Hands[i]));
    }
    public final static EDocID[] VarDocs = ObjectArrays.concat(Fields, Hands, EDocID.class);

    // End enum declarations, start enum methods.
    private IVDoc<? extends ICDoc> vDoc;

    EDocID() {
        this(null);
    }

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
        //if (vDoc == null) { throw new NullPointerException("No document found for " + this.name() + "."); }
        return vDoc;
    }
}
