package forge.card.replacement;

import java.util.HashMap;

import forge.Card;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceDraw extends ReplacementEffect {

    public ReplaceDraw(final HashMap<String, String> params, final Card host) {
        super(params, host);
    }
    
    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(HashMap<String, Object> runParams) {
        if(!runParams.get("Event").equals("Draw")) {
            return false;
        }
        if(this.getMapParams().containsKey("ValidPlayer")) {
            if(!matchesValid(runParams.get("Affected"),this.getMapParams().get("ValidPlayer").split(","),this.getHostCard())) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public ReplacementEffect getCopy() {
        return new ReplaceDraw(this.getMapParams(),hostCard);
    }

}
