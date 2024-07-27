package me.c7dev.tensegrity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import me.c7dev.tensegrity.DexSession.EditType;
import me.c7dev.tensegrity.api.DexterityAPI;
import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.transaction.BlockTransaction;
import me.c7dev.tensegrity.transaction.ConvertTransaction;
import me.c7dev.tensegrity.transaction.DeconvertTransaction;
import me.c7dev.tensegrity.transaction.RecenterTransaction;
import me.c7dev.tensegrity.transaction.RemoveTransaction;
import me.c7dev.tensegrity.transaction.RotationTransaction;
import me.c7dev.tensegrity.transaction.ScaleTransaction;
import me.c7dev.tensegrity.transaction.Transaction;
import me.c7dev.tensegrity.util.ClickedBlockDisplay;
import me.c7dev.tensegrity.util.ColorEnum;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexBlockState;
import me.c7dev.tensegrity.util.DexTransformation;
import me.c7dev.tensegrity.util.DexUtils;
import me.c7dev.tensegrity.util.RotationPlan;

public class DexterityCommand implements CommandExecutor, TabCompleter {
	
	private Dexterity plugin;
	private DexterityAPI api;
	String noperm, cc, cc2, cc3, usage_format, selected_str;
	
	private String[] commands = {
		"align", "clone", "convert", "deconvert", "deselect", "glow", "highlight", "list", "mask", "merge", "move", 
		"name", "pos1", "recenter", "redo", "remove", "replace", "rotate", "scale", "select", "undo", "unmerge", "unsave", "wand"
	};
	private String[] descriptions = new String[commands.length];
	private String[] command_strs = new String[commands.length];
	
	public DexterityCommand(Dexterity plugin) {
		this.plugin= plugin;
		cc = plugin.getChatColor();
		cc2 = plugin.getChatColor2();
		cc3 = plugin.getChatColor3();
		api = plugin.api();
		plugin.getCommand("dex").setExecutor(this);
		noperm = plugin.getConfigString("no-permission");
		usage_format = plugin.getConfigString("usage-format");
		selected_str = plugin.getConfigString("selected");
		
		for (int i = 0; i < commands.length; i++) {
			descriptions[i] = plugin.getConfigString(commands[i] + "-description");
			command_strs[i] = cc2 + "- /d " + commands[i] + " §8- " + cc + descriptions[i];
		}
	}
	
	public boolean withPermission(Player p, String perm) {
		if (p.hasPermission("dexterity.command." + perm)) return true;
		else {
			p.sendMessage(noperm);
			return false;
		}
	}
	
	public DexterityDisplay getSelected(DexSession session, String perm) {
		if (perm != null) {
			if (!session.getPlayer().hasPermission("dexterity.command." + perm)) {
				session.getPlayer().sendMessage(noperm);
				return null;
			}
		}
		DexterityDisplay d = session.getSelected();
		if (d == null) {
			session.getPlayer().sendMessage(plugin.getConfigString("must-select-display"));
			return null;
		}
		return d;
	}
	
	public boolean testInEdit(DexSession session) {
		if (session.getEditType() != null) {
			session.getPlayer().sendMessage(plugin.getConfigString("must-finish-edit"));
			return true;
		}
		return false;
	}
	
	public int constructList(String[] strs, DexterityDisplay disp, String selected, int i, int level) {
		if (disp.getLabel() == null) return 0;
		int count = 1;
		
		String line = "";
		for (int j = 0; j < level; j++) line += "  ";
		
		line += disp.getLabel();
		if (disp.getBlocks().size() > 0) line = cc2 + ((selected != null && disp.getLabel().equals(selected)) ? "§d" : "") + line + "§7: " + cc + DexUtils.locationString(disp.getCenter(), 0) + " (" + disp.getCenter().getWorld().getName() + ")";
		else line = cc2 + ((selected != null && disp.getLabel().equals(selected)) ? "§d" : "") + "§l" + line + cc + ":";
		
		strs[i] = line;
		
		for (int j = 1; j <= disp.getSubdisplays().size(); j++) {
			count += constructList(strs, disp.getSubdisplays().get(j-1), selected, i+count, level+1);
		}
		
		return count;
	}
	
	public String getUsage(String command) {
		String usage = plugin.getConfigString(command + "-usage");
		return usage_format.replaceAll("\\Q%usage%\\E", usage);
	}
	
	public String getConfigString(String dir, DexSession session) {
		String s = plugin.getConfigString(dir);
		if (session != null && session.getSelected() != null && session.getSelected().getLabel() != null) {
			String label = cc2 + session.getSelected().getLabel() + cc;
			s = s.replaceAll("\\Q%label%\\E", label).replaceAll("\\Q%loclabel%\\E", label); //regex substr selector isn't working, idk
		} else {
			s = s.replaceAll("\\Q%label%\\E", selected_str);
			if (session != null && session.getSelected() != null) s = s.replaceAll("\\Q%loclabel%", "at " + cc2 + DexUtils.locationString(session.getSelected().getCenter(), 0));
			else s = s.replaceAll("\\Q%loclabel%\\E", "");
		}
		return s;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) return true;
		
