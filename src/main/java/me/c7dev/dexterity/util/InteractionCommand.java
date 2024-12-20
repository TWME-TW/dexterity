package me.c7dev.dexterity.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Stores information about a configured command that is to run when a player clicks a display
 */
public class InteractionCommand {
	
	private String cmd, perm;
	private boolean left = true, right = true, player = false;
	
	public InteractionCommand(String cmd) {
		this.cmd = cmd;
	}
	
	public InteractionCommand(ConfigurationSection s) {
		cmd = s.getString("cmd");
		left = s.getBoolean("left-click", true);
		right = s.getBoolean("right-click", true);
		player = s.getBoolean("by-player", true);
		perm = s.getString("permission");
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public boolean isLeft() {
		return left;
	}

	public void setLeft(boolean left) {
		this.left = left;
	}

	public boolean isRight() {
		return right;
	}

	public void setRight(boolean right) {
		this.right = right;
	}

	public boolean isByPlayer() {
		return player;
	}

	public void setByPlayer(boolean player) {
		this.player = player;
	}
	
	public void setPermission(String s) {
		if (s.length() > 0) perm = s;
		else perm = null;
	}
	
	public String getPermission() {
		return perm;
	}
	
	public void exec(Player p, boolean right_click) {
		if (!right && right_click) return;
		if (!left && !right_click) return;
		if (perm != null && !p.hasPermission(perm)) return;
		String c = cmd.replaceAll("\\Q%player%\\E|\\Q%name%\\E", p.getName()); //TODO papi
		
		if (player) Bukkit.dispatchCommand(p, c);
		else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
	}
	
	public Map<String, Object> serialize() {
		Map<String, Object> m = new HashMap<>();
		m.put("cmd", cmd);
		if (!left) m.put("left-click", left);
		if (!right) m.put("right-click", right);
		if (!player) m.put("by-player", player);
		if (perm != null) m.put("permission", perm);
		return m;
	}
	
}
