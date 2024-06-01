package me.c7dev.tensegrity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3f;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;
import me.c7dev.tensegrity.util.Plane;
import net.md_5.bungee.api.ChatColor;

public class Dexterity extends JavaPlugin {
	
	private HashMap<String,DexterityDisplay> displays = new HashMap<>();
	private HashMap<String,DexterityDisplay> all_displays = new HashMap<>();
	private HashMap<UUID,DexSession> sessions = new HashMap<>();
	
	public final ChatColor chat_color = ChatColor.of("#49eb9a"); //#ffa217
	public final ChatColor chat_color2 = ChatColor.of("#42f5ef"); //ffd417
	
	public static final Vector3f DEFAULT_DISP = new Vector3f(-0.5f, -0.5f, -0.5f);
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		new DexterityCommand(this);
		new EventListeners(this);
		
		loadDisplays();
	}
	
	@Override
	public void onDisable() {
		saveDisplays();
	}
	
	public ChatColor getChatColor() {
		return chat_color;
	}
	public ChatColor getChatColor2() {
		return chat_color2;
	}
	
	public String getConfigString(String dir, String def) {
		String r = getConfigString(dir);
		return r == null ? def.replaceAll("&", "§").replaceAll("\\Q[newline]\\E", "\n") : r;
	}
	
	public DexterityDisplay getClickedDisplay(Player p) {
		
		return null;
	}

	public String getConfigString(String dir) {

		String s = getConfig().getString(dir);
		if (s == null) {
			Bukkit.getLogger().warning("Could not get value from config: '" + dir + "'");
			return null;
		}
		return s
				.replace('&', ChatColor.COLOR_CHAR)
				.replaceAll("\\Q[newline]\\E", "\n")
				.replaceAll("\\n", "\n");
	}
	
	public int loadDisplays() { //load from displays.yml
		File f = new File(this.getDataFolder().getAbsolutePath() + "/displays.yml");
		try {
			if (f.createNewFile()) return 0;
		} catch (IOException ex) {
			ex.printStackTrace();
			Bukkit.getLogger().warning("Failed to save a new displays.yml file!");
			return 0;
		}
		
		displays.clear();
		sessions.clear();
		int display_count = 0;
		
		try {
			FileConfiguration afile = YamlConfiguration.loadConfiguration(f);
			
			for (String label : afile.getKeys(false)) {
				
				List<BlockDisplay> blocks = new ArrayList<>();
				boolean missing_blocks = false;
				for (String uuid : afile.getStringList(label + ".uuids")) {
					Entity entity = Bukkit.getEntity(UUID.fromString(uuid));
					if (entity != null && entity instanceof BlockDisplay) {
						blocks.add((BlockDisplay) entity);
					} else missing_blocks = true;
				}
				if (missing_blocks) {
					Bukkit.getLogger().warning("Some of the blocks for display '" + label + "' are missing!");
				}
				
				Location center = DexUtils.deserializeLocation(afile, label + ".center");
				DexterityDisplay disp = new DexterityDisplay(this, center, label);
				disp.setRotationPlane(Plane.valueOf(afile.getString(label + ".rotation-plane")));
				
				for (BlockDisplay bd : blocks) {
					disp.getBlocks().add(new DexBlock(bd, disp));
				}
				
				String parent_label = afile.getString(label + ".parent");
				if (parent_label != null) {
					DexterityDisplay parent = getDisplay(parent_label);
					if (parent == null) Bukkit.getLogger().severe("Could not find parent display '" + parent_label + "'!");
					else {
						parent.getSubdisplays().add(disp);
						disp.setParent(parent);
					}
				}
								
				if (disp.getParent() == null) displays.put(disp.getLabel(), disp);
				all_displays.put(disp.getLabel(), disp);
			}
			
			return display_count;
			
		} catch (Exception ex) {
			ex.printStackTrace();
			Bukkit.getLogger().severe("Could not load Dexterity displays!");
			return 0;
		}
	}
	
	public void saveDisplays() {
		File f = new File(this.getDataFolder().getAbsolutePath() + "/displays.yml");
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException ex) {
				ex.printStackTrace();
				Bukkit.getLogger().severe("Could not save displays.yml! Dexterity will lose arena data.");
				return;
			}
		}
		FileConfiguration afile = YamlConfiguration.loadConfiguration(f);
		for (String s : afile.getKeys(false)) afile.set(s, null);
		
		for (DexterityDisplay disp : getDisplays()) {
			saveDisplay(disp, afile);
			//TODO serialize animations
		}
		
		try {
			afile.save(f);
		} catch (Exception ex) {
			ex.printStackTrace();
			Bukkit.getLogger().severe("Could not save displays.yml! Dexterity will lose arena data.");
		}
	}
	
	private void saveDisplay(DexterityDisplay disp, FileConfiguration afile) {
		afile.set(disp.getLabel() + ".rotation-plane", disp.getRotationPlane().toString());
		afile.set(disp.getLabel() + ".center", disp.getCenter().serialize());
		List<String> uuids = new ArrayList<>();
		for (DexBlock db : disp.getBlocks()) uuids.add(db.getEntity().getUniqueId().toString());
		afile.set(disp.getLabel() + ".uuids", uuids);
				
		if (disp.getParent() != null) afile.set(disp.getLabel() + ".parent", disp.getParent().getLabel());
		
		for (DexterityDisplay sub : disp.getSubdisplays()) saveDisplay(sub, afile);
	}
	
	public void registerDisplay(String label, DexterityDisplay d) {
		if (all_displays.containsKey(label) && all_displays.get(label) != d) return;
		if (d.getParent() == null) displays.put(label, d);
		all_displays.put(label, d);
	}
	
	//////////////////////////////////////////////////////////
	
	public Set<String> getDisplayLabels(){
		return all_displays.keySet();
		//return new ArrayList<DexterityDisplay>(displays.values());
	}
	
	public Collection<DexterityDisplay> getDisplays() {
		return displays.values();
	}
	
	public DexterityDisplay getDisplay(String label) {
		if (!all_displays.containsKey(label)) return null;
		return all_displays.get(label);
	}
	
	public DexSession getEditSession(UUID u) {
		return sessions.get(u);
	}
	
	public void setEditSession(UUID u, DexSession s) {
		sessions.put(u, s);
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
		
		DexterityDisplay d = new DexterityDisplay(this, center, null);

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
		
		//RotationAnimation rotation = new RotationAnimation(d, Plane.XZ);
		//rotation.start();
		
		displays.put(d.getLabel(), d);
		all_displays.put(d.getLabel(), d);
		
		saveDisplays();
		
		return d;
	}
		
	

}
