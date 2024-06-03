package me.c7dev.tensegrity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.displays.animation.Animation;
import me.c7dev.tensegrity.displays.animation.LinearTranslationAnimation;
import me.c7dev.tensegrity.displays.animation.RideAnimation;
import me.c7dev.tensegrity.util.ColorEnum;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;
import me.c7dev.tensegrity.util.Plane;
import net.md_5.bungee.api.ChatColor;

public class DexterityCommand implements CommandExecutor, TabCompleter {
	
	private Dexterity plugin;
	ChatColor cc, cc2;
	String noperm;
	
	public String[] commands = {
		"animation", "convert", "deconvert", "deselect", "glow", "list", "merge", "move", "pos1", "remove", "rename", "rotate", 
		"scale", "select", "unmerge", "wand"
	};
	public String[] descriptions = {
		"Modify the display's animations", //animation
		"Create a display from selected region", //convert
		"Revert display back into block form", //deconvert
		"Clear selected region", //deselect
		"Make the display glow", //glow
		"List all displays", //list
		"Combine two displays", //merge
		"Teleport a display", //move
		"Set the first position", //pos1
		"Delete a display", //remove
		"Change a display's name", //rename
		"Rotate a display", //rotate
		"Resize a display", //scale
		"Select a display", //sel
		"Separate a display group", //unmerge
		"Get a wand to select block locations" //wand
	};
	public String[] command_strs = new String[commands.length];
	
	public DexterityCommand(Dexterity plugin) {
		this.plugin= plugin;
		cc = plugin.getChatColor();
		cc2 = plugin.getChatColor2();
		plugin.getCommand("dex").setExecutor(this);
		plugin.getCommand("dex").setExecutor(this);
		noperm = plugin.getConfigString("no-permission", "§cYou don't have permission!");
		
		for (int i = 0; i < commands.length; i++) {
			command_strs[i] = cc2 + "- /dex " + commands[i] + " §8- " + cc + descriptions[i];
		}
	}
	
	public DexterityDisplay getSelected(DexSession session) {
		DexterityDisplay d = session.getSelected();
		if (d == null) {
			session.getPlayer().sendMessage("§4Error: §cYou must select a display to do this!");
			return null;
		}
		return d;
	}
	
	public int constructList(String[] strs, DexterityDisplay disp, String selected, int i, int level) {
		int count = 1;
		
		String line = "";
		for (int j = 0; j < level; j++) line += "  ";
		
		line += disp.getLabel() == null ? "§oUnnamed" : disp.getLabel();
		if (disp.getBlocks().size() > 0) line = cc2 + ((selected != null && disp.getLabel().equals(selected)) ? "§d" : "") + line + "§7: " + cc + DexUtils.locationString(disp.getCenter(), 0) + " (" + disp.getCenter().getWorld().getName() + ")";
		else line = cc2 + ((selected != null && disp.getLabel().equals(selected)) ? "§d" : "") + "§l" + line + cc + ":";
		
		strs[i] = line;
		
		for (int j = 1; j <= disp.getSubdisplays().size(); j++) {
			count += constructList(strs, disp.getSubdisplays().get(j-1), selected, i+count, level+1);
		}
		
		return count;
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
		HashMap<String,String> attr_str = DexUtils.getAttributesStrings(args);
		List<String> flags = DexUtils.getFlags(args);
		String def = DexUtils.getDefaultAttribute(args);

		if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
			int page = 0;
			if (attrs.containsKey("page")) {
				page = Math.max(attrs.get("page") - 1, 0);
			} else if (args.length >= 2) page = Math.max(DexUtils.parseInt(args[1]) - 1, 0);
			int maxpage = DexUtils.maxPage(commands.length, 5);
			if (page >= maxpage) page = maxpage - 1;
						
			p.sendMessage("\n\n");
			p.sendMessage(cc + "§lDexterity Commands: §6Page §6§l" + (page+1) + "§6/" + maxpage);
			DexUtils.paginate(p, command_strs, page, 5);
		}

		else if (args[0].equalsIgnoreCase("wand")) {
			ItemStack wand = new ItemStack(Material.BLAZE_ROD);
			ItemMeta meta = wand.getItemMeta();
			meta.setDisplayName(plugin.getConfigString("wand-title", "§fDexterity Wand"));
			wand.setItemMeta(meta);
			p.getInventory().addItem(wand);
			
		}
		
