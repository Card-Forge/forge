
package forge;


public class PlayerUtil {
	public static boolean worshipFlag(Player player) {
    	if( AllZoneUtil.isCardInPlay("Ali from Cairo", player)
    			|| (AllZoneUtil.isCardInPlay("Worship", player) && AllZoneUtil.getCreaturesInPlay(player).size() > 0)
    			|| AllZoneUtil.isCardInPlay("Fortune Thief", player)
    			|| AllZoneUtil.isCardInPlay("Sustaining Spirit", player)) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
}