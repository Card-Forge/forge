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

import java.util.Arrays;
import java.util.List;

import forge.card.MagicColor;
import forge.game.card.token.TokenInfo;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.combat.Combat;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
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

    private void readMetaParams(final SpellAbility mapParams) {
        this.tokenTapped = mapParams.getParamOrDefault("TokenTapped", "False").equalsIgnoreCase("True");
        this.tokenAttacking = mapParams.getParamOrDefault("TokenAttacking", "False").equalsIgnoreCase("True");
        this.tokenBlocking = mapParams.getParam("TokenBlocking");

        this.tokenAmount = mapParams.getParamOrDefault("TokenAmount", "1");
        this.tokenOwner = mapParams.getParamOrDefault("TokenOwner", "You");
    }

    private void readParameters(final SpellAbility mapParams, Card prototype) {
        readMetaParams(mapParams);
        if (prototype == null) {
            readTokenParams(mapParams);
        }
    }

    private void readTokenParams(final SpellAbility mapParams) {
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
            	this.tokenAltImages[i] = PaperToken.makeTokenFileName(this.tokenAltImages[i].trim());
            }
        } else {
            this.tokenAltImages = null;
        }

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
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();
        Card prototype = loadTokenPrototype(sa);

        if (prototype != null) {
            return sa.getDescription();
        }

        readParameters(sa, prototype);

        final int finalPower = AbilityUtils.calculateAmount(host, this.tokenPower, sa);
        final int finalToughness = AbilityUtils.calculateAmount(host, this.tokenToughness, sa);
        final int finalAmount = AbilityUtils.calculateAmount(host, this.tokenAmount, sa);

        final String substitutedName = this.tokenName.equals("ChosenType") ? host.getChosenType() : this.tokenName;

        sb.append("Create (").append(finalAmount).append(") ");
        if (Arrays.asList(this.tokenTypes).contains("Creature")) {
            sb.append(finalPower).append("/").append(finalToughness).append(" ");
        }
        sb.append(substitutedName).append(" token");
        if (finalAmount != 1) {
            sb.append("s");
        }

        if (this.tokenOwner.equals("Opponent")) {
            sb.append(" under your opponent's control.");
        } else {
            sb.append(".");
        }

        return sb.toString();
    }

    public Card loadTokenPrototype(SpellAbility sa) {
        if (!sa.hasParam("TokenScript")) {
            return null;
        }

        final Card result = TokenInfo.getProtoType(sa.getParam("TokenScript"), sa);

        if (result != null) {
            tokenName = result.getName();
        } else {
            throw new RuntimeException("don't find Token for TokenScript: " + sa.getParam("TokenScript"));
        }

        return result;
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final SpellAbility root = sa.getRootAbility();

        // Cause of the Token Effect, in general it should be this
        // but if its a Replacement Effect, it might be something else or null
        SpellAbility cause = sa;
        if (root.isReplacementAbility() && root.hasReplacingObject("Cause")) {
            cause = (SpellAbility)root.getReplacingObject("Cause");
        }

        final boolean remember = sa.hasParam("RememberTokens");
        final boolean imprint = sa.hasParam("ImprintTokens");
        final List<Card> allTokens = Lists.newArrayList();

        boolean combatChanged = false;
        boolean inCombat = game.getPhaseHandler().inCombat();

        Card prototype = loadTokenPrototype(sa);

        readParameters(sa, prototype);
        final int finalAmount = AbilityUtils.calculateAmount(host, this.tokenAmount, sa);

        TokenInfo tokenInfo;

        if (prototype == null) {
            String originalColorDesc = parseColorForImage();

            final List<String> imageNames = Lists.newArrayListWithCapacity(1);
            if (this.tokenImage.equals("")) {
                imageNames.add(PaperToken.makeTokenFileName(originalColorDesc, tokenPower, tokenToughness, tokenOriginalName));
            } else {
                imageNames.add(0, this.tokenImage);
            }
            if (this.tokenAltImages != null) {
                imageNames.addAll(Arrays.asList(this.tokenAltImages));
            }

            String cost = determineTokenColor(host);

            final int finalPower = AbilityUtils.calculateAmount(host, this.tokenPower, sa);
            final int finalToughness = AbilityUtils.calculateAmount(host, this.tokenToughness, sa);

            final String[] substitutedTypes = Arrays.copyOf(this.tokenTypes, this.tokenTypes.length);
            for (int i = 0; i < substitutedTypes.length; i++) {
                if (substitutedTypes[i].equals("ChosenType")) {
                    substitutedTypes[i] = host.getChosenType();
                }
            }
            final String substitutedName = this.tokenName.equals("ChosenType") ? host.getChosenType() : this.tokenName;

            final String imageName = imageNames.get(MyRandom.getRandom().nextInt(imageNames.size()));
            tokenInfo = new TokenInfo(substitutedName, imageName,
                    cost, substitutedTypes, this.tokenKeywords, finalPower, finalToughness);
        } else {
            // TODO: Substitute type name for Chosen tokens
            // TODO: If host has has it's color/type altered make sure that's appropriately applied
            // TODO: Lock down final power and toughness if it's actually X values
            tokenInfo = new TokenInfo(prototype, host);
        }

        boolean useZoneTable = true;
        CardZoneTable triggerList = sa.getChangeZoneTable();
        if (triggerList == null) {
            triggerList = new CardZoneTable();
            useZoneTable = false;
        }
        if (sa.hasParam("ChangeZoneTable")) {
            sa.setChangeZoneTable(triggerList);
            useZoneTable = true;
        }

        for (final Player controller : AbilityUtils.getDefinedPlayers(host, this.tokenOwner, sa)) {
            List<Card> tokens;

            if (prototype == null) {
                tokens = tokenInfo.makeTokenWithMultiplier(controller, finalAmount, cause != null);
                grantHiddenKeywords(tokens);
                grantSvars(tokens, sa);
                grantAbilities(tokens, sa);
                grantTriggers(tokens, sa);
                grantStatics(tokens, sa);
            } else {
                tokens = TokenInfo.makeTokensFromPrototype(prototype, controller, finalAmount, cause != null);
            }

            for (Card tok : tokens) {
                if (this.tokenTapped) {
                    tok.setTapped(true);
                }

                if (sa.hasParam("AttachedTo") && !attachTokenTo(tok, sa)) {
                    continue;
                }

                // Should this be catching the Card that's returned?
                Card c = game.getAction().moveToPlay(tok, sa);
                if (c.getZone() != null) {
                    triggerList.put(ZoneType.None, c.getZone().getZoneType(), c);
                }

                if (sa.hasParam("AtEOTTrig")) {
                    addSelfTrigger(sa, sa.getParam("AtEOTTrig"), c);
                }

                if (inCombat) {
                    combatChanged = addTokenToCombat(game, c, tok.getController(), sa, host) || combatChanged;
                }

                c.updateStateForView();

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
                allTokens.add(c);
            }
            if ("Clue".equals(tokenName)) { // investigate trigger
                controller.addInvestigatedThisTurn();
            }
        }

        if (!useZoneTable) {
            triggerList.triggerChangesZoneAll(game);
            triggerList.clear();
        }

        game.fireEvent(new GameEventTokenCreated());

        if (combatChanged) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
        if (sa.hasParam("AtEOT")) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), allTokens);
        }
    }

    private String parseColorForImage() {
        String originalColorDesc = "";
        for (final String col : this.tokenOriginalColors) {
            originalColorDesc += MagicColor.toShortString(col);
            if (originalColorDesc.equals("C")) {
                return originalColorDesc;
            }
        }
        return originalColorDesc;
    }

    private String determineTokenColor(Card host) {
        final String[] substitutedColors = Arrays.copyOf(this.tokenColors, this.tokenColors.length);
        for (int i = 0; i < substitutedColors.length; i++) {
            if (substitutedColors[i].equals("ChosenColor")) {
                // this currently only supports 1 chosen color
                substitutedColors[i] = host.getChosenColor();
            }
        }

        StringBuilder sb = new StringBuilder();
        for (final String col : substitutedColors) {
            String str = MagicColor.toShortString(col);
            if (str.equals("C")) {
                return "1";
            }

            sb.append(str).append(" ");
        }

        return sb.toString().trim();
    }

    private void grantHiddenKeywords(List<Card> tokens) {
        // Grant rule changes
        if (this.tokenHiddenKeywords != null) {
            for (final String s : this.tokenHiddenKeywords) {
                for (final Card c : tokens) {
                    c.addHiddenExtrinsicKeyword(s);
                }
            }
        }
    }

    private void grantAbilities(List<Card> tokens, SpellAbility root) {
        if (this.tokenAbilities != null) {
            for (final String s : this.tokenAbilities) {
                final String actualAbility = AbilityUtils.getSVar(root, s);
                for (final Card c : tokens) {
                    final SpellAbility grantedAbility = AbilityFactory.getAbility(actualAbility, c);
                    // Set intrinsic, so that effects like Clone will copy these.
                    grantedAbility.setIntrinsic(true);
                    c.addSpellAbility(grantedAbility);
                }
            }
        }
    }

    private void grantTriggers(List<Card> tokens, SpellAbility root) {
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
    }

    private void grantSvars(List<Card> tokens, SpellAbility root) {
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
    }

    private void grantStatics(List<Card> tokens, SpellAbility root) {
        if (this.tokenStaticAbilities != null) {
            for (final String s : this.tokenStaticAbilities) {
                final String actualAbility = AbilityUtils.getSVar(root, s);
                for (final Card c : tokens) {
                    c.addStaticAbility(actualAbility);
                }
            }
        }
    }

    private boolean addTokenToCombat(Game game, Card c, Player controller, SpellAbility sa, Card host) {
        boolean combatChanged = false;
        if (this.tokenAttacking) {
            final Combat combat = game.getCombat();

            // into battlefield attacking only should work if you are the attacking player
            if (combat.getAttackingPlayer().equals(controller)) {
                final FCollectionView<GameEntity> defs = combat.getDefenders();
                final GameEntity defender = controller.getController().chooseSingleEntityForEffect(defs, sa, "Choose which defender to attack with " + c, false);
                combat.addAttacker(c, defender);
                combatChanged = true;
            }
        }
        if (this.tokenBlocking != null) {
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
        return combatChanged;
    }

    private boolean attachTokenTo(Card tok, SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        GameObject aTo = Iterables.getFirst(
                AbilityUtils.getDefinedObjects(host, sa.getParam("AttachedTo"), sa), null);

        if (aTo instanceof GameEntity) {
            GameEntity ge = (GameEntity)aTo;
            // check what the token would be on the battlefield
            Card lki = CardUtil.getLKICopy(tok);

            lki.setLastKnownZone(tok.getController().getZone(ZoneType.Battlefield));

            // double freeze tracker, so it doesn't update view
            game.getTracker().freeze();
            CardCollection preList = new CardCollection(lki);
            game.getAction().checkStaticAbilities(false, Sets.newHashSet(lki), preList);

            // TODO update when doing Attach Update
            boolean canAttach = lki.isAttachment();

            if (canAttach && !ge.canBeAttached(lki)) {
                canAttach = false;
            }

            // reset static abilities
            game.getAction().checkStaticAbilities(false);
            // clear delayed changes, this check should not have updated the view
            game.getTracker().clearDelayed();
            // need to unfreeze tracker
            game.getTracker().unfreeze();

            if (!canAttach) {
                // Token can't attach to it
                return false;
            }

            tok.attachToEntity(ge);
            return true;
        } else {
            // not a GameEntity, cant be attach
            return false;
        }
    }
}
