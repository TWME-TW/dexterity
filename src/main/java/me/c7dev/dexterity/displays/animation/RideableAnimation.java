package me.c7dev.dexterity.displays.animation;

import org.bukkit.entity.Player;

public interface RideableAnimation {
	
	public Player getMountedPlayer();
	
	public boolean mount(Player p);
	
	public void dismount();

}
