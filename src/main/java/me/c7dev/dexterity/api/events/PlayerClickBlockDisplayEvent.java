package me.c7dev.dexterity.api.events;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.ClickedBlockDisplay;

public class PlayerClickBlockDisplayEvent extends Event implements Cancellable {
	
	private Player player_;
	private ClickedBlockDisplay clicked_;
	private Action action_;
	private boolean cancelled = false;
	private DexterityDisplay display_;
	
	/**
	 * Event called when a player has clicked a block display entity
	 */
	public PlayerClickBlockDisplayEvent(Player player, ClickedBlockDisplay clicked, Action action, DexterityDisplay display) {
		player_ = player;
		clicked_ = clicked;
		action_ = action;
		display_ = display;
	}
	
	public Player getPlayer() {
		return player_;
	}
	
	public BlockDisplay getBlockDisplay() {
		return clicked_.getBlockDisplay();
	}
	
	public BlockFace getBlockFace() {
		return clicked_.getBlockFace();
	}
	
	public Location getPreciseClickLocation() {
		return clicked_.getClickLocation();
	}
	
	public Action getAction() {
		return action_;
	}
	
	public DexterityDisplay getClickedDisplay() {
		return display_;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	public Vector getSurfaceNormal() {
		return clicked_.getNormal();
	}
	
	public double getPreciseDistanceFromEye() {
		return clicked_.getDistance();
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
