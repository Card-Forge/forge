package forge.screens.home.quest;

import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBoxWrapper;
import forge.toolbox.FLabel;

/** Dictates methods required for a panel with stats/pet display. */

public interface IVQuestStats {

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getBtnRandomOpponent();
    
    /** @return {@link forge.toolbox.FLabel} */
    FLabel getBtnBazaar();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getBtnSpellShop();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getBtnUnlock();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getBtnTravel();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getLblCredits();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getLblLife();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getLblWorld();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getLblWins();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getLblLosses();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getLblNextChallengeInWins();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getLblCurrentDeck();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getLblWinStreak();

    /** @return {@link javax.swing.FComboBoxWrapper} */
    FComboBoxWrapper<String> getCbxPet();

    /** @return {@link forge.toolbox.FCheckBox} */
    FCheckBox getCbPlant();

    /** @return {@link forge.toolbox.FCheckBox} */ 
    FCheckBox getCbCharm();

    /** @return {@link forge.toolbox.FLabel} */
    FLabel getLblZep();
}
