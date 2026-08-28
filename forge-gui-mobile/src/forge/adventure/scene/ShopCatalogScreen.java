package forge.adventure.scene;

import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.shop.AdventureShopPrice;
import forge.adventure.shop.ShopCatalogOffer;
import forge.adventure.shop.ShopCatalogPurchase;
import forge.adventure.util.Config;
import forge.adventure.util.KeyBinding;
import forge.adventure.util.Paths;
import forge.adventure.util.Reward;
import forge.adventure.world.WorldSave;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.gui.GuiBase;
import forge.haptic.HapticEngine;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.ItemPool;
import forge.util.Utils;

/**
 * Searchable view of individual card offers remembered from Adventure shops.
 */
public final class ShopCatalogScreen extends FScreen {
    private static final float PADDING = Utils.scale(4);
    private static final float INVENTORY_BUTTON_WIDTH = 60;
    private static final float INVENTORY_BUTTON_HEIGHT = 30;
    private static final float INVENTORY_BUTTON_FONT_SIZE = 11;

    private float buttonWidth = Utils.scale(INVENTORY_BUTTON_WIDTH);
    private float buttonHeight = Utils.scale(INVENTORY_BUTTON_HEIGHT);
    private FSkinFont buttonFont = FSkinFont.get((int) INVENTORY_BUTTON_FONT_SIZE);

    private final CardManager cardManager = add(new CardManager(false));
    private final FLabel gold = add(new FLabel.Builder().font(buttonFont)
            .icon(FSkinImage.QUEST_COINSTACK).iconScaleFactor(0.75f).build());
    private final FLabel owned = add(new FLabel.Builder().font(buttonFont).align(Align.center).build());
    private final FLabel price = add(new FLabel.Builder().font(buttonFont).align(Align.center).build());
    private final FButton buyButton = add(new FButton(Forge.getLocalizer().getMessage("lblBuy"),
            event -> buySelectedOffer()));
    private final BackButton bottomBackButton = add(new BackButton());
    private ShopCatalogOffer selectedOffer;
    private boolean refreshing;

    ShopCatalogScreen() {
        super(Forge.getLocalizer().getMessage("lblShopCatalog"));
        cardManager.setup(ItemManagerConfig.ADVENTURE_SHOP_CATALOG);
        cardManager.setBtnAdvancedSearchOptions(true);
        cardManager.setCaption(Forge.getLocalizer().getMessage("lblCatalog"));
        cardManager.setSelectionChangedHandler(event -> refreshSelection());
        cardManager.setItemActivateHandler(event -> refreshSelection());
        buyButton.setEnabled(false);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        refresh();
    }

