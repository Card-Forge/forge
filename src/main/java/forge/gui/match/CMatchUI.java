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

import java.util.ArrayList;
import java.util.List;

import forge.AllZone;
import forge.Card;
import forge.GameEntity;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.gui.CardContainer;
import forge.gui.framework.EDocID;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CMessage;
import forge.gui.match.controllers.CPicture;
import forge.gui.match.nonsingleton.CField;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.nonsingleton.VHand;

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

    /**
     * Due to be deprecated with new multiplayer changes. Doublestrike 13-10-12.
     * 
     * @param strAvatarIcon &emsp; Filename of non-default avatar icon, if desired.
     * 
     */
    public void initMatch(final String strAvatarIcon) {
        this.initMatch(2, 1);
    }

    /**
     * Instantiates at a match with a specified number of players
     * and hands.
     * 
     * @param numFieldPanels int
     * @param numHandPanels int
     */
    public void initMatch(int numFieldPanels, int numHandPanels) {
        // TODO fix for use with multiplayer
        // Update avatars
        /*final String[] indices = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
        int i = 0;
        for (VField view : VMatchUI.SINGLETON_INSTANCE.getFieldViews()) {
            final Image img;
            // Update AI quest icon
            if (i == 1 && Singletons.getModel().getMatchState().getGameType() == GameType.Quest) {
                    String filename = ForgeProps.getFile(NewConstants.IMAGE_ICON) + File.separator;

                    if (strAvatarIcon != null) {
                        filename += strAvatarIcon;
                        final File f = new File(filename);
                        img = (f.exists()
                                ? new ImageIcon(filename).getImage()
                                : FSkin.getAvatars().get(Integer.parseInt(indices[i])));
                    }
                    else {
                        img = FSkin.getAvatars().get(Integer.parseInt(indices[i]));
                    }
            }
            else {
                img = FSkin.getAvatars().get(Integer.parseInt(indices[i]));
            }
            i++;

            view.getLblAvatar().setIcon(new ImageIcon(img));
            view.getLblAvatar().getResizeTimer().start();
        }*/

        // Instantiate all required field slots (user at 0)
        final List<VField> fields = new ArrayList<VField>();
        for (int i = 0; i < numFieldPanels; i++) {
            switch (i) {
                case 0:
                    fields.add(0, new VField(EDocID.FIELD_0, AllZone.getHumanPlayer()));
                    fields.get(0).getLayoutControl().initialize();
                    break;
                case 1:
                    fields.add(1, new VField(EDocID.FIELD_1, AllZone.getComputerPlayer()));
                    fields.get(1).getLayoutControl().initialize();
                    break;
                default:
                    // A field must be initialized after it's instantiated, to update player info.
                    // No player, no init.
                    fields.add(i, new VField(EDocID.valueOf("FIELD_" + i), null));
            }
        }

        // Instantiate all required hand slots (user at 0)
        final List<VHand> hands = new ArrayList<VHand>();
        for (int i = 0; i < numHandPanels; i++) {
            switch (i) {
                case 0:
                    // A hand must be initialized after it's instantiated, to update player info.
                    // No player, no init.
                    hands.add(0, new VHand(EDocID.HAND_0, AllZone.getHumanPlayer()));
                    hands.get(0).getLayoutControl().initialize();
                    break;
                default:
                    hands.add(i, new VHand(EDocID.valueOf("HAND_" + i), null));
            }
        }

        // Replace old instances
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
        final List<VField> fields = VMatchUI.SINGLETON_INSTANCE.getFieldViews();
        final List<CField> controllers = new ArrayList<CField>();

        for (final VField f : fields) {
            controllers.add((CField) f.getLayoutControl());
        }

        return controllers;
    }

    /**
     * Fires up trample dialog.  Very old code, due for refactoring with new UI.
     * Could possibly move to view.
     * 
     * @param attacker &emsp; {@link forge.Card}
     * @param blockers &emsp; {@link forge.CardList}
     * @param damage &emsp; int
     */
    public void assignDamage(final Card attacker, final List<Card> blockers, final int damage, GameEntity defender) {
        if (damage <= 0) {
            return;
        }

        // If the first blocker can absorb all of the damage, don't show the Assign Damage Frame
        Card firstBlocker = blockers.get(0);
        if (!attacker.hasKeyword("Deathtouch") && firstBlocker.getLethalDamage() >= damage) {
            firstBlocker.addAssignedDamage(damage, attacker);
            return;
        }

        new VAssignDamage(attacker, blockers, damage, defender);
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
        final List<CField> fieldControllers = CMatchUI.this.getFieldControls();

        // Index of field; computer is 1, human is 0
        int index = turn.isComputer() ? 1 : 0;
        VField vf = fieldControllers.get(index).getView();

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

    @Override
    public Card getCard() {
        return CDetail.SINGLETON_INSTANCE.getCurrentCard();
    }
}
