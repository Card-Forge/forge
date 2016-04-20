package forge.game.trigger;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;

import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TriggerPlaneswalkedFrom extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_PlaneswalkedTo.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerPlaneswalkedFrom(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#performTest(java.util.Map)
     */
    @Override
    public boolean performTest(final Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidCard")) {
            final CardCollection moved = (CardCollection) runParams2.get("Cards");
            for(Card c : moved) {
                if (c.isValid(this.mapParams.get("ValidCard").split(","), this
                        .getHostCard().getController(), this.getHostCard(), null)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#setTriggeringObjects(forge.card.spellability.SpellAbility)
     */
    @Override
    public void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Cards", this.getRunParams().get("Cards"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Planeswalked From: ").append(sa.getTriggeringObject("Cards"));
        return sb.toString();
    }

}
