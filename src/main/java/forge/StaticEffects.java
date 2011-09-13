package forge;

import com.esotericsoftware.minlog.Log;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.staticAbility.StaticAbility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 * <p>StaticEffects class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class StaticEffects {

    //**************** StaticAbility system **************************
    /**
     * staticEffects.
     */
    public ArrayList<StaticEffect> staticEffects;

    /**
     * clearStaticEffect.
     * TODO Write javadoc for this method.
     */
    public final void clearStaticEffects() {
        // remove all static effects
        for (int i = 0; i < staticEffects.size(); i++) {
            removeStaticEffect(staticEffects.get(i));
        }
        staticEffects = new ArrayList<StaticEffect>();

        AllZone.getTriggerHandler().cleanUpTemporaryTriggers();
    }

    /**
     * addStaticEffect.
     * TODO Write javadoc for this method.
     * @param staticEffect a StaticEffect
     */
    public final void addStaticEffect(final StaticEffect staticEffect) {
        staticEffects.add(staticEffect);
    }

    /**
     * removeStaticEffect
     * TODO Write javadoc for this method.
     * @param se a StaticEffect
     */
    final void removeStaticEffect(final StaticEffect se) {
        CardList affectedCards = se.getAffectedCards();
        ArrayList<Player> affectedPlayers = se.getAffectedPlayers();
        HashMap<String, String> params = se.getParams();

        int powerBonus = 0;
        int toughnessBonus = 0;
        boolean setPT = false;
        String[] addKeywords = null;
        String[] addHiddenKeywords = null;
        String addColors = null;

        if (params.containsKey("SetPower") || params.containsKey("SetToughness")) {
            setPT = true;
        }

        if (params.containsKey("AddPower")) {
            if (params.get("AddPower").equals("X")) {
                powerBonus = se.getXValue();
            } else if (params.get("AddPower").equals("Y")) {
                powerBonus = se.getYValue();
            } else {
                powerBonus = Integer.valueOf(params.get("AddPower"));
            }
        }

        if (params.containsKey("AddToughness")) {
            if (params.get("AddToughness").equals("X")) {
                toughnessBonus = se.getXValue();
            } else if (params.get("AddToughness").equals("Y")) {
                toughnessBonus = se.getYValue();
            } else {
                toughnessBonus = Integer.valueOf(params.get("AddToughness"));
            }
        }
        
        if (params.containsKey("AddHiddenKeyword")) {
            addHiddenKeywords = params.get("AddHiddenKeyword").split(" & ");
        }

        if (params.containsKey("AddColor")) {
            addColors = CardUtil.getShortColorsString(
                    new ArrayList<String>(Arrays.asList(params.get("AddColor").split(" & "))));
        }

        if (params.containsKey("SetColor")) {
            addColors = CardUtil.getShortColorsString(
                    new ArrayList<String>(Arrays.asList(params.get("SetColor").split(" & "))));
        }
        
        //modify players
        for (Player p : affectedPlayers) {
            if (params.containsKey("AddKeyword")) {
                addKeywords = params.get("AddKeyword").split(" & ");
            }
            
            // add keywords
            if (addKeywords != null)
                for (String keyword : addKeywords)
                    p.removeKeyword(keyword);
        }

        //modify the affected card
        for (int i = 0; i < affectedCards.size(); i++) {
            Card affectedCard = affectedCards.get(i);

            //remove set P/T
            if (!params.containsKey("CharacteristicDefining") && setPT) {
                affectedCard.removeNewPT(se.getTimestamp());
            }

            //remove P/T bonus
            affectedCard.addSemiPermanentAttackBoost(powerBonus * -1);
            affectedCard.addSemiPermanentDefenseBoost(toughnessBonus * -1);

            //remove keywords
            if (params.containsKey("AddKeyword") || params.containsKey("RemoveKeyword") || params.containsKey("RemoveAllAbilities")) {
                affectedCard.removeChangedCardKeywords(se.getTimestamp());
            }

            //remove abilities
            if (params.containsKey("AddAbility")) {
                SpellAbility[] spellAbility = affectedCard.getSpellAbility();
                for (SpellAbility s : spellAbility) {
                    if (s.getType().equals("Temporary")) {
                        affectedCard.removeSpellAbility(s);
                    }
                }
            }
            
            if(addHiddenKeywords != null) {
                for (String k : addHiddenKeywords) {
                    affectedCard.removeExtrinsicKeyword(k);
                }
            }
            
            //remove abilities
            if (params.containsKey("RemoveAllAbilities")) {
                ArrayList<SpellAbility> abilities = affectedCard.getSpellAbilities();
                for (SpellAbility ab : abilities) {
                    ab.setTemporarilySuppressed(false);
                }
                
                ArrayList<StaticAbility> staticAbilities = affectedCard.getStaticAbilities();
                for (StaticAbility stA : staticAbilities) {
                    stA.setTemporarilySuppressed(false);
                }
            }

            //remove Types
            if (params.containsKey("AddType") || params.containsKey("RemoveType")) {
                affectedCard.removeChangedCardTypes(se.getTimestamp());
            }

            //remove colors
            if (addColors != null) {
                affectedCard.removeColor(addColors, affectedCard,
                        !se.isOverwriteColors(), se.getTimestamp(affectedCard));
            }
        }
        se.clearTimestamps();
    }

    //**************** End StaticAbility system **************************

    //this is used to keep track of all state-based effects in play:
    private HashMap<String, Integer> stateBasedMap = new HashMap<String, Integer>();

    //this is used to define all cards that are state-based effects, and map the
    //corresponding commands to their cardnames
    /** Constant <code>cardToEffectsList</code>. */
    private static HashMap<String, String[]> cardToEffectsList = new HashMap<String, String[]>();

    /**
     * <p>Constructor for StaticEffects.</p>
     */
    public StaticEffects() {
        initStateBasedEffectsList();
        staticEffects = new ArrayList<StaticEffect>();
    }

    /**
     * <p>initStateBasedEffectsList.</p>
     */
    public final void initStateBasedEffectsList() {
        //value has to be an array, since certain cards have multiple commands associated with them

        cardToEffectsList.put("Avatar", new String[]{"Ajani_Avatar_Token"});
        cardToEffectsList.put("Coat of Arms", new String[]{"Coat_of_Arms"});
        cardToEffectsList.put("Gaddock Teeg", new String[]{"Gaddock_Teeg"});

        cardToEffectsList.put("Homarid", new String[]{"Homarid"});
        cardToEffectsList.put("Iona, Shield of Emeria", new String[]{"Iona_Shield_of_Emeria"});
        cardToEffectsList.put("Liu Bei, Lord of Shu", new String[]{"Liu_Bei"});

        cardToEffectsList.put("Meddling Mage", new String[]{"Meddling_Mage"});
        cardToEffectsList.put("Muraganda Petroglyphs", new String[]{"Muraganda_Petroglyphs"});
        cardToEffectsList.put("Old Man of the Sea", new String[]{"Old_Man_of_the_Sea"});

        cardToEffectsList.put("Tarmogoyf", new String[]{"Tarmogoyf"});

        cardToEffectsList.put("Umbra Stalker", new String[]{"Umbra_Stalker"});
        cardToEffectsList.put("Wolf", new String[]{"Sound_the_Call_Wolf"});

    }

    /**
     * <p>Getter for the field <code>cardToEffectsList</code>.</p>
     *
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String[]> getCardToEffectsList() {
        return cardToEffectsList;
    }

    /**
     * <p>addStateBasedEffect.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void addStateBasedEffect(final String s) {
        if (stateBasedMap.containsKey(s)) {
            stateBasedMap.put(s, stateBasedMap.get(s) + 1);
        } else {
            stateBasedMap.put(s, 1);
        }
    }

    /**
     * <p>removeStateBasedEffect.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void removeStateBasedEffect(final String s) {
        if (stateBasedMap.containsKey(s)) {
            stateBasedMap.put(s, stateBasedMap.get(s) - 1);
            if (stateBasedMap.get(s) == 0) {
                stateBasedMap.remove(s);
            }
        }
    }

    /**
     * <p>Getter for the field <code>stateBasedMap</code>.</p>
     *
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, Integer> getStateBasedMap() {
        return stateBasedMap;
    }

    /**
     * <p>reset.</p>
     */
    public final void reset() {
        stateBasedMap.clear();
    }

    /**
     * <p>rePopulateStateBasedList.</p>
     */
    public final void rePopulateStateBasedList() {
        reset();

        CardList cards = AllZoneUtil.getCardsInPlay();

        Log.debug("== Start add state effects ==");
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (cardToEffectsList.containsKey(c.getName())) {
                String[] effects = getCardToEffectsList().get(c.getName());
                for (String effect : effects) {
                    addStateBasedEffect(effect);
                    Log.debug("Added " + effect);
                }
            }
            if (c.isEmblem() && !CardFactoryUtil.checkEmblemKeyword(c).equals("")) {
                String s = CardFactoryUtil.checkEmblemKeyword(c);
                addStateBasedEffect(s);
                Log.debug("Added " + s);
            }
        }
        Log.debug("== End add state effects ==");

    }

} //end class StaticEffects
