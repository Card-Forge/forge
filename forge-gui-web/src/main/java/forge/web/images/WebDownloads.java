package forge.web.images;

import com.google.gson.JsonObject;
import forge.gui.UiCommand;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IButton;
import forge.gui.interfaces.IProgressBar;
import forge.gui.interfaces.ITextField;
import forge.localinstance.skin.FSkinProp;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.web.protocol.ToBrowser;
import forge.web.protocol.MessageJson;

import java.util.function.Consumer;

/** Runs a download through the shared service. The browser shows the progress instead of the dialog that desktop and mobile use. */
public final class WebDownloads implements IHasForgeLog {
    private WebDownloads() {
    }

    public static void run(final GuiDownloadService service, final Consumer<Boolean> callback,
            final Consumer<JsonObject> toBrowser) {
        final Progress progress = new Progress(service.getTitle(), toBrowser);
        try {
            // No dialog waits for Start and Close, so the service is set up and run to completion on this thread
            service.initialize(new EmptyField(), new EmptyField(), progress, new QuietButton(), () -> { }, null, () -> { });
            service.run();
        } catch (final RuntimeException e) {
            netLog.error(e, "Download failed");
            progress.say(Localizer.getInstance().getMessage("lblWebDownloadFailed"));
            callback.accept(false);
            return;
        }
        progress.finish();
        callback.accept(true);
    }

    /** Reports a download's progress as a notice, which the browser already knows how to show. */
    private static final class Progress extends QuietBar {
        private final String title;
        private final Consumer<JsonObject> toBrowser;
        private String description;

        Progress(final String title, final Consumer<JsonObject> toBrowser) {
            this.title = title;
            this.toBrowser = toBrowser;
        }

        void finish() {
            say(description == null ? Localizer.getInstance().getMessage("lblWebDownloadFinished") : description);
        }

        void say(final String text) {
            toBrowser.accept(MessageJson.encode(new ToBrowser.Notice(title, text, false)));
        }

        @Override public void setDescription(final String s) {
            description = s;
            say(s);
        }
    }

    /** A download dialog's progress bar that shows nothing. A subclass overrides the calls it reports. */
    public static class QuietBar implements IProgressBar {
        private volatile int maximum = 1;

        @Override public void setDescription(final String s) { }
        @Override public void setValue(final int progress) { }
        @Override public void reset() { }
        @Override public void setShowETA(final boolean b) { }
        @Override public void setShowCount(final boolean b) { }
        @Override public void setPercentMode(final boolean percentMode) { }
        @Override public void setMaximum(final int maximum0) { maximum = maximum0; }
        @Override public int getMaximum() { return maximum; }
    }

    /** A download dialog's Start button that nobody sees. Its commands are dropped unless a subclass runs them. */
    public static class QuietButton implements IButton {
        @Override public boolean isEnabled() { return true; }
        @Override public void setEnabled(final boolean b) { }
        @Override public boolean isVisible() { return true; }
        @Override public void setVisible(final boolean b) { }
        @Override public String getToolTipText() { return ""; }
        @Override public void setToolTipText(final String s) { }
        @Override public String getText() { return ""; }
        @Override public void setText(final String text) { }
        @Override public void setSelected(final boolean b) { }
        @Override public boolean isSelected() { return false; }
        @Override public void setCommand(final UiCommand command0) { }
        @Override public void setImage(final FSkinProp color) { }
        @Override public void setTextColor(final int r, final int g, final int b) { }
        @Override public boolean requestFocusInWindow() { return false; }
    }

    /** The proxy address and port fields of a download dialog, left empty because no proxy is chosen here. */
    public static final class EmptyField implements ITextField {
        @Override public String getText() { return ""; }
        @Override public void setText(final String text) { }
        @Override public boolean isEnabled() { return false; }
        @Override public void setEnabled(final boolean b) { }
        @Override public boolean isVisible() { return true; }
        @Override public void setVisible(final boolean b) { }
        @Override public String getToolTipText() { return ""; }
        @Override public void setToolTipText(final String s) { }
        @Override public boolean requestFocusInWindow() { return false; }
    }
}
