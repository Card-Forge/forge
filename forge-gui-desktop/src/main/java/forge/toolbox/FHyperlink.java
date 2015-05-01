package forge.toolbox;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.SwingWorker;

import forge.UiCommand;


@SuppressWarnings("serial")
public class FHyperlink extends FLabel {
    public static class Builder extends FLabel.Builder {
        private String bldUrl;

        public Builder() {
            bldHoverable        = true;
            bldReactOnMouseDown = true;
            bldCmd = null;
        }

        @Override
        public FHyperlink build() {
            final StringBuilder sb = new StringBuilder();
            sb.append("<html><a href='").append(bldUrl).append("'>");
            sb.append((null == bldText || bldText.isEmpty()) ? bldUrl : bldText);
            sb.append("</a></html>");

            final boolean browsingSupported = _isBrowsingSupported();
            if (browsingSupported) {
                tooltip(bldUrl);
            }
            else {
                tooltip(bldUrl + " (click to copy to clipboard)");
            }

            final URI uri;
            try {
                uri = new URI(bldUrl);
            }
            catch (final URISyntaxException e) {
                throw new RuntimeException(e);
            }

            // overwrite whatever command is there -- we could chain them if we wanted to, though
            cmdClick(new UiCommand() {
                @Override
                public void run() {
                    if (browsingSupported) {
                        // open link in default browser
                        new _LinkRunner(uri).execute();
                    }
                    else {
                        // copy link to clipboard
                        final StringSelection ss = new StringSelection(bldUrl);
                        try {
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                        }
                        catch (final IllegalStateException ex) {
                            FOptionPane.showErrorDialog(
                                    "Sorry, a problem occurred while copying this link to your system clipboard.",
                                    "A problem occurred");
                        }
                    }
                }
            });

            return new FHyperlink(this);
        }

        public Builder url(final String url) { bldUrl = url; return this; }

        private static boolean _isBrowsingSupported() {
            if (!Desktop.isDesktopSupported()) {
                return false;
            }
            return Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        }
    }

    // Call this using FLabel.Builder()...
    private FHyperlink(final Builder b) {
        super(b);

        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private static class _LinkRunner extends SwingWorker<Void, Void> {
        private final URI uri;

        private _LinkRunner(final URI u) {
            if (u == null) {
                throw new NullPointerException();
            }
            uri = u;
        }

        @Override
        protected Void doInBackground() throws Exception {
            Desktop.getDesktop().browse(uri);
            return null;
        }
    }
}