		else if (args[0].equalsIgnoreCase("test")) {
			for (Entity e : p.getWorld().getEntities()) {
				if (e instanceof BlockDisplay) e.remove();
			}
		}
		else if (args[0].equalsIgnoreCase("test2")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			float f = Float.parseFloat(args[1]);
			for (DexBlock db : d.getBlocks()) {
				db.setRotation(f, 0f);
			}
		}
		else if (args[0].equalsIgnoreCase("test3")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			DexBlock db = d.getBlocks().get(0);
			Quaternionf zero = new Quaternionf(0f, 0f, 0f, 1f);
			db.getTransformation().setDisplacement(new Vector3f(-0.5f, -0.5f, -0.5f))
				.setLeftRotation(zero).setRightRotation(zero);
			db.updateTransformation();
			db.getEntity().setRotation(Float.parseFloat(args[1]), Float.parseFloat(args[2]));
			float deg = Float.parseFloat(args[1]);
			
			/*Bukkit.broadcastMessage("A");
			new BukkitRunnable() {
				float deg = 0;
				public void run() {
					double rad = Math.toRadians(deg) / 4;
					double a = Math.cos(rad);
					Vector v = new Vector(1, 0, 0).normalize();
					v.multiply(Math.sin(rad));
					
					Quaternionf ql = new Quaternionf(v.getX(), v.getY(), v.getZ(), a);
					Quaternionf qr = new Quaternionf(-v.getX(), -v.getY(), -v.getZ(), a);
					db.getTransformation().setLeftRotation(ql).setRightRotation(ql);
					
					db.updateTransformation();
					deg++;
					//if (deg > 360) this.cancel();
				}
			}.runTaskTimer(plugin, 0, 1l);*/
		}
		else if (args[0].equalsIgnoreCase("animtest")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			d.getAnimations().clear();
			Vector delta = new Vector(0, 0, 20);
			Animation a1 = new LinearTranslationAnimation(d, plugin, 30, delta);
			Animation a2 = new LinearTranslationAnimation(d, plugin, 30, delta.clone().multiply(-1));
			a1.getSubsequentAnimations().add(a2);
			a2.getSubsequentAnimations().add(a1);
			d.getAnimations().add(a1);
			a1.start();
		}
		else if (args[0].equalsIgnoreCase("animtest2")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			d.getAnimations().clear();
			RideAnimation r = new RideAnimation(d, plugin);
			r.setSeatOffset(new Vector(0, -4.5, -0.5));
			r.setSpeed(5);
			r.mount(p);
			r.start();
		}
		
		else if (args[0].equalsIgnoreCase("sel") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("select") || args[0].equalsIgnoreCase("pos1") || args[0].equalsIgnoreCase("pos2") || args[0].equalsIgnoreCase("load")) {
			int index = -1;
			if (args[0].equalsIgnoreCase("pos1")) index = 0;
			else if (args[0].equalsIgnoreCase("pos2")) index = 1;
			
			if (index < 0) {
				if (args.length == 1) {
					p.sendMessage("§4Usage: §c/dex sel <name>");
					return true;
				}
			}
			if (def != null) {
				DexterityDisplay disp = plugin.getDisplay(def);
				if (disp != null) {
					session.setSelected(disp);
					p.sendMessage(cc + "Selected " + cc2 + def + cc + "!");
					return true;
				} else if (index < 0) {
					p.sendMessage("§4Error: §cCould not find display '" + def + "'");
					return true;
				}
			}

			Location loc = p.getLocation();
			
			if (args.length == 4) {
				try {
					double x = Double.parseDouble(args[1]);
					double y = Double.parseDouble(args[2]);
					double z = Double.parseDouble(args[3]);
					loc = new Location(p.getWorld(), x, y, z, 0, 0);
					index = -1;
				} catch (Exception ex) {
					p.sendMessage("§4Error: §cx, y, and z must be numbers!");
					return true;
				}
			}
			session.setLocation(loc, index == 0);
		}
		
		else if (args[0].equalsIgnoreCase("desel") || args[0].equalsIgnoreCase("deselect") || args[0].equalsIgnoreCase("clear")) {
			if (session.getSelected() != null) {
				session.setSelected(null);
				p.sendMessage(cc + "Cleared selection!");
			}
		}
		
