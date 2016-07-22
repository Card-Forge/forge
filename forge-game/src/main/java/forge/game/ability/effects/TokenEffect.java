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
package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.combat.Combat;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.item.PaperToken;
import forge.util.collect.FCollectionView;
import forge.util.MyRandom;

public class TokenEffect extends SpellAbilityEffect {

    private String tokenOwner;
    private String[] tokenColors;
    private String[] tokenOriginalColors;
    private String tokenImage;
    private String[] tokenAltImages;
    private String[] tokenAbilities;
    private String[] tokenTriggers;
    private String[] tokenSVars;
    private String[] tokenStaticAbilities;
    private boolean tokenTapped;
    private boolean tokenAttacking;
    private String tokenBlocking;
    private String tokenAmount;
    private String tokenToughness;
    private String tokenPower;
    private String[] tokenTypes;
    private String[] tokenOriginalTypes;
    private String tokenName;
    private String tokenOriginalName;
    private String[] tokenKeywords;
    private String[] tokenHiddenKeywords;

    private void readParameters(final SpellAbility mapParams) {
        String image;
        String[] keywords;
        
        if (mapParams.hasParam("TokenKeywords")) {
            // TODO: Change this Split to a semicolon or something else
            keywords = mapParams.getParam("TokenKeywords").split("<>");
        } else {
            keywords = new String[0];
        }

        if (mapParams.hasParam("TokenHiddenKeywords")) {
            this.tokenHiddenKeywords = mapParams.getParam("TokenHiddenKeywords").split("&");
        }

        if (mapParams.hasParam("TokenImage")) {
            image = PaperToken.makeTokenFileName(mapParams.getParam("TokenImage"));
        } else {
            image = "";
        }

        if (mapParams.hasParam("TokenAltImages")) {
            this.tokenAltImages = mapParams.getParam("TokenAltImages").split(",");
            for (int i = 0; i < tokenAltImages.length; i++) {
            	this.tokenAltImages[i] = PaperToken.makeTokenFileName(this.tokenAltImages[i]);
            }
        } else {
            this.tokenAltImages = null;
        }

        this.tokenTapped = mapParams.hasParam("TokenTapped") && mapParams.getParam("TokenTapped").equals("True");
        this.tokenAttacking = mapParams.hasParam("TokenAttacking") && mapParams.getParam("TokenAttacking").equals("True");

        if (mapParams.hasParam("TokenAbilities")) {
            this.tokenAbilities = mapParams.getParam("TokenAbilities").split(",");
        } else {
            this.tokenAbilities = null;
        }
        if (mapParams.hasParam("TokenTriggers")) {
            this.tokenTriggers = mapParams.getParam("TokenTriggers").split(",");
        } else {
            this.tokenTriggers = null;
        }
        if (mapParams.hasParam("TokenSVars")) {
            this.tokenSVars = mapParams.getParam("TokenSVars").split(",");
        } else {
            this.tokenSVars = null;
        }
        if (mapParams.hasParam("TokenStaticAbilities")) {
            this.tokenStaticAbilities = mapParams.getParam("TokenStaticAbilities").split(",");
        } else {
            this.tokenStaticAbilities = null;
        }
        this.tokenBlocking = mapParams.getParam("TokenBlocking");
        this.tokenAmount = mapParams.getParamOrDefault("TokenAmount", "1");
        this.tokenPower = mapParams.getParam("TokenPower");
        this.tokenToughness = mapParams.getParam("TokenToughness");

        this.tokenOriginalTypes = mapParams.getOriginalMapParams().get("TokenTypes").split(",");
        this.tokenTypes = mapParams.getParam("TokenTypes").split(",");

        if (mapParams.hasParam("TokenName")) {
            this.tokenOriginalName = mapParams.getOriginalMapParams().get("TokenName");
            this.tokenName = mapParams.getParam("TokenName");
        } else {
            this.tokenOriginalName = StringUtils.join(new CardType(Lists.newArrayList(this.tokenOriginalTypes)).getSubtypes(), " ");
            this.tokenName = StringUtils.join(new CardType(Lists.newArrayList(this.tokenTypes)).getSubtypes(), " ");
        }

        this.tokenOriginalColors = mapParams.getOriginalMapParams().get("TokenColors").split(",");
        this.tokenColors = mapParams.getParam("TokenColors").split(",");

        this.tokenKeywords = keywords;
        this.tokenImage = image;
        if (mapParams.hasParam("TokenOwner")) {
            this.tokenOwner = mapParams.getParam("TokenOwner");
        } else {
            this.tokenOwner = "You";
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();

        readParameters(sa);

        final int finalPower = AbilityUtils.calculateAmount(host, this.tokenPower, sa);
        final int finalToughness = AbilityUtils.calculateAmount(host, this.tokenToughness, sa);
        final int finalAmount = AbilityUtils.calculateAmount(host, this.tokenAmount, sa);

        final String substitutedName = this.tokenName.equals("ChosenType") ? host.getChosenType() : this.tokenName;

        sb.append("Put (").append(finalAmount).append(") ");
        if (Arrays.asList(this.tokenTypes).contains("Creature")) {
            sb.append(finalPower).append("/").append(finalToughness).append(" ");
        }
        sb.append(substitutedName).append(" token");
        if (finalAmount != 1) {
            sb.append("s");
        }
        sb.append(" onto the battlefield");

        if (this.tokenOwner.equals("Opponent")) {
            sb.append(" under your opponent's control.");
        } else {
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final SpellAbility root = sa.getRootAbility();
        readParameters(sa);

        String cost = "";

        // Construct original colors
        String originalColorDesc = "";
        for (final String col : this.tokenOriginalColors) {
            if (col.equalsIgnoreCase("White")) {
            	originalColorDesc += "W ";
            } else if (col.equalsIgnoreCase("Blue")) {
            	originalColorDesc += "U ";
            } else if (col.equalsIgnoreCase("Black")) {
            	originalColorDesc += "B ";
            } else if (col.equalsIgnoreCase("Red")) {
            	originalColorDesc += "R ";
            } else if (col.equalsIgnoreCase("Green")) {
            	originalColorDesc += "G ";
            } else if (col.equalsIgnoreCase("Colorless")) {
            	originalColorDesc = "C";
            }
        }

        final List<String> imageNames = new ArrayList<String>(1);
        if (this.tokenImage.equals("")) {
            imageNames.add(PaperToken.makeTokenFileName(originalColorDesc.replace(" ", ""), tokenPower, tokenToughness, tokenOriginalName));
        } else {
            imageNames.add(0, this.tokenImage);
        }
        if (this.tokenAltImages != null) {
        	imageNames.addAll(Arrays.asList(this.tokenAltImages));
        }

        final String[] substitutedColors = Arrays.copyOf(this.tokenColors, this.tokenColors.length);
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
        for (final char c : colorDesc.toCharArray()) {
            cost += c + ' ';
        }

        cost = colorDesc.replace('C', '1').trim();

        final int finalPower = AbilityUtils.calculateAmount(host, this.tokenPower, sa);
        final int finalToughness = AbilityUtils.calculateAmount(host, this.tokenToughness, sa);
        final int finalAmount = AbilityUtils.calculateAmount(host, this.tokenAmount, sa);

        final String[] substitutedTypes = Arrays.copyOf(this.tokenTypes, this.tokenTypes.length);
        for (int i = 0; i < substitutedTypes.length; i++) {
            if (substitutedTypes[i].equals("ChosenType")) {
                substitutedTypes[i] = host.getChosenType();
            }
        }
        final String substitutedName = this.tokenName.equals("ChosenType") ? host.getChosenType() : this.tokenName;

        final boolean remember = sa.hasParam("RememberTokens");
        final boolean imprint = sa.hasParam("ImprintTokens");
        for (final Player controller : AbilityUtils.getDefinedPlayers(host, this.tokenOwner, sa)) {
            for (int i = 0; i < finalAmount; i++) {
                final String imageName = imageNames.get(MyRandom.getRandom().nextInt(imageNames.size()));
                final CardFactory.TokenInfo tokenInfo = new CardFactory.TokenInfo(substitutedName, imageName,
                        cost, substitutedTypes, this.tokenKeywords, finalPower, finalToughness);
                final List<Card> tokens = CardFactory.makeToken(tokenInfo, controller);
                for (Card tok : tokens) {
                    if (this.tokenTapped) {
                        tok.setTapped(true);
                    }
                    controller.getGame().getAction().moveToPlay(tok);
                }
                controller.getGame().fireEvent(new GameEventTokenCreated());
                
                // Grant rule changes
                if (this.tokenHiddenKeywords != null) {
                    for (final String s : this.tokenHiddenKeywords) {
                        for (final Card c : tokens) {
                            c.addHiddenExtrinsicKeyword(s);
                        }
                    }
                }

                // Grant abilities
                if (this.tokenAbilities != null) {
                    for (final String s : this.tokenAbilities) {
                        final String actualAbility = AbilityUtils.getSVar(root, s);
                        for (final Card c : tokens) {
                            final SpellAbility grantedAbility = AbilityFactory.getAbility(actualAbility, c);
                            c.addSpellAbility(grantedAbility);
                            // added ability to intrinsic list so copies and clones work
                            c.getCurrentState().addUnparsedAbility(actualAbility);
                        }
                    }
                }

                // Grant triggers
                if (this.tokenTriggers != null) {

                    for (final String s : this.tokenTriggers) {
                        final String actualTrigger = AbilityUtils.getSVar(root, s);

                        for (final Card c : tokens) {

                            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c, true);
                            final String ability = AbilityUtils.getSVar(root, parsedTrigger.getMapParams().get("Execute"));
                            parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(ability, c));
                            c.addTrigger(parsedTrigger);
                        }
                    }
                }

                // Grant SVars
                if (this.tokenSVars != null) {
                    for (final String s : this.tokenSVars) {
                        String actualSVar = AbilityUtils.getSVar(root, s);
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
                if (this.tokenStaticAbilities != null) {
                    for (final String s : this.tokenStaticAbilities) {
                        final String actualAbility = AbilityUtils.getSVar(root, s);
                        for (final Card c : tokens) {
                            c.addStaticAbilityString(actualAbility);
                            c.addStaticAbility(actualAbility);
                        }
                    }
                }

                boolean combatChanged = false;
                final Game game = controller.getGame();
                for (final Card c : tokens) {
                    if (this.tokenAttacking && game.getPhaseHandler().inCombat()) {
                        final Combat combat = game.getPhaseHandler().getCombat();
                        final FCollectionView<GameEntity> defs = combat.getDefenders();
                        final GameEntity defender = c.getController().getController().chooseSingleEntityForEffect(defs, sa, "Choose which defender to attack with " + c, false);
                        combat.addAttacker(c, defender);
                        combatChanged = true;
                    }
                    if (this.tokenBlocking != null && game.getPhaseHandler().inCombat()) {
                        final Combat combat = game.getPhaseHandler().getCombat();
                        final Card attacker = Iterables.getFirst(AbilityUtils.getDefinedCards(host, this.tokenBlocking, sa), null);
                        if (attacker != null) {
                            final boolean wasBlocked = combat.isBlocked(attacker);
                            combat.addBlocker(attacker, c);
                            combat.orderAttackersForDamageAssignment(c);

                            // Run triggers for new blocker and add it to damage assignment order
                            if (!wasBlocked) {
                                combat.setBlocked(attacker, true);
                                combat.addBlockerToDamageAssignmentOrder(attacker, c);
                            }
                            combatChanged = true;
                        }
                    }
                    if (remember) {
                        game.getCardState(sa.getHostCard()).addRemembered(c);
                    }
                    if (imprint) {
                        game.getCardState(sa.getHostCard()).addImprintedCard(c);
                    }
                    if (sa.hasParam("RememberSource")) {
                        game.getCardState(c).addRemembered(host);
                    }
                    if (sa.hasParam("TokenRemembered")) {
                        final Card token = game.getCardState(c);
                        final String remembered = sa.getParam("TokenRemembered");
                        for (final Object o : AbilityUtils.getDefinedObjects(host, remembered, sa)) {
                            token.addRemembered(o);
                        }
                    }
                }
                if (tokenName.equals("Clue")) { // investigate trigger
                    controller.addInvestigatedThisTurn();
                }
                if (combatChanged) {
                    game.updateCombatForView();
                    game.fireEvent(new GameEventCombatChanged());
                }                
                if (sa.hasParam("AtEOT")) {
                    registerDelayedTrigger(sa, sa.getParam("AtEOT"), tokens);
                }
            }
        }
    }
}
