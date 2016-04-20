package forge.ai.ability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Predicate;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostRemoveCounter;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.item.PaperToken;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_Token class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryToken.java 17656 2012-10-22 19:32:56Z Max mtg $
 */
public class TokenAi extends SpellAbilityAi {


    private String tokenAmount;
    private String tokenName;
    private String[] tokenTypes;
    private String[] tokenKeywords;
    private String tokenPower;
    private String tokenToughness;
    /**
     * <p>
     * Constructor for AbilityFactory_Token.
     * </p>
     *
     *            a {@link forge.game.ability.AbilityFactory} object.
     */
    private void readParameters(final SpellAbility mapParams) {
        String[] keywords;

        if (mapParams.hasParam("TokenKeywords")) {
            // TODO: Change this Split to a semicolon or something else
            keywords = mapParams.getParam("TokenKeywords").split("<>");
        } else {
            keywords = new String[0];
        }


        this.tokenAmount = mapParams.getParamOrDefault("TokenAmount", "1");
        this.tokenPower = mapParams.getParam("TokenPower");
        this.tokenToughness = mapParams.getParam("TokenToughness");
        this.tokenName = mapParams.getParam("TokenName");
        this.tokenTypes = mapParams.getParam("TokenTypes").split(",");
        this.tokenKeywords = keywords;

    }

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        final Game game = ai.getGame();
        readParameters(sa);

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (sa.hasParam("AILogic")) {
            if ("Never".equals(sa.getParam("AILogic"))) {
                return false;
            }
        }
        
        Player opp = ai.getOpponent();
        for (final String type : this.tokenTypes) {
            if (type.equals("Legendary")) {
                // Don't kill AIs Legendary tokens
                if (ai.isCardInPlay(this.tokenName)) {
                    return false;
                }
            }
        }

        boolean haste = false;
        boolean oneShot = sa.getSubAbility() != null
                && sa.getSubAbility().getApi() == ApiType.DelayedTrigger;
        for (final String kw : this.tokenKeywords) {
            if (kw.equals("Haste")) {
                haste = true;
            }
        }

        boolean sacOnStack = false; //check if a negative sacrifice effect is on stack
        if (!game.getStack().isEmpty()) {
            SpellAbility topStack = game.getStack().peekAbility();
            if (topStack.getApi() == ApiType.Sacrifice) {
                sacOnStack = true;
            }
        }
        
        boolean pwAbility = sa.getRestrictions().isPwAbility();
        if (pwAbility) {
            // Planeswalker token ability with loyalty costs should be played in Main1 or it might
            // never be used due to other positive abilities. AI is kept from spamming them by the
            // loyalty cost of each usage. Zero/loyalty gain token abilities can be evaluated as
            // per normal.
            boolean hasCost = false;
            for (CostPart c : sa.getPayCosts().getCostParts()) {
                if (c instanceof CostRemoveCounter) {
                    hasCost = true;
                    break;
                }
            }
            pwAbility = hasCost;
        }
        
