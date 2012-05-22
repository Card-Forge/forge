package forge.gui.deckeditor;

import javax.swing.ImageIcon;

import forge.card.CardRules;
import forge.gui.deckeditor.views.ITableContainer;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.toolbox.FSkin;
import forge.item.InventoryItem;
import forge.item.ItemPoolView;

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

    /**
     * setStats.
     * 
     * @param <T> &emsp; the generic type
     * @param deck &emsp; ItemPoolView<InventoryITem>
     * @param view &emsp; {@link forge.gui.deckeditor.views.ITableContainer}
     */
    public static <T extends InventoryItem> void setStats(final ItemPoolView<T> deck, final ITableContainer view) {
        view.getLblTotal().setText(String.valueOf(deck.countAll()));

        view.getLblCreature().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_CREATURE.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblLand().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_LAND.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblEnchantment().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_ENCHANTMENT.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblArtifact().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_ARTIFACT.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblInstant().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_INSTANT.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblSorcery().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_SORCERY.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblPlaneswalker().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_PLANESWALKER.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblColorless().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_COLORLESS.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblBlack().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_BLACK.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblBlue().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_BLUE.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblGreen().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_GREEN.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblRed().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_RED.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));

        view.getLblWhite().setText(String.valueOf(CardRules.Predicates.Presets
                .IS_WHITE.aggregate(deck, deck.getFnToCard(), deck.getFnToCount())));
    } // getStats()

    /**
     * Set all components visible that may have been hidden
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
    }
}
