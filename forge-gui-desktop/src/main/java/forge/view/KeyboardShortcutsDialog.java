package forge.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import forge.control.KeyboardShortcuts;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.screens.home.settings.VSubmenuPreferences.KeyboardShortcutField;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class KeyboardShortcutsDialog extends FDialog {

    public KeyboardShortcutsDialog() {
        super(false, true, "10");
        final Localizer localizer = Localizer.getInstance();
        setTitle(localizer.getMessage("lblKeyboardShortcuts"));
        setSize(500, 600);
        setMinimumSize(new Dimension(350, 300));

        // Scrollable panel that always matches viewport width, preventing horizontal scroll.
        final JPanel content = new ScrollablePanel(new MigLayout("insets 10 15 20 15, gap 5, wrap 2, fillx", "[grow][120!]"));
        content.setOpaque(false);

        // Section 1: Configurable shortcuts
        for (final Shortcut shortcut : KeyboardShortcuts.getCachedShortcuts()) {
            final FLabel descLabel = new FLabel.Builder()
                    .text(shortcut.getDescription())
                    .fontAlign(SwingConstants.LEFT)
                    .fontSize(11).build();
            content.add(descLabel, "growx");

            final KeyboardShortcutField field = new KeyboardShortcutField(shortcut);
            field.setFont(FSkin.getRelativeFont(11));
            // Clear existing binding on click so a new key press replaces it.
            field.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(final java.awt.event.MouseEvent e) {
                    field.setCodeString("");
                }
            });
            // Transfer focus away once a non-modifier key completes the binding.
            field.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(final KeyEvent e) {
                    final int code = e.getKeyCode();
                    if (code != KeyEvent.VK_SHIFT && code != KeyEvent.VK_CONTROL
                            && code != KeyEvent.VK_ALT && code != KeyEvent.VK_META) {
                        content.requestFocusInWindow();
                    }
                }
            });
            content.add(field, "w 120!, h 22!");
        }

        // Section 2: Fixed menu accelerators
        final FLabel headerFixed = new FLabel.Builder()
                .text(localizer.getMessage("lblMenuShortcuts"))
                .fontAlign(SwingConstants.LEFT)
                .fontSize(14).fontStyle(Font.BOLD).build();
        content.add(headerFixed, "span 2, gaptop 10, gapbottom 5, wrap");

        final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        final String modPrefix = InputEvent.getModifiersExText(menuMask) + "+";

        addFixedRow(content, "Forge Wiki", KeyEvent.getKeyText(KeyEvent.VK_F1));
        addFixedRow(content, "Full Screen", KeyEvent.getKeyText(KeyEvent.VK_F11));
        addFixedRow(content, "Undo", modPrefix + KeyEvent.getKeyText(KeyEvent.VK_Z));
        addFixedRow(content, "Concede", modPrefix + KeyEvent.getKeyText(KeyEvent.VK_Q));
        addFixedRow(content, "Alpha Strike", modPrefix + KeyEvent.getKeyText(KeyEvent.VK_A));
        addFixedRow(content, "End Turn", modPrefix + KeyEvent.getKeyText(KeyEvent.VK_E));
        addFixedRow(content, "Toggle Panel Tabs", modPrefix + KeyEvent.getKeyText(KeyEvent.VK_T));
        addFixedRow(content, "Toggle Card Overlays", modPrefix + KeyEvent.getKeyText(KeyEvent.VK_O));

        final FScrollPane scrollPane = new FScrollPane(content, false,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, "w 100%!, h 100%!");
        setDefaultFocus(scrollPane);
    }

    private static void addFixedRow(final JPanel panel, final String description, final String keyText) {
        final FLabel descLabel = new FLabel.Builder()
                .text(description).fontAlign(SwingConstants.LEFT).fontSize(11).build();
        panel.add(descLabel, "growx");

        final JLabel field = new JLabel(keyText);
        field.setOpaque(true);
        field.setBackground(new Color(200, 200, 200));
        field.setFont(FSkin.getRelativeFont(11).getBaseFont());
        field.setBorder(BorderFactory.createCompoundBorder(
                UIManager.getBorder("TextField.border"),
                BorderFactory.createEmptyBorder(0, 2, 0, 2)));
        panel.add(field, "w 120!, h 22!");
    }

    /** JPanel that implements Scrollable to track viewport width, preventing horizontal scrolling. */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        ScrollablePanel(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
