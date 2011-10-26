package forge.quest.gui.main;

import java.awt.BorderLayout;
import java.awt.Color;
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
    protected Color backgroundColor;
    private boolean selected;
    private QuestEvent event;
    private String iconfilename;

    /** The root panel. */
    public JPanel rootPanel = new JPanel();

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
        this.iconfilename = qe.icon;
        File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
        File file = new File(base, iconfilename);

        if (!file.exists()) {
            file = new File(base, "Unknown.jpg");
            this.iconfilename = "Unknown.jpg";
        }

        ImageIcon icon = new ImageIcon(file.toString());

        this.backgroundColor = getBackground();
        this.setLayout(new BorderLayout(5, 5));

        JLabel iconLabel;

        if (icon.getIconHeight() == -1) {
            iconLabel = new JLabel(GuiUtils.getEmptyIcon(40, 40));
        } else {
            iconLabel = new JLabel(GuiUtils.getResizedIcon(icon, 40, 40));
        }

        iconLabel.setBorder(new LineBorder(Color.BLACK));
        iconLabel.setAlignmentY(TOP_ALIGNMENT);

        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);
        iconPanel.add(iconLabel, BorderLayout.NORTH);
        this.add(iconPanel, BorderLayout.WEST);

        rootPanel.setOpaque(false);
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        this.add(rootPanel, BorderLayout.CENTER);

        JPanel centerTopPanel = new JPanel();
        centerTopPanel.setOpaque(false);
        centerTopPanel.setAlignmentX(LEFT_ALIGNMENT);
        centerTopPanel.setLayout(new BoxLayout(centerTopPanel, BoxLayout.X_AXIS));

        JLabel nameLabel = new JLabel(qe.getTitle());
        GuiUtils.setFontSize(nameLabel, 20);
        nameLabel.setAlignmentY(BOTTOM_ALIGNMENT);
        centerTopPanel.add(nameLabel);

        GuiUtils.addExpandingHorizontalSpace(centerTopPanel);

        JLabel difficultyLabel = new JLabel(qe.getDifficulty());
        difficultyLabel.setAlignmentY(BOTTOM_ALIGNMENT);
        centerTopPanel.add(difficultyLabel);
        rootPanel.add(centerTopPanel);

        GuiUtils.addGap(rootPanel);

        JLabel descriptionLabel = new JLabel(qe.getDescription());
        descriptionLabel.setAlignmentX(LEFT_ALIGNMENT);
        rootPanel.add(descriptionLabel);

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
        return selected;
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
            this.setBackground(backgroundColor.darker());
        } else {
            this.setBackground(backgroundColor);
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
}
