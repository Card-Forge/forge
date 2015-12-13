package forge.interfaces;

import java.util.List;

import forge.assets.FSkinProp;
import forge.item.PaperCard;
import forge.planarconquest.ConquestReward;

public interface IWinLoseView<T extends IButton> {
    T getBtnContinue();
    T getBtnRestart();
    T getBtnQuit();
    void hide();

    void showRewards(Runnable runnable);
    void showCards(String title, List<PaperCard> cards);
    void showConquestRewards(String title, List<ConquestReward> rewards);
    void showMessage(String message, String title, FSkinProp icon);
}
