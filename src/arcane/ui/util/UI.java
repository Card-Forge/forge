
package arcane.ui.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
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

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.ViewportLayout;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;

import com.google.common.collect.MapMaker;

/**
 * UI utility functions.
 */
public class UI {
	static private ConcurrentMap<URI, Image> imageCache = new MapMaker().softValues().makeMap();

	static public JToggleButton getToggleButton () {
		JToggleButton button = new JToggleButton();
		button.setMargin(new Insets(2, 4, 2, 4));
		return button;
	}

	static public JButton getButton () {
		JButton button = new JButton();
		button.setMargin(new Insets(2, 4, 2, 4));
		return button;
	}

	static public void setTitle (JPanel panel, String title) {
		Border border = panel.getBorder();
		if (border instanceof TitledBorder) {
			((TitledBorder)panel.getBorder()).setTitle(title);
			panel.repaint();
		} else
			panel.setBorder(BorderFactory.createTitledBorder(title));
	}

	static public URL getFileURL (String path) {
		File file = new File(path);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException ignored) {
			}
		}
		return UI.class.getResource(path);
	}

	static public ImageIcon getImageIcon (String path) {
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
			if(stream != null)
			    stream.close();
		    }
		} catch (IOException ex) {
			throw new RuntimeException("Error reading image: " + path);
		}
	}

	static public void setHTMLEditorKit (JEditorPane editorPane) {
		editorPane.getDocument().putProperty("imageCache", imageCache); // Read internally by ImageView, but never written.
		// Extend all this shit to cache images.
		editorPane.setEditorKit(new HTMLEditorKit() {
			private static final long serialVersionUID = -562969765076450440L;

			public ViewFactory getViewFactory () {
				return new HTMLFactory() {
					public View create (Element elem) {
						Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
						if (o instanceof HTML.Tag) {
							HTML.Tag kind = (HTML.Tag)o;
							if (kind == HTML.Tag.IMG) return new ImageView(elem) {
								public URL getImageURL () {
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

	static public void setVerticalScrollingView (JScrollPane scrollPane, final Component view) {
		final JViewport viewport = new JViewport();
		viewport.setLayout(new ViewportLayout() {
			private static final long serialVersionUID = -4436977380450713628L;

			public void layoutContainer (Container parent) {
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

	static public String getDisplayManaCost (String manaCost) {
		manaCost = manaCost.replace("/", "");
		manaCost = manaCost.replace("X 0", "X");
		// A pipe in the cost means "process left of the pipe as the card color, but display right of the pipe as the cost".
		int pipePosition = manaCost.indexOf("{|}");
		if (pipePosition != -1) manaCost = manaCost.substring(pipePosition + 3);
		return manaCost;
	}

	static public void invokeLater (Runnable runnable) {
		EventQueue.invokeLater(runnable);
	}

	static public void invokeAndWait (Runnable runnable) {
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

	static public void setSystemLookAndFeel () {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			System.err.println("Error setting look and feel:");
			ex.printStackTrace();
		}
	}

	static public void setDefaultFont (Font font) {
		for (Object key : Collections.list(UIManager.getDefaults().keys())) {
			Object value = UIManager.get(key);
			if (value instanceof javax.swing.plaf.FontUIResource) UIManager.put(key, font);
		}
	}
}
