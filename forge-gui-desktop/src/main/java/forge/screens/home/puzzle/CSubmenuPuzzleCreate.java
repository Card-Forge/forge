package forge.screens.home.puzzle;

import forge.gui.framework.ICDoc;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public enum CSubmenuPuzzleCreate implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    private VSubmenuPuzzleCreate view = VSubmenuPuzzleCreate.SINGLETON_INSTANCE;

    @Override
    public void register() {

    }

    @Override
    public void initialize() {

    }

    @Override
    public void update() {
        MenuUtil.setMenuProvider(this);
    }

    @Override
    public List<JMenu> getMenus() {
        final List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(PuzzleGameMenu.getMenu());
        return menus;
    }
}
