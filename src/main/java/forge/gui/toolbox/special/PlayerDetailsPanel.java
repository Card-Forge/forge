package forge.gui.toolbox.special;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.Constant.Preferences;
import forge.card.MagicColor;
import forge.card.mana.ManaPool;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.ForgeAction;
import forge.gui.match.controllers.CPlayers;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinProp;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PlayerDetailsPanel extends JPanel {
    private static final long serialVersionUID = 8444559244193214459L;
    
    private Player player;
    
    // Info labels
    private FLabel lblHand = getBuiltFLabel(FSkin.ZoneImages.ICO_HAND, "99", "Cards in hand");
    private FLabel lblGraveyard = getBuiltFLabel(FSkin.ZoneImages.ICO_GRAVEYARD, "99", "Cards in graveyard");
    private FLabel lblLibrary = getBuiltFLabel(FSkin.ZoneImages.ICO_LIBRARY, "99", "Cards in library");
    private FLabel lblExile = getBuiltFLabel(FSkin.ZoneImages.ICO_EXILE, "99", "Exiled cards");
    private FLabel lblFlashback = getBuiltFLabel(FSkin.ZoneImages.ICO_FLASHBACK, "99", "Flashback cards");
    private FLabel lblPoison = getBuiltFLabel(FSkin.ZoneImages.ICO_POISON, "99", "Poison counters");
    private final List<Pair<FLabel, Byte>> manaLabels = new ArrayList<Pair<FLabel,Byte>>();

    private FLabel getBuiltFLabel(SkinProp p0, String s0, String s1) {
        return new FLabel.Builder().icon(new ImageIcon(FSkin.getImage(p0)))
            .opaque(false).fontSize(14)
            .fontStyle(Font.BOLD).iconInBackground()
            .text(s0).tooltip(s1).fontAlign(SwingConstants.RIGHT).build();
    }

    public PlayerDetailsPanel(Player player) {
        
        this.player = player;
        
        manaLabels.add(Pair.of(getBuiltFLabel(FSkin.ManaImages.IMG_BLACK, "99", "Black mana"), MagicColor.BLACK));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkin.ManaImages.IMG_BLUE, "99", "Blue mana"), MagicColor.BLUE));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkin.ManaImages.IMG_GREEN, "99", "Green mana"), MagicColor.GREEN));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkin.ManaImages.IMG_RED, "99", "Red mana"), MagicColor.RED));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkin.ManaImages.IMG_WHITE, "99", "White mana"), MagicColor.WHITE));
        manaLabels.add(Pair.of(getBuiltFLabel(FSkin.ManaImages.IMG_COLORLESS, "99", "Colorless mana"), (byte)0));

        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, wrap"));
        populateDetails();
        
        updateZones();
        updateManaPool();
        //updateDetails();
    }
    


    /** Adds various labels to pool area JPanel container. */
    private void populateDetails() {
        final JPanel row1 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row2 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row3 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row4 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row5 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row6 = new JPanel(new MigLayout("insets 0, gap 0"));

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
        this.getLblHand().setText("" + player.getZone(ZoneType.Hand).size());
        final String handMaxToolTip = player.isUnlimitedHandSize()
                ? "no maximum hand size" : String.valueOf(player.getMaxHandSize());
        this.getLblHand().setToolTipText("Cards in hand (max: " + handMaxToolTip + ")");
        this.getLblGraveyard().setText("" + player.getZone(ZoneType.Graveyard).size());
        this.getLblLibrary().setText("" + player.getZone(ZoneType.Library).size());
        this.getLblFlashback().setText("" + player.getCardsActivableInExternalZones().size());
        this.getLblExile().setText("" + player.getZone(ZoneType.Exile).size());
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
        Color poisonFg = player.getPoisonCounters() >= 8 ? Color.red : FSkin.getColor(FSkin.Colors.CLR_TEXT);
        this.getLblPoison().setForeground(poisonFg);
    }

    /**
     * Handles observer update of the mana pool.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     */
    public void updateManaPool() {
        ManaPool m = player.getManaPool();
        for(Pair<FLabel, Byte> label : manaLabels)
            label.getKey().setText(Integer.toString(m.getAmountOfColor(label.getRight())));
    }

    

    /** @return {@link javax.swing.JLabel} */
    public FLabel getLblHand() {
        return this.lblHand;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getLblLibrary() {
        return this.lblLibrary;
    }
    
    public final Iterable<Pair<FLabel, Byte>> getManaLabels() {
        return manaLabels;
    }

    /** @return  {@link javax.swing.JLabel} */
    public JLabel getLblGraveyard() {
        return this.lblGraveyard;
    }

    /** @return  {@link javax.swing.JLabel} */
    public JLabel getLblExile() {
        return this.lblExile;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblFlashback() {
        return this.lblFlashback;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblPoison() {
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
    
        if (Preferences.DEV_MODE) {
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