    public void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        PaperCard selected = cardManager.getSelectedItem();
        ItemPool<PaperCard> cards = new ItemPool<>(PaperCard.class);
        for (ShopCatalogOffer offer : WorldSave.getCurrentSave().getShopCatalog().getAvailableOffers()) {
            cards.add(offer.getCard());
        }
        cardManager.setPool(cards);
        if (selected == null || !cardManager.setSelectedItem(selected)) {
            cardManager.setSelectedIndex(0);
        }
        refreshing = false;
        refreshSelection();
    }

    private void refreshSelection() {
        if (refreshing) {
            return;
        }
        PaperCard selected = cardManager.getSelectedItem();
        selectedOffer = null;
        int selectedPrice = Integer.MAX_VALUE;
        int availableCount = 0;
        boolean unlimited = false;
        if (selected != null) {
            for (ShopCatalogOffer offer : WorldSave.getCurrentSave().getShopCatalog().getAvailableOffers()) {
                if (selected.equals(offer.getCard())) {
                    availableCount++;
                    unlimited |= offer.isUnlimited();
                    int offerPrice = getPrice(offer);
                    if (offerPrice < selectedPrice) {
                        selectedOffer = offer;
                        selectedPrice = offerPrice;
                    }
                }
            }
        }

        int goldAmount = AdventurePlayer.current().getGold();
        int ownedCount = selected == null ? 0 : AdventurePlayer.current().getCollectionCards(true).count(selected);
        gold.setText(Integer.toString(goldAmount));
        owned.setText(Forge.getLocalizer().getMessage("lblOwned") + ": " + ownedCount + "\n"
                + Forge.getLocalizer().getMessage("lblAvailable") + ": "
                + (unlimited ? Forge.getLocalizer().getMessage("lblUnlimited") : availableCount));
        price.setText(Forge.getLocalizer().getMessage("lblPrice") + ": "
                + (selectedOffer == null ? "-" : selectedPrice));
        buyButton.setEnabled(selectedOffer != null && goldAmount >= selectedPrice);
    }

    private int getPrice(ShopCatalogOffer offer) {
        PointOfInterestChanges changes = WorldSave.getCurrentSave().getPointOfInterestChanges(
                offer.getId().getPointOfInterestChangesKey());
        return AdventureShopPrice.calculate(new Reward(offer.getCard()), changes,
                offer.getId().getShopObjectId());
    }

    private void buySelectedOffer() {
        if (selectedOffer != null) {
            buy(selectedOffer);
        }
    }

    private void buy(ShopCatalogOffer offer) {
        ShopCatalogPurchase.Result result = ShopCatalogPurchase.purchase(offer.getId());
        if (result.getStatus() == ShopCatalogPurchase.Status.InsufficientGold) {
            FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblNotEnoughGold"));
            refreshSelection();
            return;
        }
        if (result.getStatus() == ShopCatalogPurchase.Status.Unavailable) {
            FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblShopOfferUnavailable"));
            refresh();
            return;
        }
        HapticEngine.vibrate(FPref.UI_VIBRATE_ON_SHOP_ACTION, 5);
        SoundSystem.instance.play(SoundEffectType.FlipCoin, false);
        refresh();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float adventureScaleX = width / Scene.getIntendedWidth();
        float adventureScaleY = height / Scene.getIntendedHeight();
        buttonWidth = INVENTORY_BUTTON_WIDTH * adventureScaleX;
        buttonHeight = INVENTORY_BUTTON_HEIGHT * adventureScaleY;
        buttonFont = FSkinFont._get(Math.round(INVENTORY_BUTTON_FONT_SIZE * adventureScaleY));
        gold.setFont(buttonFont);
        owned.setFont(buttonFont);
        price.setFont(buttonFont);
        buyButton.setFont(buttonFont);
        bottomBackButton.setFont(buttonFont);

        float footerHeight = buttonHeight + 2 * PADDING;
        cardManager.setBounds(0, startY, width, height - startY - footerHeight);

        float footerY = height - buttonHeight - PADDING;
        bottomBackButton.setBounds(width - buttonWidth - PADDING,
                footerY, buttonWidth, buttonHeight);
        buyButton.setBounds(width - 2 * buttonWidth - 2 * PADDING,
                footerY, buttonWidth, buttonHeight);

        float detailsWidth = width - 2 * buttonWidth - 4 * PADDING;
        float detailWidth = detailsWidth / 3;
        gold.setBounds(PADDING, footerY, detailWidth, buttonHeight);
        owned.setBounds(PADDING + detailWidth, footerY, detailWidth, buttonHeight);
        price.setBounds(PADDING + 2 * detailWidth, footerY, detailWidth, buttonHeight);
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null;
    }

    private static final class BackButton extends FButton {
        private final String label = Forge.getLocalizer().getMessage("lblBack");
        private final TextureAtlas keyAtlas = Config.instance().getAtlas(Paths.KEYS_ATLAS);

        private BackButton() {
            super("", event -> Forge.back());
        }

        @Override
        public void draw(Graphics g) {
            super.draw(g);
            TextureAtlas.AtlasRegion hotkey = getHotkeyRegion();
            float hotkeySize = getHeight() * 0.6f;
            float hotkeyInset = getHeight() / 6;
            float textWidth = getWidth();
            if (hotkey != null) {
                float iconWidth = hotkeySize * hotkey.getRegionWidth() / hotkey.getRegionHeight();
                float iconX = getWidth() - iconWidth - hotkeyInset;
                g.drawImage(hotkey, iconX, (getHeight() - hotkeySize) / 2, iconWidth, hotkeySize);
                textWidth = iconX;
            }
            g.drawText(label, getFont(), FLabel.getDefaultTextColor(),
                    hotkeyInset, 0, textWidth - hotkeyInset, getHeight(), false, Align.center, true);
        }

        private TextureAtlas.AtlasRegion getHotkeyRegion() {
            if (Forge.isPortraitMode || (Controllers.getCurrent() == null && GuiBase.isAndroid())) {
                return null;
            }
            String keyName = Controllers.getCurrent() == null ? "Escape" : "XBox_B";
            if (KeyBinding.Back.isPressed()) {
                keyName += "_pressed";
            }
            return keyAtlas.findRegion(keyName);
        }
    }
}
