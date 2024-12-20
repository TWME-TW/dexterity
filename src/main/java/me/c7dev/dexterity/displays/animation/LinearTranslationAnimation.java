package me.c7dev.dexterity.displays.animation;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.c7dev.dexterity.displays.DexterityDisplay;

public class LinearTranslationAnimation extends Animation {
	
	private Location start_loc;
	private Location end_loc;
			
	public LinearTranslationAnimation(DexterityDisplay display, int ticks, Location end_loc) {
		super(display, ticks);
		
		start_loc = display.getCenter();
		this.end_loc = end_loc;
		Vector displacement = end_loc.toVector().subtract(start_loc.toVector());
				
		super.setRunnable(new BukkitRunnable() {
			Vector dtick = displacement.clone().multiply(1.0/ticks);
						
			@Override
			public void run() {
				if (isPaused()) return;
				display.teleport(dtick);
				tick();
			}
		});
	}
	
	public Location getStartLocation() {
		return start_loc;
	}
	
	public Location getEndLocation() {
		return end_loc;
	}
	
	@Override
	public void reset() {
		if (super.isStarted()) super.setPaused(true);
		getDisplay().teleport(start_loc);
	}
}
