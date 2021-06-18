/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2021  Forge Team
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
package forge.screens.match.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardZoom;
import forge.card.MagicColor;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.toolbox.FDialog;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.Callback;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.Utils;
import forge.util.WaitCallback;

public class VAssignGenericAmount extends FDialog {
    private static final float CARD_GAP_X = Utils.scale(10);
    private static final float ADD_BTN_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.75f;

    private final Callback<Map<Object, Integer>> callback;
    private final int totalAmountToAssign;

    private final String lblAmount;
    private final FLabel lblTotalAmount;
    private final boolean atLeastOne;

    private final EffectSourcePanel pnlSource;
    private final TargetsPanel pnlTargets;

    private final List<AssignTarget> targetsList = new ArrayList<>();
    private final Map<Object, AssignTarget> targetsMap = new HashMap<>();

    /** Constructor.
     *
     * @param attacker0 {@link forge.game.card.Card}
     * @param targets Map<GameEntity, Integer>, map of GameEntity and its maximum assignable amount
     * @param amount Total amount to be assigned
     * @param atLeastOne Must assign at least one amount to each target
     */
    public VAssignGenericAmount(final CardView effectSource, final Map<Object, Integer> targets, final int amount, final boolean atLeastOne, final String amountLabel, final WaitCallback<Map<Object, Integer>> waitCallback) {
        super(Localizer.getInstance().getMessage("lbLAssignAmountForEffect", amountLabel, CardTranslation.getTranslatedName(effectSource.getName())) , 2);

        callback = waitCallback;
        totalAmountToAssign = amount;
        this.atLeastOne = atLeastOne;

        lblAmount = amountLabel;
        lblTotalAmount = add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblTotalAmountText", lblAmount)).align(Align.center).build());

        pnlSource = add(new EffectSourcePanel(effectSource));
        pnlTargets = add(new TargetsPanel(targets));

        initButton(0, Localizer.getInstance().getMessage("lblOK"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                finish();
            }
        });
        initButton(1, Localizer.getInstance().getMessage("lblReset"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                resetAssignedDamage();
                initialAssignAmount();
            }
        });

        initialAssignAmount();
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float w = width - 2 * padding;

        float x = padding;
        float labelHeight = lblTotalAmount.getAutoSizeBounds().height;
        float y = maxHeight - labelHeight + padding;

        float dtOffset = ADD_BTN_HEIGHT + targetsList.get(0).label.getAutoSizeBounds().height;
        float cardPanelHeight = (y - dtOffset - labelHeight - 3 * padding) / 2;
        float cardPanelWidth = cardPanelHeight / FCardPanel.ASPECT_RATIO;

        y = padding;
        pnlSource.setBounds(x + (w - cardPanelWidth) / 2, y, cardPanelWidth, cardPanelHeight);

        y += cardPanelHeight + padding;
        lblTotalAmount.setBounds(x, y, w, labelHeight);

        y += labelHeight + padding;
        pnlTargets.setBounds(0, y, width, cardPanelHeight + dtOffset);

        return maxHeight;
    }

    private class TargetsPanel extends FScrollPane {
        private TargetsPanel(final Map<Object, Integer> targets) {
            for (final Map.Entry<Object, Integer> e : targets.entrySet()) {
                addDamageTarget(e.getKey(), e.getValue());
            }
        }

        private void addDamageTarget(Object entity, int max) {
            AssignTarget at = add(new AssignTarget(entity, max));
            targetsMap.put(entity, at);
            targetsList.add(at);
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float cardPanelHeight = visibleHeight - ADD_BTN_HEIGHT - targetsList.get(0).label.getAutoSizeBounds().height;
            float width = cardPanelHeight / FCardPanel.ASPECT_RATIO;
            float dx = width + CARD_GAP_X;

            float x = (visibleWidth - targetsList.size() * dx + CARD_GAP_X) / 2;
            if (x < FOptionPane.PADDING) {
                x = FOptionPane.PADDING;
            }

            for (AssignTarget at : targetsList) {
                at.setBounds(x, 0, width, visibleHeight);
                x += dx;
            }
            return new ScrollBounds(x - CARD_GAP_X + FOptionPane.PADDING, visibleHeight);
        }
    }

    private class AssignTarget extends FContainer {
        private final Object entity;
        private final FDisplayObject obj;
        private final FLabel label, btnSubtract, btnAdd;
        private final int max;
        private int amount;

        public AssignTarget(Object entity0, int max0) {
            entity = entity0;
            max = max0;
            if (entity instanceof CardView) {
                obj = add(new EffectSourcePanel((CardView)entity));
            } else if (entity instanceof PlayerView) {
                PlayerView player = (PlayerView)entity;
                obj = add(new MiscTargetPanel(player.getName(), MatchController.getPlayerAvatar(player)));
            } else if (entity instanceof Byte) {
                FSkinImage manaSymbol;
                Byte color = (Byte) entity;
                if (color == MagicColor.WHITE) {
                    manaSymbol = FSkinImage.MANA_W;
                } else if (color == MagicColor.BLUE) {
                    manaSymbol = FSkinImage.MANA_U;
                } else if (color == MagicColor.BLACK) {
                    manaSymbol = FSkinImage.MANA_B;
                } else if (color == MagicColor.RED) {
                    manaSymbol = FSkinImage.MANA_R;
                } else if (color == MagicColor.GREEN) {
                    manaSymbol = FSkinImage.MANA_G;
                } else { // Should never come here, but add this to avoid compile error
                    manaSymbol = FSkinImage.MANA_COLORLESS;
                }
                obj = add(new MiscTargetPanel("", manaSymbol));
            } else {
                obj = add(new MiscTargetPanel(entity.toString(), FSkinImage.UNKNOWN));
            }
            label = add(new FLabel.Builder().text("0").font(FSkinFont.get(18)).align(Align.center).build());
            btnSubtract = add(new FLabel.ButtonBuilder().icon(FSkinImage.MINUS).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    assignAmountTo(entity, false);
                }
            }).build());
            btnAdd = add(new FLabel.ButtonBuilder().icon(Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    assignAmountTo(entity, true);
                }
            }).build());
        }

        @Override
        protected void doLayout(float width, float height) {
            float y = 0;
            obj.setBounds(0, y, width, FCardPanel.ASPECT_RATIO * width);
            y += obj.getHeight();

            label.setBounds(0, y, width, label.getAutoSizeBounds().height);
            y += label.getHeight();

            float buttonSize = (width - FOptionPane.PADDING) / 2;
            btnSubtract.setBounds(0, y, buttonSize, ADD_BTN_HEIGHT);
            btnAdd.setBounds(width - buttonSize, y, buttonSize, ADD_BTN_HEIGHT);
        }
    }

    private static class EffectSourcePanel extends FCardPanel {
        private EffectSourcePanel(CardView card) {
            super(card);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            CardZoom.show(getCard());
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            CardZoom.show(getCard());
            return true;
        }

        @Override
        protected float getPadding() {
            return 0;
        }
    }

    private static class MiscTargetPanel extends FDisplayObject {
        private static final FSkinFont FONT = FSkinFont.get(18);
        private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
        private final String name;
        private final FImage image;

        private MiscTargetPanel(String name0, FImage image0) {
            name = name0;
            image = image0;
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();
            g.drawImage(image, 0, 0, w, w);
            g.drawText(name, FONT, FORE_COLOR, 0, w, w, h - w, false, Align.center, true);
        }
    }

    private void assignAmountTo(Object source, boolean isAdding) {
        AssignTarget at = targetsMap.get(source);
        int assigned = at.amount;
        int leftToAssign = Math.max(0, at.max - assigned);
        int amountToAdd = isAdding ? 1 : -1;
        int remainingAmount = Math.min(getRemainingAmount(), leftToAssign);

        if (amountToAdd > remainingAmount) {
            amountToAdd = remainingAmount;
        }
        if (atLeastOne && assigned + amountToAdd < 1) {
            amountToAdd = 1 - assigned;
        }

        if (0 == amountToAdd || amountToAdd + assigned < 0) {
            return;
        }

        addAssignedAmount(at, amountToAdd);
        updateLabels();
    }

    private void initialAssignAmount() {
        if (!atLeastOne) {
            updateLabels();
            return;
        }

        for(AssignTarget at : targetsList) {
            addAssignedAmount(at, 1);
        }
        updateLabels();
    }

    private void resetAssignedDamage() {
        for (AssignTarget at : targetsList) {
            at.amount = 0;
        }
    }

    private void addAssignedAmount(final AssignTarget at, int addedAmount) {
        // If we don't have enough left or we're trying to unassign too much return
        int canAssign = getRemainingAmount();
        if (canAssign < addedAmount) {
            addedAmount = canAssign;
        }

        at.amount = Math.max(0, addedAmount + at.amount);
    }

    private int getRemainingAmount() {
        int spent = 0;
        for (AssignTarget at : targetsList) {
            spent += at.amount;
        }
        return totalAmountToAssign - spent;
    }

    /** Updates labels and other UI elements.*/
    private void updateLabels() {
        int amountLeft = totalAmountToAssign;

        for (AssignTarget at : targetsList) {
            amountLeft -= at.amount;
            StringBuilder sb = new StringBuilder();
            sb.append(at.amount);
            if (at.max - at.amount == 0) {
                sb.append(" (").append(Localizer.getInstance().getMessage("lblMax")).append(")");
            }
            at.label.setText(sb.toString());
        }

        lblTotalAmount.setText(TextUtil.concatNoSpace(Localizer.getInstance().getMessage("lblAvailableAmount", lblAmount) + ": ",
                String.valueOf(amountLeft), " (of ", String.valueOf(totalAmountToAssign), ")"));
        setButtonEnabled(0, amountLeft == 0);
    }

    // Dumps damage onto cards. Damage must be stored first, because if it is
    // assigned dynamically, the cards die off and further damage to them can't
    // be modified.
    private void finish() {
        if (getRemainingAmount() > 0) {
            return;
        }
        hide();
        callback.run(getAssignedMap());
    }

    public Map<Object, Integer> getAssignedMap() {
        Map<Object, Integer> result = new HashMap<>(targetsList.size());
        for (AssignTarget at : targetsList)
            result.put(at.entity, at.amount);
        return result;
    }
}
