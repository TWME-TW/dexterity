package me.c7dev.dexterity;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;

import me.c7dev.dexterity.api.DexRotation;
import me.c7dev.dexterity.api.DexterityAPI;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.AxisPair;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.InteractionCommand;
import me.c7dev.dexterity.util.OrientationKey;
import me.c7dev.dexterity.util.RollOffset;
import net.md_5.bungee.api.ChatColor;

public class Dexterity extends JavaPlugin {
	
	private HashMap<String,DexterityDisplay> displays = new HashMap<>();
	private HashMap<String,DexterityDisplay> all_displays = new HashMap<>();
	private HashMap<UUID,DexSession> sessions = new HashMap<>();
	private HashMap<UUID,DexBlock> display_map = new HashMap<>();
	private FileConfiguration lang, defaultLang;
	
	private String chat_color, chat_color2, chat_color3;
	private DexterityAPI api;
	private int max_volume = 25000;
	private WorldEditPlugin we = null;
	private boolean legacy = false;
	
	public static final String defaultLangName = "en-US.yml";
		
	@Override
	public void onEnable() {
		saveDefaultConfig();
		if (!checkIfLegacy()) {
			Bukkit.getLogger().severe("§cYour server must be on 1.19.4 or higher to be able to use Block Displays! Plugin disabled.");
			return;
		}
		api = new DexterityAPI(this);
		
		loadConfigSettings();
		
		
		new DexterityCommand(this);
		new EventListeners(this);
		
		
		Plugin we_plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		if (we_plugin != null) we = (WorldEditPlugin) we_plugin;
		
		new BukkitRunnable() {
			@Override
			public void run() {
				loadDisplays();				
			}
		}.runTask(this);
	}
	
	@Override
	public void onDisable() {
		api.clearAllMarkers();
		saveDisplays();
	}
	
	public void loadConfigSettings() {
		chat_color = parseChatColor(getConfig().getString("primary-color"));
		chat_color2 = parseChatColor(getConfig().getString("secondary-color"));
		chat_color3 = parseChatColor(getConfig().getString("tertiary-color"));
		int config_mv = getConfig().getInt("max-selection-volume");
		if (config_mv > 0) max_volume = config_mv;
		loadLanguageFile(false);
		//TODO wand item type
	}
	
	public void reload() {
		api.clearAllMarkers();
		saveDisplays();
		
		reloadConfig();
		loadConfigSettings();
	}
	
	public DexterityAPI api() {
		return api;
	}
	
	public static DexterityAPI getAPI() {
		Dexterity plugin = Dexterity.getPlugin(Dexterity.class);
		return plugin.api();
	}
	
	public String getChatColor() {
		return chat_color;
	}
	public String getChatColor2() {
		return chat_color2;
	}
	public String getChatColor3() {
		return chat_color3;
	}
	
	public int getMaxVolume() {
		return max_volume;
	}
	
	public boolean usingWorldEdit() {
		return we != null;
	}
	
	public WorldEditPlugin getWorldEdit() {
		return we;
	}
	
	public Region getSelection(Player p) {
		if (we == null) return null;
		try {
			return we.getSession(p).getSelection();
		} catch (Exception ex) {
			return null;
		}
	}
	
	public boolean isLegacy() {
		return legacy;
	}
	
	private boolean checkIfLegacy() {
		Pattern verpattern = Pattern.compile("\\(MC: (.*)\\)");
		Matcher matcher = verpattern.matcher(Bukkit.getVersion());
		if (matcher.find()) {
			String[] version = matcher.group(1).split("\\.");
			if (version.length > 2) {
				int vernum = Integer.parseInt(version[1]);
				int sub = Integer.parseInt(version[2]);
				if (vernum < 19 || (vernum == 19 && sub < 4)) return false;
				legacy = (vernum < 20 || (vernum == 20 && sub < 2));
			}
		}
		return true;
	}
	
