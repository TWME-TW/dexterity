package me.c7dev.tensegrity;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexUtils;
import net.md_5.bungee.api.ChatColor;

public class DexSession {
	
	private Player p;
	private List<Location> locations = new ArrayList<>();
	private List<DexterityDisplay> displays = new ArrayList<>();
	private DexterityDisplay selected = null;
	private Dexterity plugin;
	private ChatColor cc, cc2;
	private double click_cooldown = 0;
	
	public DexSession(Player player, Dexterity plugin) {
		p = player;
		this.plugin = plugin;
		cc = plugin.getChatColor(); cc2 = plugin.getChatColor2();
		plugin.setEditSession(player.getUniqueId(), this);
		
		locations.add(null); //add first 2 locations
		locations.add(null);
	}
	
	public List<Location> getLocations(){
		return locations;
	}
	public List<DexterityDisplay> getEdits(){
		return displays;
	}
	public Player getPlayer() {
		return p;
	}
	
	public DexterityDisplay getSelected() {
		return selected;
	}
	public void setSelected(DexterityDisplay o) {
		selected = o;
	}
	
	public World getWorld() {
		for (Location loc : locations) {
			if (loc != null) return loc.getWorld();
		}
		return null;
	}
	
	public void setLocation(Location loc, boolean continuous, int index) {
		int decimals = 3;
		if (System.currentTimeMillis() - click_cooldown < 100) return;
		click_cooldown = System.currentTimeMillis();
		
		if (!continuous){
			loc.setX(loc.getBlockX());
			loc.setY(loc.getBlockY());
			loc.setZ(loc.getBlockZ());
			decimals = 0;
		}
		loc.setYaw(0);
		loc.setPitch(0);
		
		World world = getWorld();
		if (world != null && !loc.getWorld().getName().equals(world.getName())) {
			p.sendMessage("§4Error: §cPoints must be set in the same world! Use /dex desel");
			return;
		}
		
		if (index < 0 || index == locations.size()) {
			locations.add(loc);
			p.sendMessage(cc + "Set point " + locations.size() + " to " + cc2 + "X:" + DexUtils.round(loc.getX(), decimals) + 
					" Y:" + DexUtils.round(loc.getY(), decimals) +
					" Z:" + DexUtils.round(loc.getZ(), decimals));
		} else {
			if (index > locations.size() - 1) {
				p.sendMessage("§4Error: §cThere are only " + locations.size() + " points!");
				return;
			}
			
			locations.set(index, loc);
			p.sendMessage(cc + "Set point #" + (index + 1) + " to " + cc2 + DexUtils.locationString(loc, decimals));
		}
	}
	
	public void deleteLocation(int index) {
		if (index < 0) {
			locations.clear();
			p.sendMessage(cc + "Cleared point selection.");
		} else {
			if (locations.size() == 0) {
				p.sendMessage("§4Error: §cNo points have been set!");
				return;
			}
			if (index > locations.size() - 1) {
				p.sendMessage("§4Error: §cThere are only " + locations.size() + " points!");
				return;
			}
			locations.remove(index);
			p.sendMessage(cc + "Deleted point #" + (index + 1) + ".");
		}
	}

}
