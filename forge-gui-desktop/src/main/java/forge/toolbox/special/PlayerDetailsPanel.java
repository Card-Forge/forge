package forge.toolbox.special;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.assets.FSkinProp;
import forge.card.MagicColor;
import forge.gui.ForgeAction;
import forge.properties.ForgePreferences;
import forge.screens.match.controllers.CPlayers;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.view.PlayerView;


public class PlayerDetailsPanel extends JPanel {
    private static final long serialVersionUID = 8444559244193214459L;
    
    private PlayerView player;
    
    // Info labels
    private FLabel lblHand = getBuiltFLabel(FSkinProp.IMG_ZONE_HAND, "99", "Cards in hand");
    private FLabel lblGraveyard = getBuiltFLabel(FSkinProp.IMG_ZONE_GRAVEYARD, "99", "Cards in graveyard");
    private FLabel lblLibrary = getBuiltFLabel(FSkinProp.IMG_ZONE_LIBRARY, "99", "Cards in library");
    private FLabel lblExile = getBuiltFLabel(FSkinProp.IMG_ZONE_EXILE, "99", "Exiled cards");
    private FLabel lblFlashback = getBuiltFLabel(FSkinProp.IMG_ZONE_FLASHBACK, "99", "Flashback cards");
    private FLabel lblPoison = getBuiltFLabel(FSkinProp.IMG_ZONE_POISON, "99", "Poison counters");
    private final List<Pair<FLabel, Byte>> manaLabels = new ArrayList<Pair<FLabel,Byte>>();

    private FLabel getBuiltFLabel(FSkinProp p0, String s0, String s1) {
        return new FLabel.Builder().icon(FSkin.getImage(p0))
            .opaque(false).fontSize(14)
            .fontStyle(Font.BOLD).iconInBackground()
            .text(s0).tooltip(s1).fontAlign(SwingConstants.RIGHT).build();
    }

