package forge.interfaces;

import java.util.List;

import forge.assets.FSkinProp;
import forge.item.PaperCard;

public interface IWinLoseView<T extends IButton> {
    T getBtnContinue();
    T getBtnRestart();
    T getBtnQuit();
    void hide();

    void showRewards(Runnable runnable);
    void showCards(String title, List<PaperCard> cards);
    void showMessage(String message, String title, FSkinProp icon);
}
