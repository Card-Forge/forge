package forge;

import java.util.ArrayList;
import java.util.Iterator;

import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * PlayerZone_ComesIntoPlay class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class PlayerZoneComesIntoPlay extends DefaultPlayerZone {
    /** Constant <code>serialVersionUID=5750837078903423978L</code>. */
    private static final long serialVersionUID = 5750837078903423978L;

    private boolean trigger = true;
    private boolean leavesTrigger = true;

    /**
     * <p>
     * Constructor for PlayerZone_ComesIntoPlay.
     * </p>
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.Player} object.
     */
    public PlayerZoneComesIntoPlay(final Constant.Zone zone, final Player player) {
        super(zone, player);
    }

    /** {@inheritDoc} */
    @Override
    public final void add(final Object o) {
        if (o == null) {
            throw new RuntimeException("PlayerZone_ComesInto Play : add() object is null");
        }

        super.add(o);

        final Card c = (Card) o;
        final Player player = c.getController();

        if (trigger
                && ((CardFactoryUtil.oppHasKismet(c.getController())
                        && (c.isLand() || c.isCreature() || c.isArtifact()))
                        || (AllZoneUtil.isCardInPlay("Urabrask the Hidden", c.getController().getOpponent()) && c
                                .isCreature())
                        || (AllZoneUtil.isCardInPlay("Root Maze") && (c.isLand() || c.isArtifact())) || (AllZoneUtil
                        .isCardInPlay("Orb of Dreams") && c.isPermanent()))) {
            // it enters the battlefield this way, and should not fire triggers
            c.setTapped(true);
        }

        // cannot use addComesIntoPlayCommand - trigger might be set to false;
        // Keep track of max lands can play per turn
        int addMax = 0;

        boolean adjustLandPlays = false;
        boolean eachPlayer = false;

        if (c.getName().equals("Exploration") || c.getName().equals("Oracle of Mul Daya")) {
            addMax = 1;
            adjustLandPlays = true;
        } else if (c.getName().equals("Azusa, Lost but Seeking")) {
            addMax = 2;
            adjustLandPlays = true;
        } else if (c.getName().equals("Storm Cauldron") || c.getName().equals("Rites of Flourishing")) {
            // these two aren't in yet, but will just need the other part of the
            // card to work with more lands
            adjustLandPlays = true;
            eachPlayer = true;
            addMax = 1;
        }
        // 7/13: fastbond code removed, fastbond should be unlimited and will be
        // handled elsewhere.

        if (adjustLandPlays) {
            if (eachPlayer) {
                AllZone.getHumanPlayer().addMaxLandsToPlay(addMax);
                AllZone.getComputerPlayer().addMaxLandsToPlay(addMax);
            } else {
                c.getController().addMaxLandsToPlay(addMax);
            }
        }

        if (trigger) {
            c.setSickness(true); // summoning sickness
            c.comesIntoPlay();

            if (c.isLand()) {
                CardList list = c.getController().getCardsIn(Zone.Battlefield);

                /*
                 * CardList listValakut = list.filter(new CardListFilter() {
                 * public boolean addCard(Card c) { return
                 * c.getName().contains("Valakut, the Molten Pinnacle"); } });
                 */

                list = list.filter(new CardListFilter() {
                    public boolean addCard(final Card c) {
                        return c.hasKeyword("Landfall")
                                || c.hasKeyword("Landfall - Whenever a land enters the battlefield under your control, "
                                        + "CARDNAME gets +2/+2 until end of turn.");
                    }
                });

                for (int i = 0; i < list.size(); i++) {
                    GameActionUtil.executeLandfallEffects(list.get(i));
                }
                /*
                 * // Check for a mountain if (!listValakut.isEmpty() &&
                 * c.isType("Mountain") ) { for (int i = 0; i <
                 * listValakut.size(); i++) { boolean b =
                 * GameActionUtil.executeValakutEffect(listValakut.get(i),c); if
                 * (!b) { // Not enough mountains to activate Valakut -- stop
                 * the loop break; } } }
                 */

                // Tectonic Instability
                CardList tis = AllZoneUtil.getCardsIn(Zone.Battlefield, "Tectonic Instability");
                final Card tisLand = c;
                for (Card ti : tis) {
                    final Card source = ti;
                    SpellAbility ability = new Ability(source, "") {
                        @Override
                        public void resolve() {
                            CardList lands = tisLand.getController().getCardsIn(Zone.Battlefield);
                            lands = lands.filter(CardListFilter.LANDS);
                            for (Card land : lands) {
                                land.tap();
                            }
                        }
                    };
                    StringBuilder sb = new StringBuilder();
                    sb.append(source).append(" - tap all lands ");
                    sb.append(tisLand.getController()).append(" controls.");
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }

                CardList les = c.getOwner().getOpponent().getCardsIn(Zone.Battlefield, "Land Equilibrium");
                final Card lesLand = c;
                if (les.size() > 0) {
                    final Card source = les.get(0);
                    SpellAbility ability = new Ability(source, "") {
                        @Override
                        public void resolve() {
                            CardList lands = AllZoneUtil.getPlayerLandsInPlay(lesLand.getOwner());
                            lesLand.getOwner().sacrificePermanent(source.getName() + " - Select a land to sacrifice",
                                    lands);
                        }
                    };
                    StringBuilder sb = new StringBuilder();
                    sb.append(source).append(" - ");
                    sb.append(tisLand.getController()).append(" sacrifices a land.");
                    ability.setStackDescription(sb.toString());
                    CardList pLands = AllZoneUtil.getPlayerLandsInPlay(lesLand.getOwner());
                    CardList oLands = AllZoneUtil.getPlayerLandsInPlay(lesLand.getOwner().getOpponent());
                    // (pLands - 1) because this land is in play, and the
                    // ability is before it is in play
                    if (oLands.size() <= (pLands.size() - 1)) {
                        AllZone.getStack().addSimultaneousStackEntry(ability);

                    }
                }

            } // isLand()
        }

        if (AllZone.getStaticEffects().getCardToEffectsList().containsKey(c.getName())) {
            String[] effects = AllZone.getStaticEffects().getCardToEffectsList().get(c.getName());
            for (String effect : effects) {
                AllZone.getStaticEffects().addStateBasedEffect(effect);
            }
        }

        CardList meek = player.getCardsIn(Zone.Graveyard, "Sword of the Meek");

        if (meek.size() > 0 && c.isCreature() && c.getNetAttack() == 1 && c.getNetDefense() == 1) {
            for (int i = 0; i < meek.size(); i++) {
                final Card crd = meek.get(i);

                Ability ability = new Ability(meek.get(i), "0") {
                    @Override
                    public void resolve() {
                        if (crd.getController().isHuman()) {
                            if (GameActionUtil.showYesNoDialog(crd, "Attach " + crd + " to " + c + "?")) {
                                if (player.getZone(Zone.Graveyard).contains(crd) && AllZoneUtil.isCardInPlay(c)
                                        && c.isCreature() && c.getNetAttack() == 1 && c.getNetDefense() == 1) {
                                    AllZone.getGameAction().moveToPlay(crd);

                                    crd.equipCard(c);
                                }
                            }

                        } else {
                            if (player.getZone(Zone.Graveyard).contains(crd) && AllZoneUtil.isCardInPlay(c)
                                    && c.isCreature() && c.getNetAttack() == 1 && c.getNetDefense() == 1) {
                                AllZone.getGameAction().moveToPlay(crd);

                                crd.equipCard(c);
                            }
                        }
                    }
                };

                StringBuilder sb = new StringBuilder();
                sb.append(crd);
                sb.append(" - Whenever a 1/1 creature enters the battlefield under your control, you may ");
                sb.append("return Sword of the Meek from your graveyard to the battlefield, ");
                sb.append("then attach it to that creature.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }

    } // end add()

    /** {@inheritDoc} */
    @Override
    public final void remove(final Object o) {

        super.remove(o);

        Card c = (Card) o;

        // Keep track of max lands can play per turn
        int addMax = 0;

        boolean adjustLandPlays = false;
        boolean eachPlayer = false;

        if (c.getName().equals("Exploration") || c.getName().equals("Oracle of Mul Daya")) {
            addMax = -1;
            adjustLandPlays = true;
        } else if (c.getName().equals("Azusa, Lost but Seeking")) {
            addMax = -2;
            adjustLandPlays = true;
        } else if (c.getName().equals("Storm Cauldron") || c.getName().equals("Rites of Flourishing")) {
            // once their second half of their abilities are programmed these
            // two can be added in
            adjustLandPlays = true;
            eachPlayer = true;
            addMax = -1;
        }
        // 7/12: fastbond code removed, fastbond should be unlimited and will be
        // handled elsewhere.

        if (adjustLandPlays) {
            if (eachPlayer) {
                AllZone.getHumanPlayer().addMaxLandsToPlay(addMax);
                AllZone.getComputerPlayer().addMaxLandsToPlay(addMax);
            } else {
                c.getController().addMaxLandsToPlay(addMax);
            }
        }

        if (leavesTrigger) {
            c.leavesPlay();
        }

        if (AllZone.getStaticEffects().getCardToEffectsList().containsKey(c.getName())) {
            String[] effects = AllZone.getStaticEffects().getCardToEffectsList().get(c.getName());
            String tempEffect = "";
            for (String effect : effects) {
                tempEffect = effect;
                AllZone.getStaticEffects().removeStateBasedEffect(effect);
                Command comm = GameActionUtil.getCommands().get(tempEffect); // this
                                                                        // is to
                                                                        // make
                                                                        // sure
                                                                        // cards
                                                                        // reset
                                                                        // correctly
                comm.execute();
            }

        }

        for (String effect : AllZone.getStaticEffects().getStateBasedMap().keySet()) {
            Command com = GameActionUtil.getCommands().get(effect);
            com.execute();
        }

    }

    /**
     * <p>
     * Setter for the field <code>trigger</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setTrigger(final boolean b) {
        trigger = b;
    }

    /**
     * <p>
     * Setter for the field <code>leavesTrigger</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setLeavesTrigger(final boolean b) {
        leavesTrigger = b;
    }

    /**
     * <p>
     * setTriggers.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setTriggers(final boolean b) {
        trigger = b;
        leavesTrigger = b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.DefaultPlayerZone#getCards(boolean)
     */
    @Override
    public final Card[] getCards(final boolean filter) {
        // Battlefield filters out Phased Out cards by default. Needs to call
        // getCards(false) to get Phased Out cards
        Card[] c;
        if (!filter) {
            c = new Card[getCardList().size()];
            getCardList().toArray(c);
        } else {
            Iterator<Card> itr = getCardList().iterator();
            ArrayList<Card> list = new ArrayList<Card>();
            while (itr.hasNext()) {
                Card crd = itr.next();
                if (!crd.isPhasedOut()) {
                    list.add(crd);
                }
            }
            c = new Card[list.size()];
            list.toArray(c);
        }
        return c;
    }
}
