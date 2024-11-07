package me.c7dev.dexterity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import me.c7dev.dexterity.DexSession.AxisType;
import me.c7dev.dexterity.DexSession.EditType;
import me.c7dev.dexterity.api.DexRotation;
import me.c7dev.dexterity.api.DexterityAPI;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.displays.schematics.Schematic;
import me.c7dev.dexterity.displays.schematics.SchematicBuilder;
import me.c7dev.dexterity.transaction.BlockTransaction;
import me.c7dev.dexterity.transaction.BuildTransaction;
import me.c7dev.dexterity.transaction.ConvertTransaction;
import me.c7dev.dexterity.transaction.DeconvertTransaction;
import me.c7dev.dexterity.transaction.RemoveTransaction;
import me.c7dev.dexterity.transaction.RotationTransaction;
import me.c7dev.dexterity.transaction.ScaleTransaction;
import me.c7dev.dexterity.transaction.Transaction;
import me.c7dev.dexterity.util.ClickedBlockDisplay;
import me.c7dev.dexterity.util.ColorEnum;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexTransformation;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.DexterityException;
import me.c7dev.dexterity.util.InteractionCommand;
import me.c7dev.dexterity.util.Mask;
import me.c7dev.dexterity.util.RotationPlan;

/**
 * Defines all sub-commands for the /d or /dex in-game command
 */
public class DexterityCommand implements CommandExecutor, TabCompleter {
	
	private Dexterity plugin;
	private DexterityAPI api;
	private String noperm, cc, cc2, usage_format, selected_str, loclabel_prefix;
	
	private String[] commands = {
		"align", "axis", "clone", "command", "consolidate", "convert", "deconvert", "deselect", "glow", "highlight", "info", "list", "mask", 
		"merge", "move", "name", "pos1", "recenter", "redo", "reload", "remove", "replace", "rotate", "scale", "select", "schem", "undo", "unsave", "tile", "wand"
	};
	private String[] descriptions = new String[commands.length];
	private String[] command_strs = new String[commands.length];
	