		else if (args[0].equalsIgnoreCase("glow")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			if (args[1].equalsIgnoreCase("none") || args[1].equalsIgnoreCase("off")) {
				d.setGlow(null);
				p.sendMessage(cc + "Disabled glow for " + cc2 + d.getLabel() + cc + "!");
				return true;
			}
			ColorEnum c;
			try {
				c = ColorEnum.valueOf(args[1].toUpperCase());
			} catch (Exception ex) {
				p.sendMessage("§4Error: §cUnknown color '" + args[1].toUpperCase() + "'!");
				return true;
			}
			d.setGlow(c.getColor());
			p.sendMessage(cc + "Set the glow for " + cc2 + d.getLabel() + cc + "!");
		}
		
		else if (args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("a")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			if (args.length == 1) {
				//TODO
			}
			else if (args[1].equalsIgnoreCase("start") || args[1].equalsIgnoreCase("unpause")) {
				d.startAnimations();
				p.sendMessage(cc + "Started animations on " + cc2 + d.getLabel() + cc + "!");
			}
			else if (args[1].equalsIgnoreCase("pause") || args[1].equalsIgnoreCase("stop")) {
				d.stopAnimations(args[1].equalsIgnoreCase("pause"));
				p.sendMessage(cc + "Stopped animations on " + cc2 + d.getLabel() + cc + "!");
			}
			else if (args[1].equalsIgnoreCase("reset")) {
				
			}
			else if (args[1].equalsIgnoreCase("edit")) {
				session.openAnimationEditor();
			}
			else {
				p.sendMessage("§cUnknown sub-command!");
			}
		}
		
		else if (args[0].equalsIgnoreCase("convert") || args[0].equalsIgnoreCase("conv")) {
			if (session.getLocation1() != null && session.getLocation2() != null) {
				DexterityDisplay d = plugin.getAPI().createDisplay(session.getLocation1(), session.getLocation2());
				
				session.setSelected(d);
				
				session.clearLocationSelection();
				
				p.sendMessage(cc + "Created a new display: " + cc2 + d.getLabel() + cc + "!");
				
			} else p.sendMessage("§4Error: §cBoth locations must be set!");
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
			if (d.setLabel(args[1])) p.sendMessage(cc + "Renamed this display to " + cc2 + args[1]);
			else p.sendMessage("§4Error: §cThis name is already in use!");
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
				plane = Plane.valueOf(attr_str.get("plane").toUpperCase());
				if (plane == null) {
					p.sendMessage("§4Error: §cValid planes are XZ, XY, and ZY.");
					return true;
				}
			}
			
