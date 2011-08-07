package forge.card.abilityFactory;

import forge.*;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>AbilityFactory class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class AbilityFactory {

    private Card hostC = null;

    /**
     * <p>getHostCard.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getHostCard() {
        return hostC;
    }

    private HashMap<String, String> mapParams = new HashMap<String, String>();

    /**
     * <p>Getter for the field <code>mapParams</code>.</p>
     *
     * @return a {@link java.util.HashMap} object.
     */
    public HashMap<String, String> getMapParams() {
        return mapParams;
    }

    private boolean isAb = false;
    private boolean isSp = false;
    private boolean isDb = false;

    /**
     * <p>isAbility.</p>
     *
     * @return a boolean.
     */
    public boolean isAbility() {
        return isAb;
    }

    /**
     * <p>isSpell.</p>
     *
     * @return a boolean.
     */
    public boolean isSpell() {
        return isSp;
    }

    /**
     * <p>isDrawback.</p>
     *
     * @return a boolean.
     */
    public boolean isDrawback() {
        return isDb;
    }

    private Cost abCost = null;

    /**
     * <p>Getter for the field <code>abCost</code>.</p>
     *
     * @return a {@link forge.card.spellability.Cost} object.
     */
    public Cost getAbCost() {
        return abCost;
    }

    private boolean isTargeted = false;
    private boolean hasValid = false;
    private Target abTgt = null;

    /**
     * <p>isTargeted.</p>
     *
     * @return a boolean.
     */
    public boolean isTargeted() {
        return isTargeted;
    }

    /**
     * <p>hasValid.</p>
     *
     * @return a boolean.
     */
    public boolean hasValid() {
        return hasValid;
    }

    /**
     * <p>Getter for the field <code>abTgt</code>.</p>
     *
     * @return a {@link forge.card.spellability.Target} object.
     */
    public Target getAbTgt() {
        return abTgt;
    }

    /**
     * <p>isCurse.</p>
     *
     * @return a boolean.
     */
    public boolean isCurse() {
        return mapParams.containsKey("IsCurse");
    }

    private boolean hasSubAb = false;

    /**
     * <p>hasSubAbility.</p>
     *
     * @return a boolean.
     */
    public boolean hasSubAbility() {
        return hasSubAb;
    }

    private boolean hasSpDesc = false;

    /**
     * <p>hasSpDescription.</p>
     *
     * @return a boolean.
     */
    public boolean hasSpDescription() {
        return hasSpDesc;
    }

    private String API = "";

    /**
     * <p>getAPI.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAPI() {
        return API;
    }

    //*******************************************************

    /**
     * <p>Getter for the field <code>mapParams</code>.</p>
     *
     * @param abString a {@link java.lang.String} object.
     * @param hostCard a {@link forge.Card} object.
     * @return a {@link java.util.HashMap} object.
     */
    public HashMap<String, String> getMapParams(String abString, Card hostCard) {
        HashMap<String, String> mapParameters = new HashMap<String, String>();

        if (!(abString.length() > 0))
            throw new RuntimeException("AbilityFactory : getAbility -- abString too short in " + hostCard.getName() + ": [" + abString + "]");

        String a[] = abString.split("\\|");

        for (int aCnt = 0; aCnt < a.length; aCnt++)
            a[aCnt] = a[aCnt].trim();

        if (!(a.length > 0))
            throw new RuntimeException("AbilityFactory : getAbility -- a[] too short in " + hostCard.getName());

        for (int i = 0; i < a.length; i++) {
            String aa[] = a[i].split("\\$");

            for (int aaCnt = 0; aaCnt < aa.length; aaCnt++)
                aa[aaCnt] = aa[aaCnt].trim();

            if (aa.length != 2) {
                StringBuilder sb = new StringBuilder();
                sb.append("AbilityFactory Parsing Error in getAbility() : Split length of ");
                sb.append(a[i]).append(" in ").append(hostCard.getName()).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParameters.put(aa[0], aa[1]);
        }

        return mapParameters;
    }

    /**
     * <p>getAbility.</p>
     *
     * @param abString a {@link java.lang.String} object.
     * @param hostCard a {@link forge.Card} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getAbility(String abString, Card hostCard) {

        SpellAbility SA = null;

        hostC = hostCard;

        mapParams = getMapParams(abString, hostCard);

        // parse universal parameters

        if (mapParams.containsKey("AB")) {
            isAb = true;
            API = mapParams.get("AB");
        } else if (mapParams.containsKey("SP")) {
            isSp = true;
            API = mapParams.get("SP");
        } else if (mapParams.containsKey("DB")) {
            isDb = true;
            API = mapParams.get("DB");
        } else
            throw new RuntimeException("AbilityFactory : getAbility -- no API in " + hostCard.getName());

        if (!isDb) {
            if (!mapParams.containsKey("Cost"))
                throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + hostCard.getName());
            abCost = new Cost(mapParams.get("Cost"), hostCard.getName(), isAb);
        }

        if (mapParams.containsKey("ValidTgts")) {
            hasValid = true;
            isTargeted = true;
        }

        if (mapParams.containsKey("Tgt")) {
            isTargeted = true;
        }

        if (isTargeted) {
            String min = mapParams.containsKey("TargetMin") ? mapParams.get("TargetMin") : "1";
            String max = mapParams.containsKey("TargetMax") ? mapParams.get("TargetMax") : "1";

            if (hasValid) {
                // TgtPrompt now optional
                StringBuilder sb = new StringBuilder();
                if (hostC != null) sb.append(hostC + " - ");
                String prompt = mapParams.containsKey("TgtPrompt") ? mapParams.get("TgtPrompt") : "Select target " + mapParams.get("ValidTgts");
                sb.append(prompt);
                abTgt = new Target(hostC, sb.toString(), mapParams.get("ValidTgts").split(","), min, max);
            } else
                abTgt = new Target(hostC, mapParams.get("Tgt"), min, max);

            if (mapParams.containsKey("TgtZone"))    // if Targeting something not in play, this Key should be set
                abTgt.setZone(mapParams.get("TgtZone"));

            // Target Type mostly for Counter: Spell,Activated,Triggered,Ability (or any combination of)
            // Ability = both activated and triggered abilities
            if (mapParams.containsKey("TargetType"))
                abTgt.setTargetSpellAbilityType(mapParams.get("TargetType"));

            // TargetValidTargeting most for Counter: e.g. target spell that targets X.
            if (mapParams.containsKey("TargetValidTargeting"))
                abTgt.setSAValidTargeting(mapParams.get("TargetValidTargeting"));
        }

        hasSubAb = mapParams.containsKey("SubAbility");

        hasSpDesc = mapParams.containsKey("SpellDescription");

        // ***********************************
        // Match API keywords

        if (API.equals("DealDamage")) {
            AbilityFactory_DealDamage dd = new AbilityFactory_DealDamage(this);

            if (isAb)
                SA = dd.getAbility();
            else if (isSp)
                SA = dd.getSpell();
            else if (isDb)
                SA = dd.getDrawback();
        }

        else if (API.equals("DamageAll")) {
            AbilityFactory_DealDamage dd = new AbilityFactory_DealDamage(this);
            if (isAb)
                SA = dd.getAbilityDamageAll();
            else if (isSp)
                SA = dd.getSpellDamageAll();
            else if (isDb)
                SA = dd.getDrawbackDamageAll();
        }

        else if (API.equals("PutCounter")) {
            if (isAb)
                SA = AbilityFactory_Counters.createAbilityPutCounters(this);
            else if (isSp)
                SA = AbilityFactory_Counters.createSpellPutCounters(this);
            else if (isDb)
                SA = AbilityFactory_Counters.createDrawbackPutCounters(this);
        }

        else if (API.equals("PutCounterAll")) {
            if (isAb)
                SA = AbilityFactory_Counters.createAbilityPutCounterAll(this);
            else if (isSp)
                SA = AbilityFactory_Counters.createSpellPutCounterAll(this);
            else if (isDb)
                SA = AbilityFactory_Counters.createDrawbackPutCounterAll(this);
        }

        else if (API.equals("RemoveCounter")) {
            if (isAb)
                SA = AbilityFactory_Counters.createAbilityRemoveCounters(this);
            else if (isSp)
                SA = AbilityFactory_Counters.createSpellRemoveCounters(this);
            else if (isDb)
                SA = AbilityFactory_Counters.createDrawbackRemoveCounters(this);
        }

        else if (API.equals("RemoveCounterAll")) {
            if (isAb)
                SA = AbilityFactory_Counters.createAbilityRemoveCounterAll(this);
            else if (isSp)
                SA = AbilityFactory_Counters.createSpellRemoveCounterAll(this);
            else if (isDb)
                SA = AbilityFactory_Counters.createDrawbackRemoveCounterAll(this);
        }

        else if (API.equals("Proliferate")) {
            if (isAb)
                SA = AbilityFactory_Counters.createAbilityProliferate(this);
            else if (isSp)
                SA = AbilityFactory_Counters.createSpellProliferate(this);
            else if (isDb)
                SA = AbilityFactory_Counters.createDrawbackProliferate(this);
        }

        else if (API.equals("ChangeZone")) {
            if (isAb)
                SA = AbilityFactory_ChangeZone.createAbilityChangeZone(this);
            else if (isSp)
                SA = AbilityFactory_ChangeZone.createSpellChangeZone(this);
            else if (isDb)
                SA = AbilityFactory_ChangeZone.createDrawbackChangeZone(this);
        }

        else if (API.equals("ChangeZoneAll")) {
            if (isAb)
                SA = AbilityFactory_ChangeZone.createAbilityChangeZoneAll(this);
            else if (isSp)
                SA = AbilityFactory_ChangeZone.createSpellChangeZoneAll(this);
            else if (isDb)
                SA = AbilityFactory_ChangeZone.createDrawbackChangeZoneAll(this);
        }

        else if (API.equals("Pump")) {
            AbilityFactory_Pump afPump = new AbilityFactory_Pump(this);

            if (isAb)
                SA = afPump.getAbilityPump();
            else if (isSp)
                SA = afPump.getSpellPump();
            else if (isDb)
                SA = afPump.getDrawbackPump();

            if (isAb || isSp)
                hostCard.setSVar("PlayMain1", "TRUE");
        }

        else if (API.equals("PumpAll")) {
            AbilityFactory_Pump afPump = new AbilityFactory_Pump(this);

            if (isAb)
                SA = afPump.getAbilityPumpAll();
            else if (isSp)
                SA = afPump.getSpellPumpAll();
            else if (isDb)
                SA = afPump.getDrawbackPumpAll();

            if (isAb || isSp)
                hostCard.setSVar("PlayMain1", "TRUE");
        }

        else if (API.equals("GainLife")) {
            if (isAb)
                SA = AbilityFactory_AlterLife.createAbilityGainLife(this);
            else if (isSp)
                SA = AbilityFactory_AlterLife.createSpellGainLife(this);
            else if (isDb)
                SA = AbilityFactory_AlterLife.createDrawbackGainLife(this);
        }

        else if (API.equals("LoseLife")) {
            if (isAb)
                SA = AbilityFactory_AlterLife.createAbilityLoseLife(this);
            else if (isSp)
                SA = AbilityFactory_AlterLife.createSpellLoseLife(this);
            else if (isDb)
                SA = AbilityFactory_AlterLife.createDrawbackLoseLife(this);
        }

        else if (API.equals("SetLife")) {
            if (isAb)
                SA = AbilityFactory_AlterLife.createAbilitySetLife(this);
            else if (isSp)
                SA = AbilityFactory_AlterLife.createSpellSetLife(this);
            else if (isDb)
                SA = AbilityFactory_AlterLife.createDrawbackSetLife(this);
        }

        else if (API.equals("Poison")) {
            if (isAb)
                SA = AbilityFactory_AlterLife.createAbilityPoison(this);
            else if (isSp)
                SA = AbilityFactory_AlterLife.createSpellPoison(this);
            else if (isDb)
                SA = AbilityFactory_AlterLife.createDrawbackPoison(this);
        }

        else if (API.equals("Fog")) {
            if (isAb)
                SA = AbilityFactory_Combat.createAbilityFog(this);
            else if (isSp)
                SA = AbilityFactory_Combat.createSpellFog(this);
            else if (isDb)
                SA = AbilityFactory_Combat.createDrawbackFog(this);
        }

        else if (API.equals("Untap")) {
            if (isAb)
                SA = AbilityFactory_PermanentState.createAbilityUntap(this);
            else if (isSp)
                SA = AbilityFactory_PermanentState.createSpellUntap(this);
            else if (isDb)
                SA = AbilityFactory_PermanentState.createDrawbackUntap(this);
        }

        else if (API.equals("UntapAll")) {
            if (isAb)
                SA = AbilityFactory_PermanentState.createAbilityUntapAll(this);
            else if (isSp)
                SA = AbilityFactory_PermanentState.createSpellUntapAll(this);
            else if (isDb)
                SA = AbilityFactory_PermanentState.createDrawbackUntapAll(this);
        }

        else if (API.equals("Tap")) {
            if (isAb)
                SA = AbilityFactory_PermanentState.createAbilityTap(this);
            else if (isSp)
                SA = AbilityFactory_PermanentState.createSpellTap(this);
            else if (isDb)
                SA = AbilityFactory_PermanentState.createDrawbackTap(this);
        }

        else if (API.equals("TapAll")) {
            if (isAb)
                SA = AbilityFactory_PermanentState.createAbilityTapAll(this);
            else if (isSp)
                SA = AbilityFactory_PermanentState.createSpellTapAll(this);
            else if (isDb)
                SA = AbilityFactory_PermanentState.createDrawbackTapAll(this);
        }

        else if (API.equals("TapOrUntap")) {
            if (isAb)
                SA = AbilityFactory_PermanentState.createAbilityTapOrUntap(this);
            else if (isSp)
                SA = AbilityFactory_PermanentState.createSpellTapOrUntap(this);
            else if (isDb)
                SA = AbilityFactory_PermanentState.createDrawbackTapOrUntap(this);
        }

        else if (API.equals("PreventDamage")) {
            if (isAb)
                SA = AbilityFactory_PreventDamage.getAbilityPreventDamage(this);
            else if (isSp)
                SA = AbilityFactory_PreventDamage.getSpellPreventDamage(this);
            else if (isDb) {
                SA = AbilityFactory_PreventDamage.createDrawbackPreventDamage(this);
            }
        }

        else if (API.equals("Regenerate")) {
            if (isAb)
                SA = AbilityFactory_Regenerate.getAbilityRegenerate(this);
            else if (isSp)
                SA = AbilityFactory_Regenerate.getSpellRegenerate(this);
            else if (isDb) {
                SA = AbilityFactory_Regenerate.createDrawbackRegenerate(this);
            }
        }

        else if (API.equals("Draw")) {
            if (isAb)
                SA = AbilityFactory_ZoneAffecting.createAbilityDraw(this);
            else if (isSp)
                SA = AbilityFactory_ZoneAffecting.createSpellDraw(this);
            else if (isDb)
                SA = AbilityFactory_ZoneAffecting.createDrawbackDraw(this);
        }

        else if (API.equals("Mill")) {
            if (isAb)
                SA = AbilityFactory_ZoneAffecting.createAbilityMill(this);
            else if (isSp)
                SA = AbilityFactory_ZoneAffecting.createSpellMill(this);
            else if (isDb)
                SA = AbilityFactory_ZoneAffecting.createDrawbackMill(this);
        }

        else if (API.equals("Scry")) {
            if (isAb)
                SA = AbilityFactory_Reveal.createAbilityScry(this);
            else if (isSp)
                SA = AbilityFactory_Reveal.createSpellScry(this);
            else if (isDb)
                SA = AbilityFactory_Reveal.createDrawbackScry(this);
        }

        else if (API.equals("RearrangeTopOfLibrary")) {
            if (isAb)
                SA = AbilityFactory_Reveal.createRearrangeTopOfLibraryAbility(this);
            else if (isSp)
                SA = AbilityFactory_Reveal.createRearrangeTopOfLibrarySpell(this);
            else if (isDb)
                SA = AbilityFactory_Reveal.createRearrangeTopOfLibraryDrawback(this);
        }

        else if (API.equals("Sacrifice")) {
            if (isAb)
                SA = AbilityFactory_Sacrifice.createAbilitySacrifice(this);
            else if (isSp)
                SA = AbilityFactory_Sacrifice.createSpellSacrifice(this);
            else if (isDb)
                SA = AbilityFactory_Sacrifice.createDrawbackSacrifice(this);
        }

        else if (API.equals("SacrificeAll")) {
            if (isAb)
                SA = AbilityFactory_Sacrifice.createAbilitySacrificeAll(this);
            else if (isSp)
                SA = AbilityFactory_Sacrifice.createSpellSacrificeAll(this);
            else if (isDb)
                SA = AbilityFactory_Sacrifice.createDrawbackSacrificeAll(this);
        }

        else if (API.equals("Destroy")) {
            if (isAb)
                SA = AbilityFactory_Destroy.createAbilityDestroy(this);
            else if (isSp)
                SA = AbilityFactory_Destroy.createSpellDestroy(this);
            else if (isDb) {
                SA = AbilityFactory_Destroy.createDrawbackDestroy(this);
            }
        }

        else if (API.equals("DestroyAll")) {
            if (isAb)
                SA = AbilityFactory_Destroy.createAbilityDestroyAll(this);
            else if (isSp)
                SA = AbilityFactory_Destroy.createSpellDestroyAll(this);
            else if (isDb)
                SA = AbilityFactory_Destroy.createDrawbackDestroyAll(this);
        }

        else if (API.equals("Mana")) {
            String produced = mapParams.get("Produced");
            if (isAb)
                SA = AbilityFactory_Mana.createAbilityMana(this, produced);
            if (isSp)
                SA = AbilityFactory_Mana.createSpellMana(this, produced);
            if (isDb)
                SA = AbilityFactory_Mana.createDrawbackMana(this, produced);
        }

        else if (API.equals("ManaReflected")) {
            // Reflected mana will have a filler for produced of "1"
            if (isAb)
                SA = AbilityFactory_Mana.createAbilityManaReflected(this, "1");
            if (isSp) {    // shouldn't really happen i think?
                SA = AbilityFactory_Mana.createSpellManaReflected(this, "1");
            }
        }

        else if (API.equals("Token")) {
            AbilityFactory_Token AFT = new AbilityFactory_Token(this);

            if (isAb)
                SA = AFT.getAbility();
            else if (isSp)
                SA = AFT.getSpell();
            else if (isDb)
                SA = AFT.getDrawback();
        }

        else if (API.equals("GainControl")) {
            AbilityFactory_GainControl afControl = new AbilityFactory_GainControl(this);

            if (isAb)
                SA = afControl.getAbilityGainControl();
            else if (isSp)
                SA = afControl.getSpellGainControl();
            else if (isDb) {
                SA = afControl.getDrawbackGainControl();
            }
        }

        else if (API.equals("Discard")) {
            if (isAb)
                SA = AbilityFactory_ZoneAffecting.createAbilityDiscard(this);
            else if (isSp)
                SA = AbilityFactory_ZoneAffecting.createSpellDiscard(this);
            else if (isDb)
                SA = AbilityFactory_ZoneAffecting.createDrawbackDiscard(this);
        }

        else if (API.equals("Counter")) {
            AbilityFactory_CounterMagic c = new AbilityFactory_CounterMagic(this);

            if (isTargeted)    // Since all "Counter" ABs Counter things on the Stack no need for it to be everywhere
                abTgt.setZone("Stack");

            if (isAb)
                SA = c.getAbilityCounter(this);
            else if (isSp)
                SA = c.getSpellCounter(this);
            else if (isDb)
                SA = c.getDrawbackCounter(this);
        }

        else if (API.equals("AddTurn")) {
            if (isAb)
                SA = AbilityFactory_Turns.createAbilityAddTurn(this);
            else if (isSp)
                SA = AbilityFactory_Turns.createSpellAddTurn(this);
            else if (isDb)
                SA = AbilityFactory_Turns.createDrawbackAddTurn(this);
        }

        else if (API.equals("Clash")) {
            if (isAb)
                SA = AbilityFactory_Clash.getAbilityClash(this);
            else if (isSp)
                SA = AbilityFactory_Clash.getSpellClash(this);
            else if (isDb)
                SA = AbilityFactory_Clash.getDrawbackClash(this);
        }

        else if (API.equals("Animate")) {
            if (isAb)
                SA = AbilityFactory_Animate.createAbilityAnimate(this);
            else if (isSp)
                SA = AbilityFactory_Animate.createSpellAnimate(this);
            else if (isDb)
                SA = AbilityFactory_Animate.createDrawbackAnimate(this);
        }

        else if (API.equals("Effect")) {
            if (isAb)
                SA = AbilityFactory_Effect.createAbilityEffect(this);
            else if (isSp)
                SA = AbilityFactory_Effect.createSpellEffect(this);
            else if (isDb)
                SA = AbilityFactory_Effect.createDrawbackEffect(this);
        }

        else if (API.equals("WinsGame")) {
            if (isAb)
                SA = AbilityFactory_EndGameCondition.createAbilityWinsGame(this);
            else if (isSp)
                SA = AbilityFactory_EndGameCondition.createSpellWinsGame(this);
            else if (isDb)
                SA = AbilityFactory_EndGameCondition.createDrawbackWinsGame(this);
        }

        else if (API.equals("LosesGame")) {
            if (isAb)
                SA = AbilityFactory_EndGameCondition.createAbilityLosesGame(this);
            else if (isSp)
                SA = AbilityFactory_EndGameCondition.createSpellLosesGame(this);
            else if (isDb)
                SA = AbilityFactory_EndGameCondition.createDrawbackLosesGame(this);
        }

        else if (API.equals("RevealHand")) {
            if (isAb)
                SA = AbilityFactory_Reveal.createAbilityRevealHand(this);
            else if (isSp)
                SA = AbilityFactory_Reveal.createSpellRevealHand(this);
            else if (isDb)
                SA = AbilityFactory_Reveal.createDrawbackRevealHand(this);
        }

        else if (API.equals("Dig")) {
            if (isAb)
                SA = AbilityFactory_Reveal.createAbilityDig(this);
            else if (isSp)
                SA = AbilityFactory_Reveal.createSpellDig(this);
            else if (isDb)
                SA = AbilityFactory_Reveal.createDrawbackDig(this);
        }

        else if (API.equals("Shuffle")) {
            if (isAb)
                SA = AbilityFactory_ZoneAffecting.createAbilityShuffle(this);
            else if (isSp)
                SA = AbilityFactory_ZoneAffecting.createSpellShuffle(this);
            else if (isDb)
                SA = AbilityFactory_ZoneAffecting.createDrawbackShuffle(this);
        }

        else if (API.equals("ChooseType")) {
            if (isAb)
                SA = AbilityFactory_Choose.createAbilityChooseType(this);
            else if (isSp)
                SA = AbilityFactory_Choose.createSpellChooseType(this);
            else if (isDb)
                SA = AbilityFactory_Choose.createDrawbackChooseType(this);
        }

        else if (API.equals("ChooseColor")) {
            if (isAb)
                SA = AbilityFactory_Choose.createAbilityChooseColor(this);
            else if (isSp)
                SA = AbilityFactory_Choose.createSpellChooseColor(this);
            else if (isDb)
                SA = AbilityFactory_Choose.createDrawbackChooseColor(this);
        }

        else if (API.equals("CopyPermanent")) {
            if (isAb)
                SA = AbilityFactory_Copy.createAbilityCopyPermanent(this);
            else if (isSp)
                SA = AbilityFactory_Copy.createSpellCopyPermanent(this);
            else if (isDb)
                SA = AbilityFactory_Copy.createDrawbackCopyPermanent(this);
        }

        else if (API.equals("CopySpell")) {
            if (isTargeted)    // Since all "CopySpell" ABs copy things on the Stack no need for it to be everywhere
                abTgt.setZone("Stack");

            if (isAb)
                SA = AbilityFactory_Copy.createAbilityCopySpell(this);
            else if (isSp)
                SA = AbilityFactory_Copy.createSpellCopySpell(this);
            else if (isDb)
                SA = AbilityFactory_Copy.createDrawbackCopySpell(this);

            hostCard.setCopiesSpells(true);
        }

        else if (API.equals("FlipACoin")) {
            if (isAb)
                SA = AbilityFactory_Clash.createAbilityFlip(this);
            else if (isSp)
                SA = AbilityFactory_Clash.createSpellFlip(this);
            else if (isDb)
                SA = AbilityFactory_Clash.createDrawbackFlip(this);
        }

        else if (API.equals("DelayedTrigger")) {
        	if (isAb)
                SA = AbilityFactory_DelayedTrigger.getAbility(this);
            else if (isSp)
                SA = AbilityFactory_DelayedTrigger.getSpell(this);
            if (isDb)
                SA = AbilityFactory_DelayedTrigger.getDrawback(this);
        }

        else if (API.equals("Cleanup")) {
            if (isDb)
                SA = AbilityFactory_Cleanup.getDrawback(this);
        }

        else if (API.equals("RegenerateAll")) {
            if (isAb)
                SA = AbilityFactory_Regenerate.getAbilityRegenerateAll(this);
            else if (isSp)
                SA = AbilityFactory_Regenerate.getSpellRegenerateAll(this);
            else if (isDb) {
                SA = AbilityFactory_Regenerate.createDrawbackRegenerateAll(this);
            }
        }

        else if (API.equals("AnimateAll")) {
            if (isAb)
                SA = AbilityFactory_Animate.createAbilityAnimateAll(this);
            else if (isSp)
                SA = AbilityFactory_Animate.createSpellAnimateAll(this);
            else if (isDb)
                SA = AbilityFactory_Animate.createDrawbackAnimateAll(this);
        }

        else if (API.equals("Debuff")) {
            if (isAb)
                SA = AbilityFactory_Debuff.createAbilityDebuff(this);
            else if (isSp)
                SA = AbilityFactory_Debuff.createSpellDebuff(this);
            else if (isDb)
                SA = AbilityFactory_Debuff.createDrawbackDebuff(this);
        }

        else if (API.equals("DebuffAll")) {
            if (isAb)
                SA = AbilityFactory_Debuff.createAbilityDebuffAll(this);
            else if (isSp)
                SA = AbilityFactory_Debuff.createSpellDebuffAll(this);
            else if (isDb)
                SA = AbilityFactory_Debuff.createDrawbackDebuffAll(this);
        }

        else if (API.equals("DrainMana")) {
            if (isAb)
                SA = AbilityFactory_Mana.createAbilityDrainMana(this);
            else if (isSp)
                SA = AbilityFactory_Mana.createSpellDrainMana(this);
            else if (isDb)
                SA = AbilityFactory_Mana.createDrawbackDrainMana(this);
        }
        
        else if (API.equals("Protection")) {
            if (isAb)
                SA = AbilityFactory_Protection.createAbilityProtection(this);
            else if (isSp)
                SA = AbilityFactory_Protection.createSpellProtection(this);
            else if (isDb)
                SA = AbilityFactory_Protection.createDrawbackProtection(this);
        }

        else if (API.equals("Attach")) {
            if (isAb)
                SA = AbilityFactory_Attach.createAbilityAttach(this);
            else if (isSp)
                SA = AbilityFactory_Attach.createSpellAttach(this);
            else if (isDb)
                SA = AbilityFactory_Attach.createDrawbackAttach(this);
        }
        
        else if (API.equals("ProtectionAll")) {
            if (isAb)
                SA = AbilityFactory_Protection.createAbilityProtectionAll(this);
            else if (isSp)
                SA = AbilityFactory_Protection.createSpellProtectionAll(this);
            else if (isDb)
                SA = AbilityFactory_Protection.createDrawbackProtectionAll(this);
        }
        
        else if(API.equals("MustAttack")) {
            if (isAb) {
                SA = AbilityFactory_Combat.createAbilityMustAttack(this);
            }
            else if (isSp) {
                SA = AbilityFactory_Combat.createSpellMustAttack(this);
            }
            else if (isDb) {
                SA = AbilityFactory_Combat.createDrawbackMustAttack(this);
            }
        }
        
        
        if (SA == null)
            throw new RuntimeException("AbilityFactory : SpellAbility was not created for " + hostCard.getName() + ". Looking for API: " + API);

        // *********************************************
        // set universal properties of the SpellAbility

        SA.setAbilityFactory(this);

        if (hasSubAbility())
            SA.setSubAbility(getSubAbility());

        if (SA instanceof Spell_Permanent)
        	SA.setDescription(SA.getSourceCard().getName());
        else if (hasSpDesc) {
            StringBuilder sb = new StringBuilder();

            if (!isDb) {    // SubAbilities don't have Costs or Cost descriptors
                if (mapParams.containsKey("PrecostDesc"))
                    sb.append(mapParams.get("PrecostDesc")).append(" ");
                if (mapParams.containsKey("CostDesc"))
                    sb.append(mapParams.get("CostDesc")).append(" ");
                else
                    sb.append(abCost.toString());
            }

            sb.append(mapParams.get("SpellDescription"));

            SA.setDescription(sb.toString());
        } else
            SA.setDescription("");

        // StackDescriptions are overwritten by the AF type instead of through this
        //if (!isTargeted)
        //	SA.setStackDescription(hostCard.getName());

        makeRestrictions(SA);
        makeConditions(SA);

        return SA;
    }

    /**
     * <p>makeRestrictions.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private void makeRestrictions(SpellAbility sa) {
        // SpellAbility_Restrictions should be added in here
        SpellAbility_Restriction restrict = sa.getRestrictions();
        if (mapParams.containsKey("Flashback")) {
            sa.setFlashBackAbility(true);
        }
        restrict.setRestrictions(mapParams);
    }

    /**
     * <p>makeConditions.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private void makeConditions(SpellAbility sa) {
        // SpellAbility_Restrictions should be added in here
        SpellAbility_Condition condition = sa.getConditions();
        if (mapParams.containsKey("Flashback")) {
            sa.setFlashBackAbility(true);
        }
        condition.setConditions(mapParams);
    }

    /**
     * <p>checkConditional.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean checkConditional(SpellAbility sa) {
        return sa.getConditions().checkConditions(sa);
    }

    // Easy creation of SubAbilities
    /**
     * <p>getSubAbility.</p>
     *
     * @return a {@link forge.card.spellability.Ability_Sub} object.
     */
    public Ability_Sub getSubAbility() {
        Ability_Sub abSub = null;

        String sSub = getMapParams().get("SubAbility");

        if (sSub.startsWith("SVar=")) {
            sSub = sSub.replace("SVar=", "");
        }

        sSub = getHostCard().getSVar(sSub);

        if (!sSub.equals("")) {
            // Older style Drawback no longer supported
            AbilityFactory afDB = new AbilityFactory();
            abSub = (Ability_Sub) afDB.getAbility(sSub, getHostCard());
        } else {
            System.out.println("SubAbility not found for: " + getHostCard());
        }

        return abSub;
    }

    /**
     * <p>playReusable.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean playReusable(SpellAbility sa) {
        // TODO probably also consider if winter orb or similar are out

    	if (sa.getPayCosts() == null)
    		// This is only true for Drawbacks and triggers
    		return true;
    	
        return (sa.getPayCosts().isReusuableResource() && AllZone.getPhase().is(Constant.Phase.End_Of_Turn)
                && AllZone.getPhase().isNextTurn(AllZone.getComputerPlayer()));
    }

    //returns true if it's better to wait until blockers are declared
    /**
     * <p>waitForBlocking.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean waitForBlocking(SpellAbility sa) {

        return (sa.getSourceCard().isCreature() && sa.getPayCosts().getTap()
                && (AllZone.getPhase().isBefore(Constant.Phase.Combat_Declare_Blockers_InstantAbility)
                || AllZone.getPhase().isNextTurn(AllZone.getHumanPlayer())));
    }

    /**
     * <p>isSorcerySpeed.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean isSorcerySpeed(SpellAbility sa) {
        if (sa.isSpell())
            return sa.getSourceCard().isSorcery();
        else if (sa.isAbility())
            return sa.getRestrictions().getSorcerySpeed();

        return false;
    }

    // Utility functions used by the AFs
    /**
     * <p>calculateAmount.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param amount a {@link java.lang.String} object.
     * @param ability a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int calculateAmount(Card card, String amount, SpellAbility ability) {
        // amount can be anything, not just 'X' as long as sVar exists

        // If Amount is -X, strip the minus sign before looking for an SVar of that kind
        int multiplier = 1;
        if (amount.startsWith("-")) {
            multiplier = -1;
            amount = amount.substring(1);
        }

        if (!card.getSVar(amount).equals("")) {
            String calcX[] = card.getSVar(amount).split("\\$");
            if (calcX.length == 1 || calcX[1].equals("none"))
                return 0;

            if (calcX[0].startsWith("Count"))
                return CardFactoryUtil.xCount(card, calcX[1]) * multiplier;
            
            if (calcX[0].startsWith("Number"))
                return CardFactoryUtil.xCount(card, card.getSVar(amount)) * multiplier;

            else if (ability != null) {
                //Player attribute counting
                if (calcX[0].startsWith("TargetedPlayer")) {
                    ArrayList<Player> players = new ArrayList<Player>();
                    SpellAbility saTargeting = (ability.getTarget() == null) ? findParentsTargetedPlayer(ability) : ability;
                    if (saTargeting.getTarget() != null) {
                        players.addAll(saTargeting.getTarget().getTargetPlayers());
                    } else {
                        players.addAll(getDefinedPlayers(card, saTargeting.getAbilityFactory().getMapParams().get("Defined"), saTargeting));
                    }
                    return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
                }
                if (calcX[0].startsWith("TargetedController")) {
                    ArrayList<Player> players = new ArrayList<Player>();
                    ArrayList<Card> list = getDefinedCards(card, "Targeted", ability);
                    ArrayList<SpellAbility> sas = getDefinedSpellAbilities(card, "Targeted", ability);

                    for (Card c : list) {
                        Player p = c.getController();
                        if (!players.contains(p))
                            players.add(p);
                    }
                    for (SpellAbility s : sas) {
                        Player p = s.getSourceCard().getController();
                        if (!players.contains(p)) players.add(p);
                    }
                    return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
                }

                CardList list = new CardList();
                if (calcX[0].startsWith("Sacrificed"))
                    list = findRootAbility(ability).getPaidList("Sacrificed");

                else if (calcX[0].startsWith("Discarded"))
                    list = findRootAbility(ability).getPaidList("Discarded");

                else if (calcX[0].startsWith("Exiled")) {
                    list = findRootAbility(ability).getPaidList("Exiled");
                } else if (calcX[0].startsWith("Tapped")) {
                    list = findRootAbility(ability).getPaidList("Tapped");
                } else if (calcX[0].startsWith("Targeted")) {
                    Target t = ability.getTarget();
                    if (null != t) {
                        ArrayList<Object> all = t.getTargets();
                        if (!all.isEmpty() && all.get(0) instanceof SpellAbility) {
                            SpellAbility saTargeting = findParentsTargetedSpellAbility(ability);
                            list = new CardList();
                            ArrayList<SpellAbility> sas = saTargeting.getTarget().getTargetSAs();
                            for (SpellAbility sa : sas) {
                                list.add(sa.getSourceCard());
                            }
                        } else {
                            SpellAbility saTargeting = findParentsTargetedCard(ability);
                            list = new CardList(saTargeting.getTarget().getTargetCards().toArray());
                        }
                    } else {
                        SpellAbility parent = findParentsTargetedCard(ability);

                        ArrayList<Object> all = parent.getTarget().getTargets();
                        if (!all.isEmpty() && all.get(0) instanceof SpellAbility) {
                            list = new CardList();
                            ArrayList<SpellAbility> sas = parent.getTarget().getTargetSAs();
                            for (SpellAbility sa : sas) {
                                list.add(sa.getSourceCard());
                            }
                        } else {
                            SpellAbility saTargeting = findParentsTargetedCard(ability);
                            list = new CardList(saTargeting.getTarget().getTargetCards().toArray());
                        }
                    }
                } else if (calcX[0].startsWith("Triggered")) {
                    SpellAbility root = ability.getRootSpellAbility();
                    list = new CardList();
                    list.add((Card) root.getTriggeringObject(calcX[0].substring(9)));
                } else if (calcX[0].startsWith("Remembered")) {
                    // Add whole Remembered list to handlePaid
                    list = new CardList();
                    for (Object o : card.getRemembered()) {
                        if (o instanceof Card)
                            list.add(AllZoneUtil.getCardState((Card) o));
                    }
                } else if (calcX[0].startsWith("Imprinted")) {
                    // Add whole Imprinted list to handlePaid
                    list = new CardList();
                    for (Card c : card.getImprinted())
                        list.add(AllZoneUtil.getCardState(c));
                } else if (calcX[0].startsWith("TriggerCount")) {
                    // TriggerCount is similar to a regular Count, but just pulls Integer Values from Trigger objects
                    String[] l = calcX[1].split("/");
                    String[] m = CardFactoryUtil.parseMath(l);
                    int count = (Integer) ability.getTriggeringObject(l[0]);

                    return CardFactoryUtil.doXMath(count, m, card) * multiplier;
                } else
                    return 0;

                return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
            } else
                return 0;
        }

        return Integer.parseInt(amount) * multiplier;
    }

    // should the three getDefined functions be merged into one? Or better to have separate?
    // If we only have one, each function needs to Cast the Object to the appropriate type when using
    // But then we only need update one function at a time once the casting is everywhere.
    // Probably will move to One function solution sometime in the future
    /**
     * <p>getDefinedCards.</p>
     *
     * @param hostCard a {@link forge.Card} object.
     * @param def a {@link java.lang.String} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Card> getDefinedCards(Card hostCard, String def, SpellAbility sa) {
        ArrayList<Card> cards = new ArrayList<Card>();
        String defined = (def == null) ? "Self" : def;    // default to Self

        Card c = null;

        if (defined.equals("Self"))
            c = hostCard;

        else if (defined.equals("Equipped"))
            c = hostCard.getEquippingCard();

        else if (defined.equals("Enchanted"))
            c = hostCard.getEnchantingCard();

        else if (defined.equals("Targeted")) {
            SpellAbility parent = findParentsTargetedCard(sa);
            cards.addAll(parent.getTarget().getTargetCards());
        } else if (defined.startsWith("Triggered") && sa != null) {
            SpellAbility root = sa.getRootSpellAbility();
            Object crd = root.getTriggeringObject(defined.substring(9));
            if (crd instanceof Card) {
                c = AllZoneUtil.getCardState((Card) crd);
            }
        } else if (defined.equals("Remembered")) {
            for (Object o : hostCard.getRemembered()) {
                if (o instanceof Card)
                    cards.add(AllZoneUtil.getCardState((Card) o));
            }
        } else if (defined.equals("Clones")) {
            for (Card clone : hostCard.getClones()) {
                cards.add(AllZoneUtil.getCardState(clone));
            }
        } else if (defined.equals("Imprinted")) {
            for (Card imprint : hostCard.getImprinted()) {
                cards.add(AllZoneUtil.getCardState(imprint));
            }
        } else {
            CardList list = null;
            if (defined.startsWith("Sacrificed"))
                list = findRootAbility(sa).getPaidList("Sacrificed");

            else if (defined.startsWith("Discarded"))
                list = findRootAbility(sa).getPaidList("Discarded");

            else if (defined.startsWith("Exiled"))
                list = findRootAbility(sa).getPaidList("Exiled");

            else if (defined.startsWith("Tapped"))
                list = findRootAbility(sa).getPaidList("Tapped");

            else
                return cards;

            for (Card cl : list)
                cards.add(cl);
        }

        if (c != null)
            cards.add(c);

        return cards;
    }

    /**
     * <p>getDefinedPlayers.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param def a {@link java.lang.String} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Player> getDefinedPlayers(Card card, String def, SpellAbility sa) {
        ArrayList<Player> players = new ArrayList<Player>();
        String defined = (def == null) ? "You" : def;

        if (defined.equals("Targeted")) {
            Target tgt = sa.getTarget();
            SpellAbility parent = sa;

            do {

                if (!(parent instanceof Ability_Sub)) // did not find any targets
                    return players;
                parent = ((Ability_Sub) parent).getParent();
                tgt = parent.getTarget();
            } while (tgt == null || tgt.getTargetPlayers().size() == 0);

            players.addAll(tgt.getTargetPlayers());
        } else if (defined.equals("TargetedController")) {
            ArrayList<Card> list = getDefinedCards(card, "Targeted", sa);
            ArrayList<SpellAbility> sas = getDefinedSpellAbilities(card, "Targeted", sa);

            for (Card c : list) {
                Player p = c.getController();
                if (!players.contains(p))
                    players.add(p);
            }
            for (SpellAbility s : sas) {
                Player p = s.getSourceCard().getController();
                if (!players.contains(p)) players.add(p);
            }
        } else if (defined.equals("TargetedOwner")) {
            ArrayList<Card> list = getDefinedCards(card, "Targeted", sa);

            for (Card c : list) {
                Player p = c.getOwner();
                if (!players.contains(p))
                    players.add(p);
            }
        } else if (defined.equals("Remembered")) {
            for (Object rem : card.getRemembered()) {
                if (rem instanceof Player)
                    players.add((Player) rem);
            }
        } else if (defined.startsWith("Triggered")) {
            SpellAbility root = sa.getRootSpellAbility();
            Object o = null;
            if (defined.endsWith("Controller")) {
                String triggeringType = defined.substring(9);
                triggeringType = triggeringType.substring(0, triggeringType.length() - 10);
                Object c = root.getTriggeringObject(triggeringType);
                if (c instanceof Card) {
                    o = ((Card) c).getController();
                }
                if (c instanceof SpellAbility) {
                    o = ((SpellAbility) c).getSourceCard().getController();
                }
            } else if (defined.endsWith("Owner")) {
                String triggeringType = defined.substring(9);
                triggeringType = triggeringType.substring(0, triggeringType.length() - 5);
                Object c = root.getTriggeringObject(triggeringType);
                if (c instanceof Card) {
                    o = ((Card) c).getOwner();
                }
            } else {
                String triggeringType = defined.substring(9);
                o = root.getTriggeringObject(triggeringType);
            }
            if (o != null) {
                if (o instanceof Player) {
                    Player p = (Player) o;
                    if (!players.contains(p))
                        players.add(p);
                }
            }
        } else if (defined.equals("EnchantedController")) {
            Player p = card.getEnchantingCard().getController();
            if (!players.contains(p))
                players.add(p);
        } else if (defined.equals("EnchantedOwner")) {
            Player p = card.getEnchantingCard().getOwner();
            if (!players.contains(p))
                players.add(p);
        } else if (defined.equals("AttackingPlayer")) {
            Player p = AllZone.getCombat().getAttackingPlayer();
            if (!players.contains(p))
                players.add(p);
        } else if (defined.equals("DefendingPlayer")) {
            Player p = AllZone.getCombat().getDefendingPlayer();
            if (!players.contains(p))
                players.add(p);
        } else {
            if (defined.equals("You") || defined.equals("Each"))
                players.add(sa.getActivatingPlayer());

            if (defined.equals("Opponent") || defined.equals("Each"))
                players.add(sa.getActivatingPlayer().getOpponent());
        }
        return players;
    }

    /**
     * <p>getDefinedSpellAbilities.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param def a {@link java.lang.String} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<SpellAbility> getDefinedSpellAbilities(Card card, String def, SpellAbility sa) {
        ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
        String defined = (def == null) ? "Self" : def;    // default to Self

        SpellAbility s = null;

        //TODO - this probably needs to be fleshed out a bit, but the basics work
        if (defined.equals("Self")) {
            s = sa;
        } else if (defined.equals("Targeted")) {
            SpellAbility parent = findParentsTargetedSpellAbility(sa);
            sas.addAll(parent.getTarget().getTargetSAs());
        } else if (defined.startsWith("Triggered")) {
        	SpellAbility root = sa.getRootSpellAbility();
        	
            String triggeringType = defined.substring(9);
            Object o = root.getTriggeringObject(triggeringType); 
            if (o instanceof SpellAbility)
                s = (SpellAbility) o;
        } else if (defined.startsWith("Imprinted")) {
            for (Card imp : card.getImprinted())
                sas.addAll(imp.getSpellAbilities());
        }

        if (s != null)
            sas.add(s);

        return sas;
    }

    /**
     * <p>getDefinedObjects.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param def a {@link java.lang.String} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Object> getDefinedObjects(Card card, String def, SpellAbility sa) {
        ArrayList<Object> objects = new ArrayList<Object>();
        String defined = (def == null) ? "Self" : def;

        objects.addAll(getDefinedPlayers(card, defined, sa));
        objects.addAll(getDefinedCards(card, defined, sa));
        objects.addAll(getDefinedSpellAbilities(card, defined, sa));
        return objects;
    }


    /**
     * <p>findRootAbility.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility findRootAbility(SpellAbility sa) {
        SpellAbility parent = sa;
        while (parent instanceof Ability_Sub)
            parent = ((Ability_Sub) parent).getParent();

        return parent;
    }

    /**
     * <p>findParentsTargetedCard.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility findParentsTargetedCard(SpellAbility sa) {
        SpellAbility parent = sa;

        do {
            if (!(parent instanceof Ability_Sub))
                return parent;
            parent = ((Ability_Sub) parent).getParent();
        } while (parent.getTarget() == null || parent.getTarget().getTargetCards().size() == 0);

        return parent;
    }

    /**
     * <p>findParentsTargetedSpellAbility.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    private static SpellAbility findParentsTargetedSpellAbility(SpellAbility sa) {
        SpellAbility parent = sa;

        do {
            if (!(parent instanceof Ability_Sub))
                return parent;
            parent = ((Ability_Sub) parent).getParent();
        } while (parent.getTarget() == null || parent.getTarget().getTargetSAs().size() == 0);

        return parent;
    }

    /**
     * <p>findParentsTargetedPlayer.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility findParentsTargetedPlayer(SpellAbility sa) {
        SpellAbility parent = sa;

        do {
            if (!(parent instanceof Ability_Sub))
                return parent;
            parent = ((Ability_Sub) parent).getParent();
        } while (parent.getTarget() == null || parent.getTarget().getTargetPlayers().size() == 0);

        return parent;
    }

    /**
     * <p>predictThreatenedObjects.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public static ArrayList<Object> predictThreatenedObjects(AbilityFactory saviourAf) {
        ArrayList<Object> objects = new ArrayList<Object>();
        if (AllZone.getStack().size() == 0)
            return objects;

        // check stack for something that will kill this
        SpellAbility topStack = AllZone.getStack().peekAbility();
        objects.addAll(predictThreatenedObjects(saviourAf, topStack));

        return objects;
    }

    /**
     * <p>predictThreatenedObjects.</p>
     *
     * @param topStack a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public static ArrayList<Object> predictThreatenedObjects(AbilityFactory saviourAf, SpellAbility topStack) {
        ArrayList<Object> objects = new ArrayList<Object>();
        ArrayList<Object> threatened = new ArrayList<Object>();
        String saviourApi = "";
        if (saviourAf != null)
        	saviourApi = saviourAf.getAPI();

        if (topStack == null)
            return objects;

        Card source = topStack.getSourceCard();
        AbilityFactory topAf = topStack.getAbilityFactory();

        // Can only Predict things from AFs
        if (topAf != null) {
            Target tgt = topStack.getTarget();

            if (tgt == null) {
                objects = getDefinedObjects(source, topAf.getMapParams().get("Defined"), topStack);
            } else {
                objects = tgt.getTargets();
            }

            // Determine if Defined Objects are "threatened" will be destroyed due to this SA

            String threatApi = topAf.getAPI();
            HashMap<String, String> threatParams = topAf.getMapParams();
            
            //Lethal Damage => prevent damage/regeneration/bounce
            if (threatApi.equals("DealDamage") || threatApi.equals("DamageAll")) {
                // If PredictDamage is >= Lethal Damage
                int dmg = AbilityFactory.calculateAmount(topStack.getSourceCard(), topAf.getMapParams().get("NumDmg"), topStack);
                for (Object o : objects) {
                    if (o instanceof Card) {
                        Card c = (Card) o;
                        
                        // indestructible
                        if (c.hasKeyword("Indestructible"))
                        	continue;
                        
                        //already regenerated
                        if (c.getShield() > 0)
                        	continue;
                        
                        //don't use it on creatures that can't be regenerated
                        if (saviourApi.equals("Regenerate") && !c.canBeShielded())
                        	continue;
                        
                        //don't bounce or blink a permanent that the human player owns or is a token
                        if (saviourApi.equals("ChangeZone") && (c.getOwner().isHuman() || c.isToken()))
                        	continue;
                        
                        if (c.predictDamage(dmg, source, false) >= c.getKillDamage())
                            threatened.add(c);
                    } else if (o instanceof Player) {
                        Player p = (Player) o;

                        if (source.hasKeyword("Infect")) {
                            if (p.predictDamage(dmg, source, false) >= p.getPoisonCounters())
                                threatened.add(p);
                        } else if (p.predictDamage(dmg, source, false) >= p.getLife())
                            threatened.add(p);
                    }
                } 
            } 
            //Destroy => regeneration/bounce
            else if ((threatApi.equals("Destroy") || threatApi.equals("DestroyAll"))
            		&& ((saviourApi.equals("Regenerate") && !threatParams.containsKey("NoRegen"))
            				|| saviourApi.equals("ChangeZone"))){
            	for (Object o : objects)
                    if (o instanceof Card) {
                        Card c = (Card) o;
                        // indestructible
                        if (c.hasKeyword("Indestructible"))
                        	continue;
                        
                        //already regenerated
                        if (c.getShield() > 0)
                        	continue;
                        
                        //don't bounce or blink a permanent that the human player owns or is a token
                        if (saviourApi.equals("ChangeZone") && (c.getOwner().isHuman() || c.isToken()))
                        	continue;
                        
                        //don't use it on creatures that can't be regenerated
                        if (saviourApi.equals("Regenerate") && !c.canBeShielded())
                        	continue;
                        
                        threatened.add(c);
                    }
            	
            }
            //Exiling => bounce
            else if ((threatApi.equals("ChangeZone") || threatApi.equals("ChangeZoneAll"))
            		&& saviourApi.equals("ChangeZone") 
            		&& threatParams.containsKey("Destination") && threatParams.get("Destination").equals("Exile")) {
            	for (Object o : objects)
                    if (o instanceof Card) {
                        Card c = (Card) o;
                        
                        //don't bounce or blink a permanent that the human player owns or is a token
                        if (saviourApi.equals("ChangeZone") && (c.getOwner().isHuman() || c.isToken()))
                        	continue;
                        
                        threatened.add(c);
                    }
            }
        }

        threatened.addAll(predictThreatenedObjects(saviourAf, topStack.getSubAbility()));
        return threatened;
    }

    /**
     * <p>handleRemembering.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     */
    public static void handleRemembering(AbilityFactory AF) {
        HashMap<String, String> params = AF.getMapParams();
        Card host;

        if (!params.containsKey("RememberTargets") && !params.containsKey("Imprint")) {
            return;
        }

        host = AF.getHostCard();

        if (params.containsKey("ForgetOtherTargets")) {
            host.clearRemembered();
        }
        if (params.containsKey("Unimprint")) {
            host.clearImprinted();
        }

        Target tgt = AF.getAbTgt();

        if (params.containsKey("RememberTargets")) {
            ArrayList<Object> tgts = (tgt == null) ? new ArrayList<Object>() : tgt.getTargets();
            for (Object o : tgts) {
                host.addRemembered(o);
            }
        }
        if (params.containsKey("Imprint")) {
            ArrayList<Card> tgts = (tgt == null) ? new ArrayList<Card>() : tgt.getTargetCards();
            host.addImprinted(tgts);
        }
    }
    
    /**
     * <p>filterListByType.</p>
     *
     * @param list a {@link forge.CardList} object.
     * @param params a {@link java.util.HashMap} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList filterListByType(CardList list, String type, SpellAbility sa) {
        if (type == null)
            return list;

        // Filter List Can send a different Source card in for things like Mishra and Lobotomy

        Card source = sa.getSourceCard();
        if (type.contains("Triggered")) {
            Object o = sa.getTriggeringObject("Card");

            // I won't the card attached to the Triggering object
            if (!(o instanceof Card))
                return new CardList();

            source = (Card) (o);
            type = type.replace("Triggered", "Card");
        } else if (type.contains("Remembered")) {
            boolean hasRememberedCard = false;
            for (Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    hasRememberedCard = true;
                    source = (Card) o;
                    type = type.replace("Remembered", "Card");
                    break;
                }
            }

            if (!hasRememberedCard)
                return new CardList();
        }

        return list.getValidCards(type.split(","), sa.getActivatingPlayer(), source);
    }

    /**
     * <p>passUnlessCost.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param usedStack a boolean.
     */
    public static void passUnlessCost(final SpellAbility sa, final boolean usedStack) {
        Card source = sa.getSourceCard();
        AbilityFactory af = sa.getAbilityFactory();
        final HashMap<String, String> params = af.getMapParams();

        //Nothing to do
        if (params.get("UnlessCost") == null) {
            sa.resolve();
            return;
        }

        //The player who has the chance to cancel the ability
        String pays = params.containsKey("UnlessPayer") ? params.get("UnlessPayer") : "TargetedController";
        Player payer = getDefinedPlayers(sa.getSourceCard(), pays, sa).get(0);

        //The cost
        String unlessCost = params.get("UnlessCost").trim();
        if (unlessCost.equals("X"))
            unlessCost = Integer.toString(AbilityFactory.calculateAmount(source, params.get("UnlessCost"), sa));

        Ability ability = new Ability(source, unlessCost) {
            @Override
            public void resolve() {
                ;
            }
        };

        final Command paidCommand = new Command() {
            private static final long serialVersionUID = 8094833091127334678L;

            public void execute() {
                resolveSubAbilities(sa);
                if(usedStack)
                	AllZone.getStack().finishResolving(sa, false);
            }
        };

        final Command unpaidCommand = new Command() {
            private static final long serialVersionUID = 8094833091127334678L;

            public void execute() {
                sa.resolve();
                if (params.containsKey("PowerSink")) GameActionUtil.doPowerSink(AllZone.getHumanPlayer());
                resolveSubAbilities(sa);
                if(usedStack)
                	AllZone.getStack().finishResolving(sa, false);
            }
        };

        if (payer.isHuman()) {
            GameActionUtil.payManaDuringAbilityResolve(source + "\r\n", ability.getManaCost(),
                    paidCommand, unpaidCommand);
        } else {
            if (ComputerUtil.canPayCost(ability)) {
                ComputerUtil.playNoStack(ability); //Unless cost was payed - no resolve
                resolveSubAbilities(sa);
                if(usedStack)
                	AllZone.getStack().finishResolving(sa, false);
            } else {
                sa.resolve();
                if (params.containsKey("PowerSink")) GameActionUtil.doPowerSink(AllZone.getComputerPlayer());
                resolveSubAbilities(sa);
                if(usedStack)
                	AllZone.getStack().finishResolving(sa, false);
            }
        }
    }

    /**
     * <p>resolve.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param usedStack a boolean.
     */
    public static void resolve(SpellAbility sa, boolean usedStack) {
        if (sa == null) return;
        AbilityFactory af = sa.getAbilityFactory();
        if(af == null) {
        	sa.resolve();
        	return;
        }
        HashMap<String, String> params = af.getMapParams();


        //check conditions
        if (AbilityFactory.checkConditional(sa)) {
            if (params.get("UnlessCost") == null) {
                sa.resolve();

                //try to resolve subabilities (see null check above)
                resolveSubAbilities(sa);
                if(usedStack)
                	AllZone.getStack().finishResolving(sa, false);
            } else passUnlessCost(sa, usedStack);
        } else
            resolveSubAbilities(sa);
    }

    /**
     * <p>resolveSubAbilities.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static void resolveSubAbilities(SpellAbility sa) {
        Ability_Sub abSub = sa.getSubAbility();
        if (abSub == null || sa.isWrapper()) return;
        //check conditions
        if (AbilityFactory.checkConditional(abSub)) {
            abSub.resolve();
        }
        resolveSubAbilities(abSub);
    }

}//end class AbilityFactory
