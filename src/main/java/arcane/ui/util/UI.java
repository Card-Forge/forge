package arcane.ui.util;

import com.google.common.collect.MapMaker;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ConcurrentMap;

/**
 * UI utility functions.
 *
 * @author Forge
 * @version $Id$
 */
public class UI {
    /** Constant <code>imageCache</code> */
    static private ConcurrentMap<URI, Image> imageCache = new MapMaker().softValues().makeMap();

    /**
     * <p>getToggleButton.</p>
     *
     * @return a {@link javax.swing.JToggleButton} object.
     */
    static public JToggleButton getToggleButton() {
        JToggleButton button = new JToggleButton();
        button.setMargin(new Insets(2, 4, 2, 4));
        return button;
    }

    /**
     * <p>getButton.</p>
     *
     * @return a {@link javax.swing.JButton} object.
     */
    static public JButton getButton() {
        JButton button = new JButton();
        button.setMargin(new Insets(2, 4, 2, 4));
        return button;
    }

    /**
     * <p>setTitle.</p>
     *
     * @param panel a {@link javax.swing.JPanel} object.
     * @param title a {@link java.lang.String} object.
     */
    static public void setTitle(JPanel panel, String title) {
        Border border = panel.getBorder();
        if (border instanceof TitledBorder) {
            ((TitledBorder) panel.getBorder()).setTitle(title);
            panel.repaint();
        } else
            panel.setBorder(BorderFactory.createTitledBorder(title));
    }

    /**
     * <p>getFileURL.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @return a {@link java.net.URL} object.
     */
    static public URL getFileURL(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException ignored) {
            }
        }
        return UI.class.getResource(path);
    }

    /**
     * <p>getImageIcon.</p>
     *
     * @param path a {@link java.lang.String} object.
     * @return a {@link javax.swing.ImageIcon} object.
     */
    static public ImageIcon getImageIcon(String path) {
        InputStream stream = null;
        try {
            try {
                stream = UI.class.getResourceAsStream(path);
                if (stream == null && new File(path).exists()) stream = new FileInputStream(path);
                if (stream == null) throw new RuntimeException("Image not found: " + path);
                byte[] data = new byte[stream.available()];
                stream.read(data);
                return new ImageIcon(data);
            } finally {
                if (stream != null)
                    stream.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error reading image: " + path);
        }
    }

    /**
     * <p>setHTMLEditorKit.</p>
     *
     * @param editorPane a {@link javax.swing.JEditorPane} object.
     */
    static public void setHTMLEditorKit(JEditorPane editorPane) {
        editorPane.getDocument().putProperty("imageCache", imageCache); // Read internally by ImageView, but never written.
        // Extend all this shit to cache images.
        editorPane.setEditorKit(new HTMLEditorKit() {
            private static final long serialVersionUID = -562969765076450440L;

            public ViewFactory getViewFactory() {
                return new HTMLFactory() {
                    public View create(Element elem) {
                        Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
                        if (o instanceof HTML.Tag) {
                            HTML.Tag kind = (HTML.Tag) o;
                            if (kind == HTML.Tag.IMG) return new ImageView(elem) {
                                public URL getImageURL() {
                                    URL url = super.getImageURL();
                                    // Put an image into the cache to be read by other ImageView methods.
                                    if (url != null && imageCache.get(url) == null)
                                        try {
                                            imageCache.put(url.toURI(), Toolkit.getDefaultToolkit().createImage(url));
                                        } catch (URISyntaxException e) {
                                        }
                                    return url;
                                }
                            };
                        }
                        return super.create(elem);
                    }
                };
            }
        });
    }

    /**
     * <p>setVerticalScrollingView.</p>
     *
     * @param scrollPane a {@link javax.swing.JScrollPane} object.
     * @param view a {@link java.awt.Component} object.
     */
    static public void setVerticalScrollingView(JScrollPane scrollPane, final Component view) {
        final JViewport viewport = new JViewport();
        viewport.setLayout(new ViewportLayout() {
            private static final long serialVersionUID = -4436977380450713628L;

            public void layoutContainer(Container parent) {
                viewport.setViewPosition(new Point(0, 0));
                Dimension viewportSize = viewport.getSize();
                int width = viewportSize.width;
                int height = Math.max(view.getPreferredSize().height, viewportSize.height);
                viewport.setViewSize(new Dimension(width, height));
            }
        });
        viewport.setView(view);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewport(viewport);
    }

    /**
     * <p>getDisplayManaCost.</p>
     *
     * @param manaCost a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    static public String getDisplayManaCost(String manaCost) {
        manaCost = manaCost.replace("/", "");
        manaCost = manaCost.replace("X 0", "X");
        // A pipe in the cost means "process left of the pipe as the card color, but display right of the pipe as the cost".
        int pipePosition = manaCost.indexOf("{|}");
        if (pipePosition != -1) manaCost = manaCost.substring(pipePosition + 3);
        return manaCost;
    }

    /**
     * <p>invokeLater.</p>
     *
     * @param runnable a {@link java.lang.Runnable} object.
     */
    static public void invokeLater(Runnable runnable) {
        EventQueue.invokeLater(runnable);
    }

    /**
     * <p>invokeAndWait.</p>
     *
     * @param runnable a {@link java.lang.Runnable} object.
     */
    static public void invokeAndWait(Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
            return;
        }
        try {
            EventQueue.invokeAndWait(runnable);
        } catch (InterruptedException ex) {
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * <p>setSystemLookAndFeel.</p>
     */
    static public void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Error setting look and feel:");
            ex.printStackTrace();
        }
    }

    /**
     * <p>setDefaultFont.</p>
     *
     * @param font a {@link java.awt.Font} object.
     */
    static public void setDefaultFont(Font font) {
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) UIManager.put(key, font);
        }
    }
}
