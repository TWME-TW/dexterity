package me.c7dev.tensegrity.transaction;

import org.bukkit.Color;
import org.bukkit.Location;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class RecenterTransaction implements Transaction {
	
	private Location old_loc, new_loc = null;
	private DexterityDisplay disp;
	private boolean isUndone = false, isCommitted = false;
	
	public RecenterTransaction(DexterityDisplay d) {
		disp = d;
		old_loc = d.getCenter();
	}
	
	public void commit(Location loc) {
		if (isCommitted || loc == null) return;
		new_loc = loc.clone();
	}
	
	public DexterityDisplay undo() {
		disp.setCenter(old_loc);
		disp.getPlugin().getAPI().markerPoint(old_loc, Color.AQUA, 4);
		return null;
	}
	
	public void redo() {
		disp.setCenter(new_loc);
		disp.getPlugin().getAPI().markerPoint(new_loc, Color.AQUA, 4);
	}
	
	public boolean isPossible() {
		return true;
	}

	public boolean isUndone() {
		return isUndone;
	}
	
	public boolean isCommitted() {
		return isCommitted;
	}
}
