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
package forge.gui.match;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import forge.Card;
import forge.GameEntity;
import forge.Singletons;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.gui.CardContainer;
import forge.gui.framework.EDocID;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CMessage;
import forge.gui.match.controllers.CPicture;
import forge.gui.match.nonsingleton.CField;
import forge.gui.match.nonsingleton.VCommand;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.nonsingleton.VHand;
import forge.gui.toolbox.FSkin;
import forge.item.InventoryItem;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * Constructs instance of match UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CMatchUI implements CardContainer {
    /** */
    SINGLETON_INSTANCE;

    private Image getPlayerAvatar(final Player p, final int defaultIndex) {
        String strAvatarIcon = p.getLobbyPlayer().getPicture();
        if (strAvatarIcon != null) {
            final File f = new File(ForgeProps.getFile(NewConstants.IMAGE_ICON), strAvatarIcon);
            if (f.exists()) {
                return new ImageIcon(f.getPath()).getImage();
            }
        }
        int iAvatar = p.getLobbyPlayer().getAvatarIndex();
        return FSkin.getAvatars().get(iAvatar >= 0 ? iAvatar : defaultIndex);
    }


    private void setAvatar(final VField view, final Image img) {

        view.getLblAvatar().setIcon(new ImageIcon(img));
        view.getLblAvatar().getResizeTimer().start();
    }

    /**
     * Instantiates at a match with a specified number of players
     * and hands.
     * 
     * @param numFieldPanels int
     * @param numHandPanels int
     */
    public void initMatch(final List<Player> players, Player localPlayer) {
        // TODO fix for use with multiplayer

        final String[] indices = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");

        // Instantiate all required field slots (user at 0) <-- that's not guaranteed
        final List<VField> fields = new ArrayList<VField>();
        final List<VCommand> commands = new ArrayList<VCommand>();

        VField humanField = new VField(EDocID.valueOf("FIELD_0"), localPlayer, localPlayer);
        VCommand humanCommand = new VCommand(EDocID.COMMAND_0, localPlayer);
        fields.add(0, humanField);
        commands.add(0, humanCommand);
        setAvatar(humanField, FSkin.getAvatars().get(Integer.parseInt(indices[0])));
        humanField.getLayoutControl().initialize();
        humanCommand.getLayoutControl().initialize();

        int i = 1;
        for (Player p : players) {
            if (p.equals(localPlayer)) {
                continue;
            }
            // A field must be initialized after it's instantiated, to update player info.
            // No player, no init.
            VField f = new VField(EDocID.valueOf("FIELD_" + i), p, localPlayer);
            setAvatar(f, getPlayerAvatar(p, Integer.parseInt(indices[i % 2])));
            f.getLayoutControl().initialize();
            fields.add(f);
            VCommand c = new VCommand(EDocID.valueOf("COMMAND_" + i), p);
            c.getLayoutControl().initialize();
            commands.add(c);
            i++;
        }


        // Instantiate all required hand slots (user at 0)
        final List<VHand> hands = new ArrayList<VHand>();
        VHand newHand = new VHand(EDocID.HAND_0, localPlayer);
        newHand.getLayoutControl().initialize();
        hands.add(newHand);

// Max: 2+ hand are needed at 2HG (but this is quite far now) - yet it's nice to have this possibility
//        for (int i = 0; i < numHandPanels; i++) {
//            switch (i) {
//                    hands.add(i, new VHand(EDocID.valueOf("HAND_" + i), null));
//            }
//        }

        // Replace old instances
        VMatchUI.SINGLETON_INSTANCE.setCommandViews(commands);
        VMatchUI.SINGLETON_INSTANCE.setFieldViews(fields);
        VMatchUI.SINGLETON_INSTANCE.setHandViews(hands);
    }

    /**
     * Resets all phase buttons in all fields to "inactive", so highlight won't
     * be drawn on them. "Enabled" state remains the same.
     */
    // This method is in the top-level controller because it affects ALL fields
    // (not just one).
    public void resetAllPhaseButtons() {
        for (final CField c : CMatchUI.this.getFieldControls()) {
            c.resetPhaseButtons();
        }
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showMessage(final String s0) {
        CMessage.SINGLETON_INSTANCE.setMessage(s0);
    }

    /**
     * Gets the field controllers.
     * 
     * @return List<CField>
     */
    public List<CField> getFieldControls() {
        final List<CField> controllers = new ArrayList<CField>();

        for (final VField f : VMatchUI.SINGLETON_INSTANCE.getFieldViews()) {
            controllers.add(f.getLayoutControl());
        }

        return controllers;
    }

    public VField getFieldViewFor(Player p) {
        for (final VField f : VMatchUI.SINGLETON_INSTANCE.getFieldViews()) {
            if (f.getLayoutControl().getPlayer().equals(p)) {
                return f;
            }
        }
        return null;
    }

    /**
     * 
     * Fires up trample dialog.  Very old code, due for refactoring with new UI.
     * Could possibly move to view.
     * 
     * @param attacker &emsp; {@link forge.Card}
     * @param blockers &emsp; {@link forge.CardList}
     * @param damage &emsp; int
     */
    public Map<Card, Integer> getDamageToAssign(final Card attacker, final List<Card> blockers, final int damage, GameEntity defender) {
        if (damage <= 0) {
            return new HashMap<Card, Integer>();
        }

        // If the first blocker can absorb all of the damage, don't show the Assign Damage Frame
        Card firstBlocker = blockers.get(0);
        if (!attacker.hasKeyword("Deathtouch") && firstBlocker.getLethalDamage() >= damage) {
            Map<Card, Integer> res = new HashMap<Card, Integer>();
            res.put(firstBlocker, damage);
            return res;
        }

        VAssignDamage v = new VAssignDamage(attacker, blockers, damage, defender);
        return v.getDamageMap();
    }

    /**
     * 
     * Checks if game control should stop at a phase, for either
     * a forced programmatic stop, or a user-induced phase toggle.
     * @param turn &emsp; {@link forge.game.player.Player}
     * @param phase &emsp; {@link java.lang.String}
     * @return boolean
     */
    public final boolean stopAtPhase(final Player turn, final PhaseType phase) {
        VField vf = getFieldViewFor(turn);

        switch (phase) {
            case UPKEEP: return vf.getLblUpkeep().getEnabled();
            case DRAW: return vf.getLblDraw().getEnabled();
            case MAIN1: return vf.getLblMain1().getEnabled();
            case COMBAT_BEGIN: return vf.getLblBeginCombat().getEnabled();
            case COMBAT_DECLARE_ATTACKERS: return vf.getLblDeclareAttackers().getEnabled();
            case COMBAT_DECLARE_BLOCKERS: return vf.getLblDeclareBlockers().getEnabled();
            case COMBAT_FIRST_STRIKE_DAMAGE: return vf.getLblFirstStrike().getEnabled();
            case COMBAT_DAMAGE: return vf.getLblCombatDamage().getEnabled();
            case COMBAT_END: return vf.getLblEndCombat().getEnabled();
            case MAIN2: return vf.getLblMain2().getEnabled();
            case END_OF_TURN: return vf.getLblEndTurn().getEnabled();
            default:
        }

        return true;
    }

    @Override
    public void setCard(final Card c) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showCard(c);
    }

    public void setCard(final InventoryItem c) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showCard(c);
    }

    @Override
    public Card getCard() {
        return CDetail.SINGLETON_INSTANCE.getCurrentCard();
    }
}
