package forge.gamemodes.quest;

import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.quest.QuestDraftUtils.Mode;
import forge.gamemodes.quest.data.QuestEventDraftContainer;
import forge.gui.interfaces.IButton;

public interface IQuestTournamentView {
    Mode getMode();
    void setMode(Mode mode0);
    void populate();
    void updateEventList(QuestEventDraftContainer events);
    void updateTournamentBoxLabel(String playerID, int iconID, int box, boolean first);
    void startDraft(BoosterDraft draft);
    void editDeck(boolean isNew);

    IButton getLblCredits();
    IButton getLblFirst();
    IButton getLblSecond();
    IButton getLblThird();
    IButton getLblFourth();
    IButton getBtnSpendToken();
    IButton getBtnLeaveTournament();
}
