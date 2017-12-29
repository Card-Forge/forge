package forge.menu;

import java.util.ArrayList;
import java.util.List;

public abstract class FDropDownMenu extends FDropDown {
    protected final List<FMenuItem> items = new ArrayList<FMenuItem>();

    public FDropDownMenu() {
    }

    @Override
    protected boolean autoHide() {
        return true;
    }

    protected abstract void buildMenu();

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        clear();
        items.clear();

        buildMenu();

        //ensure text is all aligned if some items have icons and others don't
        boolean allowForIcon = false;
        for (FMenuItem item : items) {
            if (item.hasIcon()) {
                allowForIcon = true;
                break;
            }
        }
        for (FMenuItem item : items) {
            item.setAllowForIcon(allowForIcon);
        }

        //determine needed width of menu
        float width = determineMenuWidth();
        if (width > maxWidth) {
            width = maxWidth;
        }

        //set bounds for each item
        float y = 0;
        for (FMenuItem item : items) {
            item.setBounds(0, y, width, FMenuItem.HEIGHT);
            y += FMenuItem.HEIGHT;
        }

        return new ScrollBounds(width, y);
    }

    protected float determineMenuWidth() {
        float width = 0;
        for (FMenuItem item : items) {
            float minWidth = item.getMinWidth();
            if (width < minWidth) {
                width = minWidth;
            }
        }
        return width;
    }

    public void addItem(FMenuItem item) {
        if (item.isVisible()) {
            items.add(add(item));
        }
    }

    @Override
    public boolean tap(float x, float y, int count) {
        super.tap(x, y, count);
        if (getDropDownOwner() instanceof FSubMenu) {
            return false; //return false so owning sub menu can be hidden
        }
        return true;
    }
}