    public PlayerDetailsPanel(final PlayerView player) {
        this.player = player;
        
        manaLabels.add(Pair.of(getBuiltFLabel(FSkinProp.IMG_MANA_B, "99", "Black mana"), MagicColor.BLACK));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkinProp.IMG_MANA_U, "99", "Blue mana"), MagicColor.BLUE));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkinProp.IMG_MANA_G, "99", "Green mana"), MagicColor.GREEN));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkinProp.IMG_MANA_R, "99", "Red mana"), MagicColor.RED));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkinProp.IMG_MANA_W, "99", "White mana"), MagicColor.WHITE));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkinProp.IMG_MANA_COLORLESS, "99", "Colorless mana"), (byte)0));

        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, wrap"));
        populateDetails();
        
        updateZones();
        updateManaPool();
        //updateDetails();
    }
    


    /** Adds various labels to pool area JPanel container. */
    private void populateDetails() {
        final SkinnedPanel row1 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row2 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row3 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row4 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row5 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row6 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));

        row1.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row2.setOpaque(false);
        row3.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row4.setOpaque(false);
        row5.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row6.setOpaque(false);

        // Hand, library, graveyard, exile, flashback, poison labels
        final String constraintsCell = "w 45%!, h 100%!, gap 0 5% 2px 2px";

        row1.add(lblHand, constraintsCell);
        row1.add(lblLibrary, constraintsCell);

        row2.add(lblGraveyard, constraintsCell);
        row2.add(lblExile, constraintsCell);

        row3.add(lblFlashback, constraintsCell);
        row3.add(lblPoison, constraintsCell);

        row4.add(manaLabels.get(0).getLeft(), constraintsCell);
        row4.add(manaLabels.get(1).getLeft(), constraintsCell);

        row5.add(manaLabels.get(2).getLeft(), constraintsCell);
        row5.add(manaLabels.get(3).getLeft(), constraintsCell);

        row6.add(manaLabels.get(4).getLeft(), constraintsCell);
        row6.add(manaLabels.get(5).getLeft(), constraintsCell);

        final String constraintsRow = "w 100%!, h 16%!";
        this.add(row1, constraintsRow + ", gap 0 0 4% 0");
        this.add(row2, constraintsRow);
        this.add(row3, constraintsRow);
        this.add(row4, constraintsRow);
        this.add(row5, constraintsRow);
        this.add(row6, constraintsRow);
    }
    
    /**
     * Handles observer update of player Zones - hand, graveyard, etc.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     */
    public void updateZones() {
        this.getLblHand().setText("" + player.getHandCards().size());
        final String handMaxToolTip = player.hasUnlimitedHandSize()
                ? "no maximum hand size" : String.valueOf(player.getMaxHandSize());
        this.getLblHand().setToolTipText("Cards in hand (max: " + handMaxToolTip + ")");
        this.getLblGraveyard().setText("" + player.getGraveCards().size());
        this.getLblLibrary().setText("" + player.getLibraryCards().size());
        this.getLblFlashback().setText("" + player.getFlashbackCards().size());
        this.getLblExile().setText("" + player.getExileCards().size());
    }

    
    /**
     * Handles observer update of non-Zone details - life, poison, etc. Also
     * updates "players" panel in tabber for this player.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     */
    public void updateDetails() {
        // "Players" panel update
        CPlayers.SINGLETON_INSTANCE.update();

        // Poison/life
        this.getLblPoison().setText("" + player.getPoisonCounters());
        if (player.getPoisonCounters() < 8) {
            this.getLblPoison().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
        else {
            this.getLblPoison().setForeground(Color.red);
        }
    }

    /**
     * Handles observer update of the mana pool.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     */
    public void updateManaPool() {
        for (final Pair<FLabel, Byte> label : manaLabels)
            label.getKey().setText(Integer.toString(player.getMana(label.getRight())));
    }

    public FLabel getLblHand() {
        return this.lblHand;
    }

    public FLabel getLblLibrary() {
        return this.lblLibrary;
    }
    
    public final Iterable<Pair<FLabel, Byte>> getManaLabels() {
        return manaLabels;
    }

    public FLabel getLblGraveyard() {
        return this.lblGraveyard;
    }

    public FLabel getLblExile() {
        return this.lblExile;
    }

    public FLabel getLblFlashback() {
        return this.lblFlashback;
    }

    public FLabel getLblPoison() {
        return this.lblPoison;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param handAction
     * @param libraryAction
     * @param exileAction
     * @param graveAction
     * @param flashBackAction
     * @param manaAction 
     */
    public void setupMouseActions(final ForgeAction handAction, final ForgeAction libraryAction, final ForgeAction exileAction,
                                  final ForgeAction graveAction, final ForgeAction flashBackAction, final Function<Byte, Void> manaAction) {

        // Detail label listeners
        lblGraveyard.setHoverable(true);
        lblGraveyard.addMouseListener(new MouseAdapter() { @Override public void mousePressed(final MouseEvent e) { graveAction.actionPerformed(null); } } );
    
        lblExile.setHoverable(true);
        lblExile.addMouseListener(new MouseAdapter() { @Override public void mousePressed(final MouseEvent e) { exileAction.actionPerformed(null); } } );
    
        if (ForgePreferences.DEV_MODE) {
            lblLibrary.setHoverable(true);
            lblLibrary.addMouseListener(new MouseAdapter() { @Override public void mousePressed(final MouseEvent e) { libraryAction.actionPerformed(null); } } );
        }
    
        lblHand.setHoverable(true);
        lblHand.addMouseListener(new MouseAdapter() { @Override public void mousePressed(final MouseEvent e) { handAction.actionPerformed(null); } } );
    
        lblFlashback.setHoverable(true);
        lblFlashback.addMouseListener(new MouseAdapter() { @Override public void mousePressed(final MouseEvent e) {flashBackAction.actionPerformed(null); } } );
    
        for(final Pair<FLabel, Byte> labelPair : getManaLabels()) {
            labelPair.getLeft().setHoverable(true);
            labelPair.getLeft().addMouseListener(new MouseAdapter() { @Override
                public void mousePressed(final MouseEvent e) {
                manaAction.apply(labelPair.getRight()); } }
            );
        }
        
    }
}
