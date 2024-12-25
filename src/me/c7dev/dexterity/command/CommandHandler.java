package me.c7dev.dexterity.command;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.DexSession.AxisType;
import me.c7dev.dexterity.DexSession.EditType;
import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.api.DexRotation;
import me.c7dev.dexterity.api.DexterityAPI;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.displays.animation.Animation;
import me.c7dev.dexterity.displays.animation.RideableAnimation;
import me.c7dev.dexterity.displays.animation.SitAnimation;
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
 * Implementation of all in-game /dex commands
 */
public class CommandHandler {
	
	private Dexterity plugin;
	private DexterityAPI api;
	private String noperm, cc, cc2, usage_format, selected_str, loclabel_prefix;
	
	public CommandHandler(Dexterity plugin) {
		this.plugin = plugin;
		
		cc = plugin.getChatColor();
		cc2 = plugin.getChatColor2();
		api = plugin.api();
		noperm = plugin.getConfigString("no-permission");
		usage_format = plugin.getConfigString("usage-format");
		selected_str = plugin.getConfigString("selected");
		loclabel_prefix = plugin.getConfigString("loclabel-prefix", "selection at");
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
	
	public void help(CommandContext ctx, String[] commands_str) {
		int page = 0;
		HashMap<String, Integer> attrs = ctx.getIntAttrs();
		if (attrs.containsKey("page")) {
			page = Math.max(attrs.get("page") - 1, 0);
		} else if (ctx.getArgs().length >= 2) page = Math.max(DexUtils.parseInt(ctx.getArgs()[1]) - 1, 0);
		int maxpage = DexUtils.maxPage(commands_str.length, 5);
		if (page >= maxpage) page = maxpage - 1;

		ctx.getPlayer().sendMessage(plugin.getConfigString("help-page-header").replaceAll("\\Q%page%\\E", "" + (page+1)).replaceAll("\\Q%maxpage%\\E", "" + maxpage));
		DexUtils.paginate(ctx.getPlayer(), commands_str, page, 5);
	}
	
	public void wand(CommandContext ct) {
		if (!withPermission(ct.getPlayer(), "wand")) return;
		ItemStack wand = new ItemStack(Material.BLAZE_ROD);
		ItemMeta meta = wand.getItemMeta();
		meta.setDisplayName(plugin.getConfigString("wand-title", "§fDexterity Wand"));
		wand.setItemMeta(meta);
		ct.getPlayer().getInventory().addItem(wand);
	}
	
	public void debug_centers(CommandContext ct) {
		if (!ct.getPlayer().hasPermission("dexterity.admin")) return;
		DexterityDisplay d = getSelected(ct.getSession(), null);
		if (d == null) return;
		boolean entity_centers = ct.getFlags().contains("entities");
		for (DexBlock db : d.getBlocks()) {
			api.markerPoint(db.getLocation(), Math.abs(db.getRoll()) < 0.000001 ? Color.LIME : Color.AQUA, 6);
			if (entity_centers) api.markerPoint(db.getEntity().getLocation(), Color.ORANGE, 6);
		}
	}
	
	public void debug_removetransformation(CommandContext ct) {
		if (!ct.getPlayer().hasPermission("dexterity.admin")) return;
		DexterityDisplay d = getSelected(ct.getSession(), null);
		if (d == null) return;
		for (DexBlock db : d.getBlocks()) {
			db.getEntity().setTransformation(new DexTransformation(db.getEntity().getTransformation()).setDisplacement(new Vector(0, 0, 0)).setRollOffset(new Vector(0, 0, 0)).build());
			api.markerPoint(db.getEntity().getLocation(), Color.AQUA, 2);
		}
	}
	
	public void debug_testnear(CommandContext ct) {
		if (!ct.getPlayer().hasPermission("dexterity.admin")) return;
		ClickedBlockDisplay b = api.getLookingAt(ct.getPlayer());
		if (b == null) ct.getPlayer().sendMessage(cc + "None in range");
		else {
			api.markerPoint(b.getClickLocation(), Color.RED, 4);
			ct.getPlayer().sendMessage(cc + "Clicked " + b.getBlockFace() + ", " + b.getBlockDisplay().getBlock().getMaterial());
		}
	}
	
	public void debug_kill(CommandContext ct) {
		if (!ct.getPlayer().hasPermission("dexterity.admin")) return;
		HashMap<String, Double> attrs_d = ct.getDoubleAttrs();
		if (!attrs_d.containsKey("radius") && !attrs_d.containsKey("r")) {
			ct.getPlayer().sendMessage(plugin.getConfigString("must-enter-value").replaceAll("\\Q%value%\\E", "radius"));
			return;
		}
		double radius = attrs_d.getOrDefault("radius", attrs_d.get("r"));
		double min_scale = attrs_d.getOrDefault("min_scale", Double.MIN_VALUE), max_scale = attrs_d.getOrDefault("max_scale", Double.MAX_VALUE);
		
		List<Entity> entities = ct.getPlayer().getNearbyEntities(radius, radius, radius);
		for (Entity e : entities) {
			if (!(e instanceof BlockDisplay)) continue;
			BlockDisplay bd = (BlockDisplay) e;
			Vector3f scale = bd.getTransformation().getScale();
			if (scale.x < min_scale || scale.x > max_scale
					|| scale.y < min_scale || scale.y > max_scale 
					|| scale.z < min_scale || scale.z > max_scale) continue;
			e.remove();
		}
	}
	
	public void paste(CommandContext ct) {
		DexSession session = ct.getSession();
		if (session.getEditType() != null) {
			switch(session.getEditType()) {
			case CLONE_MERGE:
				if (session.getSecondary() != null) {
					session.getSecondary().hardMerge(session.getSelected());
				}
			case CLONE:
				ct.getPlayer().sendMessage(getConfigString("clone-success", session));
				break;
			default:
			}
			session.finishEdit();
		}
	}
	
	public void consolidate(CommandContext ct) {
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "consolidate");
		if (d == null) return;
		
		Mask mask = session.getMask();
		
		if (ct.getDefaultArg() != null) {
			try {
				Material mat = Material.valueOf(ct.getDefaultArg().toUpperCase().trim());
				mask = new Mask(mat);
			} catch (Exception ex) {
				ct.getPlayer().sendMessage(getConfigString("unknown-material", session).replaceAll("\\Q%input%\\E", ct.getDefaultArg().toLowerCase()));
				return;
			}
		}
		
		BlockTransaction t = new BlockTransaction(d, mask);
		d.consolidate(mask, t);
		session.pushTransaction(t); //commit is async
		
		ct.getPlayer().sendMessage(getConfigString("consolidate-success", session));
	}
	