        PhaseHandler ph = game.getPhaseHandler();
        // Don't generate tokens without haste before main 2 if possible
        if (ph.getPhase().isBefore(PhaseType.MAIN2)
                && ph.isPlayerTurn(ai) && !haste
                && !sa.hasParam("ActivationPhases")
                && !ComputerUtil.castSpellInMain1(ai, sa)) {
            boolean buff = false;
            for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                if ("Creature".equals(c.getSVar("BuffedBy"))) {
                    buff = true;
                }
            }
            if (!buff && !sacOnStack && !pwAbility) {
                return false;
            }
        }
        if ((ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS))
                && !sa.hasParam("ActivationPhases") && !sa.hasParam("PlayerTurn")
                && !SpellAbilityAi.isSorcerySpeed(sa) && !haste && !sacOnStack && !pwAbility) {
            return false;
        }
        if ((ph.getPhase().isAfter(PhaseType.COMBAT_BEGIN) || game.getPhaseHandler().isPlayerTurn(opp))
                && oneShot && !sacOnStack) {
            return false;
        }

        final Card source = sa.getHostCard();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (tgt.canOnlyTgtOpponent() || "Opponent".equals(sa.getParam("AITgts"))) {
                if (sa.canTarget(opp)) {
                    sa.getTargets().add(opp);
                } else {
                    return false;
                }
            } else {
                if (sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                } else {
                	//Flash Foliage
        	        CardCollection list = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield), ai.getOpponents());
        	        list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source, sa);
        	        list = CardLists.getTargetableCards(list, sa);
        	        CardCollection betterList = CardLists.filter(list, new Predicate<Card>() {
        	            @Override
        	            public boolean apply(Card c) {
        	                return c.getLethalDamage() == 1;
        	            }
        	        });
        	        if (!betterList.isEmpty()) {
        	        	list = betterList;
        	        }
        	        betterList = CardLists.getNotKeyword(list, "Trample");
        	        if (!betterList.isEmpty()) {
        	        	list = betterList;
        	        }
        	        if (!list.isEmpty()) {
        	        	sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(list));
        	        } else {
        	        	return false;
        	        }
                    
                }
            }
        }

        if (cost != null) {
            if (!ComputerUtilCost.checkLifeCost(ai, cost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, cost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkSacrificeCost(ai, cost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(cost, source)) {
                return false;
            }
        }

        //interrupt sacrifice effect
        if (sacOnStack) {
            final int nTokens = AbilityUtils.calculateAmount(sa.getHostCard(), this.tokenAmount, sa);
            SpellAbility topStack = game.getStack().peekAbility();
            final String valid = topStack.getParamOrDefault("SacValid", "Card.Self");
            String num = sa.getParam("Amount");
            num = (num == null) ? "1" : num;
            final int nToSac = AbilityUtils.calculateAmount(topStack.getHostCard(), num, topStack);
            CardCollection list =
                    CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid.split(","), opp, topStack.getHostCard(), sa);
            list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack));
            if (!list.isEmpty() && nTokens > 0 && list.size() == nToSac) { //only care about saving single creature for now
                ComputerUtilCard.sortByEvaluateCreature(list);
                Card token = spawnToken(ai, sa);
                if (token != null) {
                    list.add(token);
                    list = CardLists.getValidCards(list, valid.split(","), ai.getOpponent(), topStack.getHostCard(), sa);
                    list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack));
                    if (ComputerUtilCard.evaluateCreature(token) < ComputerUtilCard.evaluateCreature(list.get(0))
                            && list.contains(token)) {
                        return true;
                    }
                }
            }
        }
        
        if (this.tokenAmount.equals("X") || (this.tokenPower != null && this.tokenPower.equals("X")) || (this.tokenToughness != null && this.tokenToughness.equals("X"))) {
            int x = AbilityUtils.calculateAmount(sa.getHostCard(), this.tokenAmount, sa);
            if (source.getSVar("X").equals("Count$Converge")) {
            	x = ComputerUtilMana.getConvergeCount(sa, ai);
            }
            if (source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                x = ComputerUtilMana.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(x));
            }
            if (x <= 0) {
                return false;
            }
        }

        if (SpellAbilityAi.playReusable(ai, sa)) {
            return true;
        }
        if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return true;
        }
        if (sa.isAbility()) {
            return true;
        }

        return MyRandom.getRandom().nextFloat() < .8;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        readParameters(sa);
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                sa.getTargets().add(ai.getOpponent());
            } else {
                sa.getTargets().add(ai);
            }
        }
        if ("X".equals(this.tokenAmount) || "X".equals(this.tokenPower) || "X".equals(this.tokenToughness)) {
            int x = AbilityUtils.calculateAmount(source, this.tokenAmount, sa);
            if (source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                x = ComputerUtilMana.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(x));
            }
            if (x <= 0) {
                return false;
            }
        }

        return true;
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        // TODO: AILogic
        return true;
    }

    /**
     * Create the token as a Card object.
     * @param ai owner of the new token
     * @param sa Token SpellAbility
     * @return token creature
     */
    public static Card spawnToken(Player ai, SpellAbility sa) {
        final Card host = sa.getHostCard();

        String[] tokenKeywords = sa.hasParam("TokenKeywords") ? sa.getParam("TokenKeywords").split("<>") : new String[0];
        String tokenAmount = sa.getParamOrDefault("TokenAmount", "1");
        String tokenPower = sa.getParam("TokenPower");
        String tokenToughness = sa.getParam("TokenToughness");
        String tokenName = sa.getParam("TokenName");
        String[] tokenTypes = sa.getParam("TokenTypes").split(",");
        String cost = "";
        String[] tokenColors = sa.getParam("TokenColors").split(",");
        String tokenImage = sa.hasParam("TokenImage") ? PaperToken.makeTokenFileName(sa.getParam("TokenImage")) : "";
        String[] tokenAbilities = sa.hasParam("TokenAbilities") ? sa.getParam("TokenAbilities").split(",") : null;
        String[] tokenTriggers = sa.hasParam("TokenTriggers") ? sa.getParam("TokenTriggers").split(",") : null;
        String[] tokenSVars = sa.hasParam("TokenSVars") ? sa.getParam("TokenSVars").split(",") : null;
        String[] tokenStaticAbilities = sa.hasParam("TokenStaticAbilities") ? sa.getParam("TokenStaticAbilities").split(",") : null;
        String[] tokenHiddenKeywords = sa.hasParam("TokenHiddenKeywords") ? sa.getParam("TokenHiddenKeywords").split("&") : null;
        final String[] substitutedColors = Arrays.copyOf(tokenColors, tokenColors.length);
        for (int i = 0; i < substitutedColors.length; i++) {
            if (substitutedColors[i].equals("ChosenColor")) {
                // this currently only supports 1 chosen color
                substitutedColors[i] = host.getChosenColor();
            }
        }
        String colorDesc = "";
        for (final String col : substitutedColors) {
            if (col.equalsIgnoreCase("White")) {
                colorDesc += "W ";
            } else if (col.equalsIgnoreCase("Blue")) {
                colorDesc += "U ";
            } else if (col.equalsIgnoreCase("Black")) {
                colorDesc += "B ";
            } else if (col.equalsIgnoreCase("Red")) {
                colorDesc += "R ";
            } else if (col.equalsIgnoreCase("Green")) {
                colorDesc += "G ";
            } else if (col.equalsIgnoreCase("Colorless")) {
                colorDesc = "C";
            }
        }
        
        final List<String> imageNames = new ArrayList<String>(1);
        if (tokenImage.equals("")) {
            imageNames.add(PaperToken.makeTokenFileName(colorDesc.replace(" ", ""), tokenPower, tokenToughness, tokenName));
        } else {
            imageNames.add(0, tokenImage);
        }

        for (final char c : colorDesc.toCharArray()) {
            cost += c + ' ';
        }

        cost = colorDesc.replace('C', '1').trim();

        final int finalPower = AbilityUtils.calculateAmount(host, tokenPower, sa);
        final int finalToughness = AbilityUtils.calculateAmount(host, tokenToughness, sa);
        final int finalAmount = AbilityUtils.calculateAmount(host, tokenAmount, sa);
        if (finalAmount < 1) {
            return null;
        }

        final String[] substitutedTypes = Arrays.copyOf(tokenTypes, tokenTypes.length);
        for (int i = 0; i < substitutedTypes.length; i++) {
            if (substitutedTypes[i].equals("ChosenType")) {
                substitutedTypes[i] = host.getChosenType();
            }
        }
        final String substitutedName = tokenName.equals("ChosenType") ? host.getChosenType() : tokenName;
        final String imageName = imageNames.get(MyRandom.getRandom().nextInt(imageNames.size()));
        final CardFactory.TokenInfo tokenInfo = new CardFactory.TokenInfo(substitutedName, imageName,
                cost, substitutedTypes, tokenKeywords, finalPower, finalToughness);
        final List<Card> tokens = CardFactory.makeToken(tokenInfo, ai);
        
        // Grant rule changes
        if (tokenHiddenKeywords != null) {
            for (final String s : tokenHiddenKeywords) {
                for (final Card c : tokens) {
                    c.addHiddenExtrinsicKeyword(s);
                }
            }
        }

        // Grant abilities
        if (tokenAbilities != null) {
            for (final String s : tokenAbilities) {
                final String actualAbility = host.getSVar(s);
                for (final Card c : tokens) {
                    final SpellAbility grantedAbility = AbilityFactory.getAbility(actualAbility, c);
                    c.addSpellAbility(grantedAbility);
                    // added ability to intrinsic list so copies and clones work
                    c.getCurrentState().addUnparsedAbility(actualAbility);
                }
            }
        }

        // Grant triggers
        if (tokenTriggers != null) {

            for (final String s : tokenTriggers) {
                final String actualTrigger = host.getSVar(s);

                for (final Card c : tokens) {

                    final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c, true);
                    final String ability = host.getSVar(parsedTrigger.getMapParams().get("Execute"));
                    parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(ability, c));
                    c.addTrigger(parsedTrigger);
                }
            }
        }

        // Grant SVars
        if (tokenSVars != null) {
            for (final String s : tokenSVars) {
                String actualSVar = host.getSVar(s);
                String name = s;
                if (actualSVar.startsWith("SVar")) {
                    actualSVar = actualSVar.split("SVar:")[1];
                    name = actualSVar.split(":")[0];
                    actualSVar = actualSVar.split(":")[1];
                }
                for (final Card c : tokens) {
                    c.setSVar(name, actualSVar);
                }
            }
        }

        // Grant static abilities
        if (tokenStaticAbilities != null) {
            for (final String s : tokenStaticAbilities) {
                final String actualAbility = host.getSVar(s);
                for (final Card c : tokens) {
                    c.addStaticAbilityString(actualAbility);
                    c.addStaticAbility(actualAbility);
                }
            }
        }
        return tokens.get(0);
    }
}
