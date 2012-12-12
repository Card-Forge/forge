package forge.gui.deckeditor;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import forge.Command;


import forge.card.CardRulesPredicates;
import forge.card.CardRules;
import forge.gui.deckeditor.views.ITableContainer;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.item.InventoryItem;
import forge.item.ItemPoolView;
import forge.util.Aggregates;


/** 
 * Static methods for working with top-level editor methods,
 * included but not limited to preferences IO, icon generation,
 * and stats analysis.
 *
 * <br><br>
 * <i>(S at beginning of class name denotes a static factory.)</i>
 *
 */
public final class SEditorUtil  {
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_ARTIFACT =
            new ImageIcon(FSkin.getImage(FSkin.EditorImages.IMG_ARTIFACT, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_CREATURE =
            new ImageIcon(FSkin.getImage(FSkin.EditorImages.IMG_CREATURE, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_ENCHANTMENT =
            new ImageIcon(FSkin.getImage(FSkin.EditorImages.IMG_ENCHANTMENT, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_INSTANT =
            new ImageIcon(FSkin.getImage(FSkin.EditorImages.IMG_INSTANT, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_LAND =
            new ImageIcon(FSkin.getImage(FSkin.EditorImages.IMG_LAND, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_PLANESWALKER =
            new ImageIcon(FSkin.getImage(FSkin.EditorImages.IMG_PLANESWALKER, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_SORCERY =
            new ImageIcon(FSkin.getImage(FSkin.EditorImages.IMG_SORCERY, 18, 18));

    /** Pre-cached resized version. */
    public static final ImageIcon ICO_TOTAL =
            new ImageIcon(FSkin.getImage(FSkin.ZoneImages.ICO_HAND, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_MULTI =
            new ImageIcon(FSkin.getImage(FSkin.EditorImages.IMG_MULTI, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_BLACK =
            new ImageIcon(FSkin.getImage(FSkin.ManaImages.IMG_BLACK, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_BLUE =
            new ImageIcon(FSkin.getImage(FSkin.ManaImages.IMG_BLUE, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_GREEN =
            new ImageIcon(FSkin.getImage(FSkin.ManaImages.IMG_GREEN, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_RED =
            new ImageIcon(FSkin.getImage(FSkin.ManaImages.IMG_RED, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_WHITE =
            new ImageIcon(FSkin.getImage(FSkin.ManaImages.IMG_WHITE, 18, 18));
    /** Pre-cached resized version. */
    public static final ImageIcon ICO_COLORLESS =
            new ImageIcon(FSkin.getImage(FSkin.ColorlessManaImages.IMG_X, 18, 18));

    /**
     * Divides X by Y, multiplies by 100, rounds, returns.
     * 
     * @param x0 &emsp; Numerator (int)
     * @param y0 &emsp; Denominator (int)
     * @return rounded result (int)
     */
    public static int calculatePercentage(final int x0, final int y0) {
        return (int) Math.round((double) x0 / (double) y0 * 100);
    }

    public static <T extends InventoryItem> void setLabelTextSum(JLabel label, final ItemPoolView<T> deck, Predicate<CardRules> predicate) {
        int sum = Aggregates.sum(Iterables.filter(deck, Predicates.compose(predicate, deck.getFnToCard())), deck.getFnToCount());
        label.setText(String.valueOf(sum));
    }

    /**
     * setStats.
     * 
     * @param <T> &emsp; the generic type
     * @param deck &emsp; ItemPoolView<InventoryITem>
     * @param view &emsp; {@link forge.gui.deckeditor.views.ITableContainer}
     */
    public static <T extends InventoryItem> void setStats(final ItemPoolView<T> deck, final ITableContainer view) {
        view.getLblTotal().setText(String.valueOf(deck.countAll()));

        setLabelTextSum(view.getLblCreature(), deck, CardRulesPredicates.Presets.IS_CREATURE);
        setLabelTextSum(view.getLblLand(), deck, CardRulesPredicates.Presets.IS_LAND);
        setLabelTextSum(view.getLblEnchantment(), deck, CardRulesPredicates.Presets.IS_ENCHANTMENT);
        setLabelTextSum(view.getLblArtifact(), deck, CardRulesPredicates.Presets.IS_ARTIFACT);
        setLabelTextSum(view.getLblInstant(), deck, CardRulesPredicates.Presets.IS_INSTANT);
        setLabelTextSum(view.getLblSorcery(), deck, CardRulesPredicates.Presets.IS_SORCERY);
        setLabelTextSum(view.getLblPlaneswalker(), deck, CardRulesPredicates.Presets.IS_PLANESWALKER);
        setLabelTextSum(view.getLblColorless(), deck, CardRulesPredicates.Presets.IS_COLORLESS);
        setLabelTextSum(view.getLblBlack(), deck, CardRulesPredicates.Presets.IS_BLACK);
        setLabelTextSum(view.getLblBlue(), deck, CardRulesPredicates.Presets.IS_BLUE);
        setLabelTextSum(view.getLblGreen(), deck, CardRulesPredicates.Presets.IS_GREEN);
        setLabelTextSum(view.getLblRed(), deck, CardRulesPredicates.Presets.IS_RED);
        setLabelTextSum(view.getLblWhite(), deck, CardRulesPredicates.Presets.IS_WHITE);
    } // getStats()

    /**
     * Resets components that may have been changed
     * by various configurations of the deck editor.
     */
    public static void resetUI() {
        VCardCatalog.SINGLETON_INSTANCE.getBtnAdd4().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setVisible(true);

        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(true);

        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setEnabled(true);
        VCardCatalog.SINGLETON_INSTANCE.getLblTitle().setText("");

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(true);
        VCardCatalog.SINGLETON_INSTANCE.getPnlHeader().setVisible(true);

        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText("Card Catalog");
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText("Current Deck");

        VCurrentDeck.SINGLETON_INSTANCE.getBtnDoSideboard().setVisible(false);

        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnSave())
            .setCommand(new Command() { @Override
                public void execute() { SEditorIO.saveDeck(); } });
    }
}
