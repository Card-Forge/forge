package forge.adventure.scene;

import forge.adventure.stage.GameHUD;
import forge.screens.FScreen;

/**
 * Adventure scene containing the persistent Shop Catalog.
 */
public final class ShopCatalogScene extends ForgeScene {
    private static ShopCatalogScene instance;
    private ShopCatalogScreen screen;

    public static ShopCatalogScene instance() {
        if (instance == null) {
            instance = new ShopCatalogScene();
        }
        return instance;
    }

    private ShopCatalogScene() {
    }

    @Override
    public void enter() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        ((ShopCatalogScreen) getScreen()).refresh();
        super.enter();
    }

    @Override
    public FScreen getScreen() {
        if (screen == null) {
            screen = new ShopCatalogScreen();
        }
        return screen;
    }
}
