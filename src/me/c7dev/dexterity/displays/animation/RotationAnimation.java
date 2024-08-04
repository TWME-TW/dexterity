package me.c7dev.dexterity.displays.animation;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.transaction.RotationTransaction;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.QueuedRotation;
import me.c7dev.dexterity.util.RotationPlan;

public class RotationAnimation extends Animation {
	
	private RotationTransaction t;
	private boolean no_interpolation = false;
	
	public RotationAnimation(DexterityDisplay display, int ticks, RotationPlan rotation) {
		super(display, ticks);
		RotationPlan r = rotation.clone();
		r.x_deg /= ticks; r.y_deg /= ticks; r.z_deg /= ticks;
		r.yaw_deg /= ticks; r.pitch_deg /= ticks; r.roll_deg /= ticks;
		r.async = false;
				
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
	
	public void setBlockInterpolation(boolean b) {
		no_interpolation = !b;
	}
	
	@Override
	public void beforeStart() {
		t = new RotationTransaction(super.getDisplay());
		if (no_interpolation) {
			for (DexBlock db : super.getDisplay().getBlocks()) {
				db.getEntity().setTeleportDuration(0);
			}
		}
	}
	
	@Override
	public void beforeFinish() {
		if (no_interpolation) {
			for (DexBlock db : super.getDisplay().getBlocks()) {
				db.getEntity().setTeleportDuration(DexBlock.TELEPORT_DURATION);
			}
		}
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
