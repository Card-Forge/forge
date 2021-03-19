package forge.gui.interfaces;

import java.util.List;

import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;

public interface IWinLoseView<T extends IButton> {
    T getBtnContinue();
    T getBtnRestart();
    T getBtnQuit();
    void hide();

    void showRewards(Runnable runnable);
    void showCards(String title, List<PaperCard> cards);
    void showMessage(String message, String title, FSkinProp icon);
}
