package forge.card.spellability;

import forge.*;
import forge.Constant.Zone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.trigger.Trigger;
import forge.gui.input.Input;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * <p>Spell_Permanent class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Spell_Permanent extends Spell {
    /** Constant <code>serialVersionUID=2413495058630644447L</code> */
    private static final long serialVersionUID = 2413495058630644447L;

    private boolean willChampion = false;
    private String championValid = null;
    private String championValidDesc = "";


    final Input championInputComes = new Input() {
        private static final long serialVersionUID = -7503268232821397107L;

        @Override
        public void showMessage() {
            CardList choice = (CardList) championGetCreature.execute();

            stopSetNext(CardFactoryUtil.input_targetChampionSac(getSourceCard(), championAbilityComes, choice,
                    "Select another " + championValidDesc + " you control to exile", false, false));
            ButtonUtil.disableAll(); //target this card means: sacrifice this card
        }
    };

    private final CommandReturn championGetCreature = new CommandReturn() {
        public Object execute() {
            CardList cards = getSourceCard().getController().getCardsIn(Zone.Battlefield);
            return cards.getValidCards(championValid, getSourceCard().getController(), getSourceCard());
        }
    };//CommandReturn

    final SpellAbility championAbilityComes = new Ability(getSourceCard(), "0") {
        @Override
        public void resolve() {

            Card source = getSourceCard();
            Player controller = source.getController();

            CardList creature = (CardList) championGetCreature.execute();
            if (creature.size() == 0) {
                AllZone.getGameAction().sacrifice(source);
                return;
            } else if (controller.isHuman()) {
                AllZone.getInputControl().setInput(championInputComes);
            } else { //Computer
                CardList computer = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                computer = computer.getValidCards(championValid, controller, source);
                computer.remove(source);

                computer.shuffle();
                if (computer.size() != 0) {
                    Card c = computer.get(0);
                    source.setChampionedCard(c);
                    if (AllZoneUtil.isCardInPlay(c)) {
                        AllZone.getGameAction().exile(c);
                    }

                    //Run triggers
                    HashMap<String, Object> runParams = new HashMap<String, Object>();
                    runParams.put("Card", source);
                    runParams.put("Championed", source.getChampionedCard());
                    AllZone.getTriggerHandler().runTrigger("Championed", runParams);
                } else
                    AllZone.getGameAction().sacrifice(getSourceCard());
            }//computer
        }//resolve()
    };

    Command championCommandComes = new Command() {

        private static final long serialVersionUID = -3580408066322945328L;

        public void execute() {
            StringBuilder sb = new StringBuilder();
            sb.append(getSourceCard()).append(" - When CARDNAME enters the battlefield, sacrifice it unless you exile another Faerie you control.");
            championAbilityComes.setStackDescription(sb.toString());
            AllZone.getStack().addSimultaneousStackEntry(championAbilityComes);
        }//execute()
    };//championCommandComes

    Command championCommandLeavesPlay = new Command() {

        private static final long serialVersionUID = -5903638227914705191L;

        public void execute() {

            SpellAbility ability = new Ability(getSourceCard(), "0") {
                @Override
                public void resolve() {
                    Card c = getSourceCard().getChampionedCard();
                    if (c != null && !c.isToken() && AllZoneUtil.isCardExiled(c)) {
                        AllZone.getGameAction().moveToPlay(c);
                    }
                }//resolve()
            };//SpellAbility

            StringBuilder sb = new StringBuilder();
            sb.append(getSourceCard()).append(" - When CARDNAME leaves the battlefield, exiled card returns to the battlefield.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);
        }//execute()
    };//championCommandLeavesPlay

    ///////
    ////////////////////

    /**
     * <p>Constructor for Spell_Permanent.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     */
    public Spell_Permanent(Card sourceCard) {
        // Add Costs for all SpellPermanents
        this(sourceCard, new Cost(sourceCard.getManaCost(), sourceCard.getName(), false), null);
    }//Spell_Permanent()

    /**
     * <p>Constructor for Spell_Permanent.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param cost a {@link forge.card.cost.Cost} object.
     * @param tgt a {@link forge.card.spellability.Target} object.
     */
    public Spell_Permanent(Card sourceCard, Cost cost, Target tgt) {
    	this(sourceCard, cost, tgt, true);
    }//Spell_Permanent()
    
    public Spell_Permanent(Card sourceCard, Cost cost, Target tgt, boolean setDesc) {
        super(sourceCard, cost, tgt);

        if (CardFactoryUtil.hasKeyword(sourceCard, "Champion") != -1) {
            int n = CardFactoryUtil.hasKeyword(sourceCard, "Champion");

            String toParse = sourceCard.getKeyword().get(n).toString();
            String parsed[] = toParse.split(":");
            willChampion = true;
            championValid = parsed[1];
            if (parsed.length > 2) {
                championValidDesc = parsed[2];
            } else championValidDesc = championValid;
        }

        if (sourceCard.isCreature()) {

            StringBuilder sb = new StringBuilder();
            sb.append(sourceCard.getName()).append(" - Creature ").append(sourceCard.getNetAttack());
            sb.append(" / ").append(sourceCard.getNetDefense());
            setStackDescription(sb.toString());
        } else setStackDescription(sourceCard.getName());

        if (setDesc)
        	setDescription(getStackDescription());
        
        if (willChampion) {
            sourceCard.addComesIntoPlayCommand(championCommandComes);
            sourceCard.addLeavesPlayCommand(championCommandLeavesPlay);
        }

    }//Spell_Permanent()

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        Card source = getSourceCard();
        if (AllZone.getStack().isSplitSecondOnStack() || source.isUnCastable()) return false;

        Player turn = AllZone.getPhase().getPlayerTurn();

        if (source.getName().equals("Serra Avenger")) {
            if (turn.equals(source.getController()) && turn.getTurn() <= 3)
                return false;
        } else if (source.getName().equals("Blizzard")) {
            CardList lands = AllZoneUtil.getPlayerLandsInPlay(source.getController());
            lands = lands.getType("Snow");
            if (lands.size() == 0) return false;
        }

        // Flash handled by super.canPlay
        return super.canPlay();
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlayAI() {

        Card card = getSourceCard();

        //check on legendary
        if (card.isType("Legendary")) {
            CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
            if (list.containsName(card.getName()))
                return false;
        }
        if (card.isPlaneswalker()) {
            CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
            list = list.getType("Planeswalker");

            for (int i = 0; i < list.size(); i++) {
                String subtype = card.getType().get(card.getType().size() - 1);
                CardList cl = list.getType(subtype);

                if (cl.size() > 0) {
                    return false;
                }
            }
        }
        if (card.isType("World")) {
            CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
            list = list.getType("World");
            if (list.size() > 0) return false;
        }

        if (card.isCreature()
                && card.getNetDefense() <= 0
                && !card.hasStartOfKeyword("etbCounter")
                && !card.getText().contains("Modular"))
            return false;

        if (willChampion) {
            Object o = championGetCreature.execute();
            if (o == null) return false;

            CardList cl = (CardList) championGetCreature.execute();
            if ((o == null) || !(cl.size() > 0) || !AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand))
                return false;
        }

        if (!checkETBEffects(card, this, null))
            return false;

        return super.canPlayAI();
    }//canPlayAI()

    /**
     * <p>checkETBEffects.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param api a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean checkETBEffects(Card card, SpellAbility sa, String api) {
        // Trigger play improvements
        ArrayList<Trigger> triggers = card.getTriggers();
        for (Trigger tr : triggers) {
            // These triggers all care for ETB effects

            HashMap<String, String> params = tr.getMapParams();
            if (!params.get("Mode").equals("ChangesZone"))
                continue;

            if (!params.get("Destination").equals("Battlefield"))
                continue;

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self"))
                continue;

            if (!tr.requirementsCheck())
                continue;

            if (tr.getOverridingAbility() != null)    // Don't look at Overriding Abilities yet
                continue;

            // Maybe better considerations
            AbilityFactory af = new AbilityFactory();
            SpellAbility exSA = af.getAbility(card.getSVar(params.get("Execute")), card);

            if (api != null && !af.getAPI().equals(api))
                continue;

            exSA.setActivatingPlayer(sa.getActivatingPlayer());

            // Run non-mandatory trigger.
            // These checks only work if the Executing SpellAbility is an Ability_Sub.
            if (exSA instanceof Ability_Sub && !exSA.doTrigger(false)) {
                // AI would not run this trigger if given the chance

                // if trigger is mandatory, return false
                if (params.get("OptionalDecider") == null) {
                    return false;
                }
                // else
                // otherwise, return false 50% of the time?
            }
        }

        return true;
    }


    /** {@inheritDoc} */
    @Override
    public void resolve() {
        Card c = getSourceCard();
        AllZone.getGameAction().moveToPlay(c);
    }
}
