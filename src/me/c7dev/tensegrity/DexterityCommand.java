package me.c7dev.tensegrity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;
import me.c7dev.tensegrity.util.Plane;
import net.md_5.bungee.api.ChatColor;

public class DexterityCommand implements CommandExecutor, TabCompleter {
	
	private Dexterity plugin;
	ChatColor cc, cc2;
	String noperm;
	
	public String[] commands = {
		"convert", "deconvert", "deselect", "list", "merge", "move", "pos1", "remove", "rename", "rotate", "scale", "select", "wand"
	};
	public String[] descriptions = {
		"Create a display from selected region", //convert
		"Revert display back into block form", //deconvert
		"Clear selected region", //deselect
		"List all displays", //list
		"Combine two displays", //merge
		"Teleport a display", //move
		"Set the first position", //pos1
		"Delete a display", //remove
		"Change a display's name", //rename
		"Rotate a display", //rotate
		"Resize a display", //scale
		"Select a display", //sel
		"Get a wand to select block locations" //wand
	};
	
	public DexterityCommand(Dexterity plugin) {
		this.plugin= plugin;
		cc = plugin.getChatColor();
		cc2 = plugin.getChatColor2();
		plugin.getCommand("dex").setExecutor(this);
		plugin.getCommand("dex").setExecutor(this);
		noperm = plugin.getConfigString("no-permission", "§cYou don't have permission!");
	}
	
	public DexterityDisplay getSelected(DexSession session) {
		DexterityDisplay d = session.getSelected();
		if (d == null) {
			session.getPlayer().sendMessage("§4Error: §cYou must select a display to do this!");
			return null;
		}
		return d;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) return true;
		
		Player p = (Player) sender;
		
		if (args.length == 0) {
			p.sendMessage(cc + "§lUsing Dexterity " + cc2 + "§lv1.0.0");
			if (p.hasPermission("dexterity.command")) {
				p.sendMessage(cc + "Use " + cc2 + "/dex help" + cc + " to get started!");
			}
			return true;
		}
		
		if (!p.hasPermission("dexterity.command")) {
			p.sendMessage(noperm);
			return true;
		}
		
		DexSession session = plugin.getEditSession(p.getUniqueId());
		if (session == null) {
			session = new DexSession(p, plugin);
		}
		
		HashMap<String,Integer> attrs = DexUtils.getAttributes(args);
		List<String> flags = DexUtils.getFlags(args);
		String def = DexUtils.getDefaultAttribute(args);

		if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
			int page = 0, pagelen = 5;
			if (attrs.containsKey("page")) {
				page = Math.max(attrs.get("page") - 1, 0);
			} else if (args.length >= 2) page = Math.max(DexUtils.parseInt(args[1]) - 1, 0);
			int maxpage = (commands.length/pagelen) + (commands.length % pagelen > 0 ? 1 : 0);
			
			if (page >= maxpage) page = maxpage - 1;
			
