package forge.interfaces;

public interface IWinLoseView<T extends IButton> {
    T getBtnContinue();
    T getBtnRestart();
    T getBtnQuit();
    void hide();
}
