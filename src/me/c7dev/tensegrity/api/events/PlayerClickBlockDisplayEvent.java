package me.c7dev.tensegrity.api.events;

import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public class PlayerClickBlockDisplayEvent extends Event {
	
	private Player p;
	private BlockDisplay block;
	private DexterityDisplay d;
	public PlayerClickBlockDisplayEvent(Player p_, BlockDisplay block_, DexterityDisplay d_) {
		p = p_;
		block = block_;
		d = d_;
	}
	
	public Player getPlayer() {
		return p;
	}
	public BlockDisplay getBlockDisplay() {
		return block;
	}
	public DexterityDisplay getDisplay() {
		return d;
	}
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	static public HandlerList getHandlerList() {
		return handlers;
	}

}