		Player p = (Player) sender;
		
		if (args.length == 0) {
			p.sendMessage(cc + "§lUsing §x§E§3§5§1§2§2§lD§x§E§7§6§1§1§E§le§x§E§A§7§1§1§A§lx§x§E§E§8§0§1§5§lt§x§F§1§9§0§1§1§le§x§F§5§A§0§0§D§lr§x§F§8§B§0§0§9§li§x§F§C§B§F§0§4§lt§x§F§F§C§F§0§0§ly" + cc + " (v" + plugin.getDescription().getVersion() + ")");
			if (p.hasPermission("dexterity.command")) {
				p.sendMessage(plugin.getConfigString("get-started"));
			}
			return true;
		}
		
		if (!p.hasPermission("dexterity.command")) {
			p.sendMessage(noperm);
			return true;
		}
		
		DexSession session = plugin.getEditSession(p.getUniqueId());
		
		HashMap<String,Integer> attrs = DexUtils.getAttributes(args);
		HashMap<String,String> attr_str = DexUtils.getAttributesStrings(args);
		List<String> flags = DexUtils.getFlags(args);
		List<String> defs = DexUtils.getDefaultAttributes(args);
		String def = defs.size() > 0 ? defs.get(0) : null;

		if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
			int page = 0;
			if (attrs.containsKey("page")) {
				page = Math.max(attrs.get("page") - 1, 0);
			} else if (args.length >= 2) page = Math.max(DexUtils.parseInt(args[1]) - 1, 0);
			int maxpage = DexUtils.maxPage(commands.length, 5);
			if (page >= maxpage) page = maxpage - 1;
						
			p.sendMessage(plugin.getConfigString("help-page-header").replaceAll("\\Q%page%\\E", "" + (page+1)).replaceAll("\\Q%maxpage%\\E", "" + maxpage));
			DexUtils.paginate(p, command_strs, page, 5);
		}

		else if (args[0].equalsIgnoreCase("wand")) {
			ItemStack wand = new ItemStack(Material.BLAZE_ROD);
			ItemMeta meta = wand.getItemMeta();
			meta.setDisplayName(plugin.getConfigString("wand-title", "§fDexterity Wand"));
			wand.setItemMeta(meta);
			p.getInventory().addItem(wand);
			
		}
		
