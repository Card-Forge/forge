package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;

import forge.screens.match.MatchController;
import forge.toolbox.FCheckBox;
import forge.toolbox.FChoiceList;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;

public class VAutoYields extends FDialog {
    private final FChoiceList<String> lstAutoYields;
    private final FCheckBox chkDisableAll;

    public VAutoYields() {
        super("Auto-Yields", 2);
        List<String> autoYields = new ArrayList<String>();
        for (String autoYield : MatchController.instance.getAutoYields()) {
            autoYields.add(autoYield);
        }
        lstAutoYields = add(new FChoiceList<String>(autoYields) {
            @Override
            protected void onCompactModeChange() {
                VAutoYields.this.revalidate(); //revalidate entire dialog so height updated
            }

            @Override
            protected boolean allowDefaultItemWrap() {
                return true;
            }
        });
        chkDisableAll = add(new FCheckBox("Disable All Auto Yields", MatchController.instance.getDisableAutoYields()));
        chkDisableAll.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                MatchController.instance.setDisableAutoYields(chkDisableAll.isSelected());
            }
        });
        initButton(0, "OK", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
            }
        });
        initButton(1, "Remove Yield", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                String selected = lstAutoYields.getSelectedItem();
                if (selected != null) {
                    lstAutoYields.removeItem(selected);
                    MatchController.instance.setShouldAutoYield(selected, false);
                    setButtonEnabled(1, lstAutoYields.getCount() > 0);
                    lstAutoYields.cleanUpSelections();
                    VAutoYields.this.revalidate();
                }
            }
        });
        setButtonEnabled(1, autoYields.size() > 0);
    }

    @Override
    public void show() {
        if (lstAutoYields.getCount() > 0) {
            super.show();
        }
        else {
            FOptionPane.showMessageDialog("There are no active auto-yields.", "No Auto-Yields", FOptionPane.INFORMATION_ICON);
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float x = padding;
        float y = padding;
        float w = width - 2 * padding;
        TextBounds checkBoxSize = chkDisableAll.getAutoSizeBounds();

        float listHeight = lstAutoYields.getListItemRenderer().getItemHeight() * lstAutoYields.getCount();
        float maxListHeight = maxHeight - 3 * padding - checkBoxSize.height;
        if (listHeight > maxListHeight) {
            listHeight = maxListHeight;
        }

        lstAutoYields.setBounds(x, y, w, listHeight);
        y += listHeight + padding;
        chkDisableAll.setBounds(x, y, Math.min(checkBoxSize.width, w), checkBoxSize.height);
        y += checkBoxSize.height + padding;

        return y;
    }
}
