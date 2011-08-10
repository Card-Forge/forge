package forge;

import forge.card.cardFactory.CardFactoryUtil;
import forge.card.trigger.Trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

//doesHumanAttackAndWin() uses the global variable AllZone.getComputerPlayer()
/**
 * <p>ComputerUtil_Attack2 class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class ComputerUtil_Attack2 {

    //possible attackers and blockers
    private CardList attackers;
    private CardList blockers;
    private CardList playerCreatures;
    private int blockerLife;

    private Random random = MyRandom.random;
    private final int randomInt = random.nextInt();

    private CardList humanList;   //holds human player creatures
    private CardList computerList;//holds computer creatures

    private int aiAggression = 0; // added by Masher, how aggressive the ai attack will be depending on circumstances

    /**
     * <p>Constructor for ComputerUtil_Attack2.</p>
     *
     * @param possibleAttackers an array of {@link forge.Card} objects.
     * @param possibleBlockers an array of {@link forge.Card} objects.
     * @param blockerLife a int.
     */
    public ComputerUtil_Attack2(Card[] possibleAttackers, Card[] possibleBlockers, int blockerLife) {
        this(new CardList(possibleAttackers), new CardList(possibleBlockers), blockerLife);
    }

    /**
     * <p>Constructor for ComputerUtil_Attack2.</p>
     *
     * @param possibleAttackers a {@link forge.CardList} object.
     * @param possibleBlockers a {@link forge.CardList} object.
     * @param blockerLife a int.
     */
    public ComputerUtil_Attack2(CardList possibleAttackers, CardList possibleBlockers, int blockerLife) {
        humanList = new CardList(possibleBlockers.toArray());
        humanList = humanList.getType("Creature");

        computerList = new CardList(possibleAttackers.toArray());
        computerList = computerList.getType("Creature");
        playerCreatures = new CardList(possibleBlockers.toArray());
        playerCreatures = playerCreatures.getType("Creature");

        attackers = getPossibleAttackers(possibleAttackers);
        blockers = getPossibleBlockers(possibleBlockers, attackers);
        this.blockerLife = blockerLife;
    }//constructor

    /**
     * <p>sortAttackers.</p>
     *
     * @param in a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList sortAttackers(CardList in) {
        CardList list = new CardList();

        //Cards with triggers should come first (for Battle Cry)
        for (Card attacker : in) {
            ArrayList<Trigger> registeredTriggers = AllZone.getTriggerHandler().getRegisteredTriggers();
            for (Trigger trigger : registeredTriggers) {
                HashMap<String, String> trigParams = trigger.getMapParams();
                if (trigParams.get("Mode").equals("Attacks") && trigger.getHostCard().equals(attacker))
                    list.add(attacker);
            }
        }

        for (Card attacker : in) {
            if (!list.contains(attacker)) list.add(attacker);
        }

        return list;
    }//sortAttackers()

    //Is there any reward for attacking? (for 0/1 creatures there is not)
    /**
     * <p>isEffectiveAttacker.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public boolean isEffectiveAttacker(Card attacker, Combat combat) {
        if (CombatUtil.damageIfUnblocked(attacker, AllZone.getHumanPlayer(), combat) > 0) return true;
        if (CombatUtil.poisonIfUnblocked(attacker, AllZone.getHumanPlayer(), combat) > 0) return true;

        ArrayList<Trigger> registeredTriggers = AllZone.getTriggerHandler().getRegisteredTriggers();
        for (Trigger trigger : registeredTriggers)
            if (CombatUtil.combatTriggerWillTrigger(attacker, null, trigger, combat)
                    && trigger.getHostCard().getController().isComputer()) return true;

        return false;
    }

    /**
     * <p>getPossibleAttackers.</p>
     *
     * @param in a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getPossibleAttackers(CardList in) {
        CardList list = new CardList(in.toArray());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return CombatUtil.canAttack(c);
            }
        });
        return list;
    }//getPossibleAttackers()

    /**
     * <p>getPossibleBlockers.</p>
     *
     * @param blockers a {@link forge.CardList} object.
     * @param attackers a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getPossibleBlockers(CardList blockers, CardList attackers) {
        CardList possibleBlockers = new CardList(blockers.toArray());
        final CardList attackerList = new CardList(attackers.toArray());
        possibleBlockers = possibleBlockers.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if (!c.isCreature()) return false;
                for (Card attacker : attackerList) {
                    if (CombatUtil.canBlock(attacker, c)) return true;
                }
                return false;
            }
        });
        return possibleBlockers;
    }//getPossibleBlockers()

    //this checks to make sure that the computer player
    //doesn't lose when the human player attacks
    //this method is used by getAttackers()
    /**
     * <p>notNeededAsBlockers.</p>
     *
     * @param attackers a {@link forge.CardList} object.
     * @param combat a {@link forge.Combat} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList notNeededAsBlockers(CardList attackers, Combat combat) {
        CardList notNeededAsBlockers = new CardList(attackers.toArray());
        CardListUtil.sortAttackLowFirst(attackers);
        int blockersNeeded = attackers.size();

        //don't hold back creatures that can't block any of the human creatures
        CardList list = getPossibleBlockers(attackers, humanList);

        for (int i = 0; i < list.size(); i++) {
            if (!doesHumanAttackAndWin(i)) {
                blockersNeeded = i;
                break;
            } else notNeededAsBlockers.remove(list.get(i));
        }

        if (blockersNeeded == list.size()) {
            // Human will win unless everything is kept back to block
            return notNeededAsBlockers;
        }

        // Increase the total number of blockers needed by 1 if Finest Hour in play
        // (human will get an extra first attack with a creature that untaps)
        // In addition, if the computer guesses it needs no blockers, make sure that
        // it won't be surprised by Exalted
        int humanExaltedBonus = countExaltedBonus(AllZone.getHumanPlayer());

        if (humanExaltedBonus > 0) {
            int nFinestHours = AllZoneUtil.getPlayerCardsInPlay(AllZone.getHumanPlayer(), "Finest Hour").size();

            if ((blockersNeeded == 0 || nFinestHours > 0) && humanList.size() > 0) {
                //
                // total attack = biggest creature + exalted, *2 if Rafiq is in play
                int humanBaseAttack = getAttack(humanList.get(0)) + humanExaltedBonus;
                if (nFinestHours > 0) {
                    // For Finest Hour, one creature could attack and get the bonus TWICE
                    humanBaseAttack = humanBaseAttack + humanExaltedBonus;
                }
                int totalExaltedAttack = AllZoneUtil.isCardInPlay("Rafiq of the Many", AllZone.getHumanPlayer()) ?
                        2 * humanBaseAttack : humanBaseAttack;
                if ((AllZone.getComputerPlayer().getLife() - 3) <= totalExaltedAttack) {
                    // We will lose if there is an Exalted attack -- keep one blocker
                    if (blockersNeeded == 0 && notNeededAsBlockers.size() > 0)
                        notNeededAsBlockers.remove(0);

                    // Finest Hour allows a second Exalted attack: keep a blocker for that too
                    if (nFinestHours > 0 && notNeededAsBlockers.size() > 0)
                        notNeededAsBlockers.remove(0);
                }
            }
        }

        //re-add creatures with vigilance
        for (Card c : attackers) {
            if (c.hasKeyword("Vigilance"))
                notNeededAsBlockers.add(c);
        }

        return notNeededAsBlockers;
    }

    //this uses a global variable, which isn't perfect
    /**
     * <p>doesHumanAttackAndWin.</p>
     *
     * @param nBlockingCreatures a int.
     * @return a boolean.
     */
    public boolean doesHumanAttackAndWin(int nBlockingCreatures) {
        int totalAttack = 0;
        int stop = humanList.size() - nBlockingCreatures;

        for (int i = 0; i < stop; i++)
            totalAttack += getAttack(humanList.get(i));

        //originally -3 so the computer will try to stay at 3 life
        //0 now to prevent the AI from not attacking when it's got low life
        //(seems to happen too often)
        return AllZone.getComputerPlayer().getLife() <= totalAttack;
    }

    /**
     * <p>doAssault.</p>
     *
     * @return a boolean.
     */
    private boolean doAssault() {
        //Beastmaster Ascension
        if (AllZoneUtil.isCardInPlay("Beastmaster Ascension", AllZone.getComputerPlayer()) && attackers.size() > 1) {
            CardList beastions = AllZoneUtil.getCardsInZone(Constant.Zone.Battlefield, AllZone.getComputerPlayer()).
                    getName("Beastmaster Ascension");
            int minCreatures = 7;
            for (Card beastion : beastions) {
                int counters = beastion.getCounters(Counters.QUEST);
                minCreatures = Math.min(minCreatures, 7 - counters);
            }
            if (attackers.size() >= minCreatures)
                return true;
        }

        //I think this is right but the assault code may still be a little off
        CardListUtil.sortAttackLowFirst(attackers);

        int totalAttack = 0;
        //presumes the Human will block
        for (int i = 0; i < (attackers.size() - blockers.size()); i++)
            totalAttack += getAttack(attackers.get(i));

        return blockerLife <= totalAttack;
    }//doAssault()

    /**
     * <p>chooseDefender.</p>
     *
     * @param c a {@link forge.Combat} object.
     * @param bAssault a boolean.
     */
    public void chooseDefender(Combat c, boolean bAssault) {
        // TODO: split attackers to different planeswalker/human
        // AI will only attack one Defender per combat for now
        ArrayList<Object> defs = c.getDefenders();

        if (defs.size() == 1 || bAssault) {
            c.setCurrentDefender(0);
            return;
        }

        // Randomly determine who EVERYONE is attacking
        // would be better to determine more individually
        int n = MyRandom.random.nextInt(defs.size());
        
        Object entity = AllZone.getComputerPlayer().getMustAttackEntity();
        if(null != entity) {
        	ArrayList<Object> defenders = AllZone.getCombat().getDefenders();
        	n = defenders.indexOf(entity);
        	if(-1 == n) {
        		System.out.println("getMustAttackEntity() returned something not in defenders.");
        	}
        	else {
        		c.setCurrentDefender(n);
        	}
        }
        else {
        	c.setCurrentDefender(n);
        }
        
        return;
    }


    /**
     * <p>Getter for the field <code>attackers</code>.</p>
     *
     * @return a {@link forge.Combat} object.
     */
    public Combat getAttackers() {
        //if this method is called multiple times during a turn,
        //it will always return the same value
        //randomInt is used so that the computer doesn't always
        //do the same thing on turn 3 if he had the same creatures in play
        //I know this is a little confusing
        random.setSeed(AllZone.getPhase().getTurn() + randomInt);

        Combat combat = new Combat();
        combat.setAttackingPlayer(AllZone.getCombat().getAttackingPlayer());
        combat.setDefendingPlayer(AllZone.getCombat().getDefendingPlayer());

        combat.setDefenders(AllZone.getCombat().getDefenders());

        boolean bAssault = doAssault();
        // Determine who will be attacked
        chooseDefender(combat, bAssault);

        CardList attackersLeft = new CardList(attackers.toArray());

        //Attackers that don't really have a choice
        for (Card attacker : attackers) {
            if ((attacker.hasKeyword("CARDNAME attacks each turn if able.")
                    || attacker.hasKeyword("At the beginning of the end step, destroy CARDNAME.")
                    || attacker.hasKeyword("At the beginning of the end step, exile CARDNAME.")
                    || attacker.hasKeyword("At the beginning of the end step, sacrifice CARDNAME.")
                    || attacker.getSacrificeAtEOT()
                    || attacker.getSirenAttackOrDestroy()
                    || (attacker.getController().getMustAttackEntity() != null))
                    && CombatUtil.canAttack(attacker, combat)) {
                combat.addAttacker(attacker);
                attackersLeft.remove(attacker);
            }
        }

        // *******************
        // start of edits
        // *******************

        int computerForces = 0;
        int playerForces = 0;
        int playerForcesForAttritionalAttack = 0;

        // examine the potential forces
        CardList nextTurnAttackers = new CardList();
        int candidateCounterAttackDamage = 0;
        //int candidateTotalBlockDamage = 0;
        for (Card pCard : playerCreatures) {

            // if the creature can attack next turn add it to counter attackers list
            if (CombatUtil.canAttackNextTurn(pCard)) {
                nextTurnAttackers.add(pCard);
                if (pCard.getNetCombatDamage() > 0) {
                    candidateCounterAttackDamage += pCard.getNetCombatDamage();
                    //candidateTotalBlockDamage += pCard.getNetCombatDamage();
                    playerForces += 1; // player forces they might use to attack
                }
            }
            // increment player forces that are relevant to an attritional attack - includes walls
            if (CombatUtil.canBlock(pCard)) {
                playerForcesForAttritionalAttack += 1;
            }
        }

        // find the potential counter attacking damage compared to AI life total
        double aiLifeToPlayerDamageRatio = 1000000;
        if (candidateCounterAttackDamage > 0) {
            aiLifeToPlayerDamageRatio = (double) AllZone.getComputerPlayer().life / candidateCounterAttackDamage;
        }

        // get the potential damage and strength of the AI forces
        CardList candidateAttackers = new CardList();
        int candidateUnblockedDamage = 0;
        for (Card pCard : computerList) {
            // if the creature can attack then it's a potential attacker this turn, assume summoning sickness creatures will be able to
            if (CombatUtil.canAttackNextTurn(pCard)) {

                candidateAttackers.add(pCard);
                if (pCard.getNetCombatDamage() > 0) {
                    candidateUnblockedDamage += CombatUtil.damageIfUnblocked(pCard, AllZone.getHumanPlayer(), combat);
                    computerForces += 1;
                }

            }
        }

        // find the potential damage ratio the AI can cause
        double playerLifeToDamageRatio = 1000000;
        if (candidateUnblockedDamage > 0)
            playerLifeToDamageRatio = (double) AllZone.getHumanPlayer().life / candidateUnblockedDamage;

        /*System.out.println(String.valueOf(aiLifeToPlayerDamageRatio) + " = ai life to player damage ratio");
			System.out.println(String.valueOf(playerLifeToDamageRatio) + " = player life ai player damage ratio");*/

        // determine if the ai outnumbers the player
        int outNumber = computerForces - playerForces;

        // compare the ratios, higher = better for ai
        double ratioDiff = aiLifeToPlayerDamageRatio - playerLifeToDamageRatio;
        /* System.out.println(String.valueOf(ratioDiff) + " = ratio difference, higher = better for ai");
			System.out.println(String.valueOf(outNumber) + " = outNumber, higher = better for ai"); */

        // *********************
        // if outnumber and superior ratio work out whether attritional all out attacking will work
        // attritional attack will expect some creatures to die but to achieve victory by sheer weight
        // of numbers attacking turn after turn. It's not calculate very carefully, the accuracy
        // can probably be improved
        // *********************
        boolean doAttritionalAttack = false;
        // get list of attackers ordered from low power to high
        CardListUtil.sortAttackLowFirst(attackers);
        // get player life total
        int playerLife = AllZone.getHumanPlayer().life;
        // get the list of attackers up to the first blocked one
        CardList attritionalAttackers = new CardList();
        for (int x = 0; x < attackers.size() - playerForces; x++) {
            attritionalAttackers.add(attackers.getCard(x));
        }
        // until the attackers are used up or the player would run out of life
        int attackRounds = 1;
        while (attritionalAttackers.size() > 0 && playerLife > 0 && attackRounds < 99) {
            //   sum attacker damage
            int damageThisRound = 0;
            for (int y = 0; y < attritionalAttackers.size(); y++) {
                damageThisRound += attritionalAttackers.getCard(y).getNetCombatDamage();
            }
            //   remove from player life
            playerLife -= damageThisRound;
            //   shorten attacker list by the length of the blockers - assuming all blocked are killed for convenience
            for (int z = 0; z < playerForcesForAttritionalAttack; z++) {
                if (attritionalAttackers.size() > 0) {
                    attritionalAttackers.remove(attritionalAttackers.size() - 1);
                }
            }
            attackRounds += 1;
            if (playerLife <= 0) {
                doAttritionalAttack = true;
            }
        }
        //System.out.println(doAttritionalAttack + " = do attritional attack");
        // *********************
        // end attritional attack calculation
        // *********************

        // *********************
        // see how long until unblockable attackers will be fatal
        // *********************
        double unblockableDamage = 0;
        double turnsUntilDeathByUnblockable = 0;
        boolean doUnblockableAttack = false;
        for (Card attacker : attackers) {
            boolean isUnblockableCreature = true;
            // check blockers individually, as the bulk canBeBlocked doesn't check all circumstances
            for (Card blocker : blockers) {
                if (CombatUtil.canBlock(attacker, blocker)) {
                    isUnblockableCreature = false;
                }
            }
            if (isUnblockableCreature) {
                unblockableDamage += CombatUtil.damageIfUnblocked(attacker, AllZone.getHumanPlayer(), combat);
            }
        }
        if (unblockableDamage > 0) {
            turnsUntilDeathByUnblockable = AllZone.getHumanPlayer().life / unblockableDamage;
        }
        if (unblockableDamage > AllZone.getHumanPlayer().life) {
            doUnblockableAttack = true;
        }
        // *****************
        // end see how long until unblockable attackers will be fatal
        // *****************

        // decide on attack aggression based on a comparison of forces, life totals and other considerations
        // some bad "magic numbers" here, TODO replace with nice descriptive variable names
        if ((ratioDiff > 0 && doAttritionalAttack)) { // (playerLifeToDamageRatio <= 1 && ratioDiff >= 1 && outNumber > 0)  ||
            aiAggression = 5; // attack at all costs
        } else if ((playerLifeToDamageRatio < 2 && ratioDiff >= 0) || ratioDiff > 3 || (ratioDiff > 0 && outNumber > 0)) {
            aiAggression = 3;  // attack expecting to kill creatures or damage player.
        } else if (ratioDiff >= 0 || ratioDiff + outNumber >= -1) {
            // at 0 ratio expect to potentially gain an advantage by attacking first
            // if the ai has a slight advantage
            // or the ai has a significant advantage numerically but only a slight disadvantage damage/life
            aiAggression = 2; // attack expecting to destroy creatures/be unblockable
        } else if (ratioDiff < 0 && aiLifeToPlayerDamageRatio > 1) {
            // the player is overmatched but there are a few turns before death
            aiAggression = 2; // attack expecting to destroy creatures/be unblockable
        } else if (doUnblockableAttack || ((ratioDiff * -1) < turnsUntilDeathByUnblockable)) {
            aiAggression = 1; // look for unblockable creatures that might be able to attack for a bit of
            // fatal damage even if the player is significantly better
        } else if (ratioDiff < 0) {
            aiAggression = 0;
        } // stay at home to block
        System.out.println(String.valueOf(aiAggression) + " = ai aggression");

        // ****************
        // End of edits
        // ****************

        //Exalted
        if (combat.getAttackers().length == 0 && (countExaltedBonus(AllZone.getComputerPlayer()) >= 3 ||
                AllZoneUtil.isCardInPlay("Rafiq of the Many", AllZone.getComputerPlayer()) ||
                AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer(), "Battlegrace Angel").size() >= 2 ||
                (AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer(), "Finest Hour").size() >= 1) &&
                        AllZone.getPhase().isFirstCombat())
                && !bAssault) {
            int biggest = 0;
            Card att = null;
            for (int i = 0; i < attackersLeft.size(); i++) {
                if (getAttack(attackersLeft.get(i)) > biggest) {
                    biggest = getAttack(attackersLeft.get(i));
                    att = attackersLeft.get(i);
                }
            }
            if (att != null && CombatUtil.canAttack(att, combat))
                combat.addAttacker(att);

            System.out.println("Exalted");
        }

        //do assault (all creatures attack) if the computer would win the game
        //or if the computer has 4 creatures and the player has 1
        else if (bAssault) {
            System.out.println("Assault");
            CardListUtil.sortAttack(attackersLeft);
            for (int i = 0; i < attackersLeft.size(); i++)
                if (CombatUtil.canAttack(attackersLeft.get(i), combat)) combat.addAttacker(attackersLeft.get(i));
        } else {
            System.out.println("Normal attack");

            attackersLeft = notNeededAsBlockers(attackersLeft, combat);
            System.out.println(attackersLeft.size());

            attackersLeft = sortAttackers(attackersLeft);

            for (int i = 0; i < attackersLeft.size(); i++) {
                Card attacker = attackersLeft.get(i);
                int totalFirstStrikeBlockPower = 0;
                if (!attacker.hasFirstStrike() && !attacker.hasDoubleStrike())
                    totalFirstStrikeBlockPower = CombatUtil.getTotalFirstStrikeBlockPower(attacker, AllZone.getHumanPlayer());

                if (shouldAttack(attacker, blockers, combat) && (totalFirstStrikeBlockPower < attacker.getKillDamage() || aiAggression == 5)
                        && CombatUtil.canAttack(attacker, combat))
                    combat.addAttacker(attacker);
            }
        }//getAttackers()

        return combat;
    }//getAttackers()

    /**
     * <p>countExaltedBonus.</p>
     *
     * @param player a {@link forge.Player} object.
     * @return a int.
     */
    public int countExaltedBonus(Player player) {
        CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.hasKeyword("Exalted");
            }
        });

        return list.size();
    }

    /**
     * <p>getAttack.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    public int getAttack(Card c) {
        int n = c.getNetCombatDamage();

        if (c.hasKeyword("Double Strike"))
            n *= 2;

        return n;
    }

    /**
     * <p>shouldAttack.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defenders a {@link forge.CardList} object.
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public boolean shouldAttack(Card attacker, CardList defenders, Combat combat) {
        boolean canBeKilledByOne = false; // indicates if the attacker can be killed by a single blockers
        boolean canKillAll = true; // indicates if the attacker can kill all single blockers
        boolean canKillAllDangerous = true; // indicates if the attacker can kill all single blockers with wither or infect
        boolean isWorthLessThanAllKillers = true;
        boolean canBeBlocked = false;

        if (!isEffectiveAttacker(attacker, combat)) return false;

        // look at the attacker in relation to the blockers to establish a number of factors about the attacking
        // context that will be relevant to the attackers decision according to the selected strategy
        for (Card defender : defenders) {
            if (CombatUtil.canBlock(attacker, defender)) { //, combat )) {
                canBeBlocked = true;
                if (CombatUtil.canDestroyAttacker(attacker, defender, combat, false)) {
                    canBeKilledByOne = true;  // there is a single creature on the battlefield that can kill the creature
                    // see if the defending creature is of higher or lower value. We don't want to attack only to lose value
                    if (CardFactoryUtil.evaluateCreature(defender) <= CardFactoryUtil.evaluateCreature(attacker)) {
                        isWorthLessThanAllKillers = false;
                    }
                }
                // see if this attacking creature can destroy this defender, if not record that it can't kill everything
                if (!CombatUtil.canDestroyBlocker(defender, attacker, combat, false)) {
                    canKillAll = false;
                    if (defender.hasKeyword("Wither") || defender.hasKeyword("Infect")) {
                        canKillAllDangerous = false; // there is a dangerous creature that can survive an attack from this creature
                    }
                }
            }
        }

        // if the creature cannot block and can kill all opponents they might as well attack, they do nothing staying back
        if (canKillAll && !CombatUtil.canBlock(attacker) && isWorthLessThanAllKillers) {
            System.out.println(attacker.getName() + " = attacking because they can't block, expecting to kill or damage player");
            return true;
        }

        // decide if the creature should attack based on the prevailing strategy choice in aiAggression
        switch (aiAggression) {
            case 5: // all out attacking
                System.out.println(attacker.getName() + " = all out attacking");
                return true;
            case 4: // expecting to at least trade with something
                if (canKillAll || (canKillAllDangerous && !canBeKilledByOne) || !canBeBlocked) {
                    System.out.println(attacker.getName() + " = attacking expecting to at least trade with something");
                    return true;
                }
            case 3: // expecting to at least kill a creature of equal value, not be blocked
                if ((canKillAll && isWorthLessThanAllKillers) || (canKillAllDangerous && !canBeKilledByOne) || !canBeBlocked) {
                    System.out.println(attacker.getName() + " = attacking expecting to kill creature or cause damage, or is unblockable");
                    return true;
                }
            case 2: // attack expecting to attract a group block or destroying a single blocker and surviving
                if ((canKillAll && !canBeKilledByOne) || !canBeBlocked) {
                    System.out.println(attacker.getName() + " = attacking expecting to survive or attract group block");
                    return true;
                }
            case 1: // unblockable creatures only
                if (!canBeBlocked) {
                    System.out.println(attacker.getName() + " = attacking expecting not to be blocked");
                    return true;
                }
        }
        return false; // don't attack
    }

}//end class ComputerUtil_Attack2
