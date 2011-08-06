
package forge;


enum ZoneNames {

}

public enum ZCTrigger {
    ENTERFIELD("comes into play", "any > field"), //explanation: zone before last trigger check ">" zone card currently in 
    LEAVEFIELD("leaves play", "field > any"),
    DESTROY("is put into a graveyard from play", "field > grave"),
    ENTERGRAVE("is put into a graveyard from anywhere", "any > grave");
    public String   ruleText;
    public String[] triggerZones;
    
    ZCTrigger(String text, String tofrom) {
        this.ruleText = text;
        this.triggerZones = tofrom.split(" > ");
    }
    
    public boolean triggerOn(String sourceZone, String destintationZone) {
        return ((triggerZones[0].equals("any") || triggerZones[0].equals(sourceZone)) && (triggerZones[1].equals("any") || triggerZones[0].equals(sourceZone)));
    }
    
    public static ZCTrigger getTrigger(String description) {
        for(ZCTrigger t:ZCTrigger.values())
            if(t.ruleText.equals(description)) return t;
        return null;
    }
    
    @Override
    public String toString() {
        return ruleText;
    }
}
