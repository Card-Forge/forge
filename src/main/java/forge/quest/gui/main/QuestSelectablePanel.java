package forge.quest.gui.main;

import forge.gui.GuiUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * <p>QuestSelectablePanel class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestSelectablePanel extends JPanel {
    /** Constant <code>serialVersionUID=-1502285997894190742L</code> */
    private static final long serialVersionUID = -1502285997894190742L;

    protected Color backgroundColor;
    private boolean selected;

    ImageIcon icon;
    String name;
    String description;
    String difficulty;
    JPanel centerPanel = new JPanel();

    /**
     * <p>Constructor for QuestSelectablePanel.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param difficulty a {@link java.lang.String} object.
     * @param description a {@link java.lang.String} object.
     * @param icon a {@link javax.swing.ImageIcon} object.
     */
    public QuestSelectablePanel(String name, String difficulty, String description, ImageIcon icon) {
        this.backgroundColor = getBackground();

        this.name = name;
        this.difficulty = difficulty;
        this.description = description;
        this.icon = icon;

        this.setLayout(new BorderLayout(5, 5));


        JLabel iconLabel;

        if (icon == null) {
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

        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        this.add(centerPanel, BorderLayout.CENTER);

        JPanel centerTopPanel = new JPanel();
        centerTopPanel.setOpaque(false);
        centerTopPanel.setAlignmentX(LEFT_ALIGNMENT);
        centerTopPanel.setLayout(new BoxLayout(centerTopPanel, BoxLayout.X_AXIS));

        JLabel nameLabel = new JLabel(this.name);
        GuiUtils.setFontSize(nameLabel, 20);
        nameLabel.setAlignmentY(BOTTOM_ALIGNMENT);
        centerTopPanel.add(nameLabel);

        GuiUtils.addExpandingHorizontalSpace(centerTopPanel);

        JLabel difficultyLabel = new JLabel(this.difficulty);
        difficultyLabel.setAlignmentY(BOTTOM_ALIGNMENT);
        centerTopPanel.add(difficultyLabel);
        centerPanel.add(centerTopPanel);

        GuiUtils.addGap(centerPanel);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setAlignmentX(LEFT_ALIGNMENT);
        centerPanel.add(descriptionLabel);

        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        this.setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(5, 5, 5, 5)));
    }

    /**
     * <p>isSelected.</p>
     *
     * @return a boolean.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * <p>Setter for the field <code>selected</code>.</p>
     *
     * @param selected a boolean.
     */
    public void setSelected(boolean selected) {
        if (selected) {
            this.setBackground(backgroundColor.darker());
        } else {
            this.setBackground(backgroundColor);
        }

        this.selected = selected;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }
}