//		else if (args[0].equalsIgnoreCase("animtest")) {
//			DexterityDisplay d = getSelected(session);
//			if (d == null) return true;
//			d.getAnimations().clear();
//			Vector delta = new Vector(0, 0, 20);
//			Animation a1 = new LinearTranslationAnimation(d, plugin, 30, delta);
//			Animation a2 = new LinearTranslationAnimation(d, plugin, 30, delta.clone().multiply(-1));
//			a1.getSubsequentAnimations().add(a2);
//			a2.getSubsequentAnimations().add(a1);
//			d.getAnimations().add(a1);
//			a1.start();
//		}
//		else if (args[0].equalsIgnoreCase("animtest2")) {
//			DexterityDisplay d = getSelected(session, null);
//			if (d == null) return true;
//			d.getAnimations().clear();
//			RideAnimation r = new RideAnimation(d, plugin);
//			//r.setSeatOffset(new Vector(0, -4.5, -0.5));
//			//r.setSeatOffset(new Vector(0, -0.7, 0));
//			r.setSpeed(10);
//			r.setLookingMode(LookMode.YAW_ONLY);
//			r.mount(p);
//			r.start();
//		}
		else if (args[0].equalsIgnoreCase("debug:centers") && p.hasPermission("dexterity.admin")) {
			DexterityDisplay d = getSelected(session, null);
			if (d == null) return true;
			for (DexBlock db : d.getBlocks()) {
				api.markerPoint(db.getLocation(), Math.abs(db.getRoll()) < 0.000001 ? Color.LIME : Color.AQUA, 6);
			}
		}
		else if (args[0].equalsIgnoreCase("debug:removetransformation") && p.hasPermission("dexterity.admin")) {
			DexterityDisplay d = getSelected(session, null);
			if (d == null) return true;
			for (DexBlock db : d.getBlocks()) {
				db.getEntity().setTransformation(new DexTransformation(db.getEntity().getTransformation()).setDisplacement(new Vector(0, 0, 0)).setRollOffset(new Vector(0, 0, 0)).build());
				api.markerPoint(db.getEntity().getLocation(), Color.AQUA, 2);
			}
		}
		else if (args[0].equalsIgnoreCase("debug:testnear") && p.hasPermission("dexterity.admin")) {
			ClickedBlockDisplay b = api.getLookingAt(p);
			if (b == null) p.sendMessage(cc + "None in range");
			else {
				api.markerPoint(b.getClickLocation(), Color.RED, 4);
				p.sendMessage(cc + "Clicked " + b.getBlockFace() + ", " + b.getBlockDisplay().getBlock().getMaterial());
			}
		}
		
		else if (args[0].equalsIgnoreCase("set")) {
			if (session.getEditType() != null) {
				switch(session.getEditType()) {
				case CLONE_MERGE:
					if (session.getSecondary() != null) {
						session.getSecondary().hardMerge(session.getSelected());
					}
				case CLONE:
					p.sendMessage(getConfigString("clone-success", session));
					break;
				default:
				}
				session.finishEdit();
			}
		}
		
		else if (args[0].equalsIgnoreCase("recenter")) { //TODO add -auto to recalculate
			DexterityDisplay d = getSelected(session, "recenter");
			if (d == null) return true;
			
			if (!d.getCenter().getWorld().getName().equals(p.getLocation().getWorld().getName())) {
				p.sendMessage(getConfigString("must-same-world", session));
				return true;
			}
			
			Location loc = p.getLocation();
			if (!flags.contains("continuous")) DexUtils.blockLoc(loc).add(0.5, 0.5, 0.5);
			
			RecenterTransaction t = new RecenterTransaction(d);
			t.commit(loc);
			
			d.setCenter(loc);
			api.markerPoint(loc, Color.AQUA, 4);
			
			session.pushTransaction(t);
			
			p.sendMessage(getConfigString("recenter-success", session));
			
		}
		
		//TODO: solidify/unsolidify command to add barriers
		
		else if (args[0].equalsIgnoreCase("align")) {
			DexterityDisplay d = getSelected(session, "move");
			if (d == null) return true;
			
			BlockTransaction t = new BlockTransaction(d.getBlocks());
			d.align();
			t.commit(d.getBlocks());
			session.pushTransaction(t);
			
			p.sendMessage(getConfigString("align-success", session));
		}
		
		else if (args[0].equalsIgnoreCase("replace") || args[0].equalsIgnoreCase("rep")) { //TODO add to= from=
			DexterityDisplay d = getSelected(session, "replace");
			if (d == null) return true;
			
			if (args.length >= 3) {
				Material from, to;
				try {
					from = Material.valueOf(args[1].toUpperCase());
				} catch (Exception ex) {
					p.sendMessage(getConfigString("unknown-material", session).replaceAll("\\Q%input%\\E", args[1].toLowerCase()));
					return true;
				}
				try {
					to = Material.valueOf(args[2].toUpperCase());
				} catch (Exception ex) {
					p.sendMessage(getConfigString("unknown-material", session).replaceAll("\\Q%input%\\E", args[2].toLowerCase()));
					return true;
				}
				
				BlockTransaction t = new BlockTransaction(d.getBlocks(), from);
				if (to == Material.AIR) {
					List<DexBlock> remove = new ArrayList<>();
					for (DexBlock db : d.getBlocks()) {
						if (db.getEntity().getBlock().getMaterial() == from) {
							t.commitBlock(db);
							remove.add(db);
						}
					}
					for (DexBlock db : remove) {
						db.remove();
					}
				} else {
					BlockData todata = Bukkit.createBlockData(to);
					for (DexBlock db : d.getBlocks()) {
						if (db.getEntity().getBlock().getMaterial() == from) {
							t.commitBlock(db);
							db.getEntity().getBlock().copyTo(todata);
							db.getEntity().setBlock(todata);
						}
					}
				}
				
				if (t.isCommitted()) session.pushTransaction(t);
				
				p.sendMessage(getConfigString("replace-success", session)
						.replaceAll("\\Q%from%\\E", from.toString().toLowerCase())
						.replaceAll("\\Q%to%\\E", to.toString().toLowerCase()));
				
			} else p.sendMessage(getUsage("replace"));
		}
		
		else if (args[0].equalsIgnoreCase("sel") || args[0].equalsIgnoreCase("select") || args[0].equalsIgnoreCase("pos1") || args[0].equalsIgnoreCase("pos2") || args[0].equalsIgnoreCase("load")) {
			
			int index = -1;
			if (args[0].equalsIgnoreCase("pos1")) index = 0;
			else if (args[0].equalsIgnoreCase("pos2")) index = 1;
			
			if (index < 0) {
				if (args.length == 1) {
					p.sendMessage(getUsage("sel"));
					return true;
				}
			}
			if (def != null) {
				DexterityDisplay disp = plugin.getDisplay(def);
				if (disp != null) {					
					session.setSelected(disp, true);
					return true;
				} else if (index < 0) {
					p.sendMessage(plugin.getConfigString("display-not-found").replaceAll("\\Q%input%\\E", def));
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
					p.sendMessage(plugin.getConfigString("must-send-numbers-xyz"));
					return true;
				}
			}
			session.setLocation(loc, index == 0);
		}
		
		else if (args[0].equalsIgnoreCase("desel") || args[0].equalsIgnoreCase("deselect") || args[0].equalsIgnoreCase("clear")) {
			if (session.getSelected() != null) {
				session.setSelected(null, false);
				session.clearLocationSelection();
				p.sendMessage(plugin.getConfigString("desel-success"));
			}
		}
		
		else if (args[0].equalsIgnoreCase("highlight") || args[0].equalsIgnoreCase("h")) {
			DexterityDisplay d = getSelected(session, null);
			if (d == null) return true;
			api.tempHighlight(d, 50, Color.ORANGE);
		}
		
		else if (args[0].equalsIgnoreCase("clone")) {
			DexterityDisplay d = getSelected(session, "clone");
			if (d == null) return true;
			if (session.getSecondary() != null) {
				p.sendMessage(getConfigString("must-finish-edit", session));
				return true;
			}
			boolean mergeafter = flags.contains("merge");
			if (mergeafter && !d.canHardMerge()) {
				p.sendMessage(getConfigString("cannot-clone", session));
				return true;
			}
			
			p.sendMessage(getConfigString("to-finish-edit", session));
			
			DexterityDisplay clone = new DexterityDisplay(plugin, d.getCenter(), d.getScale().clone());
//			clone.setBaseRotation((float) d.getYaw(), (float) d.getPitch(), 0f); //TODO
			
			//start clone
			List<DexBlock> blocks = new ArrayList<>();
			for (DexBlock db : d.getBlocks()) {
				DexBlockState state = db.getState();
				state.setDisplay(clone);
				blocks.add(new DexBlock(state));
			}
			clone.setEntities(blocks, false);
			
			session.startEdit(clone, mergeafter ? EditType.CLONE_MERGE : EditType.CLONE, true);
			
			if (!flags.contains("nofollow")) session.startFollowing();
			
		}
		
		else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("quit")) {
			DexterityDisplay d = getSelected(session, null);
			if (d == null) return true;
			session.cancelEdit();
			p.sendMessage(getConfigString("cancelled-edit", session));
		}
		
		else if (args[0].equalsIgnoreCase("glow")) { //TODO add transaction
			DexterityDisplay d = getSelected(session, "glow");
			if (d == null) return true;
			boolean propegate = false; //flags.contains("propegate");
			if (args[1].equalsIgnoreCase("none") || args[1].equalsIgnoreCase("off") || flags.contains("none") || flags.contains("off")) {
				d.setGlow(null, propegate);
				p.sendMessage(getConfigString("glow-success-disable", session));
				return true;
			}
			ColorEnum c;
			try {
				c = ColorEnum.valueOf(args[1].toUpperCase());
			} catch (Exception ex) {
				p.sendMessage(getConfigString("unknown-color", session).replaceAll("\\Q%input%\\E", args[1].toUpperCase()));
				return true;
			}
			d.setGlow(c.getColor(), propegate);
			if (d.getLabel() != null) p.sendMessage(getConfigString("glow-success", session));
		}
		
		else if (args[0].equalsIgnoreCase("convert") || args[0].equalsIgnoreCase("conv")) {
			if (!withPermission(p, "convert") || testInEdit(session)) return true;
			Location l1 = session.getLocation1(), l2 = session.getLocation2();
			if (l1 != null && l2 != null) {
				
				if (!l1.getWorld().getName().equals(l2.getWorld().getName())) {
					p.sendMessage(getConfigString("must-same-world-points", session));
					return true;
				}
				if (session.getSelectionVolume() > plugin.getMaxVolume()) {
					p.sendMessage(getConfigString("exceeds-max-volume", session).replaceAll("\\Q%volume%\\E", "" + plugin.getMaxVolume()));
					return true;
				}
				
				ConvertTransaction t = new ConvertTransaction();
				DexterityDisplay d = api.convertBlocks(l1, l2, t);
				
				session.setSelected(d, false);
				session.pushTransaction(t);
								
				//p.sendMessage(cc + "Created a new display: " + cc2 + d.getLabel() + cc + "!");
				p.sendMessage(getConfigString("convert-success", session));
				
			} else p.sendMessage(getConfigString("need-locations", session));
		}
		
		else if (args[0].equalsIgnoreCase("move") || args[0].equalsIgnoreCase("m")) { //TODO check if in edit session, change displacement vector
			
			DexterityDisplay d = getSelected(session, "move");
			if (d == null) return true;
			
			if (args.length == 1) {
				session.startFollowing();
				session.startEdit(d, EditType.TRANSLATE, false);
				p.sendMessage(getConfigString("to-finish-edit", session));
				return true;
			}
			
			BlockTransaction t = new BlockTransaction(d.getBlocks());
			Location loc;
			if (flags.contains("continuous") || flags.contains("c") || flags.contains("here")) {
				if (flags.contains("continuous") || flags.contains("c")) loc = p.getLocation();
				else loc = DexUtils.blockLoc(p.getLocation()).add(0.5, 0.5, 0.5);
			}
			else loc = d.getCenter();
						
			HashMap<String,Double> attr_d = DexUtils.getAttributesDoubles(args);
//			if (defs.contains("west")) loc.add(-1, 0, 0); //alias will not pick these up
			if (attr_d.containsKey("x")) loc.add(attr_d.get("x"), 0, 0);
			
//			if (defs.contains("down")) loc.add(0, -1, 0);
			if (attr_d.containsKey("y")) loc.add(0, attr_d.get("y"), 0);
			
//			if (defs.contains("south")) loc.add(0, 0, -1);
			if (attr_d.containsKey("z")) loc.add(0, 0, attr_d.get("z"));
			
						
			d.teleport(loc);
			
			t.commit(d.getBlocks());
			session.pushTransaction(t);
			
			if (session.getFollowingOffset() != null) {
				Location loc2 = p.getLocation();
				if (!p.isSneaking()) DexUtils.blockLoc(loc2);
				session.setFollowingOffset(d.getCenter().toVector().subtract(loc2.toVector()));
			}
			
		}
		else if (args[0].equalsIgnoreCase("label") || args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("rename") || args[0].equalsIgnoreCase("save")) {
			DexterityDisplay d = getSelected(session, "save");
			if (d == null) return true;
			
			if (args.length != 2) {
				if (args[0].equalsIgnoreCase("save")) p.sendMessage(getUsage("rename").replaceAll(" name", " save"));
				else p.sendMessage(getUsage("rename"));
				return true;
			}
			if (args[1].startsWith("-")) {
				p.sendMessage(plugin.getConfigString("invalid-name").replaceAll("\\Q%input%\\E", args[1]));
				return true;
			}
			if (d.setLabel(args[1])) p.sendMessage(getConfigString("rename-success", session));
			else p.sendMessage(getConfigString("name-in-use", session).replaceAll("\\Q%input%\\E", args[1]));
		}
		
		else if (args[0].equalsIgnoreCase("undo") || args[0].equalsIgnoreCase("u")) {
			int count = attrs.getOrDefault("count", -1);
			if (count < 2) session.undo();
			else session.undo(count);
		}
		else if (args[0].equalsIgnoreCase("redo")) {
			int count = attrs.getOrDefault("count", -1);
			if (count < 2) session.redo();
			else session.redo(count);
		}
		
		else if (args[0].equalsIgnoreCase("unsave")) {
			DexterityDisplay d;
			if (def != null) {
				d = plugin.getDisplay(def.toLowerCase());
				if (d == null) {
					p.sendMessage(getConfigString("display-not-found", session).replaceAll("\\Q%input%\\E", def));
					return true;
				}
			} else {
				d = getSelected(session, "unsave");
				if (d == null) return true;
				if (!d.isListed()) {
					p.sendMessage(getConfigString("not-saved", session));
					return true;
				}
			}
			
			
			String msg = getConfigString("unsave-success", session);
			d.unregister();
			p.sendMessage(msg);
		}
		
		else if (args[0].equalsIgnoreCase("mask")) {
			if (args.length < 2) p.sendMessage(getUsage("mask"));
			if (flags.contains("none") || flags.contains("off") || def.equalsIgnoreCase("none") || def.equalsIgnoreCase("off")) {
				session.setMask(null);
				p.sendMessage(getConfigString("mask-success-disable", session));
			} else {
				Material mat;
				try {
					mat = Material.valueOf(def.toUpperCase());
				} catch (Exception ex) {
					p.sendMessage(getConfigString("unknown-material", session).replaceAll("\\Q%input%\\E", args[1].toLowerCase()));
					return true;
				}
				session.setMask(mat);
				
				p.sendMessage(getConfigString("mask-success", session).replaceAll("\\Q%input%\\E", mat.toString().toLowerCase()));;
			}
		}
		
		else if (args[0].equalsIgnoreCase("rotate") || args[0].equalsIgnoreCase("r")){
			DexterityDisplay d = getSelected(session, "rotate");
			if (d == null) return true;
			
			if (args.length < 2) {
				p.sendMessage(getUsage("rotate"));
				return true;
			}
			
			RotationPlan plan = new RotationPlan();
			boolean set = flags.contains("set");
			if (flags.contains("reset")) {
				plan.reset = true;
				set = true;
			}
			HashMap<String, Double> attrs_d = DexUtils.getAttributesDoubles(args);
			
			plan.yaw_deg = attrs_d.getOrDefault("yaw", Double.MAX_VALUE);
			plan.pitch_deg = attrs_d.getOrDefault("pitch", Double.MAX_VALUE);
			plan.roll_deg = attrs_d.getOrDefault("roll", Double.MAX_VALUE);
			plan.y_deg = attrs_d.getOrDefault("y", Double.MAX_VALUE);
			plan.x_deg = attrs_d.getOrDefault("x", Double.MAX_VALUE);
			plan.z_deg = attrs_d.getOrDefault("z", Double.MAX_VALUE);
			
			try {
				switch(Math.min(defs.size(), 6)) {
				case 6:
					if (plan.z_deg == Double.MAX_VALUE) plan.z_deg = Double.parseDouble(defs.get(5));
				case 5:
					if (plan.x_deg == Double.MAX_VALUE) plan.x_deg = Double.parseDouble(defs.get(4));
				case 4: 
					if (plan.yaw_deg == Double.MAX_VALUE) plan.yaw_deg = Double.parseDouble(defs.get(3));
				case 3: 
					if (plan.roll_deg == Double.MAX_VALUE) plan.roll_deg = Double.parseDouble(defs.get(2));
				case 2: 
					if (plan.pitch_deg == Double.MAX_VALUE) plan.pitch_deg = Double.parseDouble(defs.get(1));
				case 1: 
					if (plan.y_deg == Double.MAX_VALUE) plan.y_deg = Double.parseDouble(defs.get(0));
				default:
				}
			} catch (Exception ex) {
				p.sendMessage(getConfigString("must-send-number", session));
				return true;
			}
			
			if (plan.yaw_deg == Double.MAX_VALUE) {
				plan.set_yaw = false;
				plan.yaw_deg = 0;
			} else plan.set_yaw = set;
			if (plan.pitch_deg == Double.MAX_VALUE) {
				plan.set_pitch = false;
				plan.pitch_deg = 0;
			} else plan.set_pitch = set;
			if (plan.roll_deg == Double.MAX_VALUE) {
				plan.set_roll = false;
				plan.roll_deg = 0;
			} else plan.set_roll = set;
			if (plan.y_deg == Double.MAX_VALUE) {
				plan.set_y = false;
				plan.y_deg = 0;
			} else plan.set_y = set;
			if (plan.x_deg == Double.MAX_VALUE) {
				plan.set_x = false;
				plan.x_deg = 0;
			} else plan.set_x = set;
			if (plan.z_deg == Double.MAX_VALUE) {
				plan.set_z = false;
				plan.z_deg = 0;
			} else plan.set_z = set;
									
			RotationTransaction t = new RotationTransaction(d);
			d.getRotationManager(true).setTransaction(t);
			
			if (d.rotate(plan) == null) {
				p.sendMessage(getConfigString("must-send-number", session));
				return true;
			}
			
			//t.commit(); //async, done in callback
			session.pushTransaction(t);
		}
		else if (args[0].equalsIgnoreCase("info")) {
			DexterityDisplay d = getSelected(session, null);
			if (d == null) return true;
			if (d.getLabel() == null) {
				p.sendMessage(cc + "Selected " + cc2 + d.getBlocks().size() + cc + " ghost block" + (d.getBlocks().size() == 1 ? "" : "s") + " in " + cc2 + d.getCenter().getWorld().getName());
			} else {
				p.sendMessage(cc + "Selected " + cc2 + d.getLabel());
				p.sendMessage(cc + "Parent: " + cc2 + (d.getParent() == null ? "[None]" : d.getParent().getLabel()));
			}
		}
		else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("restore") || args[0].equalsIgnoreCase("deconvert") || args[0].equalsIgnoreCase("deconv")) {
			boolean res = !args[0].equalsIgnoreCase("remove");
			if ((res && !withPermission(p, "remove")) || (!res && !withPermission(p, "deconvert"))) return true;
			
			if (testInEdit(session)) return true;
			DexterityDisplay d = session.getSelected();
			if (d == null) {
				if (def == null) return true;
				d = plugin.getDisplay(def);
				if (d == null) {
					p.sendMessage(plugin.getConfigString("display-not-found").replaceAll("\\Q%input%\\E", def));
					return true;
				}
			}
			
			Transaction t;
			if (res) {
				t = new DeconvertTransaction(d);
				DeconvertTransaction dct = (DeconvertTransaction) t;
				for (DexBlock db : d.getBlocks()) {
					
				}
			} else t = new RemoveTransaction(d);
			d.remove(res);
			if (res) p.sendMessage(getConfigString("restore-success", session));
			else p.sendMessage(getConfigString("remove-success", session));
			session.setSelected(null, false);
			session.pushTransaction(t);
		}
		
		else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("lsit")) {
			if (!withPermission(p, "list")) return true;
			if (plugin.getDisplays().size() == 0) {
				p.sendMessage(plugin.getConfigString("no-saved-displays"));
				return true;
			}
			
			int page = 0;
			if (attrs.containsKey("page")) {
				page = Math.max(attrs.get("page") - 1, 0);
			} else if (args.length >= 2) page = Math.max(DexUtils.parseInt(args[1]) - 1, 0);			
			int maxpage = DexUtils.maxPage(plugin.getDisplays().size(), 10);
			if (page >= maxpage) page = maxpage - 1;
			
			int total = 0;
			for (DexterityDisplay d : plugin.getDisplays()) {
				if (d.getLabel() == null) continue;
				total += d.getGroupSize();
			}
			
			p.sendMessage(plugin.getConfigString("list-page-header").replaceAll("\\Q%page%\\E", "" + (page+1)).replaceAll("\\Q%maxpage%\\E", ""+maxpage));
			String[] strs = new String[total];
			int i = 0;
			for (DexterityDisplay disp : plugin.getDisplays()) {
				if (disp.getLabel() == null) continue;
				i += constructList(strs, disp, session.getSelected() == null ? null : session.getSelected().getLabel(), i, 0);
			}
			DexUtils.paginate(p, strs, page, 10);
			
		}
		
		else if (args[0].equalsIgnoreCase("scale") || args[0].equalsIgnoreCase("s")) {
			DexterityDisplay d = getSelected(session, "scale");
			if (d == null) return true;
			
			boolean set = flags.contains("set");
			
			ScaleTransaction t = new ScaleTransaction(d);
			if (attrs.containsKey("x") || attrs.containsKey("y") || attrs.containsKey("z")) {
				HashMap<String, Double> attrsd = DexUtils.getAttributesDoubles(args);
				float sx = Math.abs(attrsd.getOrDefault("x", set ? d.getScale().getX() : 1).floatValue());
				float sy = Math.abs(attrsd.getOrDefault("y", set ? d.getScale().getY() : 1).floatValue());
				float sz = Math.abs(attrsd.getOrDefault("z", set ? d.getScale().getZ() : 1).floatValue());
				
				if (sx == 0 || sy == 0 || sz == 0) {
					p.sendMessage(getConfigString("must-send-number", session));
					return true;
				}
				
				String scale_str = sx + ", " + sy + ", " + sz;
				if (set) {
					d.setScale(new Vector(sx, sy, sz));
					p.sendMessage(getConfigString("scale-success-set", session).replaceAll("\\Q%scale%\\E", scale_str));
				}
				else {
					d.scale(new Vector(sx, sy, sz));
					p.sendMessage(getConfigString("scale-success", session).replaceAll("\\Q%scale%\\E", scale_str));
				}
				
			} else {
				float scale = 1;
				try {
					scale = Float.parseFloat(def);
				} catch(Exception ex) {
					p.sendMessage(getConfigString("must-send-number", session));
					return true;
				}

				if (set) {
					d.setScale(scale);
					p.sendMessage(getConfigString("scale-success-set", session).replaceAll("\\Q%scale%\\E", scale + ""));
				} else {
					d.scale(scale);
					p.sendMessage(getConfigString("scale-success", session).replaceAll("\\Q%scale%\\E", scale + ""));
				}
			}
			t.commit();
			session.pushTransaction(t);
		}
		else if (args[0].equalsIgnoreCase("merge")) {
			if (def == null) {
				p.sendMessage(getUsage("merge"));
				return true;
			}
			DexterityDisplay d = getSelected(session, "merge");
			if (d == null || testInEdit(session)) return true;
			DexterityDisplay parent = plugin.getDisplay(def);
			if (parent == null) {
				p.sendMessage(plugin.getConfigString("display-not-found").replaceAll("\\Q%input%\\E", def));
				return true;
			}
			String new_group = attr_str.get("new_group");
//			if (d.getLabel() == null) {
//				p.sendMessage(plugin.getConfigString("must-save-display"));
//				return true;
//			}
			if (d == parent || d.equals(parent)) {
				p.sendMessage(getConfigString("must-be-different", session));
				return true;
			}
			if (!d.getCenter().getWorld().getName().equals(parent.getCenter().getWorld().getName())) {
				p.sendMessage(getConfigString("must-same-world", session));
				return true;
			}
			if (d.getParent() != null) {
				p.sendMessage(getConfigString("cannot-merge-subgroups", session));
				return true;
			}
			if (d.containsSubdisplay(parent)) {
				p.sendMessage(getConfigString("already-merged", session));
				return true;
			}
			if (new_group != null && plugin.getDisplayLabels().contains(new_group)) {
				p.sendMessage(getConfigString("group-name-in-use", session));
				return true;
			}
			
			boolean hard = true; //flags.contains("hard");
			if (hard) {
				if (!d.canHardMerge() || !parent.canHardMerge()) {
					p.sendMessage(getConfigString("cannot-hard-merge", session));
					return true;
				}
				
				if (parent.hardMerge(d)) {
					session.setSelected(parent, false);
					p.sendMessage(getConfigString("merge-success-hard", session));
				}
				else p.sendMessage(getConfigString("failed-merge", session));
			} else {
				DexterityDisplay g = d.merge(parent, new_group);
				if (g != null) {
					session.setSelected(g, false);
					if (new_group == null) p.sendMessage(getConfigString("merge-success", session).replaceAll("\\Q%parentlabel%\\E", parent.getLabel()));
					else p.sendMessage(getConfigString("merge-success-newgroup", session).replaceAll("\\Q%input%\\E", new_group));
				} else p.sendMessage(getConfigString("failed-merge", session));
			}
		}
		else if (args[0].equalsIgnoreCase("unmerge")) {
			DexterityDisplay d = getSelected(session, "merge");
			if (d == null || testInEdit(session)) return true;
			if (d.getLabel() == null) {
				p.sendMessage(getConfigString("must-save-display", session));
				return true;
			}
			if (d.getParent() == null) {
				p.sendMessage(getConfigString("nothing-to-unmerge", session));
				return true;
			}
			d.unmerge();
			p.sendMessage(getConfigString("unmerge-success", session));
		}
		
		else {
			p.sendMessage(plugin.getConfigString("unknown-subcommand"));
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
		boolean add_labels = false, finalized = false;
		
		if (argsr.length <= 1) {
			for (String s : commands) ret.add(s);
			ret.add("cancel");
			ret.add("set");
		}
		else if (argsr[0].equalsIgnoreCase("?") || argsr[0].equalsIgnoreCase("help") || argsr[0].equalsIgnoreCase("list")) {
			ret.add("page=");
		}
		else if (argsr[0].equalsIgnoreCase("sel") || argsr[0].equalsIgnoreCase("select")) {
			add_labels = true;
		}
		else if (argsr[0].equalsIgnoreCase("replace") || argsr[0].equalsIgnoreCase("rep")) {
			if (argsr.length <= 3 && argsr[argsr.length - 1].length() >= 2) {
				ret = DexUtils.materials(argsr[argsr.length - 1]);
			}
		}
		else if (argsr[0].equalsIgnoreCase("mask")) {
			if (argsr.length <= 2 && argsr[argsr.length - 1].length() >= 2) {
				ret = DexUtils.materials(argsr[argsr.length - 1]);
				ret.add("-none");
			}
		}
		else if (argsr[0].equalsIgnoreCase("remove") || argsr[0].equalsIgnoreCase("restore") || argsr[0].equalsIgnoreCase("deconvert") || argsr[0].equalsIgnoreCase("deconv")) {
			add_labels = true;
		}
		else if (argsr[0].equalsIgnoreCase("recenter")) {
			ret.add("-continuous");
		}
		else if (argsr[0].equalsIgnoreCase("scale") || argsr[0].equalsIgnoreCase("s")) {
			ret.add("x=");
			ret.add("y=");
			ret.add("z=");
			ret.add("-set");
		}
//		else if (argsr[0].equalsIgnoreCase("merge")) {
//			ret.add("new_group=");
//			ret.add("-hard");
//			add_labels = true;
//		}
		else if (argsr[0].equalsIgnoreCase("glow")) {
			ret.add("-none");
//			ret.add("-propegate");
			for (ColorEnum c : ColorEnum.values()) ret.add(c.toString());
		}
		else if (argsr[0].equalsIgnoreCase("move") || argsr[0].equalsIgnoreCase("m")) {
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
		else if (argsr[0].equalsIgnoreCase("clone")) {
			ret.add("-nofollow");
			ret.add("-merge");
		}
		else if (argsr[0].equalsIgnoreCase("undo") || argsr[0].equalsIgnoreCase("u") || argsr[0].equalsIgnoreCase("redo")) {
			ret.add("count=");
		}
		else if (argsr[0].equalsIgnoreCase("rotate") || argsr[0].equalsIgnoreCase("r")) {
//			if (!params.contains("plane")) ret.add("plane=");
//			else if (params.size() == 2) {
//				ret.add("plane=XY");
//				ret.add("plane=XZ");
//				ret.add("plane=ZY");
//			}
			ret.add("yaw=");
			ret.add("pitch=");
			ret.add("roll=");
			ret.add("y=");
			ret.add("x=");
			ret.add("z=");
			ret.add("-set");
			ret.add("-reset");
		}
		if (add_labels) for (String s : plugin.getDisplayLabels()) ret.add(s);
		return ret;
	}

}