	public <T extends Entity> T spawn(Location loc, Class<T> type, Consumer<T> c) {
		T entity;
		World w = loc.getWorld();
		if (legacy) { //backwards compatability with MC versions 1.20.1 and below
			entity = w.spawn(loc, type);
			c.accept(entity);
		} else {
			entity = w.spawn(loc, type, c);
			if (entity instanceof Display) {
				Display bd = (Display) entity;
				bd.setTeleportDuration(DexBlock.TELEPORT_DURATION);
			}
		}
		return entity;
	}
	
	private String parseChatColor(String s) {
		if (s.startsWith("#")) return ChatColor.of(s).toString();
		return s.replace('&', ChatColor.COLOR_CHAR);
	}
	
	public World getDefaultWorld() {
		return Bukkit.getServer().getWorlds().size() == 0 ? null : Bukkit.getServer().getWorlds().get(0);
	}
	
	@Deprecated
	public void setMappedDisplay(DexBlock b) {
		display_map.put(b.getEntity().getUniqueId(), b);
		if (!b.getUniqueId().equals(b.getEntity().getUniqueId())) display_map.put(b.getUniqueId(), b);
	}
	public DexBlock getMappedDisplay(UUID block) {
		return display_map.get(block);
	}
	@Deprecated
	public void clearMappedDisplay(DexBlock block) {
		display_map.remove(block.getEntity().getUniqueId());
		display_map.remove(block.getUniqueId());
	}
	
	public String getConfigString(String dir, String def) {
		String r = getConfigString(dir);
		return r == null ? def.replaceAll("&", "§").replaceAll("\\Q[newline]\\E", "\n") : r;
	}
	
	public String getAuthor() {
		return api.getAuthor();
	}

	public String getConfigString(String dir) {
		
		FileConfiguration use = lang;
		if (use == null) {
			if (defaultLang == null) return "§c§o[No language file loaded]";
			use = defaultLang;
		}

		String s = use.getString(dir);
		if (s == null) {
			if (defaultLang != null && use != defaultLang) s = defaultLang.getString(dir);
			if (s == null) {
				Bukkit.getLogger().warning("Could not get value from config: '" + dir + "'");
				return "§c[Language file missing '§c§o" + dir + "§r§c']";
			}
			
			Bukkit.getLogger().warning("Language file is missing '" + dir + "', using the value from the default instead.");
		}
		return s
				.replaceAll("\\Q&^\\E", chat_color)
				.replaceAll("\\Q&**\\E", chat_color3)
				.replaceAll("\\Q&*\\E", chat_color2)
				.replace('&', ChatColor.COLOR_CHAR)
				.replaceAll("\\Q[newline]\\E", "\n")
				.replaceAll("\\n", "\n");
	}
	
