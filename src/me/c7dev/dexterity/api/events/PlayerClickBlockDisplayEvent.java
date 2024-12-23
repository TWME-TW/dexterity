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
	
	/**
	 * Gets the player who clicked the block display
	 * @return
	 */
	public Player getPlayer() {
		return player_;
	}
	
	/**
	 * Gets the living block display entity
	 * @return
	 */
	public BlockDisplay getBlockDisplay() {
		return clicked_.getBlockDisplay();
	}
	
	/**
	 * Gets the cardinal direction of the face that was clicked
	 * @return
	 */
	public BlockFace getBlockFace() {
		return clicked_.getBlockFace();
	}
	
	/**
	 * Gets the location that exists on the surface of the block that is the precise point that the player is looking at
	 * @return
	 */
	public Location getPreciseClickLocation() {
		return clicked_.getClickLocation();
	}
	
	/**
	 * Retrieves Action determining if player left or right clicked
	 * @return
	 */
	public Action getAction() {
		return action_;
	}
	
	/**
	 * Returns the DexterityDisplay if the clicked block display is part of one
	 * @return
	 */
	public DexterityDisplay getClickedDisplay() {
		return display_;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
	
	/**
	 * Returns a Vector that is perpendicular to the surface of the face of the block
	 * @return
	 */
	public Vector getSurfaceNormal() {
		return clicked_.getNormal();
	}
	
	/**
	 * Returns the number of blocks of distance from the surface of the block to the player's eye
	 * @return
	 */
	public double getPreciseDistanceFromEye() {
		return clicked_.getDistance();
	}
	
	/**
	 * Returns a vector relative to the face of the block display with the offset from the click location to the block display face center's location
	 * @return
	 */
	public Vector getOffsetFromFaceCenter() {
		return clicked_.getOffsetFromFaceCenter();
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
