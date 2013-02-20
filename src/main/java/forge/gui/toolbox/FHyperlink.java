package forge.gui.toolbox;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import forge.Command;



@SuppressWarnings("serial")
public class FHyperlink extends FLabel {
    public static class Builder extends FLabel.Builder {
        private String bldUrl;
        
        public Builder() {
            bldHoverable        = true;
            bldReactOnMouseDown = true;
            bldCmd = null;
        }
        
        public FHyperlink build() {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><a href='").append(bldUrl).append("'>");
            sb.append((null == bldText || bldText.isEmpty()) ? bldUrl : bldText);
            sb.append("</a></html>");

            final boolean browsingSupported = _isBrowsingSupported();
            if (browsingSupported) {
                tooltip(bldUrl);
            } else {
                tooltip(bldUrl + " (click to copy to clipboard)");
            }
            
            final URI uri;
            try {
                uri = new URI(bldUrl);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            
            // overwrite whatever command is there -- we could chain them if we wanted to, though
            cmdClick(new Command() {
                @Override
                public void execute() {
                    if (browsingSupported) {
                        // open link in default browser
                        new _LinkRunner(uri).execute();
                    } else {
                        // copy link to clipboard
                        StringSelection ss = new StringSelection(bldUrl);
                        try {
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                        } catch (IllegalStateException ex) {
                            JOptionPane.showMessageDialog(null,
                                    "Sorry, a problem occurred while copying this link to your system clipboard.",
                                    "A problem occured", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
            
            return new FHyperlink(this);
        }
        
        public Builder url(String url) { bldUrl = url; return this; }
        
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

        private _LinkRunner(URI u) {
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
