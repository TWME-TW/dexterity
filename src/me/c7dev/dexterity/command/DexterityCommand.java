package me.c7dev.dexterity.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.ColorEnum;
import me.c7dev.dexterity.util.DexUtils;

/**
 * Command registration class - for API, use {@link CommandHandler}
 */
public class DexterityCommand implements CommandExecutor, TabCompleter {
	
	private Dexterity plugin;
	private String cc, cc2;
	private CommandHandler handler;
	
	private String[] commands = {
		"align", "axis", "clone", "command", "consolidate", "convert", "deconvert", "deselect", "glow", "highlight", "info", "list", "mask", 
		"merge", "move", "name", "owner", "pos1", "recenter", "redo", "reload", "remove", "replace", "rotate", "scale", "schem", "seat", "select", 
		"undo", "unsave", "tile", "wand"
	};
	private String[] descriptions = new String[commands.length];
	private String[] command_strs = new String[commands.length];
	
	public DexterityCommand(Dexterity plugin) {
		this.plugin= plugin;
		cc = plugin.getChatColor();
		cc2 = plugin.getChatColor2();
		plugin.getCommand("dex").setExecutor(this);
		
		handler = new CommandHandler(plugin);
		
		for (int i = 0; i < commands.length; i++) {
			descriptions[i] = plugin.getConfigString(commands[i] + "-description");
			command_strs[i] = cc + "- " + cc2 + "/d " + commands[i] + " §8- " + cc + descriptions[i];
		}
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
			p.sendMessage(plugin.getConfigString("no-permission"));
			return true;
		}
		
		args[0] = args[0].toLowerCase();
		CommandContext ctx = new CommandContext(plugin, p, args);
		
