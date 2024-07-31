package forge.screens.workshop.views;

import javax.swing.JPanel;

import com.google.common.collect.Iterables;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerContainer;
import forge.model.FModel;
import forge.screens.match.controllers.CDetailPicture;
import forge.screens.workshop.controllers.CCardScript;
import forge.screens.workshop.controllers.CWorkshopCatalog;
import forge.util.ItemPool;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of card catalog in workshop.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VWorkshopCatalog implements IVDoc<CWorkshopCatalog> {
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();
    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblCardCatalog"));
    private final ItemManagerContainer cardManagerContainer = new ItemManagerContainer();
    private final CardManager cardManager;
    private final CDetailPicture cDetailPicture = new CDetailPicture();

    //========== Constructor
    VWorkshopCatalog() {
        this.cardManager = new CardManager(cDetailPicture, true, false, false);
        this.cardManager.setCaption(localizer.getMessage("lblCatalog"));
        final Iterable<PaperCard> allCards = Iterables.concat(FModel.getMagicDb().getCommonCards().getAllCardsNoAlt(), FModel.getMagicDb().getVariantCards().getAllCards());
        this.cardManager.setPool(ItemPool.createFrom(allCards, PaperCard.class), true);
        this.cardManagerContainer.setItemManager(this.cardManager);

        this.cardManager.addSelectionListener(e -> {
            final PaperCard card = cardManager.getSelectedItem();
            cDetailPicture.showItem(card);
            CCardScript.SINGLETON_INSTANCE.showCard(card);
        });
    }

    //========== Overridden from IVDoc

    @Override
    public EDocID getDocumentID() {
        return EDocID.WORKSHOP_CATALOG;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CWorkshopCatalog getLayoutControl() {
        return CWorkshopCatalog.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    @Override
    public void populate() {
        final JPanel parentBody = parentCell.getBody();
        parentBody.setLayout(new MigLayout("insets 5, gap 0, wrap, hidemode 3"));
        parentBody.add(cardManagerContainer, "push, grow");
    }

    public CardManager getCardManager() {
        return cardManager;
    }

    public CDetailPicture getCDetailPicture() {
        return cDetailPicture;
    }
}
