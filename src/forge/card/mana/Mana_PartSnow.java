package forge.card.mana;
public class Mana_PartSnow extends Mana_Part {

	private boolean isPaid = false;
	@Override
	public boolean isNeeded(String mana) {
		return !isPaid && mana.equals("S");
	}
	
	public boolean isNeeded(Mana mana) {
		return !isPaid && mana.isSnow();
	}

    @Override
    public boolean isColor(String mana) {
        //ManaPart method
        return mana.indexOf("S") != -1;
    }
    
    @Override
	public boolean isColor(Mana mana) {
    	return mana.isSnow();
	}
	
	@Override
	public boolean isPaid() {
		return isPaid;
	}
	
    @Override
    public boolean isEasierToPay(Mana_Part mp)
    {
    	if (mp instanceof Mana_PartColorless) return false;
    	return toString().length() >= mp.toString().length();
    }

	@Override
	public void reduce(String mana) {
		if (!mana.equals("S"))
			throw new RuntimeException("Mana_PartSnow: reduce() error, "
				+ mana + " is not snow mana");
		isPaid = true;
	}
	
	@Override
	public void reduce(Mana mana) {
		if (!mana.isSnow())
			throw new RuntimeException("Mana_PartSnow: reduce() error, "
				+ mana + " is not snow mana");
		isPaid = true;
	}

	@Override
	public String toString() {
		return (isPaid ? "" : "S");
	}

	@Override
	public int getConvertedManaCost() {
		return 1;
	}

}
