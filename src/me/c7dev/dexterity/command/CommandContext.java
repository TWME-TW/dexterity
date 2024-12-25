package me.c7dev.dexterity.command;

import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.util.DexUtils;

/**
 * Context required for any in-game /dex command
 */
public class CommandContext {

	private Player p;
	private String[] args;
	private Dexterity plugin;
	private DexSession session;
	private HashMap<String,Integer> attr;
	private HashMap<String,String> attr_str;
	private HashMap<String, Double> attr_d;
	private List<String> flags, defs;
	
	public CommandContext(Dexterity plugin, Player p, String[] args) {
		this.p = p;
		this.args = args;
		this.plugin = plugin;
		this.session = plugin.getEditSession(p.getUniqueId());
	}
	
	public HashMap<String,Integer> getIntAttrs() {
		if (attr == null) attr = DexUtils.getAttributes(args);
		return attr;
	}
	
	public HashMap<String, String> getStringAttrs(){
		if (attr_str == null) attr_str = DexUtils.getAttributesStrings(args);
		return attr_str;
	}
	
	public HashMap<String, Double> getDoubleAttrs(){
		if (attr_d == null) attr_d = DexUtils.getAttributesDoubles(args);
		return attr_d;
	}
	
	public List<String> getFlags(){
		if (flags == null) flags = DexUtils.getFlags(args);
		return flags;
	}
	
	public List<String> getDefaultArgs(){
		if (defs == null) defs = DexUtils.getDefaultAttributes(args);
		return defs;
	}
	
	public String getDefaultArg() {
		List<String> ldefs = getDefaultArgs();
		return ldefs.size() > 0 ? ldefs.get(0) : null;
	}
	
	public Player getPlayer() {
		return p;
	}
	
	public String[] getArgs() {
		return args;
	}
	
	public Dexterity getPlugin() {
		return plugin;
	}
	
	public DexSession getSession() {
		return session;
	}

}