	public DexterityCommand(Dexterity plugin) {
		this.plugin= plugin;
		cc = plugin.getChatColor();
		cc2 = plugin.getChatColor2();
		api = plugin.api();
		plugin.getCommand("dex").setExecutor(this);
		noperm = plugin.getConfigString("no-permission");
		usage_format = plugin.getConfigString("usage-format");
		selected_str = plugin.getConfigString("selected");
		loclabel_prefix = plugin.getConfigString("loclabel-prefix", "selection at");
		
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
	
	public List<String> listSchematics() {
		List<String> r = new ArrayList<>();
		try {
			File f = new File(plugin.getDataFolder().getAbsolutePath() + "/schematics");
			for (File sub : f.listFiles()) {
				String name = sub.getName().toLowerCase();
				if (name.endsWith(".dex") || name.endsWith(".dexterity")) {
					r.add(name.replaceAll("\\.dexterity|\\.dex", ""));
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return r;
	}
	
	public int constructList(String[] strs, DexterityDisplay disp, String selected, int i, int level) {
		if (disp.getLabel() == null) return 0;
		int count = 1;
		
		String line = "";
		for (int j = 0; j < level; j++) line += "  ";
		
		line += disp.getLabel();
		if (disp.getBlocksCount() > 0) line = cc2 + ((selected != null && disp.getLabel().equals(selected)) ? "§d" : "") + line + "§7: " + cc + DexUtils.locationString(disp.getCenter(), 0) + " (" + disp.getCenter().getWorld().getName() + ")";
		else line = cc2 + ((selected != null && disp.getLabel().equals(selected)) ? "§d" : "") + "§l" + line + cc + ":";
		
		strs[i] = line;
		
		DexterityDisplay[] subs = disp.getSubdisplays();
		for (int j = 1; j <= subs.length; j++) {
			count += constructList(strs, subs[j-1], selected, i+count, level+1);
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
			if (session != null && session.getSelected() != null) s = s.replaceAll("\\Q%loclabel%", loclabel_prefix + " " + cc2 + DexUtils.locationString(session.getSelected().getCenter(), 0) + cc);
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
		
		args[0] = args[0].toLowerCase();
		DexSession session = plugin.getEditSession(p.getUniqueId());
		
		HashMap<String,Integer> attrs = DexUtils.getAttributes(args);
		HashMap<String,String> attr_str = DexUtils.getAttributesStrings(args);
		List<String> flags = DexUtils.getFlags(args);
		List<String> defs = DexUtils.getDefaultAttributes(args);
		String def = defs.size() > 0 ? defs.get(0) : null;

		if (args[0].equals("help") || args[0].equals("?")) {
			int page = 0;
			if (attrs.containsKey("page")) {
				page = Math.max(attrs.get("page") - 1, 0);
			} else if (args.length >= 2) page = Math.max(DexUtils.parseInt(args[1]) - 1, 0);
			int maxpage = DexUtils.maxPage(commands.length, 5);
			if (page >= maxpage) page = maxpage - 1;

			p.sendMessage(plugin.getConfigString("help-page-header").replaceAll("\\Q%page%\\E", "" + (page+1)).replaceAll("\\Q%maxpage%\\E", "" + maxpage));
			DexUtils.paginate(p, command_strs, page, 5);
		}
		
		else if (args[0].equals("wand")) {
			if (!withPermission(p, "wand")) return true;
			ItemStack wand = new ItemStack(Material.BLAZE_ROD);
			ItemMeta meta = wand.getItemMeta();
			meta.setDisplayName(plugin.getConfigString("wand-title", "§fDexterity Wand"));
			wand.setItemMeta(meta);
			p.getInventory().addItem(wand);
		}
		
//		else if (args[0].equals("animtest")) {
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
//		else if (args[0].equals("animtest2")) {
//			DexterityDisplay d = getSelected(session, null);
//			if (d == null) return true;
//			RideAnimation r = new RideAnimation(d);
//			//r.setSeatOffset(new Vector(0, -4.5, -0.5));
//			//r.setSeatOffset(new Vector(0, -0.7, 0));
//			r.setSpeed(5);
//			r.mount(p);
//			r.start();
//		}
//		else if (args[0].equals("animtest3")) {
//			DexterityDisplay d = getSelected(session, null);
//			if (d == null) return true;
////			d.getAnimations().clear();
//			RotationPlan plan = new RotationPlan();
//			plan.yaw_deg = 360;
//			RotationAnimation r = new RotationAnimation(d, 20, plan);
//			d.addAnimation(r);
//			r.start();
//		}
				
		else if (args[0].equals("debug:centers") && p.hasPermission("dexterity.admin")){
			DexterityDisplay d = getSelected(session, null);
			if (d == null) return true;
			boolean entity_centers = flags.contains("entities");
			for (DexBlock db : d.getBlocks()) {
				api.markerPoint(db.getLocation(), Math.abs(db.getRoll()) < 0.000001 ? Color.LIME : Color.AQUA, 6);
				if (entity_centers) api.markerPoint(db.getEntity().getLocation(), Color.ORANGE, 6);
			}
		}
		
		else if (args[0].equals("debug:removetransformation") && p.hasPermission("dexterity.admin")) {
			DexterityDisplay d = getSelected(session, null);
			if (d == null) return true;
			for (DexBlock db : d.getBlocks()) {
				db.getEntity().setTransformation(new DexTransformation(db.getEntity().getTransformation()).setDisplacement(new Vector(0, 0, 0)).setRollOffset(new Vector(0, 0, 0)).build());
				api.markerPoint(db.getEntity().getLocation(), Color.AQUA, 2);
			}
		}
		else if (args[0].equals("debug:testnear") && p.hasPermission("dexterity.admin")) {
			ClickedBlockDisplay b = api.getLookingAt(p);
			if (b == null) p.sendMessage(cc + "None in range");
			else {
				api.markerPoint(b.getClickLocation(), Color.RED, 4);
				p.sendMessage(cc + "Clicked " + b.getBlockFace() + ", " + b.getBlockDisplay().getBlock().getMaterial());
			}
		}
		
		else if (args[0].equals("paste") || args[0].equals("set") || args[0].equals("p")) {
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
		
		else if (args[0].equals("consolidate")) {
			DexterityDisplay d = getSelected(session, "consolidate");
			if (d == null) return true;
			
			Mask mask = session.getMask();
			
			if (def != null) {
				try {
					Material mat = Material.valueOf(def.toUpperCase().trim());
					mask = new Mask(mat);
				} catch (Exception ex) {
					p.sendMessage(getConfigString("unknown-material", session).replaceAll("\\Q%input%\\E", def.toLowerCase()));
					return true;
				}
			}
			
			BlockTransaction t = new BlockTransaction(d, mask);
			d.consolidate(mask, t);
			session.pushTransaction(t); //commit is async
			
			p.sendMessage(getConfigString("consolidate-success", session));
		}
		
		else if (args[0].equals("recenter")) { //TODO add -auto to recalculate
			DexterityDisplay d = getSelected(session, "recenter");
			if (d == null) return true;
			
			if (!d.getCenter().getWorld().getName().equals(p.getLocation().getWorld().getName())) {
				p.sendMessage(getConfigString("must-same-world", session));
				return true;
			}
			
			Location loc = p.getLocation();
			if (!flags.contains("continuous")) DexUtils.blockLoc(loc).add(0.5, 0.5, 0.5);
			
//			RecenterTransaction t = new RecenterTransaction(d);
			BlockTransaction t = new BlockTransaction(d);
			t.commitCenter(loc);
			t.commitEmpty();
//			t.commit(loc);
			
			d.setCenter(loc);
			api.markerPoint(loc, Color.AQUA, 4);
			
			session.pushTransaction(t);
			
			p.sendMessage(getConfigString("recenter-success", session));
			
		}
		
		//TODO: solidify/unsolidify command to add barriers
		
		else if (args[0].equals("align")) {
			DexterityDisplay d = getSelected(session, "move");
			if (d == null) return true;
			
			BlockTransaction t = new BlockTransaction(d);
			d.align();
			t.commit(d.getBlocks());
			session.pushTransaction(t);
			
			if (session.getFollowingOffset() != null) {
				Location loc2 = p.getLocation();
				if (!p.isSneaking()) DexUtils.blockLoc(loc2);
				session.setFollowingOffset(d.getCenter().toVector().subtract(loc2.toVector()));
			}
			
			p.sendMessage(getConfigString("align-success", session));
		}
		
		else if (args[0].equals("axis")) {
			DexterityDisplay d = getSelected(session, "axis");
			if (d == null) return true;
			
			if (args.length == 1) {
				p.sendMessage(getUsage("axis"));
				return true;
			}
			
			if (args[1].equalsIgnoreCase("show")) {
				if (!p.hasPermission("dexterity.axis.show")) {
					p.sendMessage(noperm);
					return true;
				}
				
				if (args.length == 2) session.setShowingAxes(AxisType.SCALE);
				else {
					if (args[2].equalsIgnoreCase("rotation")) session.setShowingAxes(AxisType.ROTATE);
					else if (args[2].equalsIgnoreCase("scale")) session.setShowingAxes(AxisType.SCALE);
					else {
						p.sendMessage(plugin.getConfigString("unknown-input").replaceAll("\\Q%input%\\E", args[2]));
						return true;
					}
				}
			}
			else if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("reset")) {
				HashMap<String, Double> attrs_d = DexUtils.getAttributesDoubles(args);
				boolean set_scale = args.length < 3 || args[2].equals("scale");
				double s = set_scale ? 1d : 0d;
				double x = Math.abs(attrs_d.getOrDefault("x", attrs_d.getOrDefault("pitch", s))),
						y = Math.abs(attrs_d.getOrDefault("y", attrs_d.getOrDefault("yaw", s))), 
						z = Math.abs(attrs_d.getOrDefault("z", attrs_d.getOrDefault("roll", s)));
				
				if (set_scale) {
					Vector curr_scale = d.getScale();
					if (x == 0) x = curr_scale.getX();
					if (y == 0) y = curr_scale.getY();
					if (z == 0) z = curr_scale.getZ();
					
					if (x == 0 || y == 0 || z == 0) {
						p.sendMessage(plugin.getConfigString("must-send-number"));
						return true;
					}
					ScaleTransaction t = new ScaleTransaction(d);
					d.resetScale(new Vector(x, y, z));
					t.commitEmpty();
					session.pushTransaction(t);
					session.updateAxisDisplays();
					
					p.sendMessage(getConfigString("axis-set-success", session).replaceAll("\\Q%type%\\E", "scale"));
				} 
				else if (args[2].equalsIgnoreCase("rotation")) {
					RotationTransaction t = new RotationTransaction(d);
					d.setBaseRotation((float) y, (float) x, (float) z);
					t.commitEmpty();
					session.pushTransaction(t);
					session.updateAxisDisplays();
					
					p.sendMessage(getConfigString("axis-set-success", session).replaceAll("\\Q%type%\\E", "rotation"));
				}
				else {
					p.sendMessage(plugin.getConfigString("unknown-input").replaceAll("\\Q%input%\\E", args[2]));
					return true;
				}
				
			}
			else if (args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("hide")) {
				session.setShowingAxes(null);
			}
			else p.sendMessage(plugin.getConfigString("unknown-input").replaceAll("\\Q%input%\\E", args[1].toLowerCase()));
		}
		
		else if (args[0].equals("reload")) {
			if (!withPermission(p, "admin")) return true;
			plugin.reload();
			p.sendMessage(plugin.getConfigString("reload-success"));
		}
		
		else if (args[0].equals("replace") || args[0].equals("rep")) {
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
				
				BlockTransaction t = new BlockTransaction(d, new Mask(from));
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
							if (!plugin.isLegacy()) db.getEntity().getBlock().copyTo(todata);
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
		
		else if (args[0].equals("sel") || args[0].equals("select") || args[0].equals("pos1") || args[0].equals("pos2") || args[0].equals("load")) {
			
			int index = -1;
			if (args[0].equals("pos1")) index = 0;
			else if (args[0].equals("pos2")) index = 1;
			
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
			session.setContinuousLocation(loc, index == 0, new Vector(0, 0, 0), true);
//			session.setLocation(loc, index == 0);
		}
		
		else if (args[0].equals("desel") || args[0].equals("deselect") || args[0].equals("clear")) {
			if (session.getSelected() != null) {
				session.setSelected(null, false);
				session.clearLocationSelection();
				p.sendMessage(plugin.getConfigString("desel-success"));
			}
		}
		
		else if (args[0].equals("highlight") || args[0].equals("h")) {
			DexterityDisplay d = getSelected(session, "highlight");
			if (d == null) return true;
			api.tempHighlight(d, 50, Color.ORANGE);
		}
		
		else if (args[0].equals("clone")) {
			if (!withPermission(p, "clone") || testInEdit(session)) return true;
			DexterityDisplay d = session.getSelected();
			if (d == null && def == null) {
				p.sendMessage(plugin.getConfigString("must-select-display"));
				return true;
			}
			
			if (def != null) {
				d = plugin.api().getDisplay(def);
				if (d == null) {
					p.sendMessage(plugin.getConfigString("display-not-found").replaceAll("\\Q%input%\\E", def));
					return true;
				}
				session.setSelected(d, false);
			}
			
			boolean mergeafter = flags.contains("merge");
			if (mergeafter && !d.canHardMerge()) {
				p.sendMessage(getConfigString("cannot-clone", session));
				return true;
			}
			
			p.sendMessage(getConfigString("to-finish-edit", session));
			
			DexterityDisplay clone = api.clone(d);
			
			if (!clone.getCenter().getWorld().getName().equals(p.getWorld().getName()) || clone.getCenter().distance(p.getLocation()) >= 80) clone.teleport(p.getLocation());
			
			session.startEdit(clone, mergeafter ? EditType.CLONE_MERGE : EditType.CLONE, true);
			
			if (!flags.contains("nofollow")) session.startFollowing();
			
		}
		
		else if (args[0].equals("tile") || args[0].equals("stack")) {
			DexterityDisplay d = getSelected(session, "tile");
			if (d == null) return true;
			
			Vector delta = new Vector();
			HashMap<String, Double> attrs_d = DexUtils.getAttributesDoubles(args);
			int count = Math.abs(attrs_d.getOrDefault("count", 0d).intValue());
			
			if (count == 0) { //check valid count
				if (def != null) {
					try {
						count = Integer.parseInt(def);
					} catch(Exception ex) {
						p.sendMessage(plugin.getConfigString("must-enter-value").replaceAll("\\Q%value%\\E", "count"));
						return true;
					}
				}
				else {
					p.sendMessage(plugin.getConfigString("must-enter-value").replaceAll("\\Q%value%\\E", "count"));
					return true;
				}
			}
			
			if (d.getBlocksCount()*(count+1) > session.getPermittedVolume()) { //check volume
				p.sendMessage(plugin.getConfigString("exceeds-max-volume").replaceAll("\\Q%volume%\\E", "" + (int) session.getPermittedVolume()));
				return true;
			}
			
			if (attrs_d.containsKey("x")) delta.setX(attrs_d.get("x"));
			if (attrs_d.containsKey("y")) delta.setY(attrs_d.get("y"));
			if (attrs_d.containsKey("z")) delta.setZ(attrs_d.get("z"));
			if (attrs_d.containsKey("rx") || attrs_d.containsKey("ry") || attrs_d.containsKey("rz") || def != null) {
				DexRotation rot = d.getRotationManager(true);
				if (attrs_d.containsKey("rx")) delta.add(rot.getXAxis().multiply(attrs_d.get("rx")));
				if (attrs_d.containsKey("ry")) delta.add(rot.getYAxis().multiply(attrs_d.get("ry")));
				if (attrs_d.containsKey("rz")) delta.add(rot.getZAxis().multiply(attrs_d.get("rz")));
			}
			
			if (delta.getX() == 0 && delta.getY() == 0 && delta.getZ() == 0) { //cannot be 0 delta
				p.sendMessage(plugin.getConfigString("must-send-numbers-xyz"));
				return true;
			}
			
			Location loc = d.getCenter();
			DexterityDisplay toMerge = new DexterityDisplay(plugin, d.getCenter(), d.getScale());
			BuildTransaction t = new BuildTransaction(d);
			Vector centerv = d.getCenter().toVector();
			
			for (int i = 0; i < count; i++) {
				loc.add(delta);
				DexterityDisplay c = api.clone(d);
//				c.teleport(loc);
				for (DexBlock db : c.getBlocks()) {
					Vector diff = new Vector(loc.getX() - centerv.getX(), loc.getY() - centerv.getY(), loc.getZ() - centerv.getZ());
					db.move(diff);
					t.addBlock(db);
				}
				toMerge.hardMerge(c);
			}
			
			d.hardMerge(toMerge);
			t.commit();
			session.pushTransaction(t);
			
			p.sendMessage(getConfigString("tile-success", session));
			
		}
		
		else if (args[0].equals("cancel") || args[0].equals("quit")) {
			DexterityDisplay d = getSelected(session, null);
			if (d == null) return true;
			session.cancelEdit();
			p.sendMessage(getConfigString("cancelled-edit", session));
		}
		
		else if (args[0].equals("glow")) { //TODO add transaction
			DexterityDisplay d = getSelected(session, "glow");
			if (d == null) return true;
//			if (args.length < 2 || (def == null && flags.size() == 0)) {
//				p.sendMessage(getUsage("glow"));
//				return true;
//			}
			
			boolean propegate = false; //flags.contains("propegate");
			if (args.length < 2 || (def != null && (def.equals("none") || def.equals("off"))) || flags.contains("none") || flags.contains("off")) {
				d.setGlow(null, propegate);
				p.sendMessage(getConfigString("glow-success-disable", session));
				return true;
			}
			
			ColorEnum c;
			try {
				c = ColorEnum.valueOf(def.toUpperCase());
			} catch (Exception ex) {
				p.sendMessage(getConfigString("unknown-color", session).replaceAll("\\Q%input%\\E", args[1].toUpperCase()));
				return true;
			}
			d.setGlow(c.getColor(), propegate);
			if (d.getLabel() != null) p.sendMessage(getConfigString("glow-success", session));
		}
		
		else if (args[0].equals("command") || args[0].equals("cmd")) {
			DexterityDisplay d = getSelected(session, "cmd");
			if (d == null) return true;
			
			if (args.length <= 1) {
				p.sendMessage(getUsage("cmd"));
				return true;
			}
				
			if (args[1].equalsIgnoreCase("add")) {
				if (defs.size() == 1 || args.length < 2){
					p.sendMessage(getUsage("cmd-add"));
					return true;
				}
				if (!d.isSaved()) {
					p.sendMessage(getConfigString("not-saved", session));
					return true;
				}

				StringBuilder cmd_strb = new StringBuilder();
				boolean appending = false;
				for (int i = 2; i < args.length; i++) {
					String arg = args[i];
					if (!appending && !arg.contains("=") && !arg.startsWith("-") && !arg.contains(":")) appending = true;
					if (appending) {
						cmd_strb.append(arg);
						cmd_strb.append(" ");
					}
				}
				String cmd_str = cmd_strb.toString().trim();
				if (cmd_str.length() == 0) {
					p.sendMessage("cmd-add");
					return true;
				}
				InteractionCommand command = new InteractionCommand(cmd_str);

				//set flags
				if (flags.contains("left_only") || flags.contains("r")) {
					command.setLeft(true);
					command.setRight(false);
				} else if (flags.contains("right_only") || flags.contains("l")) {
					command.setLeft(false);
					command.setRight(true);
				}
				
				boolean by_player = flags.contains("player") || flags.contains("p") || !p.hasPermission("dexterity.command.cmd.console");
				command.setByPlayer(by_player);
				
				if (attr_str.containsKey("permission")) command.setPermission(attr_str.get("permission"));

				d.addCommand(command);
				p.sendMessage(getConfigString("cmd-add-success", session).replaceAll("\\Q%id%\\E", "" + d.getCommandCount()));
			}
			
			else if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("rem")) {
				if (args.length < 3) {
					p.sendMessage(getUsage("cmd-remove"));
					return true;
				}
				
				int index;
				try {
					index = Integer.parseInt(args[2]);
					if (index != 0) index -= 1;
				} catch (Exception ex) {
					p.sendMessage(getConfigString("must-send-number", session));
					return true;
				}
				if (index < 0) index = d.getCommandCount() + index + 1; //ex. input -1 for the last command
				if (index < d.getCommandCount()) {
					InteractionCommand command = d.getCommands()[index];
					d.removeCommand(command);
				}
				p.sendMessage(getConfigString("cmd-remove-success", session).replaceAll("\\Q%id%\\E", (index+1) + ""));			
			}
			
			else if (args[1].equalsIgnoreCase("list")) {
				if (d.getCommandCount() == 0) p.sendMessage(getConfigString("list-empty", session));
				else {
					InteractionCommand[] cmds = d.getCommands();
					for (int i = 0; i < cmds.length; i++) {
						p.sendMessage(cc2 + (i+1) + "." + cc + " " + cmds[i].getCmd());
					}
				}
			}
			
			else p.sendMessage(getUsage("cmd"));
			
		}
		
		else if (args[0].equals("convert") || args[0].equals("conv")) {
			if (!withPermission(p, "convert") || testInEdit(session)) return true;
			Location l1 = session.getLocation1(), l2 = session.getLocation2();
			if (l1 != null && l2 != null) {
				
				if (!l1.getWorld().getName().equals(l2.getWorld().getName())) {
					p.sendMessage(getConfigString("must-same-world-points", session));
					return true;
				}
				double vol = session.getPermittedVolume();
				if (session.getSelectionVolume() > vol) {
					p.sendMessage(getConfigString("exceeds-max-volume", session).replaceAll("\\Q%volume%\\E", "" + vol));
					return true;
				}
				
				ConvertTransaction t = new ConvertTransaction();
				session.setCancelPhysics(true);
				
				DexterityDisplay d = api.convertBlocks(l1, l2, t, (int) vol + 1);
				
				session.setCancelPhysics(false);
				if (session.getSelected() != null && !session.getSelected().isSaved() && session.getSelected().getCenter().getWorld().getName().equals(d.getCenter().getWorld().getName())) session.getSelected().hardMerge(d); //within cuboid selection
				else session.setSelected(d, false);
				
				session.pushTransaction(t);
								
				//p.sendMessage(cc + "Created a new display: " + cc2 + d.getLabel() + cc + "!");
				p.sendMessage(getConfigString("convert-success", session));
				
			} else p.sendMessage(getConfigString("need-locations", session));
		}
		
		else if (args[0].equals("move") || args[0].equals("m")) { //TODO check if in edit session, change displacement vector
			
			DexterityDisplay d = getSelected(session, "move");
			if (d == null) return true;
			
			boolean same_world = d.getCenter().getWorld().getName().equals(p.getWorld().getName());
			if (args.length == 1 || !same_world) {
				if (!same_world) d.teleport(p.getLocation());
				session.startFollowing();
				session.startEdit(d, EditType.TRANSLATE, false, new BlockTransaction(d));
				p.sendMessage(getConfigString("to-finish-edit", session));
				return true;
			}
			
			BlockTransaction t = new BlockTransaction(d);
			Location loc;
			if (flags.contains("continuous") || flags.contains("c")) loc = p.getLocation();
			else if (flags.contains("here")) loc = DexUtils.blockLoc(p.getLocation()).add(0.5, 0.5, 0.5);
			else loc = d.getCenter();
						
			HashMap<String,Double> attr_d = DexUtils.getAttributesDoubles(args);
			if (attr_d.containsKey("x")) loc.add(attr_d.get("x"), 0, 0);
			if (attr_d.containsKey("y")) loc.add(0, attr_d.get("y"), 0);
			if (attr_d.containsKey("z")) loc.add(0, 0, attr_d.get("z"));
			if (attr_d.containsKey("rx") || attr_d.containsKey("ry") || attr_d.containsKey("rz")) {
				DexRotation rot = d.getRotationManager(false);
				Vector x, y, z;
				if (rot == null) {
					x = new Vector(1, 0, 0);
					y = new Vector(0, 1, 0);
					z = new Vector(0, 0, 1);
				} else {
					x = rot.getXAxis();
					y = rot.getYAxis();
					z = rot.getZAxis();
				}
				
				if (attr_d.containsKey("rx")) loc.add(x.multiply(attr_d.get("rx")));
				if (attr_d.containsKey("ry")) loc.add(y.multiply(attr_d.get("ry")));
				if (attr_d.containsKey("rz")) loc.add(z.multiply(attr_d.get("rz")));
			}
						
			d.teleport(loc);
			
			t.commit(d.getBlocks());
			t.commitCenter(d.getCenter());
			session.pushTransaction(t);
			
			if (session.getFollowingOffset() != null) {
				Location loc2 = p.getLocation();
				if (!p.isSneaking()) DexUtils.blockLoc(loc2);
				session.setFollowingOffset(d.getCenter().toVector().subtract(loc2.toVector()));
			}
			
		}
		else if (args[0].equals("label") || args[0].equals("name") || args[0].equals("rename") || args[0].equals("save")) {
			DexterityDisplay d = getSelected(session, "save");
			if (d == null) return true;
			
			if (args.length != 2) {
				if (args[0].equals("save")) p.sendMessage(getUsage("rename").replaceAll(" name", " save"));
				else p.sendMessage(getUsage("rename"));
				return true;
			}
			if (d.getBlocksCount() == 0) {
				p.sendMessage(getConfigString("must-select-display", session));
				return true;
			}
			if (args[1].startsWith("-") || args[1].contains(".")) {
				p.sendMessage(plugin.getConfigString("invalid-name").replaceAll("\\Q%input%\\E", args[1]));
				return true;
			}
			
			if (d.setLabel(args[1])) p.sendMessage(getConfigString("rename-success", session));
			else p.sendMessage(getConfigString("name-in-use", session).replaceAll("\\Q%input%\\E", args[1]));
		}
		
		else if (args[0].equals("undo") || args[0].equals("u")) {
			int count = attrs.getOrDefault("count", -1);
			if (count < 2) session.undo();
			else session.undo(count);
		}
		else if (args[0].equals("redo")) {
			int count = attrs.getOrDefault("count", -1);
			if (count < 2) session.redo();
			else session.redo(count);
		}
		
		else if (args[0].equals("unsave")) {
			DexterityDisplay d;
			if (def != null) {
				d = plugin.getDisplay(def.toLowerCase());
				if (d == null) {
					p.sendMessage(getConfigString("display-not-found", session).replaceAll("\\Q%input%\\E", def));
					return true;
				}
			} else {
				d = getSelected(session, "save");
				if (d == null) return true;
				if (!d.isSaved()) {
					p.sendMessage(getConfigString("not-saved", session));
					return true;
				}
			}
			
			
			String msg = getConfigString("unsave-success", session);
			d.unregister();
			p.sendMessage(msg);
		}
		
		else if (args[0].equals("mask")) {
			if (args.length < 2 || flags.contains("none") || flags.contains("off") || def.equals("none") || def.equals("off")) {
				session.setMask(null);
				p.sendMessage(getConfigString("mask-success-disable", session));
			} else {
				Mask m = new Mask();
				for (String mat : defs) {
					try {
						m.addMaterialsList(mat);
					} catch (IllegalArgumentException ex) {
						p.sendMessage(plugin.getConfigString("unknown-material").replaceAll("\\Q%input%\\E", mat));
						return true;
					}
				}
				
				if (flags.contains("invert")) m.setNegative(true);

				session.setMask(m);
				
				p.sendMessage(getConfigString("mask-success", session).replaceAll("\\Q%input%\\E", m.toString()));
			}
		}
		
		else if (args[0].equals("rotate") || args[0].equals("r")){
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
			List<String> defs_n = DexUtils.getDefaultAttributesWithFlags(args);
			
			plan.yaw_deg = attrs_d.getOrDefault("yaw", Double.MAX_VALUE);
			plan.pitch_deg = attrs_d.getOrDefault("pitch", Double.MAX_VALUE);
			plan.roll_deg = attrs_d.getOrDefault("roll", Double.MAX_VALUE);
			plan.y_deg = attrs_d.getOrDefault("y", Double.MAX_VALUE);
			plan.x_deg = attrs_d.getOrDefault("x", Double.MAX_VALUE);
			plan.z_deg = attrs_d.getOrDefault("z", Double.MAX_VALUE);
			
			
			try {
				switch(Math.min(defs.size(), 6)) {
				case 6:
					if (plan.z_deg == Double.MAX_VALUE) plan.z_deg = Double.parseDouble(defs_n.get(5));
				case 5:
					if (plan.x_deg == Double.MAX_VALUE) plan.x_deg = Double.parseDouble(defs_n.get(4));
				case 4: 
					if (plan.yaw_deg == Double.MAX_VALUE) plan.yaw_deg = Double.parseDouble(defs_n.get(3));
				case 3: 
					if (plan.roll_deg == Double.MAX_VALUE) plan.roll_deg = Double.parseDouble(defs_n.get(2));
				case 2: 
					if (plan.pitch_deg == Double.MAX_VALUE) plan.pitch_deg = Double.parseDouble(defs_n.get(1));
				case 1: 
					if (plan.y_deg == Double.MAX_VALUE) plan.y_deg = Double.parseDouble(defs_n.get(0));
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
		
		else if (args[0].equals("schem") || args[0].equals("schematic")) {
			if (!withPermission(p, "schem")) return true;

			if (args.length == 1) p.sendMessage(getUsage("schematic"));
			else if (args[1].equalsIgnoreCase("import") || args[1].equalsIgnoreCase("load")) {
				if (!withPermission(p, "schem.import") || testInEdit(session)) return true;
				
				if (args.length == 2 || defs.size() < 2) {
					p.sendMessage(getUsage("schem"));
					return true;
				}

				String name = defs.get(1);
				DexterityDisplay d;
				Schematic schem;
				try {
					schem = new Schematic(plugin, name);
					d = schem.paste(p.getLocation());
				} catch (Exception ex) {
					ex.printStackTrace();
					p.sendMessage(plugin.getConfigString("console-exception"));
					return true;
				}
				session.setSelected(d, false);
				p.sendMessage(getConfigString("schem-import-success", session).replaceAll("\\Q%author%\\E", schem.getAuthor()));
			}
			else if (args[1].equalsIgnoreCase("export") || args[1].equalsIgnoreCase("save")) {
				if (!withPermission(p, "schem.export")) return true;
				DexterityDisplay d = getSelected(session, "export");
				if (d == null || testInEdit(session)) return true;

				if (!d.isSaved()) {
					p.sendMessage(plugin.getConfigString("not-saved"));
					return true;
				}
				
				SchematicBuilder builder = new SchematicBuilder(plugin, d);
				String author = p.getName();
				if (attr_str.containsKey("author")) author = attr_str.get("author");
				boolean overwrite = flags.contains("overwrite");
				
				int res = builder.save(d.getLabel().toLowerCase(), author, overwrite);
				
				if (res == 0) p.sendMessage(getConfigString("schem-export-success", session));
				else if (res == 1) p.sendMessage(getConfigString("file-already-exists", session).replaceAll("\\Q%input%\\E", "/schematics/" + d.getLabel().toLowerCase() + ".dexterity"));
				else if (res == -1) p.sendMessage(getConfigString("console-exception", session));
			}
			else if (args[1].equalsIgnoreCase("delete")) {
				String name = args[2].toLowerCase();
				if (!name.endsWith(".dexterity")) name += ".dexterity";
				File f = new File(plugin.getDataFolder().getAbsolutePath() + "/schematics/" + name);
				if (f.exists()) {
					try {
						f.delete();
						p.sendMessage(plugin.getConfigString("schem-delete-success").replaceAll("\\Q%label%\\E", args[2].toLowerCase()).replaceAll("\\Q%input%\\E", name));
					} catch (Exception ex) {
						ex.printStackTrace();
						p.sendMessage(plugin.getConfigString("file-not-found").replaceAll("\\Q%input%\\E", name));
					}
				} else p.sendMessage(plugin.getConfigString("file-not-found").replaceAll("\\Q%input%\\E", name));
			}
			else p.sendMessage(plugin.getConfigString("unknown-subcommand"));
		}
		
		else if (args[0].equals("info") || args[0].equals("i")) {
			DexterityDisplay d = getSelected(session, null);
			if (d == null) return true;
			p.sendMessage(cc + "Selected " + cc2 + d.getBlocksCount() + cc + " block display" + (d.getBlocksCount() == 1 ? "" : "s") + " in " + cc2 + d.getCenter().getWorld().getName() + (d.getLabel() != null ? cc + " labelled " + cc2 + d.getLabel() : ""));
			api.markerPoint(d.getCenter(), Color.AQUA, 4);
		}
		else if (args[0].equals("rm") || args[0].equals("remove") || args[0].equals("restore") || args[0].equals("deconvert") || args[0].equals("deconv")) {
			boolean res = !(args[0].equals("remove") || args[0].equals("rm"));
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
			
			api.unTempHighlight(d);
			
			Transaction t;
			if (res) {
				t = new DeconvertTransaction(d);
				session.setCancelPhysics(true);
			}
			else t = new RemoveTransaction(d);
			
			d.remove(res);
			
			if (res) {
				p.sendMessage(getConfigString("restore-success", session));
				session.setCancelPhysics(false);
			}
			else p.sendMessage(getConfigString("remove-success", session));
			session.setSelected(null, false);
			session.pushTransaction(t);
		}
		
		else if (args[0].equals("list") || args[0].equals("lsit")) {
			if (!withPermission(p, "list")) return true;
			
			int page = 0;
			if (attrs.containsKey("page")) {
				page = Math.max(attrs.get("page") - 1, 0);
			} else if (args.length >= 2) page = Math.max(DexUtils.parseInt(args[1]) - 1, 0);			
			int maxpage = DexUtils.maxPage(plugin.getDisplays().size(), 10);
			if (page >= maxpage) page = maxpage - 1;
			
			String w = null;
			if (attr_str.containsKey("world")) {
				World world = Bukkit.getWorld(attr_str.get("world"));
				if (world != null) w = world.getName();
			}
			
			int total = 0;
			for (DexterityDisplay d : plugin.getDisplays()) {
				if (d.getLabel() == null || (w != null && !d.getCenter().getWorld().getName().equals(w))) continue;
				total += d.getGroupSize();
			}
			if (total == 0) {
				p.sendMessage(plugin.getConfigString("no-saved-displays"));
				return true;
			}
			
			p.sendMessage(plugin.getConfigString("list-page-header").replaceAll("\\Q%page%\\E", "" + (page+1)).replaceAll("\\Q%maxpage%\\E", ""+maxpage));
			String[] strs = new String[total];
			int i = 0;
			for (DexterityDisplay disp : plugin.getDisplays()) {
				if (disp.getLabel() == null || (w != null && !disp.getCenter().getWorld().getName().equals(w))) continue;
				i += constructList(strs, disp, session.getSelected() == null ? null : session.getSelected().getLabel(), i, 0);
			}
			DexUtils.paginate(p, strs, page, 10);
		}
		
		else if (args[0].equals("scale") || args[0].equals("s") || args[0].equals("skew")) {
			DexterityDisplay d = getSelected(session, "scale");
			if (d == null) return true;
			
			boolean set = flags.contains("set");
			
			ScaleTransaction t = new ScaleTransaction(d);
			Vector scale = new Vector();
			String scale_str;
			if (attrs.containsKey("x") || attrs.containsKey("y") || attrs.containsKey("z")) {
				HashMap<String, Double> attrsd = DexUtils.getAttributesDoubles(args);
				float sx = Math.abs(attrsd.getOrDefault("x", set ? d.getScale().getX() : 1).floatValue());
				float sy = Math.abs(attrsd.getOrDefault("y", set ? d.getScale().getY() : 1).floatValue());
				float sz = Math.abs(attrsd.getOrDefault("z", set ? d.getScale().getZ() : 1).floatValue());
				
				if (sx == 0 || sy == 0 || sz == 0) {
					p.sendMessage(getConfigString("must-send-number", session));
					return true;
				}
				
				scale_str = sx + ", " + sy + ", " + sz;
				scale = new Vector(sx, sy, sz);
			} else {
				float scalar;
				try {
					scalar = Float.parseFloat(def);
				} catch(Exception ex) {
					p.sendMessage(getConfigString("must-send-number", session));
					return true;
				}
				scale_str = "" + scalar;
				scale = new Vector(scalar, scalar, scalar);
			}
			
			try {
				if (set) {
					d.setScale(scale);
					p.sendMessage(getConfigString("scale-success-set", session).replaceAll("\\Q%scale%\\E", scale_str));
				}
				else {
					d.scale(scale);
					p.sendMessage(getConfigString("scale-success", session).replaceAll("\\Q%scale%\\E", scale_str));
				}
			} catch (DexterityException ex) {
				p.sendMessage(getConfigString("selection-too-complex", session).replaceAll("\\Q%scale%\\E", scale_str));
				return true;
			}

			t.commit();
			session.pushTransaction(t);
		}
		else if (args[0].equals("merge")) {
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
//		else if (args[0].equals("unmerge")) {
//			DexterityDisplay d = getSelected(session, "merge");
//			if (d == null || testInEdit(session)) return true;
//			if (d.getLabel() == null) {
//				p.sendMessage(getConfigString("must-save-display", session));
//				return true;
//			}
//			if (d.getParent() == null) {
//				p.sendMessage(getConfigString("nothing-to-unmerge", session));
//				return true;
//			}
//			d.unmerge();
//			p.sendMessage(getConfigString("unmerge-success", session));
//		}
		
		else {
			p.sendMessage(plugin.getConfigString("unknown-subcommand"));
		}
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] argsr) {
		argsr[0] = argsr[0].toLowerCase();
		List<String> params = new ArrayList<String>();
		for (String s : argsr) {
			String[] ssplit = s.split("=");
			if (ssplit.length > 0) params.add(DexUtils.attrAlias(ssplit[0]));
		}
		
		List<String> ret = new ArrayList<String>();
		if (!sender.hasPermission("dexterity.command")) return ret;
		boolean add_labels = false;
		
		if (argsr.length <= 1) {
			for (String s : commands) ret.add(s);
			ret.add("cancel");
			ret.add("paste");
		}
		else if (argsr[0].equals("?") || argsr[0].equals("help")) {
			ret.add("page=");
		}
		else if (argsr[0].equals("list")) {
			ret.add("page=");
			ret.add("world=");
		}
		else if (argsr[0].equals("sel") || argsr[0].equals("select")) {
			add_labels = true;
		}
		else if (argsr[0].equals("replace") || argsr[0].equals("rep")) {
			if (argsr.length <= 3 && argsr[argsr.length - 1].length() >= 2) {
				ret = DexUtils.materials(argsr[argsr.length - 1]);
			}
		}
		else if (argsr[0].equals("mask")) {
			String lastarg = argsr[argsr.length-1];
			String[] larg_split = lastarg.split(",");

			if (larg_split[larg_split.length-1].length() >= 2) {
				StringBuilder prevarg_b = new StringBuilder();
				for (int i = 0; i < larg_split.length-1; i++) {
					prevarg_b.append(larg_split[i]);
					prevarg_b.append(",");
				}
				
				ret = DexUtils.materials(larg_split[larg_split.length - 1], prevarg_b.toString());
			}
			ret.add("-none");
			ret.add("-invert");
		}
		else if (argsr[0].equals("consolidate")) {
			if (argsr.length <= 2 && argsr[argsr.length - 1].length() >= 2) {
				ret = DexUtils.materials(argsr[argsr.length - 1]);
			}
		}
		else if (argsr[0].equals("remove") || argsr[0].equals("restore") || argsr[0].equals("deconvert") || argsr[0].equals("deconv")) {
			add_labels = true;
		}
		else if (argsr[0].equals("recenter")) {
			ret.add("-continuous");
		}
		else if (argsr[0].equals("scale") || argsr[0].equals("s") || argsr[0].equals("skew")) {
			ret.add("x=");
			ret.add("y=");
			ret.add("z=");
			ret.add("-set");
		}
//		else if (argsr[0].equals("merge")) {
//			ret.add("new_group=");
//			ret.add("-hard");
//			add_labels = true;
//		}
		else if (argsr[0].equals("glow")) {
			ret.add("-none");
			for (ColorEnum c : ColorEnum.values()) ret.add(c.toString());
		}
		else if (argsr[0].equals("tile")) {
			ret.add("x=");
			ret.add("y=");
			ret.add("z=");
			ret.add("rx=");
			ret.add("ry=");
			ret.add("rz=");
			ret.add("count=");
		}
		else if (argsr[0].equals("move") || argsr[0].equals("m")) {
			ret.add("x=");
			ret.add("y=");
			ret.add("z=");
			ret.add("rx=");
			ret.add("ry=");
			ret.add("rz=");
			ret.add("north=");
			ret.add("south=");
			ret.add("east=");
			ret.add("west=");
			ret.add("up=");
			ret.add("down=");
			ret.add("-here");
			ret.add("-continuous");
		}
		else if (argsr[0].equals("clone")) {
			ret.add("-nofollow");
			ret.add("-merge");
		}
		else if (argsr[0].equals("undo") || argsr[0].equals("u") || argsr[0].equals("redo")) {
			ret.add("count=");
		}
		else if (argsr[0].equals("command") || argsr[0].equals("cmd")) {
			if (argsr.length == 2) {
				ret.add("add");
				ret.add("remove");
				ret.add("list");
			} 
			else if (argsr[1].equalsIgnoreCase("add")) {
				if (argsr.length == 3) {
					ret.add("permission=");
					ret.add("-left_only");
					ret.add("-right_only");
					ret.add("-player");
				}
				else if (argsr.length == 4) {
					Player p = (Player) sender;
					DexSession session = plugin.getEditSession(p.getUniqueId());
					int len = 0;
					if (session != null && session.getSelected() != null) len = session.getSelected().getCommandCount();
					for (int i = 0; i < len; i++) ret.add("" + (i+1));
				}
			}
		}
		else if (argsr[0].equals("axis")) {
			if (argsr.length == 2) {
				ret.add("show");
				ret.add("off");
				ret.add("set");
			}
			else if (argsr.length >= 4 && argsr[1].equalsIgnoreCase("set")) {
				ret.add("x=");
				ret.add("y=");
				ret.add("z=");
			}
			else if (argsr[1].equalsIgnoreCase("show") || argsr[1].equalsIgnoreCase("set")) {
				ret.add("rotation");
				ret.add("scale");
			}
		}
		else if (argsr[0].equals("rotate") || argsr[0].equals("r")) {
			ret.add("yaw=");
			ret.add("pitch=");
			ret.add("roll=");
			ret.add("y=");
			ret.add("x=");
			ret.add("z=");
			ret.add("-set");
			ret.add("-reset");
		}
		else if (argsr[0].equals("schem")) {
			if (argsr.length == 2) {
				ret.add("load");
				ret.add("save");
				ret.add("delete");
			}
			else if (argsr.length == 3) {
				if (argsr[1].equalsIgnoreCase("export") || argsr[1].equalsIgnoreCase("save")) {
					ret.add("author=");
					ret.add("-overwrite");
				}
				else if (argsr[1].equalsIgnoreCase("import") || argsr[1].equalsIgnoreCase("load") || argsr[1].equalsIgnoreCase("delete")) {
					ret = listSchematics();
				}
			}
		}
		
		if (add_labels) for (String s : plugin.getDisplayLabels()) ret.add(s);
		return ret;
	}

}
