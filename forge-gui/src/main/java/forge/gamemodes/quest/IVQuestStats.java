package forge.gamemodes.quest;

import forge.gui.interfaces.IButton;
import forge.gui.interfaces.ICheckBox;
import forge.gui.interfaces.IComboBox;

/** Dictates methods required for a panel with stats/pet display. */

public interface IVQuestStats {

    IButton getBtnBazaar();
    IButton getBtnSpellShop();
    IButton getBtnUnlock();
    IButton getBtnTravel();
    IButton getLblCredits();
    IButton getLblLife();
    IButton getLblWorld();
    IButton getLblWins();
    IButton getLblLosses();
    IButton getLblNextChallengeInWins();
    IButton getLblCurrentDeck();
    IButton getLblWinStreak();

    IComboBox<String> getCbxPet();
    IComboBox<String> getCbxMatchLength();
    ICheckBox getCbPlant();

    IButton getLblZep();

    boolean isChallengesView();
    boolean allowHtml();
}
