/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.quest.gui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import forge.gui.GuiUtils;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * QuestSelectablePanel class.
 * </p>
 * VIEW - Creates a selectable panel, used for picking events.
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestSelectablePanel extends JPanel {
    /** Constant <code>serialVersionUID=-1502285997894190742L</code>. */
    private static final long serialVersionUID = -1502285997894190742L;

    /** The background color. */
    private final Color backgroundColor;
    private boolean selected;
    private final QuestEvent event;
    private String iconfilename;

    /** The root panel. */
    private JPanel rootPanel = new JPanel();

    /**
     * <p>
     * Constructor for QuestSelectablePanel.
     * </p>
     * VIEW - A JPanel for selecting quest events.
     * 
     * @param qe
     *            the qe
     */
    public QuestSelectablePanel(final QuestEvent qe) {
        this.event = qe;
        this.iconfilename = qe.getIcon();
        final File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
        File file = new File(base, this.iconfilename);

        if (!file.exists()) {
            file = new File(base, "Unknown.jpg");
            this.iconfilename = "Unknown.jpg";
        }

        final ImageIcon icon = new ImageIcon(file.toString());

        this.backgroundColor = this.getBackground();
        this.setLayout(new BorderLayout(5, 5));

        JLabel iconLabel;

        if (icon.getIconHeight() == -1) {
            iconLabel = new JLabel(GuiUtils.getEmptyIcon(40, 40));
        } else {
            iconLabel = new JLabel(GuiUtils.getResizedIcon(icon, 40, 40));
        }

        iconLabel.setBorder(new LineBorder(Color.BLACK));
        iconLabel.setAlignmentY(Component.TOP_ALIGNMENT);

        final JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);
        iconPanel.add(iconLabel, BorderLayout.NORTH);
        this.add(iconPanel, BorderLayout.WEST);

        this.getRootPanel().setOpaque(false);
        this.getRootPanel().setLayout(new BoxLayout(this.getRootPanel(), BoxLayout.Y_AXIS));
        this.add(this.getRootPanel(), BorderLayout.CENTER);

        final JPanel centerTopPanel = new JPanel();
        centerTopPanel.setOpaque(false);
        centerTopPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerTopPanel.setLayout(new BoxLayout(centerTopPanel, BoxLayout.X_AXIS));

        final JLabel nameLabel = new JLabel(qe.getTitle());
        GuiUtils.setFontSize(nameLabel, 20);
        nameLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        centerTopPanel.add(nameLabel);

        GuiUtils.addExpandingHorizontalSpace(centerTopPanel);

        final JLabel difficultyLabel = new JLabel(qe.getDifficulty());
        difficultyLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        centerTopPanel.add(difficultyLabel);
        this.getRootPanel().add(centerTopPanel);

        GuiUtils.addGap(this.getRootPanel());

        final JLabel descriptionLabel = new JLabel(qe.getDescription());
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.getRootPanel().add(descriptionLabel);

        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        this.setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(5, 5, 5, 5)));
    }

    /**
     * <p>
     * isSelected.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSelected() {
        return this.selected;
    }

    /**
     * <p>
     * Setter for the field <code>selected</code>.
     * </p>
     * 
     * @param selected
     *            a boolean.
     */
    public final void setSelected(final boolean selected) {
        if (selected) {
            this.setBackground(this.backgroundColor.darker());
        } else {
            this.setBackground(this.backgroundColor);
        }

        this.selected = selected;
    }

    /**
     * <p>
     * getIconFilename.
     * </p>
     * 
     * @return String
     */
    public final String getIconFilename() {
        return this.iconfilename;
    }

    /**
     * <p>
     * getEvent.
     * </p>
     * 
     * @return QuestEvent
     */
    public final QuestEvent getEvent() {
        return this.event;
    }

    /**
     * Gets the root panel.
     * 
     * @return the rootPanel
     */
    public JPanel getRootPanel() {
        return this.rootPanel;
    }

    /**
     * Sets the root panel.
     * 
     * @param rootPanel
     *            the rootPanel to set
     */
    public void setRootPanel(final JPanel rootPanel) {
        this.rootPanel = rootPanel; // TODO: Add 0 to parameter's name.
    }
}
