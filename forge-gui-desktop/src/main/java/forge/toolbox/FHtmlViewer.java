package forge.toolbox;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import forge.toolbox.FSkin.SkinnedEditorPane;

/**
 * Viewer for HTML
 *
 */
@SuppressWarnings("serial")
public class FHtmlViewer extends SkinnedEditorPane {
    /** */
    public FHtmlViewer() {
        super();
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setOpaque(false);
        this.setFocusable(false);
        this.setEditable(false);
        this.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        this.setContentType("text/html");
    }

    /** @param str {@java.lang.String} */
    public FHtmlViewer(final String str) {
        this();
        this.setText(str);
    }

    @Override
    public void setText(final String text) {
        SwingUtilities.invokeLater( new Runnable() { //need to invokeLater to avoid flicker
            @Override
            public void run() {
                setSuperText(null == text ? "" : text.replaceAll("(\r\n)|(\n)", "<br>")); //replace line breaks with <br> elements
                setCaretPosition(0); //keep scrolled to top
            }
        });
    }

    private void setSuperText(final String text) {
        super.setText(text);
    }
}
