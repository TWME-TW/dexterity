package me.c7dev.dexterity.displays.animation;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.transaction.RotationTransaction;
import me.c7dev.dexterity.util.QueuedRotation;
import me.c7dev.dexterity.util.RotationPlan;

public class RotationAnimation extends Animation {
	
	private RotationTransaction t;
	
	public RotationAnimation(DexterityDisplay display, int ticks, RotationPlan rotation) {
		super(display, ticks);
		RotationPlan r = rotation.clone();
		r.x_deg /= ticks; r.y_deg /= ticks; r.z_deg /= ticks;
		r.yaw_deg /= ticks; r.pitch_deg /= ticks; r.roll_deg /= ticks;
		r.async = true;
				
		QueuedRotation rot = display.getRotationManager(true).prepareRotation(r, null);
		
		super.setRunnable(new BukkitRunnable() {
			@Override
			public void run() {
				if (isPaused()) return;
				display.getRotationManager().rotate(rot);
				tick();
			}
		});
	}
	
	@Override
	public void beforeStart() {
		t = new RotationTransaction(super.getDisplay());
	}
	
	@Override
	public void reset() {
		if (super.isStarted()) super.setPaused(true);
		Location center = super.getDisplay().getCenter();
		t.commit();
		t.undo();
		super.getDisplay().teleport(center);
	}

}
