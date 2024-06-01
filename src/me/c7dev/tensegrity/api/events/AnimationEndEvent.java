package me.c7dev.tensegrity.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.displays.animation.Animation;

public class AnimationEndEvent {

	private Player p;
	private Animation a;
	public AnimationEndEvent(Player p_, Animation a_) {
		p = p_;
		a = a_;
	}
	
	public Player getPlayer() {
		return p;
	}
	public Animation getAnimation() {
		return a;
	}
	public DexterityDisplay getDisplay() {
		return a.getDisplay();
	}
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	static public HandlerList getHandlerList() {
		return handlers;
	}
	
}