	private void loadLanguageFile(boolean default_lang) {
		String langName;
		if (default_lang) langName = defaultLangName;
		else {
			langName = getConfig().getString("lang-path");
			if (langName == null) {
				langName = defaultLangName;
				Bukkit.getLogger().warning("No language file specified in config, loading default.");
			}
			if (!langName.contains(".")) langName += ".yml";
		}

		String dir = this.getDataFolder().getAbsolutePath() + "/" + langName;
		try {
			File f = new File(dir);
			if (f.exists()) lang = YamlConfiguration.loadConfiguration(f);
			else Bukkit.getLogger().warning("Could not find language file '" + langName + "'!");
		} catch (Exception ex) {
			ex.printStackTrace();
			Bukkit.getLogger().severe("Could not load the language file!");
		}
		
		try {
			String langPath = "";
			String[] pathSplit = dir.split("/");
			for (int i = 0; i < pathSplit.length - 1; i++) langPath += pathSplit[i] + "/";

			File df1 = new File(langPath + "/" + defaultLangName);
			if (df1.exists()) {
				defaultLang = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource(defaultLangName)));
			} else { //from scratch
				saveResource(defaultLangName, false);
				File df2 = new File(this.getDataFolder().getAbsolutePath() + "/" + defaultLangName);
				defaultLang = YamlConfiguration.loadConfiguration(df2);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			Bukkit.getLogger().severe("Could not load the default language file!");
		}
	}
	
	private void purgeHelper(DexterityDisplay d) {
		if (d.getBlocksCount() > 0) return;
		if (d.getSubdisplayCount() == 0) d.remove(false);
		else {
			for (DexterityDisplay sub : d.getSubdisplays()) purgeHelper(sub);
		}
	}
	
	private int loadDisplays() {
		File folder = new File(this.getDataFolder().getAbsolutePath() + "/displays/");
		if (!folder.exists()) {
			folder.mkdirs();
			return 0;
		}
		
		displays.clear();
		sessions.clear();
		int display_count = 0;
		
		try {
			
			for (File f : folder.listFiles()) {
				if (!f.getName().endsWith(".yml")) continue;
				String label = f.getName().replaceAll("\\.yml", "");
 			
				FileConfiguration afile = YamlConfiguration.loadConfiguration(f);

				//load entities by uuid
				List<BlockDisplay> blocks = new ArrayList<>();
				boolean missing_blocks = false;
				for (String uuid : afile.getStringList("uuids")) {
					Entity entity = Bukkit.getEntity(UUID.fromString(uuid));
					if (entity != null && entity instanceof BlockDisplay) {
						blocks.add((BlockDisplay) entity);
					} else missing_blocks = true;
				}
				if (missing_blocks) {
					Bukkit.getLogger().warning("Some of the blocks for display '" + label + "' are missing!");
				}

				Location center = DexUtils.deserializeLocation(afile, "center");
				double sx = afile.getDouble("scale-x");
				double sy = afile.getDouble("scale-y");
				double sz = afile.getDouble("scale-z");
				float base_yaw = (float) afile.getDouble("yaw");
				float base_pitch = (float) afile.getDouble("pitch");
				float base_roll = (float) afile.getDouble("roll");
				Vector scale = new Vector(sx == 0 ? 1 : sx, sy == 0 ? 1 : sy, sz == 0 ? 1 : sz);
				DexterityDisplay disp = new DexterityDisplay(this, center, scale, label);
				disp.setBaseRotation(base_yaw, base_pitch, base_roll);
				
				ConfigurationSection cmd_section = afile.getConfigurationSection("commands");
				if (cmd_section != null) {
					for (String key : cmd_section.getKeys(false)) {
						disp.addCommand(new InteractionCommand(afile.getConfigurationSection("commands." + key)));
					}
				}

				for (BlockDisplay bd : blocks) {
					disp.addBlock(new DexBlock(bd, disp));
				}

				new BukkitRunnable() {
					@Override
					public void run() {
						HashMap<OrientationKey, RollOffset> cache = new HashMap<>();
						for (DexBlock db : disp.getBlocks()) {
							db.loadRoll(cache);
						}
					}
				}.runTaskAsynchronously(this);

				String parent_label = afile.getString("parent");
				if (parent_label != null) {
					DexterityDisplay parent = getDisplay(parent_label);
					if (parent == null) Bukkit.getLogger().severe("Could not find parent display '" + parent_label + "'!");
					else {
						parent.addSubdisplay(disp);
						disp.setParent(parent);
					}
				}

				if (disp.getParent() == null) displays.put(disp.getLabel(), disp);
				all_displays.put(disp.getLabel(), disp);
			}

			//purge empty displays if any were loaded
			DexterityDisplay[] allLabeled = new DexterityDisplay[displays.size()];
			int i = 0;
			for (Entry<String,DexterityDisplay> entry : displays.entrySet()) {
				allLabeled[i] = entry.getValue();
				i++;
			}
			for (DexterityDisplay disp : allLabeled) purgeHelper(disp);

			return display_count;

		} catch (Exception ex) {
			ex.printStackTrace();
			Bukkit.getLogger().severe("Could not load Dexterity displays!");
			return 0;
		}
	}
	
	public void saveDisplays() {
		try {
			for (DexterityDisplay disp : getDisplays()) {
				saveDisplay(disp);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void saveDisplay(DexterityDisplay disp) {
		
		if (!disp.isSaved() || disp.getLabel().length() == 0 || disp.getBlocksCount() == 0) return;
		
		File f = new File(this.getDataFolder().getAbsoluteFile() + "/displays/" + disp.getLabel() + ".yml");
		try {
			if (!f.exists()) f.createNewFile();
		} catch (Exception ex) {
			ex.printStackTrace();
			Bukkit.getLogger().severe("Could not save display " + disp.getLabel() + "!");
			return;
		}
		FileConfiguration afile = YamlConfiguration.loadConfiguration(f);
		for (String s : afile.getKeys(false)) afile.set(s, null);
		
		afile.set("center", disp.getCenter().serialize());
		if (disp.getScale().getX() != 1) afile.set("scale-x", disp.getScale().getX());
		if (disp.getScale().getY() != 1) afile.set("scale-y", disp.getScale().getY());
		if (disp.getScale().getZ() != 1) afile.set("scale-z", disp.getScale().getZ());

		if (disp.getRotationManager() != null) {
			DexRotation rot = disp.getRotationManager();
			AxisPair a = new AxisPair(rot.getXAxis(), rot.getZAxis());
			Vector res = a.getPitchYawRoll();
			if (res.getY() != 0) afile.set("yaw", res.getY());
			if (res.getX() != 0) afile.set("pitch", res.getX());
			if (res.getZ() != 0) afile.set("roll", res.getZ());
		}
		
		if (disp.getCommandCount() > 0) {
			afile.set("commands", null);
			InteractionCommand[] cmds = disp.getCommands();
			for (int i = 0; i < cmds.length; i++) {
				afile.set("commands.cmd-" + (i+1), cmds[i].serialize());
			}
		}
		
		List<String> uuids = new ArrayList<>();
		DexBlock[] blocks = disp.getBlocks();
		if (blocks.length > 0) {
			for (DexBlock db : disp.getBlocks()) uuids.add(db.getEntity().getUniqueId().toString());
			afile.set("uuids", uuids);
		} else {
			Bukkit.getLogger().warning("Jar modified, skipping save of " + disp.getLabel());
			return;
		}
				
		if (disp.getParent() != null) afile.set("parent", disp.getParent().getLabel());
		
		try {
			afile.save(f);
		} catch (IOException e) {
			e.printStackTrace();
			Bukkit.getLogger().severe("Could not save '" + disp.getLabel() + "' display!");
		}
		
		for (DexterityDisplay sub : disp.getSubdisplays()) saveDisplay(sub);
	}
	
	public void registerDisplay(String label, DexterityDisplay d) {
		if (label == null || d == null) throw new IllegalArgumentException("Parameters cannot be null!");
		if (all_displays.containsKey(label) && all_displays.get(label) != d) return;
		if (d.getParent() == null) displays.put(label, d);
		all_displays.put(label, d);
		saveDisplay(d);
	}
	
	public void unregisterDisplay(DexterityDisplay d) {
		unregisterDisplay(d, false);
	}
	
	public void unregisterDisplay(DexterityDisplay d, boolean from_merge) {
		if (!d.isSaved()) return;
		if (!from_merge) all_displays.remove(d.getLabel());
		displays.remove(d.getLabel());
		
		try {
			File f = new File(this.getDataFolder().getAbsolutePath() + "/displays/" + d.getLabel() + ".yml");
			if (f.exists()) f.delete();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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
	
	public Set<Entry<UUID, DexSession>> editSessionIter() {
		return sessions.entrySet();
	}
	
	public DexSession getEditSession(UUID u) {
		DexSession s = sessions.get(u);
		if (s == null) {
			Player p = Bukkit.getPlayer(u);
			if (p == null) return null;
			s = new DexSession(p, this);
		}
		return s;
	}
	
	public void deleteEditSession(UUID u) {
		DexSession session = sessions.get(u);
		if (session == null) return;
		session.removeAxes();
		sessions.remove(u);
	}
	
	public void setEditSession(UUID u, DexSession s) {
		deleteEditSession(u);
		sessions.put(u, s);
	}
	

}
