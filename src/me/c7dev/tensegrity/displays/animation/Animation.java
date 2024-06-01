package me.c7dev.tensegrity.displays.animation;

import java.util.List;

import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3f;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class Animation {
	
	private boolean paused = false, stopped = true;
	private BukkitRunnable runnable;
	private DexterityDisplay display;
	private List<Animation> subseq;
		
	public Animation(DexterityDisplay display, double seconds) {
		this.display = display;
	}
	
	public boolean isStarted() {
		return !paused && !stopped;
	}
	
	public boolean isPaused() {
		return paused;
	}
	public void setPaused(boolean b) {
		if (paused == b) return;
		paused = b;
		if (b && !stopped) start();
	}
	
	public void setSubsequentAnimations(List<Animation> a) {
		subseq = a;
	}
	
	public List<Animation> getSubsequentAnimations() {
		return subseq;
	}
	
	public void start() {
		stopped = false;
		if (paused) setPaused(false);
		else {
			
		}
	}
	
	public void stop() {
		stopped = true;
		paused = false;
		runnable.cancel();
		for (Animation a : subseq) a.stop();
	}
	
	public DexterityDisplay getDisplay() {
		return display;
	}

}
