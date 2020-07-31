package forge.itemmanager.filters;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.game.GameFormat;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FGroupList;
import forge.toolbox.FList;
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by maustin on 16/04/2018.
 */
public class HistoricFormatSelect extends FScreen {

    private GameFormat selectedFormat;
    private final FGroupList<GameFormat> lstFormats = add(new FGroupList<>());
    private final Set<GameFormat.FormatSubType> historicSubTypes = new HashSet<>(Arrays.asList(GameFormat.FormatSubType.Block,
            GameFormat.FormatSubType.Standard,GameFormat.FormatSubType.Extended,GameFormat.FormatSubType.Modern,
            GameFormat.FormatSubType.Legacy, GameFormat.FormatSubType.Vintage));

    private Runnable onCloseCallBack;

    public HistoricFormatSelect() {
        super(Localizer.getInstance().getMessage("lblChooseFormat"));
        for (GameFormat.FormatType group:GameFormat.FormatType.values()){
            if (group == GameFormat.FormatType.Historic){
                for (GameFormat.FormatSubType subgroup:GameFormat.FormatSubType.values()){
                    if (historicSubTypes.contains(subgroup)){
                        lstFormats.addGroup(group.name() + "-" + subgroup.name());
                    }
                }
            }else {
                lstFormats.addGroup(group.name());
            }
        }
        for (GameFormat format: FModel.getFormats().getOrderedList()){
            switch(format.getFormatType()){
                case Sanctioned:
                    lstFormats.addItem(format, 0);
                    break;
                case Casual:
                    lstFormats.addItem(format, 1);
                    break;
                case Historic:
                    switch (format.getFormatSubType()){
                        case Block:
                            lstFormats.addItem(format, 2);
                            break;
                        case Standard:
                            lstFormats.addItem(format, 3);
                            break;
                        case Extended:
                            lstFormats.addItem(format, 4);
                            break;
                        case Modern:
                            lstFormats.addItem(format, 5);
                            break;
                        case Legacy:
                            lstFormats.addItem(format, 6);
                            break;
                        case Vintage:
                            lstFormats.addItem(format, 7);
                            break;

                    }
                    break;
                case Digital:
                    lstFormats.addItem(format, 8);
                    break;
                case Custom:
                    lstFormats.addItem(format, 9);
            }
        }
        lstFormats.setListItemRenderer(new FormatRenderer());
    }

    public GameFormat getSelectedFormat() {
        return selectedFormat;
    }

    public void setOnCloseCallBack(Runnable onCloseCallBack) {
        this.onCloseCallBack = onCloseCallBack;
    }

    @Override
    public void onClose(Callback<Boolean> canCloseCallback) {
        if (selectedFormat != null) {
            if (onCloseCallBack != null) {
                onCloseCallBack.run();
            }
        }
        super.onClose(canCloseCallback);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        lstFormats.setBounds(0, startY, width, height - startY);
    }

    private class FormatRenderer extends FList.ListItemRenderer<GameFormat>{
        @Override
        public float getItemHeight() {
            return Utils.AVG_FINGER_HEIGHT;
        }

        @Override
        public boolean tap(Integer index, GameFormat value, float x, float y, int count) {
            selectedFormat=value;
            Forge.back();
            return true;
        }

        @Override
        public void drawValue(Graphics g, Integer index, GameFormat value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
            float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
            x += offset;
            y += offset;
            w -= 2 * offset;
            h -= 2 * offset;

            float textHeight = h;
            h *= 0.66f;

            g.drawText(value.toString(), font, foreColor, x, y, w - h - FList.PADDING, textHeight, false, Align.left, true);

            x += w - h;
            y += (textHeight - h) / 2;
        }
    }
}