			p.sendMessage("\n\n");
			p.sendMessage(cc + "§lDexterity Commands: §6Page §6§l" + (page+1) + "§6/" + maxpage);
			int pagestart = pagelen*page;
			int pageend = Math.min(pagestart + pagelen, commands.length);
			for (int i = pagestart; i < pageend; i++) {
				p.sendMessage(cc2 + "- /dex " + commands[i] + " §8- " + cc + descriptions[i]);
			}
		}

		else if (args[0].equalsIgnoreCase("wand")) {
			ItemStack wand = new ItemStack(Material.BLAZE_ROD);
			ItemMeta meta = wand.getItemMeta();
			meta.setDisplayName(plugin.getConfigString("wand-title", "§fDexterity Wand"));
			wand.setItemMeta(meta);
			p.getInventory().addItem(wand);
			
		}
		
		else if (args[0].equalsIgnoreCase("sel") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("select") || args[0].equalsIgnoreCase("pos1") || args[0].equalsIgnoreCase("pos2") || args[0].equalsIgnoreCase("load")) {
			int index = -1;
			if (args[0].equalsIgnoreCase("pos1")) index = 0;
			else if (args[0].equalsIgnoreCase("pos2")) index = 1;
			else if (args.length >= 2) index = DexUtils.parseInt(args[1])-1;
			
			if (index < 0) {
				if (args.length == 1) {
					p.sendMessage("§4Usage: §c/dex sel <name>");
					return true;
				}
				if (def != null) {
					DexterityDisplay disp = plugin.getDisplay(def);
					if (disp == null) {
						p.sendMessage("§4Error: §cCould not find display '" + def + "'");
						return true;
					}
					session.setSelected(disp);
					p.sendMessage(cc + "Selected " + cc2 + def + cc + "!");
					return true;
				}
			}

			Location loc = p.getLocation();
			boolean continuous = flags.contains("continuous") || flags.contains("c") || flags.contains("cont");
			
			if (args.length == 4) {
				try {
					double x = Double.parseDouble(args[1]);
					double y = Double.parseDouble(args[2]);
					double z = Double.parseDouble(args[3]);
					loc = new Location(p.getWorld(), x, y, z, 0, 0);
					index = -1;
					continuous = true;
				} catch (Exception ex) {
					p.sendMessage("§4Error: §cx, y, and z must be numbers!");
					return true;
				}
			}
			session.setLocation(loc, continuous, index);
		}
		
		else if (args[0].equalsIgnoreCase("desel") || args[0].equalsIgnoreCase("deselect") || args[0].equalsIgnoreCase("clear")) {
			int parsed_def = def != null ? DexUtils.parseInt(def) - 1 : -1;
			int index = attrs.containsKey("point_num") ? attrs.get("point_num") - 1 : parsed_def;
			session.deleteLocation(index);
		}
		
		else if (args[0].equalsIgnoreCase("convert") || args[0].equalsIgnoreCase("conv")) {
			if (session.getLocations().size() >= 2) {
				DexterityDisplay d = plugin.createDisplay(session.getLocations().get(0), session.getLocations().get(1));
				
				if (flags.contains("offset_center")) {
					if (session.getLocations().size() >= 3) {
						p.sendMessage("§7Set center of rotation to 3rd point");
						d.setCenter(session.getLocations().get(2));
					}
				}
				
				session.setSelected(d);
				
				p.sendMessage(cc + "Created a new display: " + cc2 + d.getLabel() + cc + "!");
				
			} else p.sendMessage("§4Error: §cThis command requires a minimum of 2 locations set!");
		}
		
		else if (args[0].equalsIgnoreCase("move")) {
			
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			Location loc;
			if (args.length == 1 || flags.contains("continuous") || flags.contains("c") || flags.contains("here")) {
				if (flags.contains("continuous") || flags.contains("c")) loc = p.getLocation();
				else loc = DexUtils.blockLoc(p.getLocation()).add(0.5, 0.5, 0.5);
			}
			else loc = d.getCenter();
						
			HashMap<String,Double> attr_d = DexUtils.getAttributesDoubles(args);
			if (attr_d.containsKey("x")) loc.add(attr_d.get("x"), 0, 0);
			if (attr_d.containsKey("y")) loc.add(0, attr_d.get("y"), 0);
			if (attr_d.containsKey("z")) loc.add(0, 0, attr_d.get("z"));
			
						
			d.teleport(loc);
			
		}
		else if (args[0].equalsIgnoreCase("label") || args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("rename")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			if (args.length != 2) {
				p.sendMessage("§4Usage: §c/dex rename <name>");
				return true;
			}
			d.setLabel(args[1]);
			p.sendMessage(cc + "Renamed this display to " + cc2 + args[1]);
		}
		
		else if (args[0].equalsIgnoreCase("rotate")){
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			if (args.length < 2 || def == null) {
				p.sendMessage("§4Usage: §c/dex rotate <degrees>");
				return true;
			}
			double degrees;
			try {
				degrees = Double.parseDouble(def);
			} catch (Exception ex) {
				p.sendMessage("§4Error: §cYou must send an angle!");
				return true;
			}
			
			Plane plane = Plane.XZ;
			if (attrs.containsKey("plane")) {
				HashMap<String,String> attr_str = DexUtils.getAttributesStrings(args);
				plane = Plane.valueOf(attr_str.get("plane").toUpperCase());
				if (plane == null) {
					p.sendMessage("§4Error: §cValid planes are XZ, XY, and ZY.");
					return true;
				}
			}
			
			d.rotate(degrees, plane);
			p.sendMessage(cc + "Rotated " + cc2 + d.getLabel() + cc + " by " + degrees + " degrees!");
			
		}
		else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("restore") || args[0].equalsIgnoreCase("deconvert") || args[0].equalsIgnoreCase("deconv")) {
			DexterityDisplay d = session.getSelected();
			if (d == null) {
				if (def == null) return true;
				d = plugin.getDisplay(def);
				if (d == null) {
					p.sendMessage("§4Error: §cCould not find display '" + def + "'");
					return true;
				}
			}
			boolean res = !args[0].equalsIgnoreCase("remove");
			d.remove(res);
			String label2 = d.getLabel() == null ? "at " + cc2 + DexUtils.locationString(d.getCenter(), 0) : cc2 + d.getLabel();
			p.sendMessage(cc + (res ? "Restored" : "Removed") + " the display " + label2);
		}
		
		else if (args[0].equalsIgnoreCase("list")) {
			if (plugin.getDisplays().size() == 0) {
				p.sendMessage("§cThere are no displays set up!");
				return true;
			}
			p.sendMessage("§b");
			p.sendMessage(cc2 + "§l" + plugin.getDisplays().size() + cc + " display" + (plugin.getDisplays().size() == 1 ? " has" : "s have") + " been created:"); //TODO: paginate
			for (DexterityDisplay disp : plugin.getDisplays()) {
				p.sendMessage(cc2 + (disp.getLabel() == null ? "§oUnnamed" : disp.getLabel()) + "§7: " + cc + DexUtils.locationString(disp.getCenter(), 0) + " (" + disp.getCenter().getWorld().getName() + ")");
			}
		}
		
		else if (args[0].equalsIgnoreCase("scale")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			if (def == null) {
				p.sendMessage("§4Usage: §c/dex scale <multiplier>");
				return true;
			}
			
			float scale = 1;
			try {
				scale = Float.parseFloat(def);
			} catch(Exception ex) {
				p.sendMessage("§4Error: §cYou must send a multiplier!");
				return true;
			}
			
			d.setScale(scale);
			
			p.sendMessage(cc + "Set " + cc2 + d.getLabel() + cc + " scale to " + cc2 + scale + cc + "!");
		}
		else if (args[0].equalsIgnoreCase("merge")) {
			
		}
		
		else {
			p.sendMessage("§cUnknown sub-command.");
		}
		
		//plugin.createAnimation(p.getLocation().add(0, -1, 0));
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] argsr) {
		List<String> params = new ArrayList<String>();
		for (String s : argsr) {
			String[] ssplit = s.split("=");
			if (ssplit.length > 0) params.add(DexUtils.attrAlias(ssplit[0]));
		}
		
		List<String> ret = new ArrayList<String>();
		
		if (argsr.length <= 1) {
			for (String s : commands) ret.add(s);
		}
		else if (argsr[0].equalsIgnoreCase("?") || argsr[0].equalsIgnoreCase("help")) {
			ret.add("page=");
		}
		else if (argsr[0].equalsIgnoreCase("sel") || argsr[0].equalsIgnoreCase("select") || argsr[0].equalsIgnoreCase("set")) {
			ret.add("-continuous");
		}
		else if (argsr[0].equalsIgnoreCase("move")) {
			ret.add("x=");
			ret.add("y=");
			ret.add("z=");
			ret.add("north=");
			ret.add("south=");
			ret.add("east=");
			ret.add("west=");
			ret.add("up=");
			ret.add("down=");
			ret.add("-here");
			ret.add("-continuous");
		}
		else if (argsr[0].equalsIgnoreCase("rotate")) {
			if (!params.contains("plane")) ret.add("plane=");
			else if (params.size() == 2) {
				ret.add("plane=XY");
				ret.add("plane=XZ");
				ret.add("plane=ZY");
			}
		}
		else if (argsr[0].equalsIgnoreCase("sphere")) {
			if (!params.contains("radius")) ret.add("radius=");
			if (!params.contains("filled")) ret.add("filled=");
			if (!params.contains("granularity")) ret.add("granularity=");
		}

		return ret;
	}

}
