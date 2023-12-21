package me.c7dev.tensegrity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;
import me.c7dev.tensegrity.util.Plane;
import net.md_5.bungee.api.ChatColor;

public class Dexterity extends JavaPlugin {
	
	private HashMap<Integer,DexterityDisplay> displays = new HashMap<>();
	private HashMap<UUID,DexSession> sessions = new HashMap<>();
	private HashMap<UUID,Integer> block_uuid = new HashMap<>();
	private HashMap<String,Integer> label_map = new HashMap<>();
	private int next_id = 0;
	
	private ChatColor chat_color = ChatColor.of("#49eb9a"); //#ffa217
	private ChatColor chat_color2 = ChatColor.of("#42f5ef"); //ffd417
	
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
	
	public Collection<DexterityDisplay> getDisplays(){
		return displays.values();
		//return new ArrayList<DexterityDisplay>(displays.values());
	}
	
	public DexterityDisplay getDisplay(int id) {
		return displays.get(id);
	}
	public DexterityDisplay getDisplay(String label) {
		if (!label_map.containsKey(label)) return null;
		return displays.get(label_map.get(label));
	}
	
	public DexSession getEditSession(UUID u) {
		return sessions.get(u);
	}
	
	public HashMap<String,Integer> getLabelMap(){
		return label_map;
	}
	
	public void setEditSession(UUID u, DexSession s) {
		sessions.put(u, s);
	}
	
	public ChatColor getChatColor() {
		return chat_color;
	}
	public ChatColor getChatColor2() {
		return chat_color2;
	}
	
	public int getNextDisplayId() {
		next_id++;
		return next_id - 1;
	}
	
	public String getConfigString(String dir, String def) {
		String r = getConfigString(dir);
		return r == null ? def.replaceAll("&", "§").replaceAll("\\Q[newline]\\E", "\n") : r;
	}
	
	public HashMap<UUID,Integer> getBlockUUIDMap(){
		return block_uuid;
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
		block_uuid.clear();
		sessions.clear();
		label_map.clear();
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
				if (blocks.size() == 0) { //none, skip
					Bukkit.getLogger().severe("Could not load display '" + label + "' as none of the blocks were found!");
					continue;
				}
				if (missing_blocks) {
					Bukkit.getLogger().warning("Some of the blocks for display '" + label + "' are missing!");
				}
				
				Location center = DexUtils.deserializeLocation(afile, label + ".center");
				DexterityDisplay disp = new DexterityDisplay(this, center, label);
				disp.forceSetScale(afile.getDouble(label + ".scale"));
				disp.setRotationPlane(Plane.valueOf(afile.getString(label + ".rotation-plane")));
				
				for (BlockDisplay bd : blocks) {
					disp.getBlocks().add(new DexBlock(bd, disp));
				}
				
				displays.put(disp.getID(), disp);
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
			afile.set(disp.getLabel() + ".scale", disp.getScale());
			afile.set(disp.getLabel() + ".rotation-plane", disp.getRotationPlane().toString());
			afile.set(disp.getLabel() + ".center", disp.getCenter().serialize());
			List<String> uuids = new ArrayList<>();
			for (DexBlock db : disp.getBlocks()) uuids.add(db.getEntity().getUniqueId().toString());
			afile.set(disp.getLabel() + ".uuids", uuids);
			//TODO serialize animations
		}
		
		try {
			afile.save(f);
		} catch (Exception ex) {
			ex.printStackTrace();
			Bukkit.getLogger().severe("Could not save displays.yml! Dexterity will lose arena data.");
		}
	}
	
	//////////////////////////////////////////////////////////
	
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
		
		displays.put(d.getID(), d);
		
		saveDisplays();
		
		return d;
	}
		
	

}
