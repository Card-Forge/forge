package forge.quest.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import forge.gui.GuiUtils;
import forge.gui.SelectablePanel;
import forge.quest.data.DeckSingleBattle;

/**
 * <p>PanelSingleBattle</p>
 * VIEW - A selectable panel for battles available in Quest mode.
 * 
 */
@SuppressWarnings("serial")
public class PanelSingleBattle extends SelectablePanel {
    
    private final DeckSingleBattle battle;
    
    public PanelSingleBattle(DeckSingleBattle b) {
        battle = b;
        final JPanel centerPanel = new JPanel();

        this.setLayout(new BorderLayout(5, 5));

        // Icon stuff
        JLabel iconLabel;

        if (battle.getIcon() == null) {
            iconLabel = new JLabel(GuiUtils.getEmptyIcon(40, 40));
        } else {
            iconLabel = new JLabel(GuiUtils.getResizedIcon(battle.getIcon(), 40, 40));
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

        JLabel nameLabel = new JLabel(battle.getDisplayName());
        GuiUtils.setFontSize(nameLabel, 20);
        nameLabel.setAlignmentY(BOTTOM_ALIGNMENT);
        centerTopPanel.add(nameLabel);

        GuiUtils.addExpandingHorizontalSpace(centerTopPanel);

        JLabel difficultyLabel = new JLabel(battle.getDifficulty());
        difficultyLabel.setAlignmentY(BOTTOM_ALIGNMENT);
        centerTopPanel.add(difficultyLabel);
        centerPanel.add(centerTopPanel);

        GuiUtils.addGap(centerPanel);

        JLabel descriptionLabel = new JLabel(battle.getDescription());
        descriptionLabel.setAlignmentX(LEFT_ALIGNMENT);
        centerPanel.add(descriptionLabel);

        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        this.setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(5, 5, 5, 5)));
    }
    
    /**
     * <p>getIconFilename()</p>
     * Retrieves filename of icon used in this panel's display.
     * 
     * @return
     */
    public String getIconFilename() {
        return this.battle.getIconFilename();
    }
    
    /**
     * <p>getQuest()</p>
     * 
     * @return the DeckSingleBattle model associated with this panel. 
     */
    public DeckSingleBattle getBattle() {
        return this.battle;
    }
}
