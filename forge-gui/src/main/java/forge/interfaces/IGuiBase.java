package forge.interfaces;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;

import forge.LobbyPlayer;
import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.deck.CardPool;
import forge.game.player.IHasIcon;
import forge.item.PaperCard;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.view.CardView;

public interface IGuiBase {
    boolean isRunningOnDesktop();
    String getCurrentVersion();
    String getAssetsDir();
    void invokeInEdtLater(Runnable runnable);
    void invokeInEdtAndWait(final Runnable proc);
    boolean isGuiThread();
    ISkinImage getSkinIcon(FSkinProp skinProp);
    ISkinImage getUnskinnedIcon(String path);
    ISkinImage createLayeredImage(FSkinProp background, String overlayFilename, float opacity);
    void showBugReportDialog(String title, String text, boolean showExitAppBtn);
    void showImageDialog(ISkinImage image, String message, String title);
    int showOptionDialog(String message, String title, FSkinProp icon, String[] options, int defaultOption);
    int showCardOptionDialog(CardView card, String message, String title, FSkinProp icon, String[] options, int defaultOption);
    String showInputDialog(String message, String title, FSkinProp icon, String initialInput, String[] inputOptions);
    <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display);
    <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode);
    List<PaperCard> sideboard(CardPool sideboard, CardPool main);
    String showFileDialog(String title, String defaultDir);
    File getSaveFile(File defaultFile);
    void showCardList(final String title, final String message, final List<PaperCard> list);
    boolean showBoxedProduct(final String title, final String message, final List<PaperCard> list);
    void setCard(CardView card);
    int getAvatarCount();
    void copyToClipboard(String text);
    void browseToUrl(String url) throws Exception;
    IAudioClip createAudioClip(String filename);
    IAudioMusic createAudioMusic(String filename);
    void startAltSoundSystem(String filename, boolean isSynchronized);
    void clearImageCache();
    void showSpellShop();
    void showBazaar();
    void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi);
}