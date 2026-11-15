package forge.web.console;

import forge.gui.FThreads;
import forge.gui.UiCommand;
import forge.gui.download.GuiDownloadFilteredCardImages;
import forge.gui.download.GuiDownloadService;
import forge.util.Localizer;
import forge.web.images.WebDownloads;

import java.text.NumberFormat;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Saves the image of every card to this computer ahead of time. The console's button starts it, shows its progress and cancels it. */
final class CardImageDownloads {
    private static final Localizer TEXT = Localizer.getInstance();
    private static final int RESULT_MILLIS = 4000;

    private final Consumer<String> showProgress;
    private GuiDownloadService running;

    /** showProgress receives the text for the console's button, on the Swing thread: its plain name, or a download's progress or result. */
    CardImageDownloads(final Consumer<String> showProgress) {
        this.showProgress = showProgress;
    }

    /** Starts the download, or cancels the one that is running. Runs on the Swing thread. */
    void startOrCancel() {
        if (running != null) {
            running.setCancel(true);
            running = null;
            showProgress.accept(TEXT.getMessage("lblWebImagesButton"));
            return;
        }
        final GuiDownloadService service = new GuiDownloadFilteredCardImages(card -> true);
        running = service;
        showProgress.accept(TEXT.getMessage("lblWebImagesPreparing"));
        service.initialize(new WebDownloads.EmptyField(), new WebDownloads.EmptyField(), new Bar(service), new Starter(),
                // The service runs this command just before its last count, so the result is queued behind that count on Forge's UI thread and then on Swing's
                () -> FThreads.invokeInEdtLater(() -> SwingUtilities.invokeLater(() -> {
                    if (running == service) {
                        running = null;
                        showProgress.accept(TEXT.getMessage("lblWebDownloadFinished"));
                        final Timer plain = new Timer(RESULT_MILLIS, e -> {
                            if (running == null) {
                                showProgress.accept(TEXT.getMessage("lblWebImagesButton"));
                            }
                        });
                        plain.setRepeats(false);
                        plain.start();
                    }
                })), null, null);
    }

    /** Shows the service's progress on the console's button. A cancelled download's late reports are dropped. */
    private final class Bar extends WebDownloads.QuietBar {
        private final GuiDownloadService service;

        Bar(final GuiDownloadService service) {
            this.service = service;
        }

        @Override public void setValue(final int progress) {
            final int total = getMaximum();
            SwingUtilities.invokeLater(() -> {
                if (running == service) {
                    final NumberFormat n = NumberFormat.getIntegerInstance();
                    showProgress.accept(TEXT.getMessage("lblWebImagesButtonProgress", n.format(progress), n.format(total)));
                }
            });
        }
    }

    /** Replaces the service's Start button. It runs the start and close commands as soon as the service hands each one over. */
    private static final class Starter extends WebDownloads.QuietButton {
        @Override public void setCommand(final UiCommand command) { command.run(); }
    }
}
