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
package forge.screens.match;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import forge.LobbyPlayer;
import forge.UiCommand;
import forge.FThreads;
import forge.ImageCache;
import forge.Singletons;
import forge.events.IUiEventVisitor;
import forge.events.UiEvent;
import forge.events.UiEventAttackerDeclared;
import forge.events.UiEventBlockerAssigned;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.framework.*;
import forge.item.InventoryItem;
import forge.menus.IMenuProvider;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.home.quest.QuestDraftUtils;
import forge.screens.match.controllers.*;
import forge.screens.match.menus.CMatchUIMenus;
import forge.screens.match.views.*;
import forge.toolbox.FOptionPane;
import forge.toolbox.FOverlay;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.special.PhaseLabel;
import forge.view.arcane.CardPanel;
import forge.view.arcane.PlayArea;

import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;

import java.util.*;

/**
 * Constructs instance of match UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CMatchUI implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    private List<Player> sortedPlayers;
    private VMatchUI view;

    private EventBus uiEvents;
    private IVDoc<? extends ICDoc> selectedDocBeforeCombat;
    private MatchUiEventVisitor visitor = new MatchUiEventVisitor();
    public final Map<LobbyPlayer, String> avatarImages = new HashMap<LobbyPlayer, String>();

    private CMatchUI() {
        uiEvents = new EventBus("ui events");
        uiEvents.register(Singletons.getControl().getSoundSystem());
        uiEvents.register(visitor);
    }

    private SkinImage getPlayerAvatar(final Player p, final int defaultIndex) {
        LobbyPlayer lp = p.getLobbyPlayer();
        
        if (avatarImages.containsKey(lp)) {
            return ImageCache.getIcon(avatarImages.get(lp));
        }

        int avatarIdx = lp.getAvatarIndex();
        return FSkin.getAvatars().get(avatarIdx >= 0 ? avatarIdx : defaultIndex);
    }

    private void setAvatar(VField view, SkinImage img) {
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

        final String[] indices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");

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

        int i = 0;
        for (Player p : sortedPlayers) {
            if (p.getLobbyPlayer() == localPlayer) {
                VHand newHand = new VHand(EDocID.Hands[i], p);
                newHand.getLayoutControl().initialize();
                hands.add(newHand);
            }
            i++;
        }

        if (hands.isEmpty()) { // add empty hand for matches without human
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
        for (int i = 0; i < players.size(); i++) {
            if (sortedPlayers.get(i).getLobbyPlayer() == localPlayer) {
                ixFirstHuman = i;
                break;
            }
        }
        if (ixFirstHuman > 0) {
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
            v.getPhaseIndicator().resetPhaseButtons();
        }
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showMessage(final String s0) {
        CPrompt.SINGLETON_INSTANCE.setMessage(s0);
    }

    public VField getFieldViewFor(Player p) {
        int idx = getPlayerIndex(p);
        return idx < 0 ? null :view.getFieldViews().get(idx);
    }

    public VCommand getCommandFor(Player p) {
        int idx = getPlayerIndex(p);
        return idx < 0 ? null :view.getCommandViews().get(idx);
    }

    public VHand getHandFor(Player p) {
        int idx = getPlayerIndex(p);
        List<VHand> allHands = view.getHands();
        return idx < 0 || idx >= allHands.size() ? null : allHands.get(idx);
    }

    /**
     * 
     * Fires up trample dialog.  Very old code, due for refactoring with new UI.
     * Could possibly move to view.
     * 
     * @param attacker &emsp; {@link forge.game.card.Card}
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
        if (!overrideOrder && !attacker.hasKeyword("Deathtouch") && firstBlocker.getLethalDamage() >= damage) {
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
        PhaseLabel label = vf.getPhaseIndicator().getLabelFor(phase);
        return label == null || label.getEnabled();
    }

    public void setCard(final Card c) {
        FThreads.assertExecutedByEdt(true);
        setCard(c, false);
    }

    public void setCard(final Card c, final boolean showFlipped) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showCard(c, showFlipped);
    }

    public void setCard(final InventoryItem c) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showImage(c);
    }
    
    private int getPlayerIndex(Player player) {
        return sortedPlayers.indexOf(player);
    }

    public void showCombat(Combat combat) {
        if (combat != null && combat.getAttackers().size() > 0 && combat.getAttackingPlayer().getGame().getStack().isEmpty()) {
        	if (selectedDocBeforeCombat == null) {
	        	IVDoc<? extends ICDoc> combatDoc = EDocID.REPORT_COMBAT.getDoc();
	        	if (combatDoc.getParentCell() != null) {
	            	selectedDocBeforeCombat = combatDoc.getParentCell().getSelected();
	            	if (selectedDocBeforeCombat != combatDoc) {
	            		SDisplayUtil.showTab(combatDoc);
	            	}
	            	else {
	            		selectedDocBeforeCombat = null; //don't need to cache combat doc this way
	            	}
	        	}
        	}
        }
        else if (selectedDocBeforeCombat != null) { //re-select doc that was selected before once combat finished
        	SDisplayUtil.showTab(selectedDocBeforeCombat);
        	selectedDocBeforeCombat = null;
        }
        CCombat.SINGLETON_INSTANCE.setModel(combat);
        CCombat.SINGLETON_INSTANCE.update();
    } // showBlockers()

    Set<Player> highlightedPlayers = new HashSet<Player>();
    public void setHighlighted(Player ge, boolean b) {
        if (b) highlightedPlayers.add(ge);
        else highlightedPlayers.remove(ge);
    }

    public boolean isHighlighted(Player player) {
        return highlightedPlayers.contains(player);
    }

    Set<Card> highlightedCards = new HashSet<Card>();
    // used to highlight cards in UI
    public void setUsedToPay(Card card, boolean value) {
        FThreads.assertExecutedByEdt(true);

        boolean hasChanged = value ? highlightedCards.add(card) : highlightedCards.remove(card);
        if (hasChanged) { // since we are in UI thread, may redraw the card right now
            updateSingleCard(card);
        }
    }

    public boolean isUsedToPay(Card card) {
        return highlightedCards.contains(card);
    }

    public void updateZones(List<Pair<Player, ZoneType>> zonesToUpdate) {
        //System.out.println("updateZones " + zonesToUpdate);
        for (Pair<Player, ZoneType> kv : zonesToUpdate) {
            Player owner = kv.getKey();
            ZoneType zt = kv.getValue();

            if (zt == ZoneType.Command) {
                getCommandFor(owner).getTabletop().setupPlayZone();
            } else if (zt == ZoneType.Hand) {
                VHand vHand = getHandFor(owner);
                if (null != vHand) {
                    vHand.getLayoutControl().updateHand();
                }
                getFieldViewFor(owner).getDetailsPanel().updateZones();
            } else if (zt == ZoneType.Battlefield) {
                getFieldViewFor(owner).getTabletop().setupPlayZone();
            } else if (zt == ZoneType.Ante) {
                CAntes.SINGLETON_INSTANCE.update();
            } else {
                getFieldViewFor(owner).getDetailsPanel().updateZones();
            }
        }
    }

    // Player's mana pool changes
    public void updateManaPool(List<Player> manaPoolUpdate) {
        for (Player p : manaPoolUpdate) {
            getFieldViewFor(p).getDetailsPanel().updateManaPool();
        }

    }

    // Player's lives and poison counters
    public void updateLives(List<Player> livesUpdate) {
        for (Player p : livesUpdate) {
            getFieldViewFor(p).updateDetails();
        }

    }

    public void updateCards(Set<Card> cardsToUpdate) {
        for (Card c : cardsToUpdate) {
            updateSingleCard(c);
        }
    }

    public void updateSingleCard(Card c) {
        Zone zone = c.getZone();
        if (zone != null && zone.getZoneType() == ZoneType.Battlefield) {
            PlayArea pa = getFieldViewFor(zone.getPlayer()).getTabletop();
            pa.updateCard(c, false);
        }
    }

    public void refreshCardDetails(Collection<Card> cards) {
        for (Card c : cards) {
            Zone zone = c.getZone();
            if (zone != null && zone.getZoneType() == ZoneType.Battlefield) {
                PlayArea pa = getFieldViewFor(zone.getPlayer()).getTabletop();
                CardPanel pnl = pa.getCardPanel(c.getUniqueNumber());
                if (pnl != null) {
                    pnl.updatePTOverlay();
                }
            }
        }
    }

    private final static boolean LOG_UIEVENTS = false;

    // UI-related events should arrive here
    public void fireEvent(UiEvent uiEvent) {
        if (LOG_UIEVENTS) {
            System.out.println("UI: " + uiEvent.toString()  + " \t\t " + FThreads.debugGetStackTraceItem(4, true));
        }
        uiEvents.post(uiEvent);
    }

    public class MatchUiEventVisitor implements IUiEventVisitor<Void> {
        @Override
        public Void visit(UiEventBlockerAssigned event) {
            updateSingleCard(event.blocker);
            return null;
        }

        @Override
        public Void visit(UiEventAttackerDeclared event) {
            updateSingleCard(event.attacker);
            return null;
        }

        @Subscribe
        public void receiveEvent(UiEvent evt) {
            evt.visit(this);
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        return new CMatchUIMenus().getMenus();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        Singletons.getControl().getForgeMenu().setProvider(this);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() { }

    public void repaintCardOverlays() {
        List<CardPanel> panels = getVisibleCardPanels();
        for (CardPanel panel : panels) {
            panel.repaintOverlays();
        }
    }
    private List<CardPanel> getVisibleCardPanels() {
        List<CardPanel> panels = new ArrayList<CardPanel>();
        for (VHand h : view.getHands()) {
            panels.addAll(h.getHandArea().getCardPanels());
        }
        for (VField f : view.getFieldViews()) {
            panels.addAll(f.getTabletop().getCardPanels());
        }
        for (VCommand c : view.getCommandViews()) {
            panels.addAll(c.getTabletop().getCardPanels());
        }
        return panels;
    }

    /** Concede game, bring up WinLose UI. */
    public void concede() {
        if (FOverlay.SINGLETON_INSTANCE.getPanel().isShowing() || QuestDraftUtils.aiMatchInProgress) {
            return;
        }

        Singletons.getControl().ensureScreenActive(FScreen.MATCH_SCREEN);

        String userPrompt =
                "This will end the current game and you will not be able to resume.\n\n" +
                        "Concede anyway?";
        if (FOptionPane.showConfirmDialog(userPrompt, "Concede Game?", "Concede", "Cancel", false)) {
            Singletons.getControl().stopGame();
        }
    }
}
