package forge.card;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import forge.item.PaperCard;
import forge.toolbox.FChoiceList;
import forge.toolbox.FLabel;

public class CardListPreview extends FLabel {
    public static final float CARD_PREVIEW_RATIO = 0.5f;

    private final FChoiceList<PaperCard> list;

    public CardListPreview(FChoiceList<PaperCard> list0) {
        super(new FLabel.Builder().iconScaleFactor(1).insets(new Vector2(0, 0))
                .iconInBackground(true).align(Align.center));
        list = list0;
    }

    @Override
    public boolean tap(float x, float y, int count) {
        return zoom();
    }
    @Override
    public boolean longPress(float x, float y) {
        return zoom();
    }
    private boolean zoom() {
        int index = list.getSelectedIndex();
        if (index == -1) { return false; }
        CardZoom.show(list.extractListData(), index, list);
        return true;
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            int selectedIndex = list.getSelectedIndex();
            if (velocityX > 0) {
                if (selectedIndex > 0) {
                    list.setSelectedIndex(selectedIndex - 1);
                }
            }
            else if (selectedIndex < list.getCount() - 1) {
                list.setSelectedIndex(selectedIndex + 1);
            }
            return true;
        }
        return false;
    }
}