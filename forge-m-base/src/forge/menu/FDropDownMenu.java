package forge.menu;

import java.util.ArrayList;
import java.util.List;

public abstract class FDropDownMenu extends FDropDown {
    private final List<FMenuItem> items = new ArrayList<FMenuItem>();

    public FDropDownMenu() {
    }

    @Override
    protected boolean autoHide() {
        return true;
    }

    protected abstract void buildMenu();

    @Override
    protected final ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        clear();
        items.clear();

        buildMenu();

        //ensure text is all aligned if some items have icons and others don't
        float maxIconSize = 0;
        for (FMenuItem item : items) {
            if (item.getIconSize() > maxIconSize) {
                maxIconSize = item.getIconSize();
            }
        }
        if (maxIconSize > 0) {
            for (FMenuItem item : items) {
                item.setIconSize(maxIconSize);
            }
        }

        //determine needed width of menu
        float width = 0;
        for (FMenuItem item : items) {
            float minWidth = item.getMinWidth();
            if (minWidth > width) {
                width = minWidth;
            }
        }
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
    
    public void addItem(FMenuItem item) {
        items.add(add(item));
    }

    @Override
    public boolean tap(float x, float y, int count) {
        hide(); //hide when item tapped
        return true;
    }
}
