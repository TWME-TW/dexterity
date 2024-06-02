package me.c7dev.tensegrity.displays.animation;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.DexterityDisplay;

public class Animation {
	
	private boolean paused = true, stop_req = false;
	private BukkitRunnable runnable, start_delay;
	private DexterityDisplay display;
	private List<Animation> subseq = new ArrayList<>();
	private Dexterity plugin;
	private int ticks, delay = 0, tick_count = 0;
		
	public Animation(DexterityDisplay display, Dexterity plugin, int ticks) {
		this.display = display;
		this.plugin = plugin;
		if (ticks < 1) ticks = 1;
		this.ticks = ticks;
	}
	
	public boolean tick() {
		tick_count++;
		if (tick_count >= ticks) finish();
		return tick_count >= ticks; 
	}
	
	public int getDuration() {
		return ticks;
	}
	
	public int getStartDelay() {
		return delay;
	}
	
	public void setStartDelay(int s) {
		delay = s;
	}
	
	public boolean isStarted() {
		return !paused;
	}
	
	public boolean isPaused() {
		return paused;
	}
	public void setPaused(boolean b) {
		if (paused == b) return;
		paused = b;
		//if (b && !stopped) start();
		if (b) start();
	}
	
	public void setSubsequentAnimations(List<Animation> a) {
		subseq = a;
	}
	
	public List<Animation> getSubsequentAnimations() {
		return subseq;
	}
	
	public void setRunnable(BukkitRunnable r) {
		runnable = r;
		runnable.runTaskTimer(plugin, 0, 1l);
	}
	
	public void start() {
		if (isStarted() || stop_req) return;
		start_delay = new BukkitRunnable() {
			@Override
			public void run() {
				delay = 0;
				paused = false;
			}
		};
		start_delay.runTaskLater(plugin, (long) delay);
	}
	
	public void stop() {
		if (stop_req) return;
		stop_req = true;
		if (start_delay != null && !start_delay.isCancelled()) start_delay.cancel();
		start_delay = null;
		for (Animation a : subseq) a.stop();
	}
	
	public void kill() {
		kill(true);
	}
	
	public void kill(boolean stop_subseq) {
		if (paused && !stop_subseq) return;
		paused = true;
		tick_count = 0;
		if (start_delay != null && !start_delay.isCancelled()) start_delay.cancel();
		start_delay = null;
		if (stop_subseq) for (Animation a : subseq) 
			if (a != this) a.kill();
	}
	
	public void finish() {
		if (!subseq.contains(this) || delay > 0) kill(false);
		
		if (!stop_req) {
			for (Animation a : subseq) {
				a.start();
			}
		}
	}
	
	public DexterityDisplay getDisplay() {
		return display;
	}

}
