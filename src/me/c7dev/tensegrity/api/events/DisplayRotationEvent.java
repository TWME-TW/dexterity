package me.c7dev.tensegrity.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.joml.Quaterniond;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class DisplayRotationEvent extends Event implements Cancellable {
	
	private boolean cancelled = false;
	private DexterityDisplay d;
	private Quaterniond q;
	
	public DisplayRotationEvent(DexterityDisplay display, Quaterniond rotation) {
		d = display;
		q = rotation;
	}
	
	public DexterityDisplay getDisplay() {
		return d;
	}
	
	public Quaterniond getRotation() {
		return q;
	}
	
	public double getRotatedByDeg() {
		return Math.toDegrees(Math.acos(q.w)*2);
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	public void setCancelled(boolean b) {
		cancelled = b;
	}
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	static public HandlerList getHandlerList() {
		return handlers;
	}

}
