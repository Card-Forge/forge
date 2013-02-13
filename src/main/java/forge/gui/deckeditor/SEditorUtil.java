package forge.gui.deckeditor;

import javax.swing.ImageIcon;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.Command;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.gui.deckeditor.views.ITableContainer;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPoolView;
import forge.util.Aggregates;
import forge.util.TextUtil;


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
    /** An enum to encapsulate metadata for the stats/filter objects. */
    public static enum StatTypes {
        TOTAL      (FSkin.ZoneImages.ICO_HAND,      null),
        WHITE      (FSkin.ManaImages.IMG_WHITE,     CardRulesPredicates.Presets.IS_WHITE),
        BLUE       (FSkin.ManaImages.IMG_BLUE,      CardRulesPredicates.Presets.IS_BLUE),
        BLACK      (FSkin.ManaImages.IMG_BLACK,     CardRulesPredicates.Presets.IS_BLACK),
        RED        (FSkin.ManaImages.IMG_RED,       CardRulesPredicates.Presets.IS_RED),
        GREEN      (FSkin.ManaImages.IMG_GREEN,     CardRulesPredicates.Presets.IS_GREEN),
        COLORLESS  (FSkin.ManaImages.IMG_COLORLESS, CardRulesPredicates.Presets.IS_COLORLESS),
        MULTICOLOR (FSkin.EditorImages.IMG_MULTI,   CardRulesPredicates.Presets.IS_MULTICOLOR),

        PACK         (FSkin.EditorImages.IMG_PACK,         null),
        LAND         (FSkin.EditorImages.IMG_LAND,         CardRulesPredicates.Presets.IS_LAND),
        ARTIFACT     (FSkin.EditorImages.IMG_ARTIFACT,     CardRulesPredicates.Presets.IS_ARTIFACT),
        CREATURE     (FSkin.EditorImages.IMG_CREATURE,     CardRulesPredicates.Presets.IS_CREATURE),
        ENCHANTMENT  (FSkin.EditorImages.IMG_ENCHANTMENT,  CardRulesPredicates.Presets.IS_ENCHANTMENT),
        PLANESWALKER (FSkin.EditorImages.IMG_PLANESWALKER, CardRulesPredicates.Presets.IS_PLANESWALKER),
        INSTANT      (FSkin.EditorImages.IMG_INSTANT,      CardRulesPredicates.Presets.IS_INSTANT),
        SORCERY      (FSkin.EditorImages.IMG_SORCERY,      CardRulesPredicates.Presets.IS_SORCERY);

        public final ImageIcon img;
        public final Predicate<CardRules> predicate;

        StatTypes(FSkin.SkinProp prop, Predicate<CardRules> pred) {
            img = new ImageIcon(FSkin.getImage(prop, 18, 18));
            predicate = pred;
        }

        public String toLabelString() {
            if (this == PACK) {
                return "Card packs and prebuilt decks";
            }
            return TextUtil.enumToLabel(this) + " cards";
        }
    }

    /**
     * Divides X by Y, multiplies by 100, rounds, returns.
     * 
     * @param x0 &emsp; Numerator (int)
     * @param y0 &emsp; Denominator (int)
     * @return rounded result (int)
     */
    public static int calculatePercentage(final int x0, final int y0) {
        return (int) Math.round((double) (x0 * 100) / (double) y0);
    }

    private static final Predicate<Object> totalPred = Predicates.instanceOf(CardPrinted.class);
    private static final Predicate<Object> packPred  = Predicates.not(totalPred);
    
    /**
     * setStats.
     * 
     * @param <T> &emsp; the generic type
     * @param items &emsp; ItemPoolView<InventoryITem>
     * @param view &emsp; {@link forge.gui.deckeditor.views.ITableContainer}
     */
    public static <T extends InventoryItem> void setStats(final ItemPoolView<T> items, final ITableContainer view) {
        for (StatTypes s : StatTypes.values()) {
            switch (s) {
            case TOTAL:
                view.getStatLabel(s).setText(String.valueOf(
                        Aggregates.sum(Iterables.filter(items, Predicates.compose(totalPred, items.getFnToPrinted())), items.getFnToCount())));
                break;
            case PACK:
                view.getStatLabel(s).setText(String.valueOf(
                        Aggregates.sum(Iterables.filter(items, Predicates.compose(packPred, items.getFnToPrinted())), items.getFnToCount())));
                break;
            default:
                view.getStatLabel(s).setText(String.valueOf(
                        Aggregates.sum(Iterables.filter(items, Predicates.compose(s.predicate, items.getFnToCard())), items.getFnToCount())));
            }
        }
    }

    /**
     * Resets components that may have been changed
     * by various configurations of the deck editor.
     */
    @SuppressWarnings("serial")
    public static void resetUI() {
        VCardCatalog.SINGLETON_INSTANCE.getBtnAdd4().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4().setVisible(true);

        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(true);

        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setEnabled(true);
        
        VCardCatalog.SINGLETON_INSTANCE.getPnlHeader().setVisible(false);
        VCardCatalog.SINGLETON_INSTANCE.getLblTitle().setText("");

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(true);

        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText("Card Catalog");

        VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnDoSideboard().setVisible(false);

        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getLblTitle().setText("Title:");

        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnSave())
            .setCommand(new Command() {
                @Override public void execute() { SEditorIO.saveDeck(); } });
    }
}