		switch(args[0]) {
		case "help":
		case "?":
			handler.help(ctx, command_strs);
			return true;
		case "wand":
			handler.wand(ctx);
			return true;
		case "debug:centers":
			handler.debug_centers(ctx);
			return true;
		case "debug:removetransformation":
			handler.debug_removetransformation(ctx);
			return true;
		case "debug:resettransformation":
			handler.debug_resettransformation(ctx);
			return true;
		case "debug:testnear":
			handler.debug_testnear(ctx);
			return true;
		case "debug:kill":
			handler.debug_kill(ctx);
			return true;
		case "paste":
		case "set":
		case "p":
			handler.paste(ctx);
			return true;
		case "consolidate":
			handler.consolidate(ctx);
			return true;
		case "recenter":
			handler.recenter(ctx);
			return true;
		case "align":
			handler.align(ctx);
			return true;
		case "axis":
			handler.axis(ctx);
			return true;
		case "reload":
			handler.reload(ctx);
			return true;
		case "replace":
		case "rep":
			handler.replace(ctx);
			return true;
		case "sel":
		case "select":
		case "pos1":
		case "pos2":
		case "load":
			handler.select(ctx);
			return true;
		case "desel":
		case "deselect":
		case "clear":
			handler.deselect(ctx);
			return true;
		case "highlight":
		case "h":
			handler.highlight(ctx);
			return true;
		case "clone":
			handler.clone(ctx);
			return true;
		case "owner":
		case "owners":
			handler.owner(ctx);
			return true;
		case "tile":
		case "stack":
			handler.tile(ctx);
			return true;
		case "cancel":
		case "quit":
			handler.cancel(ctx);
			return true;
		case "glow":
			handler.glow(ctx);
			return true;
		case "seat":
		case "sittable":
			handler.seat(ctx);
			return true;
		case "command":
		case "cmd":
			handler.command(ctx);
			return true;
		case "convert":
		case "conv":
			handler.convert(ctx);
			return true;
		case "move":
		case "m":
			handler.move(ctx);
			return true;
		case "save":
		case "label":
		case "name":
		case "rename":
			handler.save(ctx);
			return true;
		case "undo":
		case "u":
			handler.undo(ctx);
			return true;
		case "redo":
			handler.redo(ctx);
			return true;
		case "unsave":
			handler.unsave(ctx);
			return true;
		case "mask":
			handler.mask(ctx);
			return true;
		case "rotate":
		case "r":
			handler.rotate(ctx);
			return true;
		case "schem":
		case "schematic":
			handler.schematic(ctx);
			return true;
		case "info":
		case "i":
			handler.info(ctx);
			return true;
		case "rm":
		case "remove":
		case "restore":
		case "deconvert":
		case "deconv":
			handler.remove(ctx);
			return true;
		case "list":
		case "lsit":
			handler.list(ctx);
			return true;
		case "scale":
		case "s":
		case "skew":
			handler.scale(ctx);
			return true;
		case "merge":
			handler.merge(ctx);
			return true;
		default:
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
		
		switch(argsr[0]) {
		case "list":
			ret.add("world=");
		case "?":
		case "help":
			ret.add("page=");
			return ret;
		case "consolidate":
		case "replace":
		case "rep":
			int argthreshold = argsr[0].equals("consolidate") ? 2 : 3;
			if (argsr.length <= argthreshold && argsr[argsr.length - 1].length() >= 2) {
				ret = DexUtils.materials(argsr[argsr.length - 1]);
			}
			return ret;
		case "mask":
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
			return ret;
		case "recenter":
			ret.add("-continuous");
			return ret;
		case "r":
		case "rotate":
			ret.add("yaw=");
			ret.add("pitch=");
			ret.add("roll=");
			ret.add("-reset");
		case "scale":
		case "s":
		case "skew":
			ret.add("x=");
			ret.add("y=");
			ret.add("z=");
			ret.add("-set");
			return ret;
			
		case "tile":
		case "stack":
			ret.add("count=");
		case "move":
		case "m":
			ret.add("x=");
			ret.add("y=");
			ret.add("z=");
			ret.add("rx=");
			ret.add("ry=");
			ret.add("rz=");
			if (argsr[0].equals("move") || argsr[0].equals("m")) {
				ret.add("north=");
				ret.add("south=");
				ret.add("east=");
				ret.add("west=");
				ret.add("up=");
				ret.add("down=");
				ret.add("-here");
				ret.add("-continuous");
			}
			return ret;
		case "glow":
			ret.add("-none");
			for (ColorEnum c : ColorEnum.values()) ret.add(c.toString());
			return ret;
		case "sittable":
		case "seat":
			ret.add("y_offset=");
			return ret;
		case "clone":
			ret.add("-nofollow");
			ret.add("-merge");
			return ret;
		case "undo":
		case "u":
		case "redo":
			ret.add("count=");
			return ret;
		case "command":
		case "cmd":
			if (argsr.length == 1) return ret;
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
			return ret;
		case "owner":
			if (argsr.length == 1) return ret;
			if (argsr.length == 2) {
				ret.add("list");
				ret.add("add");
				ret.add("remove");
			}
			else if (argsr[1].equalsIgnoreCase("list")) {
				if (argsr.length == 3) ret.add("page=");
			}
			else if (argsr[1].equalsIgnoreCase("add")) {
				for (Player p : Bukkit.getOnlinePlayers()) ret.add(p.getName());
			}
			else if (argsr[1].equalsIgnoreCase("remove")) {
				Player p = (Player) sender;
				DexSession session = plugin.getEditSession(p.getUniqueId());
				DexterityDisplay s = session.getSelected();
				if (s != null) {
					OfflinePlayer[] owners = s.getOwners();
					for (OfflinePlayer owner : owners) ret.add(owner.getName());
				}
			}
			return ret;
		case "axis":
			if (argsr.length == 1) return ret;
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
			return ret;
		case "schem":
		case "schematic":
			if (argsr.length == 1) return ret;
			if (argsr.length == 2) {
				ret.add("load");
				ret.add("save");
				ret.add("delete");
				ret.add("list");
			}
			else if (argsr.length == 3) {
				if (argsr[1].equalsIgnoreCase("export") || argsr[1].equalsIgnoreCase("save")) {
					ret.add("author=");
					ret.add("-overwrite");
				}
				else if (argsr[1].equalsIgnoreCase("import") || argsr[1].equalsIgnoreCase("load") || argsr[1].equalsIgnoreCase("delete")) {
					ret = handler.listSchematics();
				}
				else if (argsr[1].equalsIgnoreCase("list") || argsr[1].equalsIgnoreCase("lsit")) ret.add("page=");
			}
			return ret;
		case "debug:centers":
			if (sender.hasPermission("dexterity.admin")) ret.add("-entities");
			return ret;
		case "debug:kill":
			if (sender.hasPermission("dexterity.admin")) {
				ret.add("radius=");
				ret.add("min_scale=");
				ret.add("max_scale=");
			}
			return ret;
		}
		
		switch(argsr[0]) {
		case "sel":
		case "select":
		case "remove":
		case "restore":
		case "deconvert":
		case "deconv":
		case "rm":
			Player p = (Player) sender;
			for (String s : plugin.getDisplayLabels(p)) ret.add(s);
			return ret;
		}
		
		if (argsr[0].startsWith("debug:") && sender.hasPermission("dexterity.admin")) {
			if (argsr.length == 1) {
				ret.add("debug:centers");
				ret.add("debug:testnear");
				ret.add("debug:resettransformation");
				ret.add("debug:kill");
				return ret;
			}
		}
		
		return ret;
	}

}
