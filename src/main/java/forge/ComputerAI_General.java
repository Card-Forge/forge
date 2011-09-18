package forge;

import com.esotericsoftware.minlog.Log;

import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Spell_Permanent;

import java.util.ArrayList;

import static forge.error.ErrorViewer.showError;


/**
 * <p>ComputerAI_General class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class ComputerAI_General implements Computer {

    /**
     * <p>Constructor for ComputerAI_General.</p>
     */
    public ComputerAI_General() {

    }

    /**
     * <p>main1.</p>
     */
    public final void main1() {
        ComputerUtil.chooseLandsToPlay();

        if (AllZone.getStack().size() == 0) {
            playCards(Constant.Phase.Main1);
        } else {
            stackResponse();
        }
    } //main1()

    /**
     * <p>main2.</p>
     */
    public final void main2() {
        ComputerUtil.chooseLandsToPlay();

        if (AllZone.getStack().size() == 0) {
            playCards(Constant.Phase.Main2);
        } else {
            stackResponse();
        }
    }

    /**
     * <p>playCards.</p>
     *
     * @param phase a {@link java.lang.String} object.
     */
    private void playCards(final String phase) {
        SpellAbility[] sp = phase.equals(Constant.Phase.Main1) ? getMain1() : getMain2();

        boolean nextPhase = ComputerUtil.playCards(sp);

        if (nextPhase) {
            AllZone.getPhase().passPriority();
        }
    } //playCards()

    /**
     * <p>getMain1.</p>
     *
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    private SpellAbility[] getMain1() {
        //Card list of all cards to consider
        CardList hand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);

        if (AllZone.getComputerManaPool().isEmpty()) {
            hand = hand.filter(new CardListFilter() {
                public boolean addCard(final Card c) {

                    if (c.getSVar("PlayMain1").equals("TRUE")) {
                        return true;
                    }

                    //timing should be handled by the AF's
                    if (c.isSorcery() || c.isAura()) {
                        return true;
                    }

                    if (c.isCreature() && (c.hasKeyword("Haste")) || c.hasKeyword("Exalted")) {
                        return true;
                    }

                    //get all cards the computer controls with BuffedBy
                    CardList buffed = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    for (int j = 0; j < buffed.size(); j++) {
                        Card buffedcard = buffed.get(j);
                        if (buffedcard.getSVar("BuffedBy").length() > 0) {
                            String buffedby = buffedcard.getSVar("BuffedBy");
                            String[] bffdby = buffedby.split(",");
                            if (c.isValidCard(bffdby, c.getController(), c)) {
                                return true;
                            }
                        }
                    } //BuffedBy

                    //get all cards the human controls with AntiBuffedBy
                    CardList antibuffed = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                    for (int k = 0; k < antibuffed.size(); k++) {
                        Card buffedcard = antibuffed.get(k);
                        if (buffedcard.getSVar("AntiBuffedBy").length() > 0) {
                            String buffedby = buffedcard.getSVar("AntiBuffedBy");
                            String[] bffdby = buffedby.split(",");
                            if (c.isValidCard(bffdby, c.getController(), c)) {
                                return true;
                            }
                        }
                    } //AntiBuffedBy

                    if (c.isLand()) {
                        return false;
                    }

                    CardList vengevines = AllZone.getComputerPlayer().getCardsIn(Zone.Graveyard, "Vengevine");
                    if (vengevines.size() > 0) {
                        CardList creatures = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                        CardList creatures2 = new CardList();
                        for (int i = 0; i < creatures.size(); i++) {
                            if (creatures.get(i).isCreature()
                                    && CardUtil.getConvertedManaCost(creatures.get(i).getManaCost()) <= 3) {
                                creatures2.add(creatures.get(i));
                            }
                        }
                        if (creatures2.size() + Phase.getComputerStartingCardspellCount() > 1
                                && c.isCreature()
                                && CardUtil.getConvertedManaCost(c.getManaCost()) <= 3) {
                            return true;
                        }
                    } // AI Improvement for Vengevine
                    // Beached As End
                    return false;
                }
            });
        }
        CardList all = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
        all.addAll(hand);

        CardList humanPlayable = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        humanPlayable = humanPlayable.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return (c.canAnyPlayerActivate());
            }
        });

        all.addAll(humanPlayable);

        return getPlayable(all);
    } //getMain1()


    /**
     * <p>getMain2.</p>
     *
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    private SpellAbility[] getMain2() {
        //Card list of all cards to consider
        CardList all = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
        //Don't play permanents with Flash before humans declare attackers step
        all = all.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                if (c.isPermanent()
                        && c.hasKeyword("Flash")
                        && (AllZone.getPhase().isPlayerTurn(AllZone.getComputerPlayer())
                        || AllZone.getPhase().isBefore(Constant.Phase.Combat_Declare_Attackers_InstantAbility)))
                {
                    return false;
                }
                return true;
            }
        });
        all.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield));
        all.addAll(CardFactoryUtil.getExternalZoneActivationCards(AllZone.getComputerPlayer()));

        // Prevent the computer from summoning Ball Lightning type creatures during main phase 2
        all = all.getNotKeyword("At the beginning of the end step, sacrifice CARDNAME.");

        all = all.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                if (c.isLand()) {
                    return false;
                }
                return true;
            }
        });

        CardList humanPlayable = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        humanPlayable = humanPlayable.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return (c.canAnyPlayerActivate());
            }
        });
        all.addAll(humanPlayable);

        return getPlayable(all);
    } //getMain2()

    /**
     * <p>getAvailableSpellAbilities.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    private CardList getAvailableSpellAbilities() {
        CardList all = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
        //Don't play permanents with Flash before humans declare attackers step
        all = all.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                if (c.isPermanent()
                        && c.hasKeyword("Flash")
                        && (AllZone.getPhase().isPlayerTurn(AllZone.getComputerPlayer())
                        || AllZone.getPhase().isBefore(Constant.Phase.Combat_Declare_Attackers_InstantAbility)))
                {
                    return false;
                }
                return true;
            }
        });
        all.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield));
        all.addAll(CardFactoryUtil.getExternalZoneActivationCards(AllZone.getComputerPlayer()));


        CardList humanPlayable = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        humanPlayable = humanPlayable.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return (c.canAnyPlayerActivate());
            }
        });
        all.addAll(humanPlayable);
        return all;
    }

    /**
     * <p>getOtherPhases.</p>
     *
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    private SpellAbility[] getOtherPhases() {
        return getPlayable(getAvailableSpellAbilities());
    }

    /**
     * <p>getPossibleCounters.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getPossibleCounters() {
        return getPlayableCounters(getAvailableSpellAbilities());
    }

    /**
     * <p>getPossibleETBCounters.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getPossibleETBCounters() {
        return getETBCounters(getAvailableSpellAbilities());
    }

    /**
     * Returns the spellAbilities from the card list that the computer is able to play.
     *
     * @param l a {@link forge.CardList} object.
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    private SpellAbility[] getPlayable(final CardList l) {
        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for (Card c : l) {
            for (SpellAbility sa : c.getSpellAbility()) {
                // if SA is from AF_Counter don't add to getPlayable
                //This try/catch should fix the "computer is thinking" bug
                try {
                    sa.setActivatingPlayer(AllZone.getComputerPlayer());
                    if (ComputerUtil.canBePlayedAndPayedByAI(sa)) {
                        spellAbility.add(sa);
                    }
                } catch (Exception ex) {
                    showError(ex, "There is an error in the card code for %s:%n", c.getName(), ex.getMessage());
                }
            }
        }
        return spellAbility.toArray(new SpellAbility[spellAbility.size()]);
    }

    /**
     * <p>getPlayableCounters.</p>
     *
     * @param l a {@link forge.CardList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getPlayableCounters(final CardList l) {
        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for (Card c : l) {
            for (SpellAbility sa : c.getSpellAbility()) {
                // Check if this AF is a Counterpsell
                if (sa.getAbilityFactory() != null && sa.getAbilityFactory().getAPI().equals("Counter")) {
                    spellAbility.add(sa);
                }
            }
        }

        return spellAbility;
    }

    /**
     * <p>getETBCounters.</p>
     *
     * @param l a {@link forge.CardList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getETBCounters(final CardList l) {
        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for (Card c : l) {
            for (SpellAbility sa : c.getSpellAbility()) {
                // Or if this Permanent has an ETB ability with Counter
                if (sa instanceof Spell_Permanent) {
                    if (Spell_Permanent.checkETBEffects(c, sa, "Counter")) {
                        spellAbility.add(sa);
                    }
                }
            }
        }

        return spellAbility;
    }

    /**
     * <p>begin_combat.</p>
     */
    public final void begin_combat() {
        stackResponse();
    }

    /**
     * <p>declare_attackers.</p>
     */
    public final void declare_attackers() {
        // 12/2/10(sol) the decision making here has moved to getAttackers()

        AllZone.setCombat(ComputerUtil.getAttackers());

        Card[] att = AllZone.getCombat().getAttackers();
        if (att.length > 0) {
            AllZone.getPhase().setCombat(true);
        }

        for (int i = 0; i < att.length; i++) {
            // tapping of attackers happens after Propaganda is paid for
            //if (!att[i].hasKeyword("Vigilance")) att[i].tap();
            Log.debug("Computer just assigned " + att[i].getName() + " as an attacker.");
        }

        AllZone.getComputerPlayer().getZone(Zone.Battlefield).updateObservers();
        CombatUtil.showCombat();

        AllZone.getPhase().setNeedToNextPhase(true);
    }

    /**
     * <p>declare_attackers_after.</p>
     */
    public final void declare_attackers_after() {
        stackResponse();
    }

    /**
     * <p>declare_blockers.</p>
     */
    public final void declare_blockers() {
        CardList blockers = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());

        AllZone.setCombat(ComputerUtil_Block2.getBlockers(AllZone.getCombat(), blockers));

        CombatUtil.showCombat();

        AllZone.getPhase().setNeedToNextPhase(true);
    }

    /**
     * <p>declare_blockers_after.</p>
     */
    public final void declare_blockers_after() {
        stackResponse();
    }

    /**
     * <p>end_of_combat.</p>
     */
    public final void end_of_combat() {
        stackResponse();
    }

    //end of Human's turn
    /**
     * <p>end_of_turn.</p>
     */
    public final void end_of_turn() {
        stackResponse();
    }

    /**
     * <p>stack_not_empty.</p>
     */
    public final void stack_not_empty() {
        stackResponse();
    }

    /**
     * <p>stackResponse.</p>
     */
    public final void stackResponse() {
        // if top of stack is empty
        SpellAbility[] sas = null;
        if (AllZone.getStack().size() == 0) {
            sas = getOtherPhases();

            boolean pass = (sas.length == 0)
                    || AllZone.getPhase().is(Constant.Phase.Upkeep, AllZone.getComputerPlayer())
                    || AllZone.getPhase().is(Constant.Phase.Draw, AllZone.getComputerPlayer())
                    || AllZone.getPhase().is(Constant.Phase.End_Of_Turn, AllZone.getComputerPlayer());
            if (!pass) {        // Each AF should check the phase individually
                pass = ComputerUtil.playCards(sas);
            }

            if (pass) {
                AllZone.getPhase().passPriority();
            }
            return;
        }

        // if top of stack is owned by me
        if (AllZone.getStack().peekInstance().getActivatingPlayer().isComputer()) {
            // probably should let my stuff resolve to force Human to respond to it
            AllZone.getPhase().passPriority();
            return;
        }

        // top of stack is owned by human,
        ArrayList<SpellAbility> possibleCounters = getPossibleCounters();

        if (possibleCounters.size() > 0 && ComputerUtil.playCounterSpell(possibleCounters)) {
            // Responding CounterSpell is on the Stack trying to Counter the Spell
            // If playCounterSpell returns true, a Spell is hitting the Stack
            return;
        }

        possibleCounters.clear();
        possibleCounters = getPossibleETBCounters();
        if (possibleCounters.size() > 0 && !ComputerUtil.playCards(possibleCounters)) {
            // Responding Permanent w/ ETB Counter is on the Stack
            // AllZone.getPhase().passPriority();
            return;
        }

        sas = getOtherPhases();
        if (sas.length > 0) {
            // Spell not Countered
            if (!ComputerUtil.playCards(sas)) {
                return;
            }
        }
        // if this hasn't been covered above, just PassPriority()
        AllZone.getPhase().passPriority();
    }
}
