package forge.toolbox.special;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.card.mana.ManaAtom;
import forge.game.player.PlayerView;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinFont;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.trackable.TrackableProperty;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

public class PlayerDetailsPanel extends JPanel {
    private static final long serialVersionUID = -6531759554646891983L;

    private final PlayerView player;

    // Info labels
    private final DetailLabel lblHand = new DetailLabel(FSkinProp.IMG_ZONE_HAND, Localizer.getInstance().getMessage("lblHandNOfMax", "%s", "%s"));
    private final DetailLabel lblGraveyard = new DetailLabel(FSkinProp.IMG_ZONE_GRAVEYARD, Localizer.getInstance().getMessage("lblGraveyardNCardsNTypes", "%s", "%s"));
    private final DetailLabel lblLibrary = new DetailLabel(FSkinProp.IMG_ZONE_LIBRARY, Localizer.getInstance().getMessage("lblLibraryNCards", "%s"));
    private final DetailLabel lblExile = new DetailLabel(FSkinProp.IMG_ZONE_EXILE, Localizer.getInstance().getMessage("lblExileNCards", "%s"));
    private final DetailLabel lblFlashback = new DetailLabel(FSkinProp.IMG_ZONE_FLASHBACK, Localizer.getInstance().getMessage("lblFlashbackNCards", "%s"));
    private final DetailLabel lblCommand = new DetailLabel(FSkinProp.IMG_PLANESWALKER, Localizer.getInstance().getMessage("lblCommandZoneNCards", "%s"));
    private final DetailLabel lblAnte = new DetailLabel(FSkinProp.IMG_ZONE_ANTE, Localizer.getInstance().getMessage("lblAnteZoneNCards", "%s"));
    private final DetailLabel lblSideboard = new DetailLabel(FSkinProp.IMG_ZONE_SIDEBOARD, Localizer.getInstance().getMessage("lblSideboardNCards", "%s"));
    private final List<Pair<DetailLabel, Byte>> manaLabels = new ArrayList<>();

    public PlayerDetailsPanel(final PlayerView player0) {
        player = player0;

        manaLabels.add(Pair.of(new DetailLabel(FSkinProp.IMG_MANA_W,         Localizer.getInstance().getMessage("lblWhiteManaOfN", "%s")),     (byte)ManaAtom.WHITE));
        manaLabels.add(Pair.of(new DetailLabel(FSkinProp.IMG_MANA_U,         Localizer.getInstance().getMessage("lblBlueManaOfN", "%s")),      (byte)ManaAtom.BLUE));
        manaLabels.add(Pair.of(new DetailLabel(FSkinProp.IMG_MANA_B,         Localizer.getInstance().getMessage("lblBlackManaOfN", "%s")),     (byte)ManaAtom.BLACK));
        manaLabels.add(Pair.of(new DetailLabel(FSkinProp.IMG_MANA_R,         Localizer.getInstance().getMessage("lblRedManaOfN", "%s")),       (byte)ManaAtom.RED));
        manaLabels.add(Pair.of(new DetailLabel(FSkinProp.IMG_MANA_G,         Localizer.getInstance().getMessage("lblGreenManaOfN", "%s")),     (byte)ManaAtom.GREEN));
        manaLabels.add(Pair.of(new DetailLabel(FSkinProp.IMG_MANA_COLORLESS, Localizer.getInstance().getMessage("lblColorlessManaOfN", "%s")), (byte)ManaAtom.COLORLESS));

        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, wrap"));
        populateDetails();

        updateZones();
        updateManaPool();
    }

