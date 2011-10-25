package forge.card.abilityFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.MyRandom;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

/**
 * <p>AbilityFactory_Token class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_Token extends AbilityFactory {
    private AbilityFactory AF = null;

    private String tokenAmount;
    private String tokenName;
    private String[] tokenTypes;
    private String tokenOwner;
    private String[] tokenColors;
    private String[] tokenKeywords;
    private String tokenPower;
    private String tokenToughness;
    private String tokenImage;
    private String[] tokenAbilities;
    private String[] tokenTriggers;
    private String[] tokenSVars;
    private String[] tokenStaticAbilities;
    private boolean tokenTapped;
    private boolean tokenAttacking;

    /**
     * <p>Constructor for AbilityFactory_Token.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     */
    public AbilityFactory_Token(final AbilityFactory af) {
        AF = af;

        HashMap<String, String> mapParams = af.getMapParams();
        String image;
        String[] keywords;

        if (mapParams.containsKey("TokenKeywords")) {
        	// TODO: Change this Split to a semicolon or something else
            keywords = mapParams.get("TokenKeywords").split("<>");
        } else {
            keywords = new String[0];
        }

        if (mapParams.containsKey("TokenImage")) {
            image = mapParams.get("TokenImage");
        } else {
            image = "";
        }

        if (mapParams.containsKey("TokenTapped")) {
            tokenTapped = mapParams.get("TokenTapped").equals("True");
        } else {
            tokenTapped = false;
        }
        if (mapParams.containsKey("TokenAttacking")) {
            tokenAttacking = mapParams.get("TokenAttacking").equals("True");
        } else {
            tokenAttacking = false;
        }

        if (mapParams.containsKey("TokenAbilities")) {
            tokenAbilities = mapParams.get("TokenAbilities").split(",");
        } else {
            tokenAbilities = null;
        }
        if (mapParams.containsKey("TokenTriggers")) {
            tokenTriggers = mapParams.get("TokenTriggers").split(",");
        } else {
            tokenTriggers = null;
        }
        if (mapParams.containsKey("TokenSVars")) {
            tokenSVars = mapParams.get("TokenSVars").split(",");
        } else {
            tokenSVars = null;
        }
        if (mapParams.containsKey("TokenStaticAbilities")) {
            tokenStaticAbilities = mapParams.get("TokenStaticAbilities").split(",");
        } else {
            tokenStaticAbilities = null;
        }

        tokenAmount = mapParams.get("TokenAmount");
        tokenPower = mapParams.get("TokenPower");
        tokenToughness = mapParams.get("TokenToughness");
        tokenName = mapParams.get("TokenName");
        tokenTypes = mapParams.get("TokenTypes").split(",");
        tokenColors = mapParams.get("TokenColors").split(",");
        tokenKeywords = keywords;
        tokenImage = image;
        if (mapParams.containsKey("TokenOwner"))
            tokenOwner = mapParams.get("TokenOwner");
        else tokenOwner = "You";
    }

    /**
     * <p>getAbility.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getAbility() {


        final SpellAbility abToken = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = 8460074843405764620L;

            @Override
            public boolean canPlayAI() {
                return tokenCanPlayAI(this);
            }

            @Override
            public void resolve() {
                doResolve(this);
            }

            @Override
            public String getStackDescription() {
                return doStackDescription(this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return tokenDoTriggerAI(this, mandatory);
            }
        };

        return abToken;
    }

    /**
     * <p>getSpell.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getSpell() {
        final SpellAbility spToken = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = -8041427947613029670L;

            @Override
            public boolean canPlayAI() {
                return tokenCanPlayAI(this);
            }

            @Override
            public void resolve() {
                doResolve(this);
            }

            @Override
            public String getStackDescription() {
                return doStackDescription(this);
            }
        };

        return spToken;
    }

    /**
     * <p>getDrawback.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getDrawback() {
        final SpellAbility dbDealDamage = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
            private static final long serialVersionUID = 7239608350643325111L;

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public String getStackDescription() {
                return doStackDescription(this);
            }

            @Override
            public void resolve() {
                doResolve(this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return tokenDoTriggerAI(this, mandatory);
            }

        }; // Spell

        return dbDealDamage;
    }

    /**
     * <p>tokenCanPlayAI.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean tokenCanPlayAI(SpellAbility sa) {
        Cost cost = sa.getPayCosts();

        for (String type : tokenTypes) {
            if (type.equals("Legendary")) {
                // Don't kill AIs Legendary tokens
                if (AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield, tokenName).size() > 0)
                    return false;
            }
        }

        boolean haste = false;
        boolean oneShot = false;
        for (String kw : tokenKeywords) {
            if (kw.equals("Haste")) haste = true;
            if (kw.equals("At the beginning of the end step, exile CARDNAME.")
                    || kw.equals("At the beginning of the end step, sacrifice CARDNAME.")) oneShot = true;
        }

        //Don't generate tokens without haste before main 2 if possible
        if (AllZone.getPhase().isBefore(Constant.Phase.Main2) && AllZone.getPhase().isPlayerTurn(AllZone.getComputerPlayer()) && !haste)
            return false;
        if ((AllZone.getPhase().isAfter(Constant.Phase.Combat_Begin) || AllZone.getPhase().isPlayerTurn(AllZone.getHumanPlayer())) && oneShot)
            return false;

        // TODO: if i don't have enough blockers and my token can block one of the unblocked creatures
        // create it after attackers are declared
        //if (AllZone.getPhase().is(Constant.Phase.Combat_Declare_Attackers_InstantAbility, AllZone.getHumanPlayer()))
        //	return true;

        // prevent run-away activations - first time will always return true
        Random r = MyRandom.random;
        final Card source = sa.getSourceCard();
        boolean chance = r.nextFloat() <= Math.pow(.9, sa.getActivationsThisTurn());

        Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent())
                tgt.addTarget(AllZone.getHumanPlayer());
            else
                tgt.addTarget(AllZone.getComputerPlayer());
        }

        if (cost != null) {
            if (!CostUtil.checkLifeCost(cost, source, 4))
                return false;

            if (!CostUtil.checkDiscardCost(cost, source))
                return false;
                
            if (!CostUtil.checkSacrificeCost(cost, source))
                return false;
                
            if (!CostUtil.checkRemoveCounterCost(cost, source))
                return false;
        }

        if (tokenAmount.equals("X")) {
            if (source.getSVar(tokenAmount).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                int xPay = ComputerUtil.determineLeftoverMana(sa);
                source.setSVar("PayX", Integer.toString(xPay));
            }
            if (AbilityFactory.calculateAmount(AF.getHostCard(), tokenAmount, sa) <= 0)
                return false;
        }

        if (AbilityFactory.playReusable(sa))
            return chance;

        if (sa.isAbility())
            return (r.nextFloat() < .9 && chance);

        return (r.nextFloat() < .6667 && chance);
    }

    /**
     * <p>tokenDoTriggerAI.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private boolean tokenDoTriggerAI(SpellAbility sa, boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa))
            return false;

        return true;
    }

    /**
     * <p>doStackDescription.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String doStackDescription(SpellAbility sa) {
        Card host = AF.getHostCard();

        int finalPower = AbilityFactory.calculateAmount(AF.getHostCard(), tokenPower, sa);
        int finalToughness = AbilityFactory.calculateAmount(AF.getHostCard(), tokenToughness, sa);
        int finalAmount = AbilityFactory.calculateAmount(AF.getHostCard(), tokenAmount, sa);
        
        String substitutedName = tokenName.equals("ChosenType") ? host.getChosenType() : tokenName;

        StringBuilder sb = new StringBuilder();

        if (sa instanceof Ability_Sub)
            sb.append(" ");
        else
            sb.append(host.getName()).append(" - ");

        sb.append("Put (").append(finalAmount).append(") ").append(finalPower).append("/").append(finalToughness);
        sb.append(" ").append(substitutedName).append(" token");
        if (finalAmount != 1) sb.append("s");
        sb.append(" onto the battlefield");

        if (tokenOwner.equals("Opponent")) {
            sb.append(" under your opponent's control.");
        } else {
            sb.append(".");
        }

        if (sa.getSubAbility() != null) {
            sb.append(sa.getSubAbility().getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>doResolve.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private void doResolve(SpellAbility sa) {
        Card host = AF.getHostCard();
        String imageName = "";
        Player controller;
        String cost = "";
        //Construct colors
        String[] substitutedColors = Arrays.copyOf(tokenColors, tokenColors.length);
        for (int i = 0; i < substitutedColors.length; i++) {
            if (substitutedColors[i].equals("ChosenColor")) {
                //this currently only supports 1 chosen color
                substitutedColors[i] = host.getChosenColor().get(0);
            }
        }
        String colorDesc = "";
        for (String col : substitutedColors) {
            if (col.equalsIgnoreCase("White")) {
                colorDesc += "W";
            } else if (col.equalsIgnoreCase("Blue")) {
                colorDesc += "U";
            } else if (col.equalsIgnoreCase("Black")) {
                colorDesc += "B";
            } else if (col.equalsIgnoreCase("Red")) {
                colorDesc += "R";
            } else if (col.equalsIgnoreCase("Green")) {
                colorDesc += "G";
            } else if (col.equalsIgnoreCase("Colorless")) {
                colorDesc = "C";
            }
        }
        if (tokenImage.equals("")) {

            imageName += colorDesc + " " + tokenPower + " " + tokenToughness + " " + tokenName;
        } else {
            imageName = tokenImage;
        }
        //System.out.println("AF_Token imageName = " + imageName);

        for (char c : colorDesc.toCharArray()) {
            cost += c + ' ';
        }

        cost = colorDesc.replace('C', '1').trim();

        controller = AbilityFactory.getDefinedPlayers(AF.getHostCard(), tokenOwner, sa).get(0);

        int finalPower = AbilityFactory.calculateAmount(AF.getHostCard(), tokenPower, sa);
        int finalToughness = AbilityFactory.calculateAmount(AF.getHostCard(), tokenToughness, sa);
        int finalAmount = AbilityFactory.calculateAmount(AF.getHostCard(), tokenAmount, sa);
        
        String[] substitutedTypes = Arrays.copyOf(tokenTypes, tokenTypes.length);
        for (int i = 0; i < substitutedTypes.length; i++) {
            if (substitutedTypes[i].equals("ChosenType")) {
                substitutedTypes[i] = host.getChosenType();
            }
        }
        String substitutedName = tokenName.equals("ChosenType") ? host.getChosenType() : tokenName;

        String remember = AF.getMapParams().get("RememberTokens");
        for (int i = 0; i < finalAmount; i++) {
            CardList tokens = CardFactoryUtil.makeToken(substitutedName, imageName, controller, cost, substitutedTypes, finalPower, finalToughness, tokenKeywords);

            //Grant abilities
            if (tokenAbilities != null) {
                AbilityFactory af = new AbilityFactory();
                for (String s : tokenAbilities) {
                    String actualAbility = AF.getHostCard().getSVar(s);
                    for (Card c : tokens) {
                        SpellAbility grantedAbility = af.getAbility(actualAbility, c);
                        c.addSpellAbility(grantedAbility);
                    }
                }
            }

            //Grant triggers
            if (tokenTriggers != null) {

                for (String s : tokenTriggers) {
                    String actualTrigger = AF.getHostCard().getSVar(s);

                    for (final Card c : tokens) {

                        Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c, true);
                        String ability = AF.getHostCard().getSVar(parsedTrigger.getMapParams().get("Execute"));
                        parsedTrigger.setOverridingAbility(new AbilityFactory().getAbility(ability, c));
                        c.addTrigger(parsedTrigger);
                    }
                }
            }

            //Grant SVars
            if (tokenSVars != null) {
                for (String s : tokenSVars) {
                    String actualSVar = AF.getHostCard().getSVar(s);
                    for (Card c : tokens) {
                        c.setSVar(s, actualSVar);
                    }
                }
            }

            //Grant static abilities
            if (tokenStaticAbilities != null) {
                for (String s : tokenStaticAbilities) {
                    String actualAbility = AF.getHostCard().getSVar(s);
                    for (Card c : tokens) {
                        c.addStaticAbility(actualAbility);
                    }
                }
            }

            for (Card c : tokens) {
                if (tokenTapped) {
                    c.tap();
                }
                if (tokenAttacking) {
                    AllZone.getCombat().addAttacker(c);
                }
                if (remember != null) {
                    AF.getHostCard().addRemembered(c);
                }
            }
        }
    }
}
