package me.c7dev.tensegrity.displays.animation;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class Animation {
	
	boolean started_instance = false, paused = false;
	DexterityDisplay display;
		
	public Animation(DexterityDisplay display) {
		this.display = display;
	}
	
	public void setPeriod(double seconds) {
		
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
		if (b) start();
		else started_instance = false;
	}
	
	public void start() {
		
	}
	
	public DexterityDisplay getDisplay() {
		return display;
	}
	
	public void remove() {
		paused = true;
		display.getAnimations().remove(this);
	}

}
