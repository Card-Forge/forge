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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import com.google.common.collect.Lists;

import forge.Card;
import forge.FThreads;
import forge.GameEntity;
import forge.ImageCache;
import forge.Singletons;
import forge.game.phase.PhaseType;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.framework.EDocID;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.controllers.CCombat;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CMessage;
import forge.gui.match.controllers.CPicture;
import forge.gui.match.nonsingleton.VCommand;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.nonsingleton.VField.PhaseLabel;
import forge.gui.match.nonsingleton.VHand;
import forge.gui.match.views.VPlayers;
import forge.gui.toolbox.FSkin;
import forge.item.InventoryItem;
import forge.properties.ForgePreferences.FPref;

/**
 * Constructs instance of match UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CMatchUI {
    SINGLETON_INSTANCE;

    private List<Player> sortedPlayers;
    private VMatchUI view;

    private ImageIcon getPlayerAvatar(final Player p, final int defaultIndex) {
        LobbyPlayer lp = p.getLobbyPlayer();
        if (null != lp.getIconImageKey()) {
            return ImageCache.getIcon(lp);
        }
        
        int avatarIdx = lp.getAvatarIndex();
        return new ImageIcon(FSkin.getAvatars().get(0 <= avatarIdx ? avatarIdx : defaultIndex));
    }


    private void setAvatar(VField view, ImageIcon img) {
        view.getLblAvatar().setIcon(img);
        view.getLblAvatar().getResizeTimer().start();
    }

    /**
     * Instantiates at a match with a specified number of players
     * and hands.
     * 
     * @param numFieldPanels int
     * @param numHandPanels int
     */
    public void initMatch(final List<Player> players, LobbyPlayer localPlayer) {
        view = VMatchUI.SINGLETON_INSTANCE;
        
        // TODO fix for use with multiplayer

        final String[] indices = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");

        // Instantiate all required field slots (user at 0)
        sortedPlayers = shiftPlayersPlaceLocalFirst(players, localPlayer);


        final List<VField> fields = new ArrayList<VField>();
        final List<VCommand> commands = new ArrayList<VCommand>();
        
        int i = 0;
        for (Player p : sortedPlayers) {
            // A field must be initialized after it's instantiated, to update player info.
            // No player, no init.
            VField f = new VField(EDocID.Fields[i], p, localPlayer);
            VCommand c = new VCommand(EDocID.Commands[i], p);
            fields.add(f);
            commands.add(c);

            //setAvatar(f, new ImageIcon(FSkin.getAvatars().get()));
            setAvatar(f, getPlayerAvatar(p, Integer.parseInt(indices[i > 2 ? 1 : 0])));
            f.getLayoutControl().initialize();
            c.getLayoutControl().initialize();
            i++;
        }
        
        // Replace old instances
        view.setCommandViews(commands);
        view.setFieldViews(fields);
        
        VPlayers.SINGLETON_INSTANCE.init(players);
        
        initHandViews(localPlayer);
    }

    public void initHandViews(LobbyPlayer localPlayer) { 
        final List<VHand> hands = new ArrayList<VHand>();
        final Iterable<VHand> oldHands = view.getHands();
        
        int i = 0;
        for (Player p : sortedPlayers) {
            
            PlayerZone hand = p.getZone(ZoneType.Hand);
            for(VHand vh : oldHands)
                hand.deleteObserver(vh.getLayoutControl());
            
            if (p.getLobbyPlayer() == localPlayer) {
                VHand newHand = new VHand(EDocID.Hands[i], p);
                newHand.getLayoutControl().initialize();
                hands.add(newHand);
            }
            i++;
        }
        
        if(hands.isEmpty()) { // add empty hand for matches without human
            VHand newHand = new VHand(EDocID.Hands[0], null);
            newHand.getLayoutControl().initialize();
            hands.add(newHand);
        }
        view.setHandViews(hands);
    }

    private List<Player> shiftPlayersPlaceLocalFirst(final List<Player> players, LobbyPlayer localPlayer) {
        // get an arranged list so that the first local player is at index 0
        List<Player> sortedPlayers = Lists.newArrayList(players);
        int ixFirstHuman = -1;
        for(int i = 0; i < players.size(); i++) {
            if( sortedPlayers.get(i).getLobbyPlayer() == localPlayer ) {
                ixFirstHuman = i;
                break;
            }
        }
        if( ixFirstHuman > 0 ) {
            sortedPlayers.add(0, sortedPlayers.remove(ixFirstHuman));
        }
        return sortedPlayers;
    }

    /**
     * Resets all phase buttons in all fields to "inactive", so highlight won't
     * be drawn on them. "Enabled" state remains the same.
     */
    // This method is in the top-level controller because it affects ALL fields
    // (not just one).
    public void resetAllPhaseButtons() {
        for (final VField v : view.getFieldViews()) {
            v.resetPhaseButtons();
        }
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showMessage(final String s0) {
        CMessage.SINGLETON_INSTANCE.setMessage(s0);
    }

    public VField getFieldViewFor(Player p) {
        int idx = getPlayerIndex(p);
        return idx < 0 ? null :view.getFieldViews().get(idx);
    }

    /**
     * 
     * Fires up trample dialog.  Very old code, due for refactoring with new UI.
     * Could possibly move to view.
     * 
     * @param attacker &emsp; {@link forge.Card}
     * @param blockers &emsp; {@link forge.CardList}
     * @param damage &emsp; int
     * @param overrideOrder overriding combatant order
     */
    @SuppressWarnings("unchecked")
    public Map<Card, Integer> getDamageToAssign(final Card attacker, final List<Card> blockers, final int damage, final GameEntity defender, final boolean overrideOrder) {
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

        final Object[] result = { null }; // how else can I extract a value from EDT thread?
        FThreads.invokeInEdtAndWait(new Runnable() {
            @Override
            public void run() {
                VAssignDamage v = new VAssignDamage(attacker, blockers, damage, defender, overrideOrder);
                result[0] = v.getDamageMap();
            }});
        return (Map<Card, Integer>)result[0];
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
        PhaseLabel label = vf.getLabelFor(phase);
        return label == null || label.getEnabled(); 
    }

    public void setCard(final Card c) {
        FThreads.assertExecutedByEdt(true);
        setCard(c, false);
    }
    
    public void setCard(final Card c, final boolean showFlipped ) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showCard(c, showFlipped);
    }

    public void setCard(final InventoryItem c) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showCard(c);
    }
    
    private int getPlayerIndex(Player player) {
        return sortedPlayers.indexOf(player);
    }    
    

    public void showCombat() {
        if ( CCombat.SINGLETON_INSTANCE.hasCombatToShow() ) {
            SDisplayUtil.showTab(EDocID.REPORT_COMBAT.getDoc());
        }
        CCombat.SINGLETON_INSTANCE.update();
    } // showBlockers()

}
