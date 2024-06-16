package me.c7dev.tensegrity.api;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import me.c7dev.tensegrity.DexSession;
import me.c7dev.tensegrity.Dexterity;
import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexBlock;

public class DexterityAPI {
	
	Dexterity plugin;
	
	public DexterityAPI(Dexterity plugin) {
		this.plugin = plugin;
	}

	public static DexterityAPI getInstance() {
		return Dexterity.getPlugin(Dexterity.class).getAPI();
	}
	
	public Set<String> getDisplayLabels() {
		return plugin.getDisplayLabels();
	}
	
	public Collection<DexterityDisplay> getDisplays() {
		return plugin.getDisplays();
	}
	
	public DexterityDisplay getDisplay(String label) {
		return plugin.getDisplay(label);
	}
	
	public DexSession getEditSession(UUID u) {
		return plugin.getEditSession(u);
	}
	
	public DexterityDisplay createDisplay(Location l1, Location l2) { //l1 and l2 bounding box, all blocks inside converted
		if (!l1.getWorld().getName().equals(l2.getWorld().getName())) return null;
		
		int xmin = Math.min(l1.getBlockX(), l2.getBlockX()), xmax = Math.max(l1.getBlockX(), l2.getBlockX());
		int ymin = Math.min(l1.getBlockY(), l2.getBlockY()), ymax = Math.max(l1.getBlockY(), l2.getBlockY());
		int zmin = Math.min(l1.getBlockZ(), l2.getBlockZ()), zmax = Math.max(l1.getBlockZ(), l2.getBlockZ());
		
		Location center = new Location(l1.getWorld(), Math.min(l1.getX(), l2.getX()) + Math.abs((l1.getX()-l2.getX())/2),
				Math.min(l1.getY(), l2.getY()) + Math.abs(((l1.getY() - l2.getY()) / 2)),
				Math.min(l1.getZ(), l2.getZ()) + Math.abs((l1.getZ() - l2.getZ()) / 2));
		center.add(0.5, 0.5, 0.5);
		
		DexterityDisplay d = new DexterityDisplay(plugin, center, null);

		for (int x = xmin; x <= xmax; x++) {
			for (int y = ymin; y <= ymax; y++) {
				for (int z = zmin; z <= zmax; z++) {
					Block b = new Location(l1.getWorld(), x, y, z).getBlock();
					if (b.getType() != Material.BARRIER && b.getType() != Material.AIR) {
						DexBlock db = new DexBlock(b, d);
						d.getBlocks().add(db);
						b.setType(Material.AIR);
						//db.setBrightness(b2.getLightFromBlocks(), b2.getLightFromSky());
					}
				}
			}
		}
		
		plugin.registerDisplay(d.getLabel(), d);
		
		plugin.saveDisplays();
		
		return d;
	}
		
	
}
