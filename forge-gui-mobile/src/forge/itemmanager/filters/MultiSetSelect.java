package forge.itemmanager.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardEdition;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FGroupList;
import forge.toolbox.FList;
import forge.util.Utils;

/** Every edition grouped by type, each with a tick. The ticked editions are handed over when the screen closes. */
public class MultiSetSelect extends FScreen {
    protected final Set<CardEdition> selectedSets = new HashSet<>();
    protected final FGroupList<CardEdition> lstSets = add(new FGroupList<>());
    private final Consumer<List<CardEdition>> onSetsChosen;

    public MultiSetSelect(String title, Collection<CardEdition> preselectedSets, Consumer<List<CardEdition>> onSetsChosen) {
        super(title);
        this.onSetsChosen = onSetsChosen;
        selectedSets.addAll(preselectedSets);

        lstSets.addGroup("Core Sets");
        lstSets.addGroup("Expansions");
        lstSets.addGroup("Starter Sets");
        lstSets.addGroup("Reprint Sets");
        lstSets.addGroup("Boxed Sets");

        lstSets.addGroup("Collector's Edition");
        lstSets.addGroup("Duel Decks");
        lstSets.addGroup("Promo Sets");
        lstSets.addGroup("Digital Sets");

        lstSets.addGroup("Draft Innovation Sets");


        lstSets.addGroup("Commander Sets");
        lstSets.addGroup("Multiplayer Sets");
        lstSets.addGroup("Other Supplemental Sets");
        lstSets.addGroup("Funny Sets");

        lstSets.addGroup("Custom Sets");

        List<CardEdition> sets = FModel.getMagicDb().getSortedEditions();
        for (CardEdition set : sets) {
            switch (set.getType()) {
                case CORE:
                    lstSets.addItem(set, 0);
                    break;
                case EXPANSION:
                    lstSets.addItem(set, 1);
                    break;
                case STARTER:
                    lstSets.addItem(set, 2);
                    break;
                case REPRINT:
                    lstSets.addItem(set, 3);
                    break;
                case BOXED_SET:
                    lstSets.addItem(set, 4);
                    break;
                case COLLECTOR_EDITION:
                    lstSets.addItem(set, 5);
                    break;
                case DUEL_DECK:
                    lstSets.addItem(set, 6);
                    break;
                case PROMO:
                    lstSets.addItem(set, 7);
                    break;
                case ONLINE:
                    lstSets.addItem(set, 8);
                    break;
                case DRAFT:
                    lstSets.addItem(set, 9);
                    break;
                case COMMANDER:
                    lstSets.addItem(set, 10);
                    break;
                case MULTIPLAYER:
                    lstSets.addItem(set, 11);
                    break;
                case OTHER:
                    lstSets.addItem(set, 12);
                    break;
                case FUNNY:
                    lstSets.addItem(set, 13);
                    break;
                default:  // THIRDPARTY - Custom Sets
                    lstSets.addItem(set, 14);
                    break;
            }
        }

        lstSets.setListItemRenderer(new SetRenderer());
    }

    protected String getLabel(CardEdition set) {
        return set.toString();
    }

    @Override
    public void onClose(Consumer<Boolean> canCloseCallback) {
        List<CardEdition> sortedSets = new ArrayList<>(selectedSets);
        Collections.sort(sortedSets);
        if (onSetsChosen != null)
            onSetsChosen.accept(sortedSets);
        super.onClose(canCloseCallback);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        lstSets.setBounds(0, startY, width, height - startY);
    }

    private class SetRenderer extends FList.ListItemRenderer<CardEdition> {
        @Override
        public float getItemHeight() {
            return Utils.AVG_FINGER_HEIGHT;
        }

        @Override
        public boolean tap(Integer index, CardEdition value, float x, float y, int count) {
            if (selectedSets.contains(value)) {
                if (count == 2) {
                    Forge.back(); //support double tap to confirm selection without unselecting double tapped item
                } else {
                    selectedSets.remove(value);
                }
            } else {
                selectedSets.add(value);
                if (count == 2) {
                    Forge.back(); //support double tap to confirm selection after selecting double tapped item
                }
            }
            return true;
        }

        @Override
        public void drawValue(Graphics g, Integer index, CardEdition value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
            float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
            x += offset;
            y += offset;
            w -= 2 * offset;
            h -= 2 * offset;

            float textHeight = h;
            h *= 0.66f;

            g.drawText(getLabel(value), font, foreColor, x, y, w - h - FList.PADDING, textHeight, false, Align.left, true);

            x += w - h;
            y += (textHeight - h) / 2;
            FCheckBox.drawCheckBox(g, SettingsScreen.DESC_COLOR, foreColor, selectedSets.contains(value), x, y, h, h);
        }
    }
}
