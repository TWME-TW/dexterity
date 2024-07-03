package me.c7dev.tensegrity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import me.c7dev.tensegrity.DexSession.EditType;
import me.c7dev.tensegrity.api.DexterityAPI;
import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.displays.animation.Animation;
import me.c7dev.tensegrity.displays.animation.LinearTranslationAnimation;
import me.c7dev.tensegrity.displays.animation.RideAnimation;
import me.c7dev.tensegrity.displays.animation.RideAnimation.LookMode;
import me.c7dev.tensegrity.util.ClickedBlockDisplay;
import me.c7dev.tensegrity.util.ColorEnum;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;

public class DexterityCommand implements CommandExecutor, TabCompleter {
	
	private Dexterity plugin;
	private DexterityAPI api;
	String noperm, cc, cc2, usage_format;
	
	public String[] commands = {
		"align", "animation", "clone", "convert", "deconvert", "deselect", "glow", "list", "merge", "move", "name", "pos1", "recenter", 
		"remove", "rotate", "scale", "select", "unmerge", "wand"
	};
	public String[] descriptions = new String[commands.length];
	public String[] command_strs = new String[commands.length];
	
	public DexterityCommand(Dexterity plugin) {
		this.plugin= plugin;
		cc = plugin.getChatColor();
		cc2 = plugin.getChatColor2();
		api = plugin.getAPI();
		plugin.getCommand("dex").setExecutor(this);
		plugin.getCommand("dex").setExecutor(this);
		noperm = plugin.getConfigString("no-permission");
		usage_format = plugin.getConfigString("usage-format");
		
		for (int i = 0; i < commands.length; i++) {
			descriptions[i] = plugin.getConfigString(commands[i] + "-description");
			command_strs[i] = cc2 + "- /d " + commands[i] + " §8- " + cc + descriptions[i];
		}
	}
	
