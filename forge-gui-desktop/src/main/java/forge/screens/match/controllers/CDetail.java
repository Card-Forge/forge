/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match.controllers;

import java.awt.event.MouseEvent;

import forge.game.card.CardView;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;
import forge.item.InventoryItemFromSet;
import forge.screens.match.views.VDetail;
import forge.toolbox.FMouseAdapter;

/**
 * Controller for {@link VDetail}. May be used as part of a
 * {@link CDetailPicture}.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public class CDetail implements ICDoc {
    private final VDetail view;
    CDetail(final CDetailPicture controller) {
        this.view = new VDetail(this);

        this.view.getPnlDetail().addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftClick(final MouseEvent e) {
                controller.flip();
            }
        });
    }

    public VDetail getView() {
        return view;
    }

    /**
     * Shows card details and/or picture in sidebar cardview tabber.
     *
     * @param c &emsp; {@link CardView} object
     * @param isInAltState whether to draw the flip side of the card.
     */
    void showCard(final CardView c, final boolean isInAltState, final boolean mayView, final boolean mayFlip) {
        final CardView toShow = mayView ? c : null;
        view.getLblFlipcard().setVisible(toShow != null && mayFlip);
        view.getPnlDetail().setCard(toShow, mayView, isInAltState);
        if (view.getParentCell() != null) {
            view.getParentCell().repaintSelf();
        }
    }

    void showItem(final InventoryItem item) {
        if (item instanceof InventoryItemFromSet) {
            view.getLblFlipcard().setVisible(false);
            view.getPnlDetail().setItem((InventoryItemFromSet)item);
            view.getParentCell().repaintSelf();
        }
    }

    @Override
    public void register() {
    }

    @Override
    public void initialize() {

    }

    @Override
    public void update() {
    }
}
