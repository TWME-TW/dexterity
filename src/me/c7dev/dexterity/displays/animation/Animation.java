package me.c7dev.dexterity.displays.animation;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.scheduler.BukkitRunnable;

import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.displays.DexterityDisplay;

public class Animation {
	
	private boolean paused = true, stop_req = false;
	private BukkitRunnable runnable, start_delay;
	private DexterityDisplay display;
	private List<Animation> subseq = new ArrayList<>();
	private Dexterity plugin;
	private int ticks = 0, delay = 0, tick_count = 0;
	private int freq = 1;
		
	public Animation(DexterityDisplay display, int ticks) {
		this.display = display;
		this.plugin = display.getPlugin();
		if (ticks < 1) ticks = 1;
		this.ticks = ticks;
//		if (!display.getAnimations().contains(this)) display.getAnimations().add(this);
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
		
		try {
			if (b) beforePause();
			else beforeUnpause();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
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
	
	public void setFrameRate(int l) {
		freq = l;
	}
	
	public int getFrameRate() {
		return freq;
	}
	
	public void setRunnable(BukkitRunnable r) {
		runnable = r;
		if (r == null) return;
		runnable.runTaskTimer(plugin, 0, freq);
	}
	
	public void start() {
		if (isStarted() || stop_req || runnable == null) return;
		start_delay = new BukkitRunnable() {
			@Override
			public void run() {
				delay = 0;
				try {
					beforeStart();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				paused = false;
			}
		};
		start_delay.runTaskLater(plugin, (long) delay);
	}
	
	public void stop() {
		if (stop_req) return;
		
		try {
			beforeStop();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
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
		try {
			beforeFinish();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
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
	
	
	//for api to override
	
	public void beforeStart() {
		
	}
	
	public void beforePause() {
		
	}
	
	public void beforeUnpause() {
		
	}
	
	public void beforeStop() {
		
	}
	
	public void beforeFinish() {
		
	}
	
	public void reset() {
		
	}

}
