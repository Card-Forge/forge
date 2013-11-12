package forge.gui.toolbox;

import javax.swing.JEditorPane;

/** 
 * Viewer for HTML
 *
 */
@SuppressWarnings("serial")
public class FHtmlViewer extends JEditorPane {
    /** */
    public FHtmlViewer() {
        super();
        FSkin.JTextComponentSkin<FHtmlViewer> skin = FSkin.get(this);
        skin.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        skin.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
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
    public void setText(String text) {
    	super.setText(text.replaceAll("(\r\n)|(\n)", "<br>")); //replace line breaks with <br> elements
    }
}