			d.rotate(degrees, plane);
			p.sendMessage(cc + "Rotated " + cc2 + d.getLabel() + cc + " by " + degrees + " degrees!");
			
		}
		else if (args[0].equalsIgnoreCase("info")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			p.sendMessage(cc + "Selected " + cc2 + d.getLabel());
			p.sendMessage(cc + "Parent: " + cc2 + (d.getParent() == null ? "[None]" : d.getParent().getLabel()));
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
			session.setSelected(null);
			String label2 = d.getLabel() == null ? "at " + cc2 + DexUtils.locationString(d.getCenter(), 0) : cc2 + d.getLabel();
			p.sendMessage(cc + (res ? "Restored" : "Removed") + " the display " + label2);
		}
		
		else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("lsit")) {
			if (plugin.getDisplays().size() == 0) {
				p.sendMessage("§cThere are no displays set up!");
				return true;
			}
			
			int page = 0;
			if (attrs.containsKey("page")) {
				page = Math.max(attrs.get("page") - 1, 0);
			} else if (args.length >= 2) page = Math.max(DexUtils.parseInt(args[1]) - 1, 0);			
			int maxpage = DexUtils.maxPage(plugin.getDisplays().size(), 10);
			if (page >= maxpage) page = maxpage - 1;
			
			int total = 0;
			for (DexterityDisplay d : plugin.getDisplays()) total += d.getGroupSize();
			
			p.sendMessage("§b");
			p.sendMessage(cc + "§lDisplay list: §6Page §6§l" + (page+1) + "§6/" + maxpage);
			String[] strs = new String[total];
			int i = 0;
			for (DexterityDisplay disp : plugin.getDisplays()) {
				i += constructList(strs, disp, session.getSelected() == null ? null : session.getSelected().getLabel(), i, 0);
			}
			DexUtils.paginate(p, strs, page, 10);
			
		}
		
		else if (args[0].equalsIgnoreCase("scale")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			if (attrs.containsKey("x") || attrs.containsKey("y") || attrs.containsKey("z")) {
				HashMap<String, Double> attrsd = DexUtils.getAttributesDoubles(args);
				float sx = attrsd.getOrDefault("x", d.getScaleX()).floatValue();
				float sy = attrsd.getOrDefault("y", d.getScaleY()).floatValue();
				float sz = attrsd.getOrDefault("z", d.getScaleZ()).floatValue();
				
				d.setScale(sx, sy, sz);
				p.sendMessage(cc + "Set " + cc2 + d.getLabel() + cc + " scale to " + cc2 + sx + ", " + sy + ", " + sz + cc + "!");
			} else {
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
		}
		else if (args[0].equalsIgnoreCase("merge")) {
			if (def == null) {
				p.sendMessage("§4Usage: §c/dex merge <sub-display>");
				return true;
			}
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			DexterityDisplay parent = plugin.getDisplay(def);
			if (parent == null) {
				p.sendMessage("§4Error: §cCould not find display '" + def + "'");
				return true;
			}
			String new_group = attr_str.get("new_group");
			if (d == parent || d.getLabel().equals(parent.getLabel())) {
				p.sendMessage("§4Error: §cMust be a different display than selected!");
				return true;
			}
			if (!d.getCenter().getWorld().getName().equals(parent.getCenter().getWorld().getName())) {
				p.sendMessage("§4Error: §cDisplays must be in the same world!");
				return true;
			}
			if (d.getParent() != null) {
				p.sendMessage("§4Error: §cCannot merge two sub-groups, unmerge first!");
				return true;
			}
			if (d.containsSubdisplay(parent)) {
				p.sendMessage("§4Error: §cThis display has already been merged with '" + d.getLabel() + "'!");
				return true;
			}
			if (new_group != null && plugin.getDisplayLabels().contains(new_group)) {
				p.sendMessage("§4Error: §cA group with this name already exists!");
				return true;
			}

			DexterityDisplay g = d.merge(parent, new_group);
			if (g != null) {
				session.setSelected(g);
				if (new_group == null) p.sendMessage(cc + "Successfully merged " + cc2 + parent.getLabel() + cc + "!");
				else p.sendMessage(cc + "Successfully created new group " + cc2 + new_group + cc + "!");
			} else p.sendMessage("§cFailed to merge!");
		}
		else if (args[0].equalsIgnoreCase("unmerge")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			if (d.getParent() == null) {
				p.sendMessage(cc + "Nothing to un-merge, " + cc2 + d.getLabel() + cc + " has no parent display!");
				return true;
			}
			d.unmerge();
			p.sendMessage(cc + "Un-merged " + cc2 + d.getLabel() + cc + "!");
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
		boolean add_labels = false;
		
		if (argsr.length <= 1) {
			for (String s : commands) ret.add(s);
		}
		else if (argsr[0].equalsIgnoreCase("?") || argsr[0].equalsIgnoreCase("help") || argsr[0].equalsIgnoreCase("list")) {
			ret.add("page=");
		}
		else if (argsr[0].equalsIgnoreCase("sel") || argsr[0].equalsIgnoreCase("select") || argsr[0].equalsIgnoreCase("set")) {
			add_labels = true;
		}
		else if (argsr[0].equalsIgnoreCase("remove") || argsr[0].equalsIgnoreCase("restore") || argsr[0].equalsIgnoreCase("deconvert") || argsr[0].equalsIgnoreCase("deconv")) {
			add_labels = true;
		}
		else if (argsr[0].equalsIgnoreCase("scale")) {
			ret.add("x=");
			ret.add("y=");
			ret.add("z=");
		}
		else if (argsr[0].equalsIgnoreCase("merge")) {
			ret.add("new_group=");
			add_labels = true;
		}
		else if (argsr[0].equalsIgnoreCase("glow")) {
			ret.add("none");
			for (ColorEnum c : ColorEnum.values()) ret.add(c.toString());
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
		else if (argsr[0].equalsIgnoreCase("animation") || argsr[0].equalsIgnoreCase("a")) {
			ret.add("start");
			ret.add("stop");
			ret.add("pause");
			ret.add("unpause");
			ret.add("reset");
			ret.add("edit");
		}
		else if (argsr[0].equalsIgnoreCase("rotate")) {
			if (!params.contains("plane")) ret.add("plane=");
			else if (params.size() == 2) {
				ret.add("plane=XY");
				ret.add("plane=XZ");
				ret.add("plane=ZY");
			}
		}
		if (add_labels) for (String s : plugin.getDisplayLabels()) ret.add(s);

		return ret;
	}

}