	public void recenter(CommandContext ct) { //TODO: add -auto to recalculate
		Player p = ct.getPlayer();
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "recenter");
		if (d == null) return;
		
		if (!d.getCenter().getWorld().getName().equals(p.getLocation().getWorld().getName())) {
			p.sendMessage(getConfigString("must-same-world", session));
			return;
		}
		
		Location loc = p.getLocation();
		if (!ct.getFlags().contains("continuous")) DexUtils.blockLoc(loc).add(0.5, 0.5, 0.5);
		
		BlockTransaction t = new BlockTransaction(d);
		t.commitCenter(loc);
		t.commitEmpty();
		
		d.setCenter(loc);
		api.markerPoint(loc, Color.AQUA, 4);
		
		session.pushTransaction(t);
		
		p.sendMessage(getConfigString("recenter-success", session));
	}
	
	public void align(CommandContext ct) {
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "move");
		if (d == null) return;
		
		BlockTransaction t = new BlockTransaction(d);
		d.align();
		t.commit(d.getBlocks());
		session.pushTransaction(t);
		
		if (session.getFollowingOffset() != null) {
			Location loc2 = ct.getPlayer().getLocation();
			if (!ct.getPlayer().isSneaking()) DexUtils.blockLoc(loc2);
			session.setFollowingOffset(d.getCenter().toVector().subtract(loc2.toVector()));
		}
		
		ct.getPlayer().sendMessage(getConfigString("align-success", session));
	}
	
	public void axis(CommandContext ct) {
		Player p = ct.getPlayer();
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "axis");
		String[] args = ct.getArgs();
		if (d == null) return;
		
		if (args.length == 1) {
			p.sendMessage(getUsage("axis"));
			return;
		}
		
		if (args[1].equalsIgnoreCase("show")) {
			if (!p.hasPermission("dexterity.axis.show")) {
				p.sendMessage(noperm);
				return;
			}
			
			if (args.length == 2) session.setShowingAxes(AxisType.SCALE);
			else {
				if (args[2].equalsIgnoreCase("rotation")) session.setShowingAxes(AxisType.ROTATE);
				else if (args[2].equalsIgnoreCase("scale")) session.setShowingAxes(AxisType.SCALE);
				else {
					p.sendMessage(plugin.getConfigString("unknown-input").replaceAll("\\Q%input%\\E", args[2]));
					return;
				}
			}
		}
		else if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("reset")) {
			HashMap<String, Double> attrs_d = ct.getDoubleAttrs();
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
					return;
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
				return;
			}
			
		}
		else if (args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("hide")) {
			session.setShowingAxes(null);
		}
		else p.sendMessage(plugin.getConfigString("unknown-input").replaceAll("\\Q%input%\\E", args[1].toLowerCase()));
	}
	
	public void reload(CommandContext ct) {
		if (!withPermission(ct.getPlayer(), "admin")) return;
		plugin.reload();
		ct.getPlayer().sendMessage(plugin.getConfigString("reload-success"));
	}
	
	public void replace(CommandContext ct) {
		Player p = ct.getPlayer();
		DexSession session = ct.getSession();
		String[] args = ct.getArgs();
		DexterityDisplay d = getSelected(session, "replace");
		if (d == null) return;
		
		if (args.length >= 3) {
			Material from, to;
			try {
				from = Material.valueOf(args[1].toUpperCase());
			} catch (Exception ex) {
				p.sendMessage(getConfigString("unknown-material", session).replaceAll("\\Q%input%\\E", args[1].toLowerCase()));
				return;
			}
			try {
				to = Material.valueOf(args[2].toUpperCase());
			} catch (Exception ex) {
				p.sendMessage(getConfigString("unknown-material", session).replaceAll("\\Q%input%\\E", args[2].toLowerCase()));
				return;
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
	
	public void select(CommandContext ct) {
		int index = -1;
		Player p = ct.getPlayer();
		DexSession session = ct.getSession();
		String[] args = ct.getArgs();
		String def = ct.getDefaultArg();
		if (args[0].equals("pos1")) index = 0;
		else if (args[0].equals("pos2")) index = 1;
		
		if (index < 0) {
			if (args.length == 1) {
				p.sendMessage(getUsage("sel"));
				return;
			}
		}
		if (def != null) {
			DexterityDisplay disp = plugin.getDisplay(def);
			if (disp != null) {	
				session.setSelected(disp, true);
				return;
			} else if (index < 0) {
				p.sendMessage(plugin.getConfigString("display-not-found").replaceAll("\\Q%input%\\E", def));
				return;
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
				return;
			}
		}
		session.setContinuousLocation(loc, index == 0, new Vector(0, 0, 0), true);
	}
	
	public void deselect(CommandContext ct) {
		DexSession session = ct.getSession();
		if (session.getSelected() != null) {
			session.setSelected(null, false);
			session.clearLocationSelection();
			ct.getPlayer().sendMessage(plugin.getConfigString("desel-success"));
		}
	}
	
	public void highlight(CommandContext ct) {
		DexterityDisplay d = getSelected(ct.getSession(), "highlight");
		if (d == null) return;
		api.tempHighlight(d, 50, Color.ORANGE);
	}
	
	public void clone(CommandContext ct) {
		Player p = ct.getPlayer();
		DexSession session = ct.getSession();
		String def = ct.getDefaultArg();
		
		if (!withPermission(p, "clone") || testInEdit(session)) return;
		DexterityDisplay d = session.getSelected();
		if (d == null && def == null) {
			p.sendMessage(plugin.getConfigString("must-select-display"));
			return;
		}
		
		if (def != null) {
			d = plugin.api().getDisplay(def);
			if (d == null) {
				p.sendMessage(plugin.getConfigString("display-not-found").replaceAll("\\Q%input%\\E", def));
				return;
			}
			session.setSelected(d, false);
		}
		
		boolean mergeafter = ct.getFlags().contains("merge"), nofollow = ct.getFlags().contains("nofollow");
		if (mergeafter && !nofollow && !d.canHardMerge()) {
			p.sendMessage(getConfigString("cannot-clone", session));
			return;
		}
		
		DexterityDisplay clone = api.clone(d);
		
		if (!clone.getCenter().getWorld().getName().equals(p.getWorld().getName()) || clone.getCenter().distance(p.getLocation()) >= 80) clone.teleport(p.getLocation());
		
		if (nofollow) {
			p.sendMessage(getConfigString("clone-success", session));
			session.setSelected(clone, false);
		} else {
			p.sendMessage(getConfigString("to-finish-edit", session));
			session.startEdit(clone, mergeafter ? EditType.CLONE_MERGE : EditType.CLONE, true);
			session.startFollowing();
		}
	}
	
	public void owner(CommandContext ct) {
		Player p = ct.getPlayer();
		DexSession session = ct.getSession();
		String[] args = ct.getArgs();
		DexterityDisplay d = getSelected(session, "owner");
		if (d == null) return;
		if (!d.isSaved()) {
			p.sendMessage(getConfigString("not-saved", session));
			return;
		}
		
		if (args.length == 1 || args[1].equalsIgnoreCase("list")) {
			int page = 0;
			HashMap<String, Integer> attrs = ct.getIntAttrs();
			if (attrs.containsKey("page")) page = Math.max(attrs.get("page") - 1, 0);
			
			List<String> owners_str = new ArrayList<>();
			OfflinePlayer[] owners = d.getOwners();
			if (owners.length == 0) owners_str.add(cc + "- " + cc2 + "*");
			else {
				for (OfflinePlayer owner : owners) owners_str.add(cc + "- " + cc2 + owner.getName());
			}
			
			int maxpage = DexUtils.maxPage(owners_str.size(), 5);
			if (page >= maxpage) page = maxpage - 1;
			p.sendMessage(plugin.getConfigString("owner-list-header").replaceAll("\\Q%page%\\E", "" + (page+1)).replaceAll("\\Q%maxpage%\\E", "" + maxpage));
			DexUtils.paginate(p, owners_str.toArray(new String[owners_str.size()]), page, 5);
		}
		else if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) {
			boolean adding = args[1].equalsIgnoreCase("add");
			if (args.length <= 2) {
				p.sendMessage(getUsage("owner-" + (adding ? "add" : "remove")));
				return;
			}
			OfflinePlayer owner = Bukkit.getOfflinePlayer(args[2]);
			if (owner == null) {
				p.sendMessage(plugin.getConfigString("player-not-found").replaceAll("\\Q%player%\\E", args[2]));
				return;
			}
			
			if (adding) d.addOwner(owner);
			else d.removeOwner(owner);
			p.sendMessage(getConfigString(adding ? "owner-add-success" : "owner-remove-success", session).replaceAll("\\Q%player%\\E", owner.getName()));
			if (d.getOwners().length == 0) p.sendMessage(getConfigString("owner-remove-success-warning", session));
		}
		else p.sendMessage("unknown-subcommand");
	}
	
	public void tile(CommandContext ct) {
		Player p = ct.getPlayer();
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "tile");
		String def = ct.getDefaultArg();
		if (d == null) return;
		
		Vector delta = new Vector();
		HashMap<String, Double> attrs_d = ct.getDoubleAttrs();
		int count = Math.abs(attrs_d.getOrDefault("count", 0d).intValue());
		
		if (count == 0) { //check valid count
			if (def != null) {
				try {
					count = Integer.parseInt(def);
				} catch(Exception ex) {
					p.sendMessage(plugin.getConfigString("must-enter-value").replaceAll("\\Q%value%\\E", "count"));
					return;
				}
			}
			else {
				p.sendMessage(plugin.getConfigString("must-enter-value").replaceAll("\\Q%value%\\E", "count"));
				return;
			}
		}
		
		if (d.getBlocksCount()*(count+1) > session.getPermittedVolume()) { //check volume
			p.sendMessage(plugin.getConfigString("exceeds-max-volume").replaceAll("\\Q%volume%\\E", "" + (int) session.getPermittedVolume()));
			return;
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
			return;
		}
		
		Location loc = d.getCenter();
		DexterityDisplay toMerge = new DexterityDisplay(plugin, d.getCenter(), d.getScale());
		BuildTransaction t = new BuildTransaction(d);
		Vector centerv = d.getCenter().toVector();
		
		for (int i = 0; i < count; i++) {
			loc.add(delta);
			DexterityDisplay c = api.clone(d);
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
	
	public void cancel(CommandContext ct) {
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, null);
		if (d == null) return;
		session.cancelEdit();
		ct.getPlayer().sendMessage(getConfigString("cancelled-edit", session));
	}
	
	public void glow(CommandContext ct) { //TODO: add transaction
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "glow");
		if (d == null) return;
		
		String[] args = ct.getArgs();
		String def = ct.getDefaultArg();
		Player p = ct.getPlayer();
		
		boolean propegate = false; //flags.contains("propegate");
		if (args.length < 2 || (def != null && (def.equals("none") || def.equals("off"))) || ct.getFlags().contains("none") || ct.getFlags().contains("off")) {
			d.setGlow(null, propegate);
			p.sendMessage(getConfigString("glow-success-disable", session));
			return;
		}
		
		ColorEnum c;
		try {
			c = ColorEnum.valueOf(def.toUpperCase());
		} catch (Exception ex) {
			p.sendMessage(getConfigString("unknown-color", session).replaceAll("\\Q%input%\\E", args[1].toUpperCase()));
			return;
		}
		d.setGlow(c.getColor(), propegate);
		if (d.getLabel() != null) p.sendMessage(getConfigString("glow-success", session));
	}
	
	public void seat(CommandContext ct) {
		DexSession session = ct.getSession();
		Player p = ct.getPlayer();
		DexterityDisplay d = getSelected(session, "seat");
		if (d == null) return;
		if (!d.isSaved()) {
			p.sendMessage(getConfigString("not-saved", session));
			return;
		}
		
		Animation anim = d.getAnimation(RideableAnimation.class);
		
		if (anim == null) {
			HashMap<String, Double> attrs_d = ct.getDoubleAttrs();
			double y_offset = attrs_d.getOrDefault("y_offset", 0d);
			
			SitAnimation a = new SitAnimation(d);
			if (y_offset != 0) a.setSeatOffset(new Vector(0, y_offset, 0));
			d.addAnimation(a);
			p.sendMessage(getConfigString("seat-success", session));
		} else {
			d.removeAnimation(anim);
			p.sendMessage(getConfigString("seat-disable-success", session));
		}
	}
	
	public void command(CommandContext ct) {
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "cmd");
		if (d == null) return;
		
		Player p = ct.getPlayer();
		List<String> defs = ct.getDefaultArgs();
		String[] args = ct.getArgs();
		List<String> flags = ct.getFlags();
		
		if (args.length <= 1) {
			p.sendMessage(getUsage("cmd"));
			return;
		}
			
		if (args[1].equalsIgnoreCase("add")) {
			if (defs.size() == 1 || args.length < 2){
				p.sendMessage(getUsage("cmd-add"));
				return;
			}
			if (!d.isSaved()) {
				p.sendMessage(getConfigString("not-saved", session));
				return;
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
				return;
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
			
			HashMap<String, String> attr_str = ct.getStringAttrs();
			if (attr_str.containsKey("permission")) command.setPermission(attr_str.get("permission"));

			d.addCommand(command);
			p.sendMessage(getConfigString("cmd-add-success", session).replaceAll("\\Q%id%\\E", "" + d.getCommandCount()));
		}
		
		else if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("rem")) {
			if (args.length < 3) {
				p.sendMessage(getUsage("cmd-remove"));
				return;
			}
			
			int index;
			try {
				index = Integer.parseInt(args[2]);
				if (index != 0) index -= 1;
			} catch (Exception ex) {
				p.sendMessage(getConfigString("must-send-number", session));
				return;
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
	
	public void convert(CommandContext ct) {
		DexSession session = ct.getSession();
		Player p = ct.getPlayer();
		if (!withPermission(p, "convert") || testInEdit(session)) return;
		Location l1 = session.getLocation1(), l2 = session.getLocation2();
		if (l1 != null && l2 != null) {
			
			if (!l1.getWorld().getName().equals(l2.getWorld().getName())) {
				p.sendMessage(getConfigString("must-same-world-points", session));
				return;
			}
			double vol = session.getPermittedVolume();
			if (session.getSelectionVolume() > vol) {
				p.sendMessage(getConfigString("exceeds-max-volume", session).replaceAll("\\Q%volume%\\E", "" + vol));
				return;
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
	
	public void move(CommandContext ct) {
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "move");
		if (d == null) return;
		
		Player p = ct.getPlayer();
		List<String> flags = ct.getFlags();
		
		boolean same_world = d.getCenter().getWorld().getName().equals(p.getWorld().getName());
		if (ct.getArgs().length == 1 || !same_world) {
			if (!same_world) d.teleport(p.getLocation());
			session.startFollowing();
			session.startEdit(d, EditType.TRANSLATE, false, new BlockTransaction(d));
			p.sendMessage(getConfigString("to-finish-edit", session));
			return;
		}
		
		BlockTransaction t = new BlockTransaction(d);
		Location loc;
		if (flags.contains("continuous") || flags.contains("c")) loc = p.getLocation();
		else if (flags.contains("here")) loc = DexUtils.blockLoc(p.getLocation()).add(0.5, 0.5, 0.5);
		else loc = d.getCenter();
					
		HashMap<String,Double> attr_d = ct.getDoubleAttrs();
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
	
	public void save(CommandContext ct) {
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "save");
		if (d == null) return;
		
		String[] args = ct.getArgs();
		Player p = ct.getPlayer();
		
		if (args.length != 2) {
			if (args[0].equals("save")) p.sendMessage(getUsage("rename").replaceAll(" name", " save"));
			else p.sendMessage(getUsage("rename"));
			return;
		}
		if (d.getBlocksCount() == 0) {
			p.sendMessage(getConfigString("must-select-display", session));
			return;
		}
		if (args[1].startsWith("-") || args[1].contains(".")) {
			p.sendMessage(plugin.getConfigString("invalid-name").replaceAll("\\Q%input%\\E", args[1]));
			return;
		}
		
		if (d.setLabel(args[1])) {
			d.addOwner(p);
			p.sendMessage(getConfigString("rename-success", session));
		}
		else p.sendMessage(getConfigString("name-in-use", session).replaceAll("\\Q%input%\\E", args[1]));
	}
	
	public void unsave(CommandContext ct) {
		DexterityDisplay d;
		String def = ct.getDefaultArg();
		Player p = ct.getPlayer();
		DexSession session = ct.getSession();
		if (def != null) {
			d = plugin.getDisplay(def.toLowerCase());
			if (d == null) {
				p.sendMessage(getConfigString("display-not-found", session).replaceAll("\\Q%input%\\E", def));
				return;
			}
		} else {
			d = getSelected(session, "save");
			if (d == null) return;
			if (!d.isSaved()) {
				p.sendMessage(getConfigString("not-saved", session));
				return;
			}
		}
		
		String msg = getConfigString("unsave-success", session);
		d.unregister();
		p.sendMessage(msg);
	}
	
	public void undo(CommandContext ct) {
		int count = ct.getIntAttrs().getOrDefault("count", -1);
		if (count < 2) ct.getSession().undo();
		else ct.getSession().undo(count);
	}
	
	public void redo(CommandContext ct) {
		int count = ct.getIntAttrs().getOrDefault("count", -1);
		if (count < 2) ct.getSession().redo();
		else ct.getSession().redo(count);
	}
	
	public void mask(CommandContext ct) {
		String[] args = ct.getArgs();
		List<String> flags = ct.getFlags();
		DexSession session = ct.getSession();
		Player p = ct.getPlayer();
		
		if (args.length < 2 || flags.contains("none") || flags.contains("off") || ct.getDefaultArg().equals("none") || ct.getDefaultArg().equals("off")) {
			session.setMask(null);
			p.sendMessage(getConfigString("mask-success-disable", session));
		} else {
			Mask m = new Mask();
			for (String mat : ct.getDefaultArgs()) {
				try {
					m.addMaterialsList(mat);
				} catch (IllegalArgumentException ex) {
					p.sendMessage(plugin.getConfigString("unknown-material").replaceAll("\\Q%input%\\E", mat));
					return;
				}
			}
			
			if (flags.contains("invert")) m.setNegative(true);

			session.setMask(m);
			
			p.sendMessage(getConfigString("mask-success", session).replaceAll("\\Q%input%\\E", m.toString()));
		}
	}
	
	public void rotate(CommandContext ct) {
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "rotate");
		if (d == null) return;
		
		String[] args = ct.getArgs();
		List<String> flags = ct.getFlags();
		Player p = ct.getPlayer();
		
		if (args.length < 2) {
			p.sendMessage(getUsage("rotate"));
			return;
		}
		
		RotationPlan plan = new RotationPlan();
		boolean set = flags.contains("set");
		if (flags.contains("reset")) {
			plan.reset = true;
			set = true;
		}
		HashMap<String, Double> attrs_d = ct.getDoubleAttrs();
		List<String> defs_n = DexUtils.getDefaultAttributesWithFlags(args);
		
		plan.yaw_deg = attrs_d.getOrDefault("yaw", Double.MAX_VALUE);
		plan.pitch_deg = attrs_d.getOrDefault("pitch", Double.MAX_VALUE);
		plan.roll_deg = attrs_d.getOrDefault("roll", Double.MAX_VALUE);
		plan.y_deg = attrs_d.getOrDefault("y", Double.MAX_VALUE);
		plan.x_deg = attrs_d.getOrDefault("x", Double.MAX_VALUE);
		plan.z_deg = attrs_d.getOrDefault("z", Double.MAX_VALUE);
		
		try {
			switch(Math.min(ct.getDefaultArgs().size(), 6)) {
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
			return;
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
			return;
		}
		
		//t.commit(); //async, done in callback
		session.pushTransaction(t);
	}
	
	public void schematic(CommandContext ct) {
		Player p = ct.getPlayer();
		if (!withPermission(p, "schem")) return;
		String[] args = ct.getArgs();
		DexSession session = ct.getSession();

		if (args.length == 1) p.sendMessage(getUsage("schematic"));
		else if (args[1].equalsIgnoreCase("import") || args[1].equalsIgnoreCase("load")) { //d schem import, /d schem load
			if (!withPermission(p, "schem.import") || testInEdit(session)) return;
			
			if (args.length == 2 || ct.getDefaultArgs().size() < 2) {
				p.sendMessage(getUsage("schem"));
				return;
			}

			String name = ct.getDefaultArgs().get(1);
			DexterityDisplay d;
			Schematic schem;
			try {
				schem = new Schematic(plugin, name);
				d = schem.paste(p.getLocation());
				d.addOwner(p);
			} catch (Exception ex) {
				ex.printStackTrace();
				p.sendMessage(plugin.getConfigString("console-exception"));
				return;
			}
			session.setSelected(d, false);
			p.sendMessage(getConfigString("schem-import-success", session).replaceAll("\\Q%author%\\E", schem.getAuthor()));
		}
		else if (args[1].equalsIgnoreCase("export") || args[1].equalsIgnoreCase("save")) { //d schem export, /d schem save
			if (!withPermission(p, "schem.export")) return;
			DexterityDisplay d = getSelected(session, "export");
			if (d == null || testInEdit(session)) return;

			if (!d.isSaved()) {
				p.sendMessage(plugin.getConfigString("not-saved"));
				return;
			}
			
			SchematicBuilder builder = new SchematicBuilder(plugin, d);
			String author = p.getName();
			HashMap<String, String> attr_str = ct.getStringAttrs();
			if (attr_str.containsKey("author")) author = attr_str.get("author");
			boolean overwrite = ct.getFlags().contains("overwrite");
			
			int res = builder.save(d.getLabel().toLowerCase(), author, overwrite);
			
			if (res == 0) p.sendMessage(getConfigString("schem-export-success", session));
			else if (res == 1) p.sendMessage(getConfigString("file-already-exists", session).replaceAll("\\Q%input%\\E", "/schematics/" + d.getLabel().toLowerCase() + ".dexterity"));
			else if (res == -1) p.sendMessage(getConfigString("console-exception", session));
		}
		else if (args[1].equalsIgnoreCase("delete")) { //d schem delete
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
		else if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("lsit")) { //d schem list
			int page = 0;
			HashMap<String, Integer> attrs = ct.getIntAttrs();
			if (attrs.containsKey("page")) page = Math.max(attrs.get("page") - 1, 0);
			else if (args.length >= 3) page = Math.max(DexUtils.parseInt(args[2]) - 1, 0);
			List<String> schems = listSchematics();
			
			if (schems.size() == 0) {
				p.sendMessage(plugin.getConfigString("list-empty"));
				return;
			}
			
			String[] formatted = new String[schems.size()];
			for (int i = 0; i < schems.size(); i++) formatted[i] = cc + "- " + cc2 + schems.get(i);
			
			int maxpage = DexUtils.maxPage(schems.size(), 5);
			if (page >= maxpage) page = maxpage - 1;
			p.sendMessage(plugin.getConfigString("schem-list-header").replaceAll("\\Q%page%\\E", "" + (page+1)).replaceAll("\\Q%maxpage%\\E", "" + maxpage));
			DexUtils.paginate(p, formatted, page, 5);
		}
		else p.sendMessage(plugin.getConfigString("unknown-subcommand"));
	}
	
	public void info(CommandContext ct) {
		DexterityDisplay d = getSelected(ct.getSession(), null);
		if (d == null) return;
		String msg = plugin.getConfigString(d.getLabel() == null ? "info-format" : "info-format-saved")
				.replaceAll("\\Q%count%\\E", "" + d.getBlocksCount())
				.replaceAll("\\Q%world%\\E", d.getCenter().getWorld().getName());
		if (d.getLabel() != null) msg = msg.replaceAll("\\Q%label%\\E", d.getLabel());
		
		ct.getPlayer().sendMessage(msg);
		api.markerPoint(d.getCenter(), Color.AQUA, 4);
	}
	
	public void remove(CommandContext ct) {
		DexSession session = ct.getSession();
		Player p = ct.getPlayer();
		boolean res = !(ct.getArgs()[0].equals("remove") || ct.getArgs()[0].equals("rm"));
		if ((res && !withPermission(p, "remove")) || (!res && !withPermission(p, "deconvert"))) return;
		
		if (testInEdit(session)) return;
		DexterityDisplay d = session.getSelected();
		if (d == null) {
			String def = ct.getDefaultArg();
			if (def == null) return;
			d = plugin.getDisplay(def);
			if (d == null) {
				p.sendMessage(plugin.getConfigString("display-not-found").replaceAll("\\Q%input%\\E", def));
				return;
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
	
	public void list(CommandContext ct) {
		Player p = ct.getPlayer();
		if (!withPermission(p, "list")) return;
		
		HashMap<String, Integer> attrs = ct.getIntAttrs();
		HashMap<String, String> attr_str = ct.getStringAttrs();
		
		int page = 0;
		if (attrs.containsKey("page")) {
			page = Math.max(attrs.get("page") - 1, 0);
		} else if (ct.getArgs().length >= 2) page = Math.max(DexUtils.parseInt(ct.getArgs()[1]) - 1, 0);			
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
			return;
		}
		
		p.sendMessage(plugin.getConfigString("list-page-header").replaceAll("\\Q%page%\\E", "" + (page+1)).replaceAll("\\Q%maxpage%\\E", ""+maxpage));
		String[] strs = new String[total];
		int i = 0;
		for (DexterityDisplay disp : plugin.getDisplays()) {
			if (disp.getLabel() == null || (w != null && !disp.getCenter().getWorld().getName().equals(w))) continue;
			DexSession session = ct.getSession();
			i += constructList(strs, disp, session.getSelected() == null ? null : session.getSelected().getLabel(), i, 0);
		}
		DexUtils.paginate(p, strs, page, 10);
	}
	
	public void scale(CommandContext ct) {
		DexSession session = ct.getSession();
		DexterityDisplay d = getSelected(session, "scale");
		if (d == null) return;
		
		boolean set = ct.getFlags().contains("set");
		HashMap<String, Double> attrsd = ct.getDoubleAttrs();
		Player p = ct.getPlayer();
		
		ScaleTransaction t = new ScaleTransaction(d);
		Vector scale = new Vector();
		String scale_str;
		if (attrsd.containsKey("x") || attrsd.containsKey("y") || attrsd.containsKey("z")) {
			float sx = Math.abs(attrsd.getOrDefault("x", set ? d.getScale().getX() : 1).floatValue());
			float sy = Math.abs(attrsd.getOrDefault("y", set ? d.getScale().getY() : 1).floatValue());
			float sz = Math.abs(attrsd.getOrDefault("z", set ? d.getScale().getZ() : 1).floatValue());
			
			if (sx == 0 || sy == 0 || sz == 0) {
				p.sendMessage(getConfigString("must-send-number", session));
				return;
			}
			
			scale_str = sx + ", " + sy + ", " + sz;
			scale = new Vector(sx, sy, sz);
		} else {
			float scalar;
			try {
				scalar = Float.parseFloat(ct.getDefaultArg());
			} catch(Exception ex) {
				p.sendMessage(getConfigString("must-send-number", session));
				return;
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
			return;
		}

		t.commit();
		session.pushTransaction(t);
	}
	
	public void merge(CommandContext ct) {
		Player p = ct.getPlayer();
		DexSession session = ct.getSession();
		String def = ct.getDefaultArg();
		if (def == null) {
			p.sendMessage(getUsage("merge"));
			return;
		}
		DexterityDisplay d = getSelected(session, "merge");
		if (d == null || testInEdit(session)) return;
		DexterityDisplay parent = plugin.getDisplay(def);
		if (parent == null) {
			p.sendMessage(plugin.getConfigString("display-not-found").replaceAll("\\Q%input%\\E", def));
			return;
		}
		HashMap<String, String> attr_str = ct.getStringAttrs();
		String new_group = attr_str.get("new_group");
		if (d == parent || d.equals(parent)) {
			p.sendMessage(getConfigString("must-be-different", session));
			return;
		}
		if (!d.getCenter().getWorld().getName().equals(parent.getCenter().getWorld().getName())) {
			p.sendMessage(getConfigString("must-same-world", session));
			return;
		}
		if (d.getParent() != null) {
			p.sendMessage(getConfigString("cannot-merge-subgroups", session));
			return;
		}
		if (d.containsSubdisplay(parent)) {
			p.sendMessage(getConfigString("already-merged", session));
			return;
		}
		if (new_group != null && plugin.getDisplayLabels().contains(new_group)) {
			p.sendMessage(getConfigString("group-name-in-use", session));
			return;
		}
		
		boolean hard = true; //flags.contains("hard");
		if (hard) {
			if (!d.canHardMerge() || !parent.canHardMerge()) {
				p.sendMessage(getConfigString("cannot-hard-merge", session));
				return;
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
	
	
}
