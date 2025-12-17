package forge.screens.home.rogue;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.*;
import forge.util.Localizer;
import forge.view.arcane.CardPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles Swing components for Rogue Commander start screen.
 * Allows player to select a commander visually and begin a new run.
 */
public enum VSubmenuRogueStart implements IVSubmenu<CSubmenuRogueStart> {
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();

    // Card display constants
    private static final int CARD_WIDTH = 223;
    private static final int CARD_HEIGHT = Math.round(CARD_WIDTH * CardPanel.ASPECT_RATIO);
    private static final int CARD_SPACING = 15;
    private static final int CARDS_PER_ROW = 4;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Start New Run");

    private final FLabel lblTitle = new FLabel.Builder()
        .text("Pick Your Rogue Commander")
        .opaque(true)
        .fontSize(16)
        .build();

    // Commander card grid
    private final CommanderGridPanel pnlCommanderGrid = new CommanderGridPanel();
    private final List<CommanderCardPanel> commanderPanels = new ArrayList<>();
    private CardZoomUtil zoomUtil; // Lazily initialized on first use

    // Commander details
    private final FLabel lblCommanderName = new FLabel.Builder()
        .text("")
        .fontSize(14)
        .build();

    private final FTextArea txtDescription = new FTextArea("");
    private final FTextArea txtTheme = new FTextArea("");

    // Action buttons
    private final FLabel btnBeginRun = new FLabel.Builder()
        .opaque(true)
        .hoverable(true)
        .text("Start Run")
        .fontSize(16)
        .build();

    private VSubmenuRogueStart() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Setup description text areas
        txtDescription.setOpaque(true);
        txtDescription.setEditable(false);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setFocusable(false);
        txtDescription.setFont(FSkin.getFont());
        txtDescription.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        txtDescription.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        txtTheme.setOpaque(true);
        txtTheme.setEditable(false);
        txtTheme.setLineWrap(true);
        txtTheme.setWrapStyleWord(true);
        txtTheme.setFocusable(false);
        txtTheme.setFont(FSkin.getFont());
        txtTheme.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        txtTheme.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_ROGUESTART;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_ROGUESTART;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.ROGUE;
    }

    @Override
    public String getMenuTitle() {
        return "Start New Run";
    }

    @Override
    public CSubmenuRogueStart getLayoutControl() {
        return CSubmenuRogueStart.SINGLETON_INSTANCE;
    }

    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlCommanderGrid, "w 98%!, gap 1% 0 15px 0");

        // Add commander details panel
        JPanel pnlDetails = createDetailsPanel();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlDetails, "w 98%!, gap 1% 0 0 15px");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new MigLayout("insets 0, gap 0, wrap 2", "[150px][grow]", "[]10px[]10px[]20px[]"));

        // Row 1: Commander name
        panel.add(new FLabel.Builder().text("Commander:").build(), "cell 0 0, alignx left, aligny top");
        panel.add(lblCommanderName, "cell 1 0, growx");

        // Row 2: Description
        panel.add(new FLabel.Builder().text("Description:").build(), "cell 0 1, alignx left, aligny top");
        panel.add(new FScrollPane(txtDescription, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
            "cell 1 1, growx, h 60px!");

        // Row 3: Theme
        panel.add(new FLabel.Builder().text("Theme:").build(), "cell 0 2, alignx left, aligny top");
        panel.add(new FScrollPane(txtTheme, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
            "cell 1 2, growx, h 40px!");

        // Row 4: Begin button
        panel.add(btnBeginRun, "cell 0 3, span 2, alignx center, w 200px!, h 40px!");

        return panel;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    // Getters for controller access
    public FLabel getLblCommanderName() {
        return lblCommanderName;
    }

    public FTextArea getTxtDescription() {
        return txtDescription;
    }

    public FTextArea getTxtTheme() {
        return txtTheme;
    }

    public FLabel getBtnBeginRun() {
        return btnBeginRun;
    }

    public List<CommanderCardPanel> getCommanderPanels() {
        return commanderPanels;
    }

    public CommanderGridPanel getCommanderGridPanel() {
        return pnlCommanderGrid;
    }

    /**
     * Get the zoom utility, creating it lazily if needed.
     * This ensures the window hierarchy is ready when zoom is first used.
     */
    public CardZoomUtil getZoomUtil() {
        if (zoomUtil == null) {
            Window window = SwingUtilities.getWindowAncestor(pnlCommanderGrid);
            if (window != null) {
                zoomUtil = new CardZoomUtil(window);
                zoomUtil.setupZoomOverlay();
            }
        }
        return zoomUtil;
    }

    /**
     * Panel that displays commander cards in a grid (max 4 per row).
     */
    public class CommanderGridPanel extends FSkin.SkinnedPanel {
        public CommanderGridPanel() {
            super(null);
            setOpaque(false);
        }

        public void clear() {
            removeAll();
            commanderPanels.clear();
        }

        public void addCommanderPanel(CommanderCardPanel panel) {
            commanderPanels.add(panel);
            add(panel);
        }

        @Override
        public void doLayout() {
            if (commanderPanels.isEmpty()) {
                return;
            }

            int totalWidth = getWidth();
            int cardIndex = 0;
            int y = 15; // Top padding

            // Layout cards in rows of up to 4
            while (cardIndex < commanderPanels.size()) {
                // Calculate how many cards in this row
                int cardsInThisRow = Math.min(CARDS_PER_ROW, commanderPanels.size() - cardIndex);
                int rowWidth = cardsInThisRow * CARD_WIDTH + (cardsInThisRow - 1) * CARD_SPACING;
                int startX = (totalWidth - rowWidth) / 2;

                // Position cards in this row
                int x = startX;
                for (int col = 0; col < cardsInThisRow; col++) {
                    CommanderCardPanel panel = commanderPanels.get(cardIndex);
                    panel.setBounds(x, y, CARD_WIDTH, CARD_HEIGHT);
                    x += CARD_WIDTH + CARD_SPACING;
                    cardIndex++;
                }

                // Move to next row
                y += CARD_HEIGHT + CARD_SPACING;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            if (commanderPanels.isEmpty()) {
                return new Dimension(0, 0);
            }

            int numRows = (int) Math.ceil(commanderPanels.size() / (double) CARDS_PER_ROW);
            int height = numRows * (CARD_HEIGHT + CARD_SPACING) + 15; // Extra padding at top and bottom
            return new Dimension(800, height);
        }
    }
}
