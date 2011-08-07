package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

public class Trigger_DamageDone extends Trigger {

	public Trigger_DamageDone(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		Card src = (Card)runParams.get("DamageSource");
		Object tgt = runParams.get("DamageTarget");
		
		if(mapParams.containsKey("ValidSource"))
		{
			if(!src.isValidCard(mapParams.get("ValidSource").split(" "), hostCard.getController(), hostCard))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("ValidTarget"))
		{
			if(!matchesValid(tgt,mapParams.get("ValidTarget").split(","),hostCard))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("CombatDamage"))
		{
			if(mapParams.get("CombatDamage").equals("True"))
			{
				if(!((Boolean)runParams.get("IsCombatDamage")))
                    return false;
			}
			else if(mapParams.get("CombatDamage").equals("False"))
			{
				if(((Boolean)runParams.get("IsCombatDamage")))
                    return false;
			}
		}

        if(mapParams.containsKey("DamageAmount"))
        {
            String fullParam = mapParams.get("DamageAmount");

            String operator = fullParam.substring(0,2);
            int operand = Integer.parseInt(fullParam.substring(2));
            int actualAmount = (Integer)runParams.get("DamageAmount");

            if(operator.equals("LT"))
            {
                if(!(actualAmount < operand))
                    return false;
            }
            else if (operator.equals("LE"))
            {
                if(!(actualAmount <= operand))
                    return false;
            }
            else if (operator.equals("EQ"))
            {
                if(!(actualAmount == operand))
                    return false;
            }
            else if (operator.equals("GE"))
            {
                if(!(actualAmount >= operand))
                    return false;
            }
            else if (operator.equals("GT"))
            {
                if(!(actualAmount > operand))
                    return false;
            }

            System.out.print("DamageDone Amount Operator: ");
            System.out.println(operator);
            System.out.print("DamageDone Amount Operand: ");
            System.out.println(operand);
        }
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_DamageDone(mapParams,hostCard);
		if(overridingAbility != null)
		{
			copy.setOverridingAbility(overridingAbility);
		}
		copy.setName(name);
        copy.setID(ID);
		
		return copy;
	}
	
	@Override
	public void setTriggeringObjects(SpellAbility sa)
	{
		sa.setTriggeringObject("Source",runParams.get("DamageSource"));
        sa.setTriggeringObject("Target",runParams.get("DamageTarget"));
        sa.setTriggeringObject("DamageAmount",runParams.get("DamageAmount"));
	}
}
