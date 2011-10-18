package forge;


import com.esotericsoftware.minlog.Log;

import forge.Constant.Zone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.trigger.Trigger;
import forge.gui.GuiUtils;
import forge.gui.input.Input_PayManaCost_Ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * <p>CombatUtil class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class CombatUtil {

    //can the creature block given the combat state?
    /**
     * <p>canBlock.</p>
     *
     * @param blocker a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean canBlock(Card blocker, Combat combat) {

        if (blocker == null) return false;

        if (combat.getAllBlockers().size() > 1 && AllZoneUtil.isCardInPlay("Caverns of Despair"))
            return false;

        if (combat.getAllBlockers().size() > 0 && AllZoneUtil.isCardInPlay("Silent Arbiter"))
            return false;

        if (combat.getAllBlockers().size() > 0 && AllZoneUtil.isCardInPlay("Dueling Grounds"))
            return false;


        return canBlock(blocker);
    }


    //can the creature block at all?
    /**
     * <p>canBlock.</p>
     *
     * @param blocker a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canBlock(Card blocker) {

        if (blocker == null) return false;

        if (blocker.isTapped() && !AllZoneUtil.isCardInPlay("Masako the Humorless", blocker.getController()))
            return false;

        if (blocker.hasKeyword("CARDNAME can't block.") || blocker.hasKeyword("CARDNAME can't attack or block.") || blocker.isPhasedOut())
            return false;

        CardList kulrath = AllZoneUtil.getCardsIn(Zone.Battlefield, "Kulrath Knight");
        if (kulrath.size() > 0) {
            for (int i = 0; i < kulrath.size(); i++) {
                Card cKK = kulrath.get(i);
                Player oppKK = cKK.getController().getOpponent();

                if (blocker.getController().equals(oppKK) && blocker.hasCounters())
                    return false;
            }
        }

        return true;
    }

    //can the attacker be blocked at all?
    /**
     * <p>canBeBlocked.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean canBeBlocked(Card attacker, Combat combat) {

        if (attacker == null) return true;

        if (attacker.hasKeyword("CARDNAME can't be blocked by more than one creature.")
                && combat.getBlockers(attacker).size() > 0) return false;

        return canBeBlocked(attacker);
    }

    //can the attacker be blocked at all?
    /**
     * <p>canBeBlocked.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canBeBlocked(Card attacker) {

        if (attacker == null) return true;

        if (attacker.hasKeyword("Unblockable")) return false;

        //Landwalk
        if (!AllZoneUtil.isCardInPlay("Staff of the Ages")) { //"Creatures with landwalk abilities can be blocked as though they didn't have those abilities."
            CardList blkCL = attacker.getController().getOpponent().getCardsIn(Zone.Battlefield);
            CardList temp = new CardList();

            if (attacker.hasKeyword("Plainswalk")) {
                temp = blkCL.getType("Plains");
                if (!AllZoneUtil.isCardInPlay("Lord Magnus")
                        && !AllZoneUtil.isCardInPlay("Great Wall")
                        && !temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Islandwalk")) {
                temp = blkCL.getType("Island");
                if (!AllZoneUtil.isCardInPlay("Undertow")
                        && !AllZoneUtil.isCardInPlay("Gosta Dirk")
                        && !temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Swampwalk")) {
                temp = blkCL.getType("Swamp");
                if (!AllZoneUtil.isCardInPlay("Ur-drago")
                        && !AllZoneUtil.isCardInPlay("Quagmire")
                        && !temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Mountainwalk")) {
                temp = blkCL.getType("Mountain");
                if (!AllZoneUtil.isCardInPlay("Crevasse")
                        && !temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Forestwalk")) {
                temp = blkCL.getType("Forest");
                if (!AllZoneUtil.isCardInPlay("Lord Magnus")
                        && !AllZoneUtil.isCardInPlay("Deadfall")
                        && !temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Legendary landwalk")) {
                temp = blkCL.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.isLand()
                                && c.isType("Legendary");
                    }
                });
                if (!temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Snow swampwalk")) {
                temp = blkCL.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.isType("Swamp") && c.isSnow();
                    }
                });
                if (!temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Snow forestwalk")) {
                temp = blkCL.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.isType("Forest") && c.isSnow();
                    }
                });
                if (!temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Snow islandwalk")) {
                temp = blkCL.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.isType("Island") && c.isSnow();
                    }
                });
                if (!temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Snow plainswalk")) {
                temp = blkCL.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.isType("Plains") && c.isSnow();
                    }
                });
                if (!temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Snow mountainwalk")) {
                temp = blkCL.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.isType("Mountain") && c.isSnow();
                    }
                });
                if (!temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Snow landwalk")) {
                temp = blkCL.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.isLand() && c.isSnow();
                    }
                });
                if (!temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Desertwalk")) {
                temp = blkCL.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.isLand()
                                && c.isType("Desert");
                    }
                });
                if (!temp.isEmpty()) return false;
            }

            if (attacker.hasKeyword("Nonbasic landwalk")) {
                temp = blkCL.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.isLand() && !c.isBasicLand();
                    }
                });
                if (!temp.isEmpty()) return false;
            }
        }
        return true;
    }

    // Has the player chosen all mandatory blocks?
    /**
     * <p>finishedMandatotyBlocks.</p>
     *
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean finishedMandatotyBlocks(Combat combat) {

        CardList blockers = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
        CardList attackers = new CardList(combat.getAttackers());

        //if a creature does not block but should, return false
        for (Card blocker : blockers) {
            //lure effects
            if (!combat.getAllBlockers().contains(blocker) && mustBlockAnAttacker(blocker, combat))
                return false;

            //"CARDNAME blocks each turn if able."
            if (!combat.getAllBlockers().contains(blocker)
                    && blocker.hasKeyword("CARDNAME blocks each turn if able."))
                for (Card attacker : attackers)
                    if (canBlock(attacker, blocker, combat))
                        return false;
        }

        return true;
    }

    // can the blocker block an attacker with a lure effect?
    /**
     * <p>mustBlockAnAttacker.</p>
     *
     * @param blocker a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean mustBlockAnAttacker(Card blocker, Combat combat) {

        if (blocker == null) return false;

        if (canBlock(blocker, combat) == false) return false;

        CardList attackersWithLure = new CardList(combat.getAttackers());
        attackersWithLure = attackersWithLure.getKeyword("All creatures able to block CARDNAME do so.");

        for (Card attacker : attackersWithLure) {
            if (canBlock(blocker, combat) && canBlock(attacker, blocker)) return true;
        }
        
        for (Card attacker : blocker.getMustBlockCards()) {
            if (canBlock(blocker, combat) && canBlock(attacker, blocker)) return true;
        }

        return false;
    }

    // can the blocker block the attacker given the combat state?
    /**
     * <p>canBlock.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param blocker a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean canBlock(Card attacker, Card blocker, Combat combat) {

        if (attacker == null || blocker == null) return false;

        if (canBlock(blocker, combat) == false) return false;
        if (canBeBlocked(attacker, combat) == false) return false;

        //if the attacker has no lure effect, but the blocker can block another attacker with lure, the blocker can't block the former
        if (!attacker.hasKeyword("All creatures able to block CARDNAME do so.")
                && !(blocker.getMustBlockCards().contains(attacker))
                && mustBlockAnAttacker(blocker, combat))
            return false;

        return canBlock(attacker, blocker);
    }


    // can the blocker block the attacker?
    /**
     * <p>canBlock.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param blocker a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canBlock(Card attacker, Card blocker) {

        if (attacker == null || blocker == null) return false;

        if (canBlock(blocker) == false) return false;
        if (canBeBlocked(attacker) == false) return false;

        if (CardFactoryUtil.hasProtectionFrom(blocker, attacker)) return false;
        
        if (blocker.hasStartOfKeyword("CARDNAME can't block ")) {
            for (String kw : blocker.getKeyword()) {
                if (kw.startsWith("CARDNAME can't block ")) {
                    String unblockableCard = kw.substring(21);
                    int ID = Integer.parseInt(unblockableCard.substring(unblockableCard.lastIndexOf("(") + 1, unblockableCard.length() - 1));
                    if (attacker.getUniqueNumber() == ID) {
                        return false;
                    }
                }
            }
        }

        //rare case:
        if (blocker.hasKeyword("Shadow")
                && blocker.hasKeyword(
                "CARDNAME can block creatures with shadow as though they didn't have shadow.")) return false;

        if (attacker.hasKeyword("Shadow")
                && !blocker.hasKeyword("Shadow")
                && !blocker.hasKeyword(
                "CARDNAME can block creatures with shadow as though they didn't have shadow.")) return false;

        if (!attacker.hasKeyword("Shadow")
                && blocker.hasKeyword("Shadow")) return false;
        
        if (attacker.hasKeyword("Creatures with power less than CARDNAME's power can't block it.")
                && attacker.getNetAttack() > blocker.getNetAttack()) 
        	return false;
        if (blocker.getNetAttack() > attacker.getNetAttack()
                && blocker.hasKeyword("CARDNAME can't be blocked by creatures with power greater than CARDNAME's power."))
            return false;
        if (blocker.getNetAttack() >= attacker.getNetDefense()
                && blocker.hasKeyword("CARDNAME can't be blocked by creatures with power equal to or greater than CARDNAME's toughness."))
            return false;

        if (attacker.hasStartOfKeyword("CantBeBlockedBy")) {
            int KeywordPosition = attacker.getKeywordPosition("CantBeBlockedBy");
            String parse = attacker.getKeyword().get(KeywordPosition).toString();
            String k[] = parse.split(" ", 2);
            final String restrictions[] = k[1].split(",");
            if (blocker.isValid(restrictions, attacker.getController(), attacker))
                return false;
        }
        
        if (blocker.hasStartOfKeyword("CantBlock")) {
            int KeywordPosition = blocker.getKeywordPosition("CantBlock");
            String parse = blocker.getKeyword().get(KeywordPosition).toString();
            String k[] = parse.split(" ", 2);
            final String restrictions[] = k[1].split(",");
            if (attacker.isValid(restrictions, blocker.getController(), blocker))
                return false;
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked by black creatures.") && blocker.isBlack()) return false;
        if (attacker.hasKeyword("CARDNAME can't be blocked by blue creatures.") && blocker.isBlue()) return false;
        if (attacker.hasKeyword("CARDNAME can't be blocked by green creatures.") && blocker.isGreen()) return false;
        if (attacker.hasKeyword("CARDNAME can't be blocked by red creatures.") && blocker.isRed()) return false;
        if (attacker.hasKeyword("CARDNAME can't be blocked by white creatures.") && blocker.isWhite()) return false;

        if (blocker.hasKeyword("CARDNAME can block only creatures with flying.")
                && !attacker.hasKeyword("Flying")) return false;

        if (attacker.hasKeyword("Flying")
                || attacker.hasKeyword("CARDNAME can't be blocked except by creatures with flying or reach.")) {
            if (!blocker.hasKeyword("Flying")
                    && !blocker.hasKeyword("Reach")) return false;
        }

        if (attacker.hasKeyword("Horsemanship")) {
            if (!blocker.hasKeyword("Horsemanship")) return false;
        }

        if (attacker.hasKeyword("Fear")) {
            if (!blocker.isArtifact() && !blocker.isBlack())
                return false;
        }

        if (attacker.hasKeyword("Intimidate")) {
            if (!blocker.isArtifact() && !blocker.sharesColorWith(attacker))
                return false;
        }

        if (attacker.hasKeyword("CARDNAME can't be blocked by Walls.")
                && blocker.isWall()) return false;

        if (attacker.hasKeyword("CARDNAME can't be blocked except by Walls.")
                && !blocker.isWall()) return false;
        
        if (attacker.hasKeyword("CARDNAME can't be blocked except by black creatures.")
        		&& !blocker.isBlack()) return false;

        if (AllZoneUtil.isCardInPlay("Shifting Sliver")) {
            if (attacker.isType("Sliver") && !blocker.isType("Sliver")) return false;
        }

        return true;
    }//canBlock()

    //can a creature attack given the combat state
    /**
     * <p>canAttack.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean canAttack(Card c, Combat combat) {

        if (combat.getAttackers().length > 1 && AllZoneUtil.isCardInPlay("Crawlspace", c.getController().getOpponent()))
            return false;

        if (combat.getAttackers().length > 1 && AllZoneUtil.isCardInPlay("Caverns of Despair"))
            return false;

        if (combat.getAttackers().length > 0 && AllZoneUtil.isCardInPlay("Silent Arbiter"))
            return false;

        if (combat.getAttackers().length > 0 && AllZoneUtil.isCardInPlay("Dueling Grounds"))
            return false;

        return canAttack(c);
    }

    //can a creature attack at the moment?
    /**
     * <p>canAttack.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canAttack(Card c) {
        if (c.isTapped() || c.isPhasedOut() || (c.hasSickness() && !c.isEnchantedBy("Instill Energy"))) return false;

        return canAttackNextTurn(c);
    }

    //can a creature attack if untapped and without summoning sickness?
    /**
     * <p>canAttackNextTurn.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canAttackNextTurn(Card c) {
        if (!c.isCreature()) return false;

        // CARDNAME can't attack if defending player controls an untapped creature with power ...
        final int powerLimit[] = {0};
        int keywordPosition = 0;
        boolean hasKeyword = false;

        ArrayList<String> attackerKeywords = c.getKeyword();
        for (int i = 0; i < attackerKeywords.size(); i++) {
            if (attackerKeywords.get(i).toString().startsWith("CARDNAME can't attack if defending player controls an untapped creature with power")) {
                hasKeyword = true;
                keywordPosition = i;
            }
        }

        // The keyword "CARDNAME can't attack if defending player controls an untapped creature with power" ... is present
        if (hasKeyword) {
            String tmpString = c.getKeyword().get(keywordPosition).toString();
            final String asSeparateWords[] = tmpString.trim().split(" ");

            if (asSeparateWords.length >= 15) {
                if (asSeparateWords[12].matches("[0-9][0-9]?")) {
                    powerLimit[0] = Integer.parseInt((asSeparateWords[12]).trim());

                    CardList list = AllZoneUtil.getCreaturesInPlay(c.getController().getOpponent());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card ct) {
                            return ((ct.isUntapped() && ct.getNetAttack() >= powerLimit[0] && asSeparateWords[14].contains("greater")) ||
                                    (ct.isUntapped() && ct.getNetAttack() <= powerLimit[0] && asSeparateWords[14].contains("less")));
                        }
                    });
                    if (!list.isEmpty()) return false;
                }
            }
        } // hasKeyword = CARDNAME can't attack if defending player controls an untapped creature with power ...

        CardList list = c.getController().getOpponent().getCardsIn(Zone.Battlefield);
        CardList temp;

        if (c.hasKeyword("CARDNAME can't attack unless defending player controls an Island.")) {
            temp = list.getType("Island");
            if (temp.isEmpty()) return false;
        }

        if (c.hasKeyword("CARDNAME can't attack unless defending player controls a Forest.")) {
            temp = list.getType("Forest");
            if (temp.isEmpty()) return false;
        }

        if (c.hasKeyword("CARDNAME can't attack unless defending player controls a Swamp.")) {
            temp = list.getType("Swamp");
            if (temp.isEmpty()) return false;
        }
        if (c.hasKeyword("CARDNAME can't attack unless defending player controls a Mountain.")) {
            temp = list.getType("Montain");
            if (temp.isEmpty()) return false;
        }
        if (c.hasKeyword("CARDNAME can't attack unless defending player controls a snow land.")) {
            temp = list.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                    return c.isLand()
                            && c.isSnow();
                }
            });
            if (temp.isEmpty()) return false;
        }

        if (c.hasKeyword("CARDNAME can't attack unless defending player controls a blue permanent.")) {
            temp = list.getColor(Constant.Color.Blue);
            if (temp.isEmpty()) return false;
        }


        if (c.getName().equals("Harbor Serpent")) {
            CardList allislands = AllZoneUtil.getCardsIn(Zone.Battlefield).getType("Island");
            if (allislands.size() < 5) return false;
        }

        //The creature won't untap next turn
        if (c.isTapped() && !PhaseUtil.canUntap(c)) return false;

        if (c.hasKeyword("CARDNAME can't attack.") || c.hasKeyword("CARDNAME can't attack or block."))
            return false;

        if (c.hasKeyword("Defender")
                && !c.hasKeyword("CARDNAME can attack as though it didn't have defender.")) {
            return false;
        }

        if (AllZoneUtil.isCardInPlay("Kulrath Knight")) {
            CardList all = AllZoneUtil.getCardsIn(Zone.Battlefield, "Kulrath Knight");
            for (int i = 0; i < all.size(); i++) {
                Card cKK = all.get(i);
                Player oppKK = cKK.getController().getOpponent();

                if (c.getController().equals(oppKK) && c.hasCounters())
                    return false;
            }
        }

        return true;
    }//canAttack()


    /**
     * <p>getTotalFirstStrikeBlockPower.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param player a {@link forge.Player} object.
     * @return a int.
     */
    public static int getTotalFirstStrikeBlockPower(Card attacker, Player player) {
        final Card att = attacker;

        CardList list = AllZoneUtil.getCreaturesInPlay(player);
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return canBlock(att, c) && (c.hasFirstStrike() || c.hasDoubleStrike());
            }
        });

        return totalDamageOfBlockers(attacker, list);

    }

    //This function takes Doran and Double Strike into account
    /**
     * <p>getAttack.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a int.
     */
    public static int getAttack(Card c) {
        int n = c.getNetCombatDamage();

        if (c.hasDoubleStrike())
            n *= 2;

        return n;
    }

    //Returns the damage an unblocked attacker would deal
    /**
     * <p>damageIfUnblocked.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param attacked a {@link forge.Player} object.
     * @param combat a {@link forge.Combat} object.
     * @return a int.
     */
    public static int damageIfUnblocked(Card attacker, Player attacked, Combat combat) {
        int damage = attacker.getNetCombatDamage();
        int sum = 0;
        damage += predictPowerBonusOfAttacker(attacker, null, combat);
        if (!attacker.hasKeyword("Infect")) {
            sum = attacked.predictDamage(damage, attacker, true);
            if (attacker.hasKeyword("Double Strike")) sum += attacked.predictDamage(damage, attacker, true);
        }
        return sum;
    }

    //Returns the poison an unblocked attacker would deal
    /**
     * <p>poisonIfUnblocked.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param attacked a {@link forge.Player} object.
     * @param combat a {@link forge.Combat} object.
     * @return a int.
     */
    public static int poisonIfUnblocked(Card attacker, Player attacked, Combat combat) {
        int damage = attacker.getNetCombatDamage();
        int poison = 0;
        damage += predictPowerBonusOfAttacker(attacker, null, null);
        if (attacker.hasKeyword("Infect")) {
            poison += attacked.predictDamage(damage, attacker, true);
            if (attacker.hasKeyword("Double Strike")) poison += attacked.predictDamage(damage, attacker, true);
        }
        if (attacker.hasKeyword("Poisonous") && damage > 0) poison += attacker.getKeywordMagnitude("Poisonous");
        return poison;
    }

    //Returns the damage unblocked attackers would deal
    /**
     * <p>sumDamageIfUnblocked.</p>
     *
     * @param attackers a {@link forge.CardList} object.
     * @param attacked a {@link forge.Player} object.
     * @return a int.
     */
    private static int sumDamageIfUnblocked(CardList attackers, Player attacked) {
        int sum = 0;
        for (Card attacker : attackers) {
            sum += damageIfUnblocked(attacker, attacked, null);
        }
        return sum;
    }

    //Returns the number of poison counters unblocked attackers would deal
    /**
     * <p>sumPoisonIfUnblocked.</p>
     *
     * @param attackers a {@link forge.CardList} object.
     * @param attacked a {@link forge.Player} object.
     * @return a int.
     */
    private static int sumPoisonIfUnblocked(CardList attackers, Player attacked) {
        int sum = 0;
        for (Card attacker : attackers) {
            sum += poisonIfUnblocked(attacker, attacked, null);
        }
        return sum;
    }

    //calculates the amount of life that will remain after the attack 
    /**
     * <p>lifeThatWouldRemain.</p>
     *
     * @param combat a {@link forge.Combat} object.
     * @return a int.
     */
    public static int lifeThatWouldRemain(Combat combat) {

        int damage = 0;

        CardList attackers = combat.sortAttackerByDefender()[0];
        CardList unblocked = new CardList();

        for (Card attacker : attackers) {

            CardList blockers = combat.getBlockers(attacker);

            if (blockers.size() == 0) unblocked.add(attacker);
            else if (attacker.hasKeyword("Trample") && getAttack(attacker) > CombatUtil.totalShieldDamage(attacker, blockers)) {
                if (!attacker.hasKeyword("Infect"))
                    damage += getAttack(attacker) - CombatUtil.totalShieldDamage(attacker, blockers);
            }
        }

        damage += sumDamageIfUnblocked(unblocked, AllZone.getComputerPlayer());

        if (!AllZone.getComputerPlayer().canLoseLife()) damage = 0;

        return AllZone.getComputerPlayer().getLife() - damage;
    }

    //calculates the amount of poison counters after the attack  
    /**
     * <p>resultingPoison.</p>
     *
     * @param combat a {@link forge.Combat} object.
     * @return a int.
     */
    public static int resultingPoison(Combat combat) {

        int poison = 0;

        CardList attackers = combat.sortAttackerByDefender()[0];
        CardList unblocked = new CardList();

        for (Card attacker : attackers) {

            CardList blockers = combat.getBlockers(attacker);

            if (blockers.size() == 0) unblocked.add(attacker);
            else if (attacker.hasKeyword("Trample") && getAttack(attacker) > CombatUtil.totalShieldDamage(attacker, blockers)) {
                if (attacker.hasKeyword("Infect"))
                    poison += getAttack(attacker) - CombatUtil.totalShieldDamage(attacker, blockers);
                if (attacker.hasKeyword("Poisonous"))
                    poison += attacker.getKeywordMagnitude("Poisonous");
            }
        }

        poison += sumPoisonIfUnblocked(unblocked, AllZone.getComputerPlayer());

        return AllZone.getComputerPlayer().getPoisonCounters() + poison;
    }

    //Checks if the life of the attacked Player/Planeswalker is in danger 
    /**
     * <p>lifeInDanger.</p>
     *
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean lifeInDanger(Combat combat) {
        // life in danger only cares about the player's life. Not about a Planeswalkers life
        if (AllZone.getComputerPlayer().cantLose())
            return false;

        if (lifeThatWouldRemain(combat) < Math.min(4, AllZone.getComputerPlayer().getLife())
                && !AllZone.getComputerPlayer().cantLoseForZeroOrLessLife())
            return true;

        return (resultingPoison(combat) > Math.max(7, AllZone.getComputerPlayer().getPoisonCounters()));
    }

    //Checks if the life of the attacked Player would be reduced
    /**
     * <p>wouldLoseLife.</p>
     *
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean wouldLoseLife(Combat combat) {

        return (lifeThatWouldRemain(combat) < AllZone.getComputerPlayer().getLife());
    }

    //Checks if the life of the attacked Player/Planeswalker is in danger 
    /**
     * <p>lifeInSeriousDanger.</p>
     *
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean lifeInSeriousDanger(Combat combat) {
        // life in danger only cares about the player's life. Not about a Planeswalkers life
        if (AllZone.getComputerPlayer().cantLose())
            return false;

        if (lifeThatWouldRemain(combat) < 1 && !AllZone.getComputerPlayer().cantLoseForZeroOrLessLife())
            return true;

        return (resultingPoison(combat) > 9);
    }

    // This calculates the amount of damage a blockgang can deal to the attacker (first strike not supported)
    /**
     * <p>totalDamageOfBlockers.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defenders a {@link forge.CardList} object.
     * @return a int.
     */
    public static int totalDamageOfBlockers(Card attacker, CardList defenders) {
        int damage = 0;

        for (Card defender : defenders) damage += dealsDamageAsBlocker(attacker, defender);

        return damage;
    }


    // This calculates the amount of damage a blocker in a blockgang can deal to the attacker
    /**
     * <p>dealsDamageAsBlocker.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defender a {@link forge.Card} object.
     * @return a int.
     */
    public static int dealsDamageAsBlocker(Card attacker, Card defender) {

        if (attacker.getName().equals("Sylvan Basilisk")
                && !defender.hasKeyword("Indestructible"))
            return 0;
        
        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking")
                && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) return 0;
            if (flankingMagnitude >= defender.getNetDefense() - defender.getDamage()
                    && !defender.hasKeyword("Indestructible"))
                return 0;

        }//flanking
        if (attacker.hasKeyword("Indestructible")
                && !(defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))) return 0;
        
        int defenderDamage = defender.getNetAttack() + predictPowerBonusOfBlocker(attacker, defender);
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
            defenderDamage = defender.getNetDefense() + predictToughnessBonusOfBlocker(attacker, defender);
        }

        // consider static Damage Prevention
        defenderDamage = attacker.predictDamage(defenderDamage, defender, true);

        if (defender.hasKeyword("Double Strike"))
            defenderDamage += attacker.predictDamage(defenderDamage, defender, true);

        return defenderDamage;
    }

    // This calculates the amount of damage a blocker in a blockgang can take from the attacker (for trampling attackers)
    /**
     * <p>totalShieldDamage.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defenders a {@link forge.CardList} object.
     * @return a int.
     */
    public static int totalShieldDamage(Card attacker, CardList defenders) {

        int defenderDefense = 0;

        for (Card defender : defenders) defenderDefense += shieldDamage(attacker, defender);

        return defenderDefense;
    }

    // This calculates the amount of damage a blocker in a blockgang can take from the attacker (for trampling attackers)
    /**
     * <p>shieldDamage.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defender a {@link forge.Card} object.
     * @return a int.
     */
    public static int shieldDamage(Card attacker, Card defender) {

        if (!canDestroyBlocker(defender, attacker, null, false)) return 100;

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking")
                && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense())
                return 0;
            if (flankingMagnitude >= defender.getNetDefense() - defender.getDamage()
                    && !defender.hasKeyword("Indestructible"))
                return 0;

        }//flanking

        int defBushidoMagnitude = defender.getKeywordMagnitude("Bushido");

        int defenderDefense = defender.getNetDefense() - flankingMagnitude + defBushidoMagnitude;

        return defenderDefense;
    }//shieldDamage

    //For AI safety measures like Regeneration
    /**
     * <p>combatantWouldBeDestroyed.</p>
     *
     * @param combatant a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean combatantWouldBeDestroyed(Card combatant) {

        if (combatant.isAttacking())
            return attackerWouldBeDestroyed(combatant);
        if (combatant.isBlocking())
            return blockerWouldBeDestroyed(combatant);
        return false;
    }

    //For AI safety measures like Regeneration
    /**
     * <p>attackerWouldBeDestroyed.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean attackerWouldBeDestroyed(Card attacker) {
        CardList blockers = AllZone.getCombat().getBlockers(attacker);

        for (Card defender : blockers) {
            if (CombatUtil.canDestroyAttacker(attacker, defender, AllZone.getCombat(), true)
                    && !(defender.hasKeyword("Wither") || defender.hasKeyword("Infect")))
                return true;
        }

        return totalDamageOfBlockers(attacker, blockers) >= attacker.getKillDamage();
    }

    //Will this trigger trigger?
    /**
     * <p>combatTriggerWillTrigger.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defender a {@link forge.Card} object.
     * @param trigger a {@link forge.card.trigger.Trigger} object.
     * @param combat a {@link forge.Combat} object.
     * @return a boolean.
     */
    public static boolean combatTriggerWillTrigger(Card attacker, Card defender, Trigger trigger, Combat combat) {
        HashMap<String, String> trigParams = trigger.getMapParams();
        boolean willTrigger = false;
        Card source = trigger.getHostCard();
        if (combat == null) combat = AllZone.getCombat();

        if (!trigger.zonesCheck()) return false;
        if (!trigger.requirementsCheck()) return false;

        if (trigParams.get("Mode").equals("Attacks")) {
            willTrigger = true;
            if (attacker.isAttacking()) return false; //The trigger should have triggered already
            if (trigParams.containsKey("ValidCard")) {
                if (!trigger.matchesValid(attacker, trigParams.get("ValidCard").split(","), source)
                        && !(combat.isAttacking(source) &&
                        trigger.matchesValid(source, trigParams.get("ValidCard").split(","), source)))
                    return false;
            }
        }

        // defender == null means unblocked
        if (defender == null && trigParams.get("Mode").equals("AttackerUnblocked")) {
            willTrigger = true;
            if (trigParams.containsKey("ValidCard"))
                if (!trigger.matchesValid(attacker, trigParams.get("ValidCard").split(","), source))
                    return false;
        }

        if (defender == null) return willTrigger;

        if (trigParams.get("Mode").equals("Blocks")) {
            willTrigger = true;
            if (trigParams.containsKey("ValidBlocked"))
                if (!trigger.matchesValid(attacker, trigParams.get("ValidBlocked").split(","), source))
                    return false;
            if (trigParams.containsKey("ValidCard"))
                if (!trigger.matchesValid(defender, trigParams.get("ValidCard").split(","), source))
                    return false;
        } else if (trigParams.get("Mode").equals("AttackerBlocked")) {
            willTrigger = true;
            if (trigParams.containsKey("ValidBlocker"))
                if (!trigger.matchesValid(defender, trigParams.get("ValidBlocker").split(","), source))
                    return false;
            if (trigParams.containsKey("ValidCard"))
                if (!trigger.matchesValid(attacker, trigParams.get("ValidCard").split(","), source))
                    return false;
        }

        return willTrigger;
    }

    //Predict the Power bonus of the blocker if blocking the attacker (Flanking, Bushido and other triggered abilities)
    /**
     * <p>predictPowerBonusOfBlocker.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defender a {@link forge.Card} object.
     * @return a int.
     */
    public static int predictPowerBonusOfBlocker(Card attacker, Card defender) {
        int power = 0;

        if (attacker.hasKeyword("Flanking")
                && !defender.hasKeyword("Flanking"))
            power -= attacker.getAmountOfKeyword("Flanking");

        //if the attacker has first strike and wither the blocker will deal less damage than expected
        if ((attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike"))
                && (attacker.hasKeyword("Wither") || attacker.hasKeyword("Infect"))
                && !(defender.hasKeyword("First Strike") || defender.hasKeyword("Double Strike")
                || defender.hasKeyword("CARDNAME can't have counters placed on it.")))
            power -= attacker.getNetCombatDamage();

        power += defender.getKeywordMagnitude("Bushido");

        ArrayList<Trigger> registeredTriggers = AllZone.getTriggerHandler().getRegisteredTriggers();
        for (Trigger trigger : registeredTriggers) {
            HashMap<String, String> trigParams = trigger.getMapParams();
            Card source = trigger.getHostCard();

            if (!combatTriggerWillTrigger(attacker, defender, trigger, null) || !trigParams.containsKey("Execute"))
                continue;
            String ability = source.getSVar(trigParams.get("Execute"));
            AbilityFactory AF = new AbilityFactory();
            HashMap<String, String> abilityParams = AF.getMapParams(ability, source);
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump"))
                continue;
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump"))
                continue;
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt"))
                continue; //targeted pumping not supported
            ArrayList<Card> list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredBlocker"))
                list.add(defender);
            if (list.isEmpty()) continue;
            if (!list.contains(defender)) continue;
            if (!abilityParams.containsKey("NumAtt")) continue;

            String att = abilityParams.get("NumAtt");
            if (att.startsWith("+"))
                att = att.substring(1);
            try {
                power += Integer.parseInt(att);
            } catch (NumberFormatException nfe) {
                //can't parse the number (X for example)
                power += 0;
            }
        }
        return power;
    }

    //Predict the Toughness bonus of the blocker if blocking the attacker (Flanking, Bushido and other triggered abilities)
    /**
     * <p>predictToughnessBonusOfBlocker.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defender a {@link forge.Card} object.
     * @return a int.
     */
    public static int predictToughnessBonusOfBlocker(Card attacker, Card defender) {
        int toughness = 0;

        if (attacker.hasKeyword("Flanking")
                && !defender.hasKeyword("Flanking"))
            toughness -= attacker.getAmountOfKeyword("Flanking");

        toughness += defender.getKeywordMagnitude("Bushido");

        ArrayList<Trigger> registeredTriggers = AllZone.getTriggerHandler().getRegisteredTriggers();
        for (Trigger trigger : registeredTriggers) {
            HashMap<String, String> trigParams = trigger.getMapParams();
            Card source = trigger.getHostCard();

            if (!combatTriggerWillTrigger(attacker, defender, trigger, null) || !trigParams.containsKey("Execute"))
                continue;
            String ability = source.getSVar(trigParams.get("Execute"));
            AbilityFactory AF = new AbilityFactory();
            HashMap<String, String> abilityParams = AF.getMapParams(ability, source);
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump"))
                continue;
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump"))
                continue;
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt"))
                continue; //targeted pumping not supported
            ArrayList<Card> list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredBlocker"))
                list.add(defender);
            if (list.isEmpty()) continue;
            if (!list.contains(defender)) continue;
            if (!abilityParams.containsKey("NumDef")) continue;

            String def = abilityParams.get("NumDef");
            if (def.startsWith("+"))
                def = def.substring(1);
            try {
                toughness += Integer.parseInt(def);
            } catch (NumberFormatException nfe) {
                //can't parse the number (X for example)
                toughness += 0;
            }
        }
        return toughness;
    }

    //Predict the Power bonus of the blocker if blocking the attacker (Flanking, Bushido and other triggered abilities)
    /**
     * <p>predictPowerBonusOfAttacker.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defender a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @return a int.
     */
    public static int predictPowerBonusOfAttacker(Card attacker, Card defender, Combat combat) {
        int power = 0;

        power += attacker.getKeywordMagnitude("Bushido");

        //if the defender has first strike and wither the attacker will deal less damage than expected
        if (null != defender) {
            if ((defender.hasKeyword("First Strike") || defender.hasKeyword("Double Strike"))
                    && (defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))
                    && !(attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike")
                    || attacker.hasKeyword("CARDNAME can't have counters placed on it.")))
                power -= defender.getNetCombatDamage();
        }

        ArrayList<Trigger> registeredTriggers = AllZone.getTriggerHandler().getRegisteredTriggers();
        for (Trigger trigger : registeredTriggers) {
            HashMap<String, String> trigParams = trigger.getMapParams();
            Card source = trigger.getHostCard();

            if (!combatTriggerWillTrigger(attacker, defender, trigger, null) || !trigParams.containsKey("Execute"))
                continue;
            String ability = source.getSVar(trigParams.get("Execute"));
            AbilityFactory AF = new AbilityFactory();
            HashMap<String, String> abilityParams = AF.getMapParams(ability, source);
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt"))
                continue; //targeted pumping not supported
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump") && !abilityParams.get("AB").equals("PumpAll"))
                continue;
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump") && !abilityParams.get("DB").equals("PumpAll"))
                continue;
            ArrayList<Card> list = new ArrayList<Card>();
            if (!abilityParams.containsKey("ValidCards")) //no pumpAll
                list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredAttacker"))
                list.add(attacker);
            if (abilityParams.containsKey("ValidCards"))
                if (attacker.isValid(abilityParams.get("ValidCards").split(","), source.getController(), source)
                        || attacker.isValid(abilityParams.get("ValidCards").replace("attacking+", "").split(",")
                        , source.getController(), source))
                    list.add(attacker);
            if (list.isEmpty()) continue;
            if (!list.contains(attacker)) continue;
            if (!abilityParams.containsKey("NumAtt")) continue;

            String att = abilityParams.get("NumAtt");
            if (att.startsWith("+"))
                att = att.substring(1);
            try {
                power += Integer.parseInt(att);
            } catch (NumberFormatException nfe) {
                //can't parse the number (X for example)
                power += 0;
            }
        }
        return power;
    }

    //Predict the Toughness bonus of the blocker if blocking the attacker (Flanking, Bushido and other triggered abilities)
    /**
     * <p>predictToughnessBonusOfAttacker.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defender a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @return a int.
     */
    public static int predictToughnessBonusOfAttacker(Card attacker, Card defender, Combat combat) {
        int toughness = 0;

        if (defender != null) {
            toughness += attacker.getKeywordMagnitude("Bushido");
        }

        ArrayList<Trigger> registeredTriggers = AllZone.getTriggerHandler().getRegisteredTriggers();
        for (Trigger trigger : registeredTriggers) {
            HashMap<String, String> trigParams = trigger.getMapParams();
            Card source = trigger.getHostCard();

            if (!combatTriggerWillTrigger(attacker, defender, trigger, null) || !trigParams.containsKey("Execute"))
                continue;
            String ability = source.getSVar(trigParams.get("Execute"));
            AbilityFactory AF = new AbilityFactory();
            HashMap<String, String> abilityParams = AF.getMapParams(ability, source);
            if (abilityParams.containsKey("ValidTgts") || abilityParams.containsKey("Tgt"))
                continue; //targeted pumping not supported
            
            // DealDamage triggers
            if ((abilityParams.containsKey("AB") && abilityParams.get("AB").equals("DealDamage"))
                    || (abilityParams.containsKey("DB") && abilityParams.get("DB").equals("DealDamage"))) {
                if (!abilityParams.containsKey("Defined") || !abilityParams.get("Defined").equals("TriggeredAttacker")) {
                    continue;
                }
                int damage = 0;
                try {
                    damage = Integer.parseInt(abilityParams.get("NumDmg"));
                } catch (NumberFormatException nfe) {
                    //can't parse the number (X for example)
                    continue;
                }
                toughness -= attacker.predictDamage(damage, 0, source, false);
                continue;
            }
            
            // Pump triggers
            if (abilityParams.containsKey("AB") && !abilityParams.get("AB").equals("Pump") && !abilityParams.get("AB").equals("PumpAll"))
                continue;
            if (abilityParams.containsKey("DB") && !abilityParams.get("DB").equals("Pump") && !abilityParams.get("DB").equals("PumpAll"))
                continue;
            ArrayList<Card> list = new ArrayList<Card>();
            if (!abilityParams.containsKey("ValidCards")) //no pumpAll
                list = AbilityFactory.getDefinedCards(source, abilityParams.get("Defined"), null);
            if (abilityParams.containsKey("Defined") && abilityParams.get("Defined").equals("TriggeredAttacker"))
                list.add(attacker);
            if (abilityParams.containsKey("ValidCards"))
                if (attacker.isValid(abilityParams.get("ValidCards").split(","), source.getController(), source)
                        || attacker.isValid(abilityParams.get("ValidCards").replace("attacking+", "").split(",")
                                , source.getController(), source))
                    list.add(attacker);
            if (list.isEmpty()) continue;
            if (!list.contains(attacker)) continue;
            if (!abilityParams.containsKey("NumDef")) continue;

            String def = abilityParams.get("NumDef");
            if (def.startsWith("+"))
                def = def.substring(1);
            try {
                toughness += Integer.parseInt(def);
            } catch (NumberFormatException nfe) {
                //can't parse the number (X for example)
                toughness += 0;
            }
        }
        return toughness;
    }

    //can the blocker destroy the attacker?
    /**
     * <p>canDestroyAttacker.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param defender a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @param withoutAbilities a boolean.
     * @return a boolean.
     */
    public static boolean canDestroyAttacker(Card attacker, Card defender, Combat combat, boolean withoutAbilities) {

        if (attacker.getName().equals("Sylvan Basilisk") && !defender.hasKeyword("Indestructible")) return false;

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) return false;
            if (flankingMagnitude >= defender.getNetDefense() - defender.getDamage() && !defender.hasKeyword("Indestructible"))
                return false;
        }//flanking

        if ((attacker.hasKeyword("Indestructible") || (ComputerUtil.canRegenerate(attacker) && !withoutAbilities)) &&
                !(defender.hasKeyword("Wither") || defender.hasKeyword("Infect"))) return false;

        int defenderDamage = defender.getNetAttack() + predictPowerBonusOfBlocker(attacker, defender);
        int attackerDamage = attacker.getNetAttack() + predictPowerBonusOfAttacker(attacker, defender, combat);
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
            defenderDamage = defender.getNetDefense() + predictToughnessBonusOfBlocker(attacker, defender);
            attackerDamage = attacker.getNetDefense() + predictToughnessBonusOfAttacker(attacker, defender, combat);
        }

        int possibleDefenderPrevention = 0;
        int possibleAttackerPrevention = 0;
        if (!withoutAbilities) {
            possibleDefenderPrevention = ComputerUtil.possibleDamagePrevention(defender);
            possibleAttackerPrevention = ComputerUtil.possibleDamagePrevention(attacker);
        }

        // consider Damage Prevention/Replacement
        defenderDamage = attacker.predictDamage(defenderDamage, possibleAttackerPrevention, defender, true);
        attackerDamage = defender.predictDamage(attackerDamage, possibleDefenderPrevention, attacker, true);

        int defenderLife = defender.getKillDamage() + predictToughnessBonusOfBlocker(attacker, defender);
        int attackerLife = attacker.getKillDamage() + predictToughnessBonusOfAttacker(attacker, defender, combat);

        if (defender.hasKeyword("Double Strike")) {
            if (defender.hasKeyword("Deathtouch") && defenderDamage > 0) return true;
            if (defenderDamage >= attackerLife) return true;

            //Attacker may kill the blocker before he can deal normal (secondary) damage
            if ((attacker.hasKeyword("Double Strike") || attacker.hasKeyword("First Strike"))
                    && !defender.hasKeyword("Indestructible")) {
                if (attackerDamage >= defenderLife) return false;
                if (attackerDamage > 0 && attacker.hasKeyword("Deathtouch")) return false;
            }
            if (attackerLife <= 2 * defenderDamage) return true;
        }//defender double strike

        else //no double strike for defender
        {
            //Attacker may kill the blocker before he can deal any damage
            if (attacker.hasKeyword("Double Strike") || attacker.hasKeyword("First Strike")
                    && !defender.hasKeyword("Indestructible") && !defender.hasKeyword("First Strike")) {

                if (attackerDamage >= defenderLife) return false;
                if (attackerDamage > 0 && attacker.hasKeyword("Deathtouch")) return false;
            }

            if (defender.hasKeyword("Deathtouch") && defenderDamage > 0) return true;

            return defenderDamage >= attackerLife;

        }//defender no double strike
        return false; //should never arrive here
    } //canDestroyAttacker


    //For AI safety measures like Regeneration
    /**
     * <p>blockerWouldBeDestroyed.</p>
     *
     * @param blocker a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean blockerWouldBeDestroyed(Card blocker) {
        Card attacker = AllZone.getCombat().getAttackerBlockedBy(blocker);

        if (canDestroyBlocker(blocker, attacker, AllZone.getCombat(), true) &&
                !(attacker.hasKeyword("Wither") || attacker.hasKeyword("Infect")))
            return true;
        return false;
    }

    //can the attacker destroy this blocker?
    /**
     * <p>canDestroyBlocker.</p>
     *
     * @param defender a {@link forge.Card} object.
     * @param attacker a {@link forge.Card} object.
     * @param combat a {@link forge.Combat} object.
     * @param withoutAbilities a boolean.
     * @return a boolean.
     */
    public static boolean canDestroyBlocker(Card defender, Card attacker, Combat combat, boolean withoutAbilities) {

        int flankingMagnitude = 0;
        if (attacker.hasKeyword("Flanking") && !defender.hasKeyword("Flanking")) {

            flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if (flankingMagnitude >= defender.getNetDefense()) return true;
            if ((flankingMagnitude >= defender.getKillDamage()) && !defender.hasKeyword("Indestructible")) return true;
        }//flanking

        if ((defender.hasKeyword("Indestructible") || (ComputerUtil.canRegenerate(defender) && !withoutAbilities)) &&
                !(attacker.hasKeyword("Wither") || attacker.hasKeyword("Infect"))) return false;

        if (attacker.getName().equals("Sylvan Basilisk") && !defender.hasKeyword("Indestructible")) return true;

        int defenderDamage = defender.getNetAttack() + predictPowerBonusOfBlocker(attacker, defender);
        int attackerDamage = attacker.getNetAttack() + predictPowerBonusOfAttacker(attacker, defender, combat);
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
            defenderDamage = defender.getNetDefense() + predictToughnessBonusOfBlocker(attacker, defender);
            attackerDamage = attacker.getNetDefense() + predictToughnessBonusOfAttacker(attacker, defender, combat);
        }

        int possibleDefenderPrevention = 0;
        int possibleAttackerPrevention = 0;
        if (!withoutAbilities) {
            possibleDefenderPrevention = ComputerUtil.possibleDamagePrevention(defender);
            possibleAttackerPrevention = ComputerUtil.possibleDamagePrevention(attacker);
        }

        // consider Damage Prevention/Replacement
        defenderDamage = attacker.predictDamage(defenderDamage, possibleAttackerPrevention, defender, true);
        attackerDamage = defender.predictDamage(attackerDamage, possibleDefenderPrevention, attacker, true);

        int defenderLife = defender.getKillDamage() + predictToughnessBonusOfBlocker(attacker, defender);
        int attackerLife = attacker.getKillDamage() + predictToughnessBonusOfAttacker(attacker, defender, combat);

        if (attacker.hasKeyword("Double Strike")) {
            if (attacker.hasKeyword("Deathtouch") && attackerDamage > 0) return true;
            if (attackerDamage >= defenderLife) return true;

            //Attacker may kill the blocker before he can deal normal (secondary) damage
            if ((defender.hasKeyword("Double Strike") || defender.hasKeyword("First Strike"))
                    && !attacker.hasKeyword("Indestructible")) {
                if (defenderDamage >= attackerLife) return false;
                if (defenderDamage > 0 && defender.hasKeyword("Deathtouch")) return false;
            }
            if (defenderLife <= 2 * attackerDamage) return true;
        }//attacker double strike

        else //no double strike for attacker
        {
            //Defender may kill the attacker before he can deal any damage
            if (defender.hasKeyword("Double Strike") || defender.hasKeyword("First Strike")
                    && !attacker.hasKeyword("Indestructible") && !attacker.hasKeyword("First Strike")) {

                if (defenderDamage >= attackerLife) return false;
                if (defenderDamage > 0 && defender.hasKeyword("Deathtouch")) return false;
            }

            if (attacker.hasKeyword("Deathtouch") && attackerDamage > 0) return true;

            return attackerDamage >= defenderLife;

        }//attacker no double strike
        return false; //should never arrive here
    }//canDestroyBlocker

    /**
     * <p>removeAllDamage.</p>
     */
    public static void removeAllDamage() {
        CardList cl = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (Card c : cl) {
            c.setDamage(0);
        }
    }

    /**
     * <p>showCombat.</p>
     */
    public static void showCombat() {
        AllZone.getDisplay().showCombat("");

        Card defend[] = null;
        StringBuilder display = new StringBuilder();

        // Loop through Defenders
        // Append Defending Player/Planeswalker
        ArrayList<Object> defenders = AllZone.getCombat().getDefenders();
        CardList attackers[] = AllZone.getCombat().sortAttackerByDefender();

        // Not a big fan of the triple nested loop here
        for (int def = 0; def < defenders.size(); def++) {
            if (attackers[def] == null || attackers[def].size() == 0)
                continue;

            if (def > 0)
                display.append("\n");

            display.append("Defender - ");
            display.append(defenders.get(def).toString());
            display.append("\n");

            CardList list = attackers[def];

            for (Card c : list) {
                //loop through attackers
                display.append("-> ");
                display.append(combatantToString(c)).append("\n");

                defend = AllZone.getCombat().getBlockers(c).toArray();

                //loop through blockers
                for (int inner = 0; inner < defend.length; inner++) {
                    display.append(" [ ");
                    display.append(combatantToString(defend[inner])).append("\n");
                }
            }//loop through attackers
        }
        AllZone.getDisplay().showCombat(display.toString().trim());

    }//showBlockers()

    /**
     * <p>combatantToString.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    private static String combatantToString(Card c) {
        StringBuilder sb = new StringBuilder();

        String name = (c.isFaceDown()) ? "Morph" : c.getName();

        sb.append(name);
        sb.append(" (").append(c.getUniqueNumber()).append(") ");
        sb.append(c.getNetAttack()).append("/").append(c.getNetDefense());

        return sb.toString();
    }

    /**
     * <p>isDoranInPlay.</p>
     *
     * @return a boolean.
     */
    public static boolean isDoranInPlay() {
        return AllZoneUtil.isCardInPlay("Doran, the Siege Tower");
    }

    /**
     * <p>checkPropagandaEffects.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param bLast a boolean.
     */
    public static void checkPropagandaEffects(Card c, final boolean bLast) {
        String cost = CardFactoryUtil.getPropagandaCost(c);
        if (cost.equals("0")) {
            if (!c.hasKeyword("Vigilance"))
                c.tap();

            if (bLast)
                PhaseUtil.handleAttackingTriggers();
            return;
        }

        final Card crd = c;

        String phase = AllZone.getPhase().getPhase();

        if (phase.equals(Constant.Phase.Combat_Declare_Attackers) || phase.equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)) {
            if (!cost.equals("0")) {
                final Ability ability = new Ability(c, cost) {
                    @Override
                    public void resolve() {

                    }
                };

                final Command unpaidCommand = new Command() {

                    private static final long serialVersionUID = -6483405139208343935L;

                    public void execute() {
                        AllZone.getCombat().removeFromCombat(crd);

                        if (bLast)
                            PhaseUtil.handleAttackingTriggers();
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = -8303368287601871955L;

                    public void execute() {
                        // if Propaganda is paid, tap this card
                        if (!crd.hasKeyword("Vigilance"))
                            crd.tap();

                        if (bLast)
                            PhaseUtil.handleAttackingTriggers();
                    }
                };

                if (c.getController().isHuman()) {
                    AllZone.getInputControl().setInput(new Input_PayManaCost_Ability(c + " - Pay to Attack\r\n",
                            ability.getManaCost(), paidCommand, unpaidCommand));
                } else { //computer
                    if (ComputerUtil.canPayCost(ability)) {
                        ComputerUtil.playNoStack(ability);
                        if (!crd.hasKeyword("Vigilance"))
                            crd.tap();
                    } else {
                        // TODO: remove the below line after Propaganda occurs during Declare_Attackers
                        AllZone.getCombat().removeFromCombat(crd);
                    }
                }
            }
        }
    }

    /**
     * <p>checkDeclareAttackers.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public static void checkDeclareAttackers(Card c) //this method checks triggered effects of attacking creatures, right before defending player declares blockers
    {
        //Run triggers
        HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Attacker", c);
        CardList otherAttackers = new CardList(AllZone.getCombat().getAttackers());
        otherAttackers.remove(c);
        runParams.put("OtherAttackers", otherAttackers);
        AllZone.getTriggerHandler().runTrigger("Attacks", runParams);

        //Annihilator:
        if (!c.getCreatureAttackedThisCombat()) {
            ArrayList<String> kws = c.getKeyword();
            Pattern p = Pattern.compile("Annihilator [0-9]+");
            Matcher m;
            for (String key : kws) {
                m = p.matcher(key);
                if (m.find()) {
                    String k[] = key.split(" ");
                    final int a = Integer.valueOf(k[1]);
                    final Card crd = c;

                    final Ability ability = new Ability(c, "0") {
                        public void resolve() {
                            if (crd.getController().isHuman()) {
                                CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                                ComputerUtil.sacrificePermanents(a, list);
                            } else {
                                AllZone.getInputControl().setInput(PlayerUtil.input_sacrificePermanents(a));
                            }

                        }
                    };
                    StringBuilder sb = new StringBuilder();
                    sb.append("Annihilator - Defending player sacrifices ").append(a).append(" permanents.");
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().add(ability);
                } //find
            } //for
        }//creatureAttacked
        //Annihilator

        //Mijae Djinn
        if (c.getName().equals("Mijae Djinn")) {
            if (!GameActionUtil.flipACoin(c.getController(), c)) {
                AllZone.getCombat().removeFromCombat(c);
                c.tap();
            }
        }//Mijae Djinn

        if (c.getName().equals("Zur the Enchanter") && !c.getCreatureAttackedThisCombat()) {
            //hack, to make sure this doesn't break grabbing an oblivion ring:
            c.setCreatureAttackedThisCombat(true);

            CardList enchantments = c.getController().getCardsIn(Zone.Library);
            enchantments = enchantments.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                    if (c.isEnchantment() && c.getCMC() <= 3) return true;
                    else return false;
                }
            });

            if (enchantments.size() > 0) {
                if (c.getController().isHuman()) {
                    Object o = GuiUtils.getChoiceOptional("Pick an enchantment to put onto the battlefield",
                            enchantments.toArray());
                    if (o != null) {
                        Card crd = (Card) o;
                        AllZone.getGameAction().moveToPlay(crd);

                        if (crd.isAura()) {
                            Object obj = null;
                            if (crd.hasKeyword("Enchant creature")) {
                                CardList creats = AllZoneUtil.getCreaturesInPlay();
                                obj = GuiUtils.getChoiceOptional("Pick a creature to attach "
                                        + crd.getName() + " to", creats.toArray());
                            } else if (crd.hasKeyword("Enchant land")
                                    || crd.hasKeyword("Enchant land you control")) {
                                CardList lands = AllZoneUtil.getLandsInPlay();
                                if (lands.size() > 0) obj = GuiUtils.getChoiceOptional(
                                        "Pick a land to attach " + crd.getName() + " to", lands.toArray());
                            }
                            if (obj != null) {
                                Card target = (Card) obj;
                                if (AllZoneUtil.isCardInPlay(target)) {
                                    crd.enchantEntity(target);
                                }
                            }
                        }
                        c.getController().shuffle();
                        //we have to have cards like glorious anthem take effect immediately:
                        for (String effect : AllZone.getStaticEffects().getStateBasedMap().keySet()) {
                            Command com = GameActionUtil.commands.get(effect);
                            com.execute();
                        }

                    }
                } else if (c.getController().isComputer()) {
                    enchantments = enchantments.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.isAura();
                        }
                    });
                    if (enchantments.size() > 0) {
                        Card card = CardFactoryUtil.AI_getBestEnchantment(enchantments, c, false);
                        AllZone.getGameAction().moveToPlay(card);
                        c.getController().shuffle();
                    }
                }
            } //enchantments.size > 0
        }//Zur the enchanter

        else if (c.getName().equals("Spectral Bears")) {
            Player opp = c.getController().getOpponent();
            CardList list = opp.getCardsIn(Zone.Battlefield);
            list = list.filter(new CardListFilter() {
                public boolean addCard(Card crd) {
                    return crd.isBlack() && !crd.isToken();
                }
            });
            if (list.size() == 0) {
                c.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
            }
        } else if (c.getName().equals("Spectral Force")) {
            Player opp = c.getController().getOpponent();
            CardList list = opp.getCardsIn(Zone.Battlefield);
            list = list.filter(CardListFilter.black);
            if (list.size() == 0) {
                c.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
            }
        } else if (c.getName().equals("Witch-Maw Nephilim") && !c.getCreatureAttackedThisCombat()
                && c.getNetAttack() >= 10) {
            final Card charger = c;
            Ability ability2 = new Ability(c, "0") {
                @Override
                public void resolve() {

                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -1703473800920781454L;

                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(charger)) {
                                charger.removeIntrinsicKeyword("Trample");
                            }
                        }
                    };//Command

                    if (AllZoneUtil.isCardInPlay(charger)) {
                        charger.addIntrinsicKeyword("Trample");

                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }//resolve
            };//ability

            StringBuilder sb2 = new StringBuilder();
            sb2.append(c.getName()).append(" - gains trample until end of turn if its power is 10 or greater.");
            ability2.setStackDescription(sb2.toString());

            AllZone.getStack().add(ability2);

        }//Witch-Maw Nephilim

        else if (c.getName().equals("Preeminent Captain") && !c.getCreatureAttackedThisCombat()) {
            System.out.println("Preeminent Captain Attacks");

            CardList soldiers = c.getController().getCardsIn(Zone.Hand);
            soldiers = soldiers.getType("Soldier");

            if (soldiers.size() > 0) {
                if (c.getController().isHuman()) {
                    Object o = GuiUtils.getChoiceOptional("Pick a soldier to put onto the battlefield",
                            soldiers.toArray());
                    if (o != null) {
                        Card card = (Card) o;
                        AllZone.getGameAction().moveToPlay(card);

                        card.tap();
                        AllZone.getCombat().addAttacker(card);

                        card.setCreatureAttackedThisCombat(true);
                    }
                } else if (c.getController().isComputer()) {
                    Card card = CardFactoryUtil.AI_getBestCreature(soldiers);
                    if (card != null) {
                        AllZone.getGameAction().moveToPlay(card);

                        card.tap();
                        AllZone.getCombat().addAttacker(card);
                        card.setCreatureAttackedThisCombat(true);
                    }
                }

            } //if (creatures.size() > 0)
        }//Preeminent Captain

        else if (c.getName().equals("Sapling of Colfenor")
                && !c.getCreatureAttackedThisCombat()) {
            Player player = c.getController();

            PlayerZone lib = player.getZone(Constant.Zone.Library);

            if (lib.size() > 0) {
                CardList cl = new CardList();
                cl.add(lib.get(0));
                GuiUtils.getChoiceOptional("Top card", cl.toArray());
                Card top = lib.get(0);
                if (top.isCreature()) {
                    player.gainLife(top.getBaseDefense(), c);
                    player.loseLife(top.getBaseAttack(), c);

                    AllZone.getGameAction().moveToHand(top);
                }
            }
        }//Sapling of Colfenor

        c.setCreatureAttackedThisCombat(true);
    }//checkDeclareAttackers

    /**
     * <p>checkUnblockedAttackers.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public static void checkUnblockedAttackers(Card c) {

        //Run triggers
        HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        AllZone.getTriggerHandler().runTrigger("AttackerUnblocked", runParams);
    }

    /**
     * <p>checkDeclareBlockers.</p>
     *
     * @param cl a {@link forge.CardList} object.
     */
    public static void checkDeclareBlockers(CardList cl) {
        for (Card c : cl) {
            if (!c.getCreatureBlockedThisCombat()) {
                for (Ability ab : CardFactoryUtil.getBushidoEffects(c)) {
                    AllZone.getStack().add(ab);
                }
            }

            c.setCreatureBlockedThisCombat(true);
        }//for

    }//checkDeclareBlockers

    /**
     * <p>checkBlockedAttackers.</p>
     *
     * @param a a {@link forge.Card} object.
     * @param b a {@link forge.Card} object.
     */
    public static void checkBlockedAttackers(final Card a, Card b) {
        //System.out.println(a.getName() + " got blocked by " + b.getName());

        //Run triggers
        HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Attacker", a);
        runParams.put("Blocker", b);
        AllZone.getTriggerHandler().runTrigger("Blocks", runParams);

        if (!a.getCreatureGotBlockedThisCombat()) {
            final int blockers = AllZone.getCombat().getBlockers(a).size();
            runParams.put("NumBlockers", blockers);
            AllZone.getTriggerHandler().runTrigger("AttackerBlocked", runParams);

            //Bushido
            for (Ability ab : CardFactoryUtil.getBushidoEffects(a))
                AllZone.getStack().add(ab);

            //Rampage
            ArrayList<String> keywords = a.getKeyword();
            Pattern p = Pattern.compile("Rampage [0-9]+");
            Matcher m;
            for (String keyword : keywords) {
                m = p.matcher(keyword);
                if (m.find()) {
                    String k[] = keyword.split(" ");
                    final int magnitude = Integer.valueOf(k[1]);
                    final int numBlockers = AllZone.getCombat().getBlockers(a).size();
                    if (numBlockers > 1) {
                        executeRampageAbility(a, magnitude, numBlockers);
                    }
                } //find
            }//end Rampage
        }

        if (a.hasKeyword("Flanking") && !b.hasKeyword("Flanking")) {
            int flankingMagnitude = 0;
            String kw = "";
            ArrayList<String> list = a.getKeyword();

            for (int i = 0; i < list.size(); i++) {
                kw = list.get(i);
                if (kw.equals("Flanking")) flankingMagnitude++;
            }
            final int mag = flankingMagnitude;
            final Card blocker = b;
            Ability ability2 = new Ability(b, "0") {
                @Override
                public void resolve() {

                    final Command untilEOT = new Command() {

                        private static final long serialVersionUID = 7662543891117427727L;

                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(blocker)) {
                                blocker.addTempAttackBoost(mag);
                                blocker.addTempDefenseBoost(mag);
                            }
                        }
                    };//Command


                    if (AllZoneUtil.isCardInPlay(blocker)) {
                        blocker.addTempAttackBoost(-mag);
                        blocker.addTempDefenseBoost(-mag);

                        AllZone.getEndOfTurn().addUntil(untilEOT);
                        System.out.println("Flanking!");
                    }
                }//resolve

            };//ability

            StringBuilder sb2 = new StringBuilder();
            sb2.append(b.getName()).append(" - gets -").append(mag).append("/-").append(mag).append(" until EOT.");
            ability2.setStackDescription(sb2.toString());

            AllZone.getStack().add(ability2);
            Log.debug("Adding Flanking!");

        }//flanking


        if (a.getName().equals("Robber Fly") && !a.getCreatureGotBlockedThisCombat()) {
            Player opp = b.getController();
            CardList list = opp.getCardsIn(Zone.Hand);
            int handSize = list.size();

            // opponent discards their hand,
            opp.discardRandom(handSize, a.getSpellAbility()[0]);
            opp.drawCards(handSize);
        }

        a.setCreatureGotBlockedThisCombat(true);

    }

    /**
     * <p>executeExaltedAbility.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param magnitude a int.
     */
    public static void executeExaltedAbility(Card c, int magnitude) {
        final Card crd = c;
        Ability ability;

        for (int i = 0; i < magnitude; i++) {
            ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 1497565871061029469L;

                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(crd)) {
                                crd.addTempAttackBoost(-1);
                                crd.addTempDefenseBoost(-1);
                            }
                        }
                    };//Command

                    if (AllZoneUtil.isCardInPlay(crd)) {
                        crd.addTempAttackBoost(1);
                        crd.addTempDefenseBoost(1);

                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }//resolve

            };//ability

            StringBuilder sb = new StringBuilder();
            sb.append(c).append(" - (Exalted) gets +1/+1 until EOT.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);
        }

        Player phasingPlayer = c.getController();
        // Finest Hour untaps the creature on the first combat phase
        if ((phasingPlayer.getCardsIn(Zone.Battlefield, "Finest Hour").size() > 0) &&
                AllZone.getPhase().isFirstCombat()) {
            // Untap the attacking creature
            Ability fhUntap = new Ability(c, "0") {
                public void resolve() {
                    crd.untap();
                }
            };

            StringBuilder sbUntap = new StringBuilder();
            sbUntap.append(c).append(" - (Exalted) untap.");
            fhUntap.setStackDescription(sbUntap.toString());

            AllZone.getStack().addSimultaneousStackEntry(fhUntap);

            // If any Finest Hours, queue up a new combat phase
            for (int ix = 0; ix < phasingPlayer.getCardsIn(Zone.Battlefield, "Finest Hour").size(); ix++) {
                Ability fhAddCombat = new Ability(c, "0") {
                    public void resolve() {
                        AllZone.getPhase().addExtraCombat();
                    }
                };

                StringBuilder sbACom = new StringBuilder();
                sbACom.append(c).append(" - (Exalted) ").append(phasingPlayer).append(" gets Extra Combat Phase.");
                fhAddCombat.setStackDescription(sbACom.toString());

                AllZone.getStack().addSimultaneousStackEntry(fhAddCombat);
            }
        }

        if (phasingPlayer.getCardsIn(Zone.Battlefield, "Sovereigns of Lost Alara").size() > 0) {
            for (int i = 0; i < phasingPlayer.getCardsIn(Zone.Battlefield, "Sovereigns of Lost Alara").size(); i++) {
                final Card attacker = c;
                Ability ability4 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList enchantments = attacker.getController().getCardsIn(Zone.Library);
                        enchantments = enchantments.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                if (attacker.hasKeyword("Protection from enchantments")
                                        || (attacker.hasKeyword("Protection from everything"))) return false;
                                return (c.isEnchantment()
                                        && c.hasKeyword("Enchant creature")
                                        && !CardFactoryUtil.hasProtectionFrom(c, attacker));
                            }
                        });
                        Player player = attacker.getController();
                        Card Enchantment = null;
                        if (player.isHuman()) {
                            Card[] Target = new Card[enchantments.size()];
                            for (int j = 0; j < enchantments.size(); j++) {
                                Card crd = enchantments.get(j);
                                Target[j] = crd;
                            }
                            Object check = GuiUtils.getChoiceOptional("Select enchantment to enchant exalted creature", Target);
                            if (check != null) {
                                Enchantment = ((Card) check);
                            }
                        } else {
                            Enchantment = CardFactoryUtil.AI_getBestEnchantment(enchantments, attacker, false);
                        }
                        if (Enchantment != null && AllZoneUtil.isCardInPlay(attacker)) {
                            GameAction.changeZone(AllZone.getZoneOf(Enchantment), 
                                    Enchantment.getOwner().getZone(Constant.Zone.Battlefield), Enchantment);
                            Enchantment.enchantEntity(attacker);
                        }
                        attacker.getController().shuffle();
                    }//resolve
                };// ability4

                StringBuilder sb4 = new StringBuilder();
                sb4.append(c).append(" - (Exalted) searches library for an Aura card that could enchant that creature, ");
                sb4.append("put it onto the battlefield attached to that creature, then shuffles library.");
                ability4.setDescription(sb4.toString());
                ability4.setStackDescription(sb4.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability4);
            } // For
        }
    }

    /**
     * executes Rampage abilities for a given card
     *
     * @param c           the card to add rampage bonus to
     * @param magnitude   the magnitude of rampage (ie Rampage 2 means magnitude should be 2)
     * @param numBlockers - the number of creatures blocking this rampaging creature
     */
    private static void executeRampageAbility(Card c, int magnitude, int numBlockers) {
        final Card crd = c;
        final int pump = magnitude;
        Ability ability;

        //numBlockers -1 since it is for every creature beyond the first
        for (int i = 0; i < numBlockers - 1; i++) {
            ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -3215615538474963181L;

                        public void execute() {
                            if (AllZoneUtil.isCardInPlay(crd)) {
                                crd.addTempAttackBoost(-pump);
                                crd.addTempDefenseBoost(-pump);
                            }
                        }
                    };//Command

                    if (AllZoneUtil.isCardInPlay(crd)) {
                        crd.addTempAttackBoost(pump);
                        crd.addTempDefenseBoost(pump);

                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }//resolve

            };//ability

            StringBuilder sb = new StringBuilder();
            sb.append(c).append(" - (Rampage) gets +").append(pump).append("/+").append(pump).append(" until EOT.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().add(ability);
        }
    }

}//end class CombatUtil
