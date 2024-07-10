package me.c7dev.tensegrity.displays.animation;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexBlock;

public class RotationAnimation extends Animation {
	
	private double[] cosList = null, sinList = null;
	private double delta = 0, delta_sin = 1;
	private int n_steps = 0;
	private boolean started_instance = false, paused = false, clockwise = true;
	private BukkitRunnable runnable;
	
	public RotationAnimation(DexterityDisplay display, Dexterity plugin, int ticks) {
		super(display, plugin, ticks);
		display.getAnimations().add(this);
		
		setPeriod(3.2);
	}
	
	public void setPeriod(double seconds_per_revolution) {
		if (seconds_per_revolution <= 0) return;
		int n_steps = (int) Math.floor(20.0*seconds_per_revolution);
		setTicksPerRevolution(n_steps);
	}
	
	public void setTicksPerRevolution(int n_ticks) {
		
		this.n_steps = n_ticks;
		delta = 2 * Math.PI / n_steps; //radians per step / 2
		
		cosList = new double[n_steps];
		sinList = new double[n_steps];
		for (int i = 0; i < n_steps; i++) {
			cosList[i] = Math.cos(i*delta);
			sinList[i] = Math.sin(i*delta);
		}
		delta_sin = Math.sin(-delta);
		
		Vector anglev = new Vector(1, 0, 0); //TODO
//		if (plane == Plane.XZ) anglev = new Vector(1, 0, 0);
//		else anglev = new Vector(0, 1, 0);
		
		for (DexBlock block : getDisplay().getBlocks()) {
			Vector displacement = block.getLocation().toVector().subtract(getDisplay().getCenter().toVector());
			double angle = displacement.angle(anglev);
			
			int step = (int) (n_steps*angle/(2*Math.PI));
			if (block.getLocation().getBlockZ() < getDisplay().getCenter().getBlockZ()) step = n_steps - step;
			
//			block.setAngleStep(step);
		}
	}
	
	public void recalculateAngleSteps() {
		setTicksPerRevolution(n_steps);
	}

	public boolean isStarted() {
		return started_instance;
	}
	
	public boolean isPaused() {
		return paused;
	}
	public void setPaused(boolean b) {
		if (paused == b) return;
		paused = b;
		if (b) {
			runnable.cancel();
			started_instance = false;
		} else start();
	}
	public boolean isClockwise() {
		return clockwise;
	}
	public void setClockwise(boolean b) {
		clockwise = b;
	}

}