	public DexterityDisplay getSelected(DexSession session) {
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
			s = s.replaceAll("\\Q%label%\\E", "selected");
			if (session != null && session.getSelected() != null) s = s.replaceAll("\\Q%loclabel%", "at " + cc2 + DexUtils.locationString(session.getSelected().getCenter(), 0));
			else s = s.replaceAll("\\Q%loclabel%\\E", "");
		}
		return s;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) return true;
		
		Player p = (Player) sender;
		
		if (args.length == 0) {
			p.sendMessage(cc + "§lUsing §6§lDexterity");
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
		
		else if (args[0].equalsIgnoreCase("test")) {
			for (Entity e : p.getWorld().getEntities()) {
				if (e instanceof BlockDisplay) e.remove();
			}
		}
		else if (args[0].equalsIgnoreCase("test3")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			DexBlock db = d.getBlocks().get(0);
			Quaternionf zero = new Quaternionf(0f, 0f, 0f, 1f);
			db.getTransformation().setDisplacement(new Vector(-0.5f, -0.5f, -0.5f))
				.setLeftRotation(zero).setRightRotation(zero);
			db.updateTransformation();
			//db.getEntity().setRotation(Float.parseFloat(args[1]), Float.parseFloat(args[2]));
			
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
			}.runTaskTimer(plugin, 0, 1l);
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
			//r.setSeatOffset(new Vector(0, -4.5, -0.5));
			//r.setSeatOffset(new Vector(0, -0.7, 0));
			r.setSpeed(10);
			r.setLookingMode(LookMode.YAW_ONLY);
			r.mount(p);
			r.start();
		}
		else if (args[0].equalsIgnoreCase("testnear")) {
			ClickedBlockDisplay b = api.getLookingAt(p);
			if (b == null) p.sendMessage("None in range");
			else {
				api.markerPoint(b.getClickLocation(), Color.RED, 4);
				p.sendMessage("Clicked " + b.getBlockFace() + ", disp = " + DexUtils.vectorString(b.getOffsetFromFaceCenter(), 3));
			}
		}
		
		else if (args[0].equalsIgnoreCase("set")) {
			if (session.getEditType() != null) {
				switch(session.getEditType()) {
				case CLONE:
					if (session.getSecondary() != null) {
						Bukkit.broadcastMessage("A");
						session.getSecondary().hardMerge(session.getSelected());
					}
					p.sendMessage(getConfigString("clone-success", session));
					break;
				default:
				}
				session.finishEdit();
			}
		}
		
		else if (args[0].equalsIgnoreCase("recenter")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			if (!d.getCenter().getWorld().getName().equals(p.getLocation().getWorld().getName())) {
				p.sendMessage(getConfigString("must-same-world", session));
				return true;
			}
			
			Location loc = p.getLocation();
			if (!flags.contains("continuous")) DexUtils.blockLoc(loc).add(0.5, 0.5, 0.5);
			
			d.setCenter(loc);
			api.markerPoint(loc, Color.AQUA, 4);
			
			p.sendMessage(getConfigString("recenter-success", session));
			
		}
		
		else if (args[0].equalsIgnoreCase("align")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			d.align();
			
			p.sendMessage(getConfigString("align-success", session));
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
		
		else if (args[0].equalsIgnoreCase("clone")) {
			DexterityDisplay d = getSelected(session);
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
			
			//start clone
			List<DexBlock> blocks = new ArrayList<>();
			for (DexBlock db : d.getBlocks()) {
				BlockDisplay block = db.getLocation().getWorld().spawn(db.getLocation(), BlockDisplay.class, a -> {
					a.setBlock(db.getEntity().getBlock());
					a.setTransformation(db.getTransformation().build());
					if (db.getEntity().isGlowing()) {
						a.setGlowColorOverride(db.getEntity().getGlowColorOverride());
						a.setGlowing(true);
					}
				});
				blocks.add(new DexBlock(block, clone));
			}
			clone.setEntities(blocks, false);
			
			session.startEdit(clone, EditType.CLONE, mergeafter);
			
			if (!flags.contains("nofollow")) session.startFollowing();
			
		}
		
		else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("quit")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			session.cancelEdit();
			p.sendMessage(getConfigString("cancelled-edit", session));
		}
		
		else if (args[0].equalsIgnoreCase("glow")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			boolean propegate = flags.contains("propegate");
			if (args[1].equalsIgnoreCase("none") || args[1].equalsIgnoreCase("off")) {
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
		
		else if (args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("a")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			if (d.getLabel() == null) {
				p.sendMessage(plugin.getConfigString("must-save-display"));
				return true;
			}
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
				p.sendMessage(plugin.getConfigString("unknown-subcommand"));
			}
		}
		
		else if (args[0].equalsIgnoreCase("convert") || args[0].equalsIgnoreCase("conv")) {
			if (testInEdit(session)) return true;
			if (session.getLocation1() != null && session.getLocation2() != null) {
				
				if (!session.getLocation1().getWorld().getName().equals(session.getLocation2().getWorld().getName())) {
					p.sendMessage(getConfigString("must-same-world-points", session));
					return true;
				}
				if (session.getSelectionVolume() > plugin.getMaxVolume()) {
					p.sendMessage(getConfigString("exceeds-max-volume", session).replaceAll("\\Q%volume%\\E", "" + plugin.getMaxVolume()));
					return true;
				}
				
				DexterityDisplay d = api.createDisplay(session.getLocation1(), session.getLocation2());
				
				session.setSelected(d, false);
								
				//p.sendMessage(cc + "Created a new display: " + cc2 + d.getLabel() + cc + "!");
				p.sendMessage(getConfigString("convert-success", session));
				
			} else p.sendMessage(getConfigString("need-locations", session));
		}
		
		else if (args[0].equalsIgnoreCase("move") || args[0].equalsIgnoreCase("m")) { //TODO check if in edit session, change displacement vector
			
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			if (session.getEditType() == EditType.TRANSLATE) {
				session.getPlayer().sendMessage(getConfigString("must-finish-edit", session));
				return true;
			} else if (args.length == 1) {
				session.startFollowing();
				session.startEdit(d, EditType.TRANSLATE, false);
				p.sendMessage(getConfigString("to-finish-edit", session));
				return true;
			}
			
			Location loc;
			if (flags.contains("continuous") || flags.contains("c") || flags.contains("here")) {
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
		else if (args[0].equalsIgnoreCase("label") || args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("rename") || args[0].equalsIgnoreCase("save")) {
			DexterityDisplay d = getSelected(session);
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
		
		else if (args[0].equalsIgnoreCase("rotate") || args[0].equalsIgnoreCase("r")){
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			if (args.length < 2) {
				p.sendMessage(getUsage("rotate"));
				return true;
			}
			
			HashMap<String, Double> attrs_d = DexUtils.getAttributesDoubles(args);
			double yaw = attrs_d.getOrDefault("yaw", Double.MAX_VALUE), pitch = attrs_d.getOrDefault("pitch", Double.MAX_VALUE);
			if (yaw == Double.MAX_VALUE && pitch == Double.MAX_VALUE) {
				try {
					if (yaw == Double.MAX_VALUE) yaw = Double.parseDouble(args[1]);
					if (args.length > 2 && pitch == Double.MAX_VALUE) {
						try {
							pitch = Double.parseDouble(args[2]);
						} catch (Exception ex) {
							pitch = 0;
						}
					}
				} catch (Exception ex) {
					p.sendMessage(getUsage("rotate"));
					return true;
				}
			}
			boolean set = flags.contains("set");
			boolean setyaw, setpitch;
			if (yaw == Double.MAX_VALUE) {
				setyaw = false;
				yaw = 0;
			} else setyaw = true;
			if (pitch == Double.MAX_VALUE) {
				setpitch = false;
				pitch = 0;
			} else setpitch = true;
						
			//TODO toggle messages in session
			if (set) {
				d.rotate((float) yaw, (float) pitch, setyaw, setpitch);
				p.sendMessage(cc + "Set rotation " + (d.getLabel() == null ? "" : "for " + cc2 + d.getLabel() + cc + " ") + "to " + cc2 + DexUtils.round(yaw, 3) + cc + " yaw, " + cc2 + DexUtils.round(pitch, 3) + cc + " pitch!");
			} else {
				d.rotate((float) yaw, (float) pitch);
				p.sendMessage(cc + "Rotated " + (d.getLabel() == null ? "display" : cc2 + d.getLabel() + cc) + " by " + cc2 + DexUtils.round(yaw, 3) + cc + " yaw, " + cc2 + DexUtils.round(pitch, 3) + cc + " pitch!");
			}
		}
		else if (args[0].equalsIgnoreCase("info")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			if (d.getLabel() == null) {
				p.sendMessage(cc + "Selected " + cc2 + d.getBlocks().size() + cc + " ghost block" + (d.getBlocks().size() == 1 ? "" : "s") + " in " + cc2 + d.getCenter().getWorld());
			} else {
				p.sendMessage(cc + "Selected " + cc2 + d.getLabel());
				p.sendMessage(cc + "Parent: " + cc2 + (d.getParent() == null ? "[None]" : d.getParent().getLabel()));
			}
		}
		else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("restore") || args[0].equalsIgnoreCase("deconvert") || args[0].equalsIgnoreCase("deconv")) {
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
			boolean res = !args[0].equalsIgnoreCase("remove");
			d.remove(res);
			if (res) p.sendMessage(getConfigString("restore-success", session));
			else p.sendMessage(getConfigString("remove-success", session));
			session.setSelected(null, false);
		}
		
		else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("lsit")) {
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
		
		else if (args[0].equalsIgnoreCase("scale")) {
			DexterityDisplay d = getSelected(session);
			if (d == null) return true;
			
			if (attrs.containsKey("x") || attrs.containsKey("y") || attrs.containsKey("z")) {
				HashMap<String, Double> attrsd = DexUtils.getAttributesDoubles(args);
				float sx = attrsd.getOrDefault("x", d.getScale().getX()).floatValue();
				float sy = attrsd.getOrDefault("y", d.getScale().getY()).floatValue();
				float sz = attrsd.getOrDefault("z", d.getScale().getZ()).floatValue();
				
				d.setScale(new Vector(sx, sy, sz));
				String scale_str = sx + ", " + sy + ", " + sz;
				p.sendMessage(getConfigString("scale-success", session).replaceAll("\\Q%scale%\\E", scale_str));
			} else {
				float scale = 1;
				try {
					scale = Float.parseFloat(def);
				} catch(Exception ex) {
					p.sendMessage(getConfigString("must-send-number", session));
					return true;
				}

				d.setScale(scale);

				p.sendMessage(getConfigString("scale-success", session).replaceAll("\\Q%scale%\\E", scale + ""));
			}
		}
		else if (args[0].equalsIgnoreCase("merge")) {
			if (def == null) {
				p.sendMessage(getUsage("merge"));
				return true;
			}
			DexterityDisplay d = getSelected(session);
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
			
			boolean hard = flags.contains("hard");
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
			DexterityDisplay d = getSelected(session);
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
		boolean add_labels = false;
		
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
		else if (argsr[0].equalsIgnoreCase("remove") || argsr[0].equalsIgnoreCase("restore") || argsr[0].equalsIgnoreCase("deconvert") || argsr[0].equalsIgnoreCase("deconv")) {
			add_labels = true;
		}
		else if (argsr[0].equalsIgnoreCase("recenter")) {
			ret.add("-continuous");
		}
		else if (argsr[0].equalsIgnoreCase("scale")) {
			ret.add("x=");
			ret.add("y=");
			ret.add("z=");
		}
		else if (argsr[0].equalsIgnoreCase("merge")) {
			ret.add("new_group=");
			ret.add("-hard");
			add_labels = true;
		}
		else if (argsr[0].equalsIgnoreCase("glow")) {
			ret.add("none");
			ret.add("-propegate");
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
		else if (argsr[0].equalsIgnoreCase("animation") || argsr[0].equalsIgnoreCase("a")) {
			ret.add("start");
			ret.add("stop");
			ret.add("pause");
			ret.add("unpause");
			ret.add("reset");
			ret.add("edit");
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
			ret.add("-set");
		}
		if (add_labels) for (String s : plugin.getDisplayLabels()) ret.add(s);

		return ret;
	}

}
