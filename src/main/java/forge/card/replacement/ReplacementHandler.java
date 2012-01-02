package forge.card.replacement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import forge.AllZone;
import forge.Card;
import forge.ComputerUtil;
import forge.GameActionUtil;
import forge.Player;
import forge.Constant.Zone;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplacementHandler {
    
    private List<ReplacementEffect> tmpEffects = new ArrayList<ReplacementEffect>();

    /**
     * 
     * Runs any applicable replacement effects.
     * @param runParams the run params,same as for triggers.
     * @return true if the event was replaced.
     */
    public boolean run(final HashMap<String,Object> runParams) {
        Object affected = runParams.get("Affected");
        List<ReplacementEffect> possibleReplacers = new ArrayList<ReplacementEffect>();
        Player decider = null;
        
        //Figure out who decides which of multiple replacements to apply
        //as well as wether or not to apply optional replacements.
        if(affected instanceof Player) {
            decider = (Player)affected;
        } else {
            decider = ((Card)affected).getController();
        }
        
        //Round up Non-static replacement effects ("Until EOT," or "The next time you would..." etc) 
        for(ReplacementEffect RE : tmpEffects) {
            if(!RE.hasRun() && RE.canReplace(runParams)) {
                possibleReplacers.add(RE);
            }
        }
        
        //Round up Static replacement effects
        for(Player p : AllZone.getPlayersInGame()) {
            for(Card crd : p.getCardsIn(Zone.Battlefield)) {
                for(ReplacementEffect RE : crd.getReplacementEffects()) {
                    if(RE.requirementsCheck()) {
                        if(!RE.hasRun() && RE.canReplace(runParams)) {
                            possibleReplacers.add(RE);
                        }
                    }
                }
            }
        }
        
        if(possibleReplacers.isEmpty()) {
            return false;
        }
        
        ReplacementEffect chosenRE = null;
        
        if(possibleReplacers.size() == 1) {
            chosenRE = possibleReplacers.get(0);
        }
        
        if(possibleReplacers.size() > 1) {
            if(decider.isHuman()) {
                chosenRE = (ReplacementEffect) GuiUtils.getChoice(
                        "Choose which replacement effect to apply.", possibleReplacers.toArray());
            } else {
                //AI logic for choosing which replacement effect to apply happens here.
                chosenRE = possibleReplacers.get(0);
            }
        }
        
        if(chosenRE != null) {
            if(chosenRE.getMapParams().containsKey("Optional")) {
                if(decider.isHuman()) {
                    StringBuilder buildQuestion = new StringBuilder("Apply replacement effect of ");
                    buildQuestion.append(chosenRE.getHostCard());
                    buildQuestion.append("?\r\n(");
                    buildQuestion.append(chosenRE.toString());
                    buildQuestion.append(")");
                    if(!GameActionUtil.showYesNoDialog(chosenRE.getHostCard(), buildQuestion.toString())) {
                        return false;
                    }
                } else {
                    //AI-logic for deciding wether or not to apply the optional replacement effect happens here.
                }
            }
            executeReplacement(runParams,chosenRE);
            return true;
        } else {
            return false;
        }
        
    }
    
    /**
     * 
     * Runs a single replacement effect.
     * @param RE the replacment effect to run
     */
    private void executeReplacement(HashMap<String,Object> runParams, ReplacementEffect RE) {

        HashMap<String,String> mapParams = RE.getMapParams();
        RE.setHasRun(true);
        
        if(mapParams.containsKey("Prevent")) {
            if(mapParams.get("Prevent").equals("True")) {
                RE.setHasRun(false);
                return; //Nothing should replace the event.
            }
        }

        String effectSVar = mapParams.get("ReplaceWith");
        String effectAbString = RE.getHostCard().getSVar(effectSVar);
        
        AbilityFactory AF = new AbilityFactory();
        
        SpellAbility effectSA = AF.getAbility(effectAbString, RE.getHostCard());
        
        RE.setReplacingObjects(runParams, effectSA);
        
        if(RE.getHostCard().getController().isHuman()) {
            AllZone.getGameAction().playSpellAbilityNoStack(effectSA, false);
        }
        else {
            ComputerUtil.playNoStack(effectSA);
        }
        
        RE.setHasRun(false);
    }
    
    /**
     * 
     * Creates an instance of the proper replacement effect object based on raw script.
     * @param repParse A raw line of script
     * @param host The cards that hosts the replacement effect.
     * @return A finished instance
     */
    public static ReplacementEffect parseReplacement(final String repParse, final Card host) {
        final HashMap<String, String> mapParams = ReplacementHandler.parseParams(repParse);
        return ReplacementHandler.parseReplacement(mapParams, host);
    }
    
    /**
     * 
     * Creates an instance of the proper replacement effect object based on a parsed script.
     * @param mapParams The parsed script
     * @param host The card that hosts the replacement effect
     * @return The finished instance
     */
    public static ReplacementEffect parseReplacement(final HashMap<String,String> mapParams, final Card host) {
        ReplacementEffect ret = null;
        
        final String eventToReplace = mapParams.get("Event");
        if(eventToReplace.equals("Draw")) {
            ret = new ReplaceDraw(mapParams,host);
        } else if(eventToReplace.equals("GainLife")) {
            ret = new ReplaceGainLife(mapParams,host);
        } else if(eventToReplace.equals("DamageDone")) {
            ret = new ReplaceDamage(mapParams,host);
        }
        
        return ret;
    }
    
    /**
     * <p>
     * parseParams.
     * </p>
     * 
     * @param repParse
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.HashMap} object.
     */
    private static HashMap<String, String> parseParams(final String repParse) {
        final HashMap<String, String> mapParams = new HashMap<String, String>();

        if (repParse.length() == 0) {
            throw new RuntimeException("ReplacementFactory : registerTrigger -- trigParse too short");
        }

        final String[] params = repParse.split("\\|");

        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
        }

        for (final String param : params) {
            final String[] splitParam = param.split("\\$");
            for (int i = 0; i < splitParam.length; i++) {
                splitParam[i] = splitParam[i].trim();
            }

            if (splitParam.length != 2) {
                final StringBuilder sb = new StringBuilder();
                sb.append("ReplacementFactory Parsing Error in registerTrigger() : Split length of ");
                sb.append(param).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParams.put(splitParam[0], splitParam[1]);
        }

        return mapParams;
    }
}
