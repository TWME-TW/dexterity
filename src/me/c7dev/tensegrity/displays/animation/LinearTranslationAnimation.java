package me.c7dev.tensegrity.displays.animation;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.DexterityDisplay;

public class LinearTranslationAnimation extends Animation {
	
	private Location start_loc;
		
	public LinearTranslationAnimation(DexterityDisplay display, Dexterity plugin, int ticks, Vector displacement) {
		super(display, plugin, ticks);
		
		start_loc = display.getCenter();
				
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
	
	@Override
	public void reset() {
		if (super.isStarted()) super.setPaused(true);
		getDisplay().teleport(start_loc);
	}
}
