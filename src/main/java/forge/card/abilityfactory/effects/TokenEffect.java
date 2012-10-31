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
package forge.card.abilityfactory.effects;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import forge.Card;

import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.game.player.Player;

public class TokenEffect extends SpellEffect {

    private String tokenOwner;
    private String[] tokenColors;
    private String tokenImage;
    private String[] tokenAbilities;
    private String[] tokenTriggers;
    private String[] tokenSVars;
    private String[] tokenStaticAbilities;
    private boolean tokenTapped;
    private boolean tokenAttacking;
    private String tokenAmount;
    private String tokenToughness;
    private String tokenPower;
    private String[] tokenTypes;
    private String tokenName;
    private String[] tokenKeywords;

    private void readParameters(final Map<String, String> mapParams) {
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

        this.tokenTapped = mapParams.containsKey("TokenTapped") && mapParams.get("TokenTapped").equals("True");
        this.tokenAttacking = mapParams.containsKey("TokenAttacking") && mapParams.get("TokenAttacking").equals("True");

        if (mapParams.containsKey("TokenAbilities")) {
            this.tokenAbilities = mapParams.get("TokenAbilities").split(",");
        } else {
            this.tokenAbilities = null;
        }
        if (mapParams.containsKey("TokenTriggers")) {
            this.tokenTriggers = mapParams.get("TokenTriggers").split(",");
        } else {
            this.tokenTriggers = null;
        }
        if (mapParams.containsKey("TokenSVars")) {
            this.tokenSVars = mapParams.get("TokenSVars").split(",");
        } else {
            this.tokenSVars = null;
        }
        if (mapParams.containsKey("TokenStaticAbilities")) {
            this.tokenStaticAbilities = mapParams.get("TokenStaticAbilities").split(",");
        } else {
            this.tokenStaticAbilities = null;
        }

        this.tokenAmount = mapParams.get("TokenAmount");
        this.tokenPower = mapParams.get("TokenPower");
        this.tokenToughness = mapParams.get("TokenToughness");
        this.tokenName = mapParams.get("TokenName");
        this.tokenTypes = mapParams.get("TokenTypes").split(",");
        this.tokenColors = mapParams.get("TokenColors").split(",");
        this.tokenKeywords = keywords;
        this.tokenImage = image;
        if (mapParams.containsKey("TokenOwner")) {
            this.tokenOwner = mapParams.get("TokenOwner");
        } else {
            this.tokenOwner = "You";
        }
    }
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();

        readParameters(params);
        
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(host.getName()).append(" - ");
        }

        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        }
        else {

            final int finalPower = AbilityFactory.calculateAmount(host, this.tokenPower, sa);
            final int finalToughness = AbilityFactory.calculateAmount(host, this.tokenToughness, sa);
            final int finalAmount = AbilityFactory.calculateAmount(host, this.tokenAmount, sa);

            final String substitutedName = this.tokenName.equals("ChosenType") ? host.getChosenType() : this.tokenName;

            sb.append("Put (").append(finalAmount).append(") ").append(finalPower).append("/").append(finalToughness);
            sb.append(" ").append(substitutedName).append(" token");
            if (finalAmount != 1) {
                sb.append("s");
            }
            sb.append(" onto the battlefield");

            if (this.tokenOwner.equals("Opponent")) {
                sb.append(" under your opponent's control.");
            } else {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    /**
     * <p>
     * doResolve.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        readParameters(params);
        
        String imageName = "";
        Player controller;
        String cost = "";
        // Construct colors
        final String[] substitutedColors = Arrays.copyOf(this.tokenColors, this.tokenColors.length);
        for (int i = 0; i < substitutedColors.length; i++) {
            if (substitutedColors[i].equals("ChosenColor")) {
                // this currently only supports 1 chosen color
                substitutedColors[i] = host.getChosenColor().get(0);
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
        if (this.tokenImage.equals("")) {
            imageName += colorDesc.replace(" ", "") + " " + this.tokenPower + " " + this.tokenToughness + " " + this.tokenName;
        } else {
            imageName = this.tokenImage;
        }
        // System.out.println("AF_Token imageName = " + imageName);

        for (final char c : colorDesc.toCharArray()) {
            cost += c + ' ';
        }

        cost = colorDesc.replace('C', '1').trim();

        controller = AbilityFactory.getDefinedPlayers(host, this.tokenOwner, sa).get(0);

        final int finalPower = AbilityFactory.calculateAmount(host, this.tokenPower, sa);
        final int finalToughness = AbilityFactory.calculateAmount(host, this.tokenToughness, sa);
        final int finalAmount = AbilityFactory.calculateAmount(host, this.tokenAmount, sa);

        final String[] substitutedTypes = Arrays.copyOf(this.tokenTypes, this.tokenTypes.length);
        for (int i = 0; i < substitutedTypes.length; i++) {
            if (substitutedTypes[i].equals("ChosenType")) {
                substitutedTypes[i] = host.getChosenType();
            }
        }
        final String substitutedName = this.tokenName.equals("ChosenType") ? host.getChosenType() : this.tokenName;

        final String remember = params.get("RememberTokens");
        for (int i = 0; i < finalAmount; i++) {
            final List<Card> tokens = CardFactoryUtil.makeToken(substitutedName, imageName, controller, cost,
                    substitutedTypes, finalPower, finalToughness, this.tokenKeywords);

            // Grant abilities
            if (this.tokenAbilities != null) {
                final AbilityFactory af = new AbilityFactory();
                for (final String s : this.tokenAbilities) {
                    final String actualAbility = host.getSVar(s);
                    for (final Card c : tokens) {
                        final SpellAbility grantedAbility = af.getAbility(actualAbility, c);
                        c.addSpellAbility(grantedAbility);
                    }
                }
            }

            // Grant triggers
            if (this.tokenTriggers != null) {

                for (final String s : this.tokenTriggers) {
                    final String actualTrigger = host.getSVar(s);

                    for (final Card c : tokens) {

                        final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c, true);
                        final String ability = host.getSVar(parsedTrigger.getMapParams().get("Execute"));
                        parsedTrigger.setOverridingAbility(new AbilityFactory().getAbility(ability, c));
                        c.addTrigger(parsedTrigger);
                    }
                }
            }

            // Grant SVars
            if (this.tokenSVars != null) {
                for (final String s : this.tokenSVars) {
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
            if (this.tokenStaticAbilities != null) {
                for (final String s : this.tokenStaticAbilities) {
                    final String actualAbility = host.getSVar(s);
                    for (final Card c : tokens) {
                        c.addStaticAbility(actualAbility);
                    }
                }
            }

            for (final Card c : tokens) {
                if (this.tokenTapped) {
                    c.setTapped(true);
                }
                if (this.tokenAttacking) {
                    Singletons.getModel().getGame().getCombat().addAttacker(c);
                }
                if (remember != null) {
                    Singletons.getModel().getGame().getCardState(sa.getSourceCard()).addRemembered(c);
                }
                if (params.get("RememberSource") != null) {
                    Singletons.getModel().getGame().getCardState(c).addRemembered(host);
                }
            }
        }
    }
}