    /** Adds various labels to pool area JPanel container. */
    private void populateDetails() {
        final SkinnedPanel row1 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row2 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row3 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row4 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row5 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row6 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));
        final SkinnedPanel row7 = new SkinnedPanel(new MigLayout("insets 0, gap 0"));

        row1.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row2.setOpaque(false);
        row3.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row4.setOpaque(false);
        row5.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row6.setOpaque(false);
        row7.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));

        // Hand, library, graveyard, exile, flashback, command
        final String constraintsCell = "w 50%-4px!, h 100%!, gapleft 2px, gapright 2px";

        row1.add(lblHand, constraintsCell);
        row1.add(lblLibrary, constraintsCell);

        row2.add(lblGraveyard, constraintsCell);
        row2.add(lblExile, constraintsCell);

        row3.add(lblFlashback, constraintsCell);
        row3.add(lblCommand, constraintsCell);

        row4.add(lblAnte, constraintsCell);
        row4.add(lblSideboard, constraintsCell);

        row5.add(manaLabels.get(0).getLeft(), constraintsCell);
        row5.add(manaLabels.get(1).getLeft(), constraintsCell);

        row6.add(manaLabels.get(2).getLeft(), constraintsCell);
        row6.add(manaLabels.get(3).getLeft(), constraintsCell);

        row7.add(manaLabels.get(4).getLeft(), constraintsCell);
        row7.add(manaLabels.get(5).getLeft(), constraintsCell);

        final String constraintsRow = "w 100%!, h 14%!";
        add(row1, constraintsRow + ", gap 0 0 2% 0");
        add(row2, constraintsRow);
        add(row3, constraintsRow);
        add(row4, constraintsRow);
        add(row5, constraintsRow);
        add(row6, constraintsRow);
        add(row7, constraintsRow);
    }

    public Component getLblLibrary() {
        return lblLibrary;
    }

    /**
     * Handles observer update of player Zones - hand, graveyard, etc.
     */
    public void updateZones() {
        final String handSize = String.valueOf(player.getHandSize()),
                graveyardSize = String.valueOf(player.getGraveyardSize()),
                deliriumCount = String.valueOf(player.getZoneTypes(TrackableProperty.Graveyard)),
                librarySize   = String.valueOf(player.getLibrarySize()),
                flashbackSize = String.valueOf(player.getFlashbackSize()),
                exileSize     = String.valueOf(player.getExileSize()),
                commandSize   = String.valueOf(player.getCommandSize()),
                anteSize      = String.valueOf(player.getAnteSize()),
                sideboardSize = String.valueOf(player.getSideboardSize());

        lblHand.setText(handSize);
        lblHand.setToolTip(handSize, player.getMaxHandString());
        lblGraveyard.setText(graveyardSize);
        lblGraveyard.setToolTip(graveyardSize, deliriumCount);
        lblLibrary.setText(librarySize);
        lblLibrary.setToolTip(librarySize);
        lblFlashback.setText(flashbackSize);
        lblFlashback.setToolTip(flashbackSize);
        lblExile.setText(exileSize);
        lblExile.setToolTip(exileSize);
        lblCommand.setText(commandSize);
        lblCommand.setToolTip(commandSize);
        lblAnte.setText(anteSize);
        lblAnte.setToolTip(anteSize);
        lblSideboard.setText(sideboardSize);
        lblSideboard.setToolTip(sideboardSize);
    }

    /**
     * Handles observer update of the mana pool.
     */
    public void updateManaPool() {
        for (final Pair<DetailLabel, Byte> label : manaLabels) {
            final String mana = String.valueOf(player.getMana(label.getRight()));
            label.getKey().setText(mana);
            label.getKey().setToolTip(mana);
        }
    }

    public void setupMouseActions(final Runnable handAction, final Runnable libraryAction, final Runnable exileAction,
                                  final Runnable graveAction, final Runnable flashBackAction, final Runnable commandAction, final Runnable anteAction, final Runnable sideboardAction,
                                  final Function<Byte, Boolean> manaAction) {
        // Detail label listeners
        lblGraveyard.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                graveAction.run();
            }
        });
        lblExile.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                exileAction.run();
            }
        });
        lblLibrary.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                libraryAction.run();
            }
        });
        lblHand.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                handAction.run();
            }
        });
        lblFlashback.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                flashBackAction.run();
            }
        });
        lblCommand.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                commandAction.run();
            }
        });
        lblAnte.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                anteAction.run();
            }
        });
        lblSideboard.addMouseListener(new FMouseAdapter() {
            @Override public void onLeftClick(final MouseEvent e) {
                sideboardAction.run();
            }
        });

        for (final Pair<DetailLabel, Byte> labelPair : manaLabels) {
            labelPair.getLeft().addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftClick(final MouseEvent e) {
                    //if shift key down, keep using mana until it runs out or no longer can be put towards the cost
                    final Byte mana = labelPair.getRight();
                    while (manaAction.apply(mana) && e.isShiftDown()) {}
                }
            });
        }
    }

    @SuppressWarnings("serial")
    private class DetailLabel extends FLabel {
        private final String tooltip;
        private DetailLabel(final FSkinProp icon, final String tooltip) {
            super(new FLabel.Builder().icon(FSkin.getImage(icon))
                    .opaque(false).fontSize(14).hoverable()
                    .fontStyle(Font.BOLD).iconInBackground()
                    .fontAlign(SwingConstants.RIGHT));

            this.tooltip = tooltip;
            setFocusable(false);
        }

        @Override
        public void setText(final String text0) {
            super.setText(text0);
            autoSizeFont();
        }

        public void setToolTip(final String... args) {
            super.setToolTipText(String.format(tooltip, args));
        }

        @Override
        protected void resetIcon() {
            super.resetIcon();
            autoSizeFont();
        }

        private void autoSizeFont() {
            final String text = getText();
            if (StringUtils.isEmpty(text)) { return; }

            final Graphics g = getGraphics();
            if (g == null) { return; }

            final int max = getMaxTextWidth();

            SkinFont font = null;
            for (int fontSize = 14; fontSize > 5; fontSize--) {
                font = FSkin.getBoldFont(fontSize);
                if (font.measureTextWidth(g, text) <= max) {
                    break;
                }
            }
            if (font != null) {
                setFont(font);
            }
        }
    }
}
