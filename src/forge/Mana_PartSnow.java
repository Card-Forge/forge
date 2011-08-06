package forge;
public class Mana_PartSnow extends Mana_Part {

	private boolean isPaid = false;
	@Override
	public boolean isNeeded(String mana) {
		return !isPaid && mana.equals("S");
	}

	@Override
	public boolean isPaid() {
		return isPaid;
	}

	@Override
	public void reduce(String mana) {
		if (!mana.equals("S"))
			throw new RuntimeException("Mana_PartSnow: reduce() error, "
				+ mana + " is not snow mana");
		isPaid = true;
	}

	@Override
	public String toString() {
		return (isPaid ? "" : "S");
	}

}
