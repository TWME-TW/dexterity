package me.c7dev.tensegrity.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DexUtils {
	
	public static double round(double d, int decimals) {
		int a = (int) (d * Math.pow(10, decimals));
		if (decimals == 0) return a;
		return a / (double) Math.pow(10, decimals);
	}
	
	public static String locationString(Location loc, int decimals) {
		if (decimals == 0) {
			return "X:" + (int) loc.getX() + 
					" Y:" + (int) loc.getY() +
					" Z:" + (int) loc.getZ();
		}
		return "X:" + DexUtils.round(loc.getX(), decimals) + 
		" Y:" + DexUtils.round(loc.getY(), decimals) +
		" Z:" + DexUtils.round(loc.getZ(), decimals);
	}
	
	public static Location blockLoc(Location loc) {
		loc.setX(loc.getBlockX());
		loc.setY(loc.getBlockY());
		loc.setZ(loc.getBlockZ());
		loc.setYaw(0);
		loc.setPitch(0);
		return loc;
	}
	
	public static String attrAlias(String s) {
		switch(s.toLowerCase()) {
		case "r": return "radius";
		case "g": return "granularity";
		case "f": return "filled";
		case "east": return "x";
		case "west": return "x";
		case "up": return "y";
		case "down": return "y";
		case "north": return "z";
		case "south": return "z";
		default: return s.toLowerCase();
		}
	}
	
	public static int valueAlias(String s) {
		switch(s.toLowerCase()) {
		case "true": return 1;
		case "yes": return 1;
		case "y": return 1;
		case "filled": return 1;
		case "t": return 1;
		case "false": return 0;
		case "no": return 0;
		case "n": return 0;
		case "unfilled": return 0;
		case "f": return 0;
		default: 
			try {
				return Integer.parseInt(s);
			} catch(Exception ex){
				return 1;
			}
		}
	}
	
	public static String stringValueAlias(String s) {
		switch(s.toLowerCase()) {
		case "zx": return "xz";
		case "yx": return "xy";
		case "yz": return "zy";
		default: return s.toLowerCase();
		}
	}
	
	public static HashMap<String,Integer> getAttributes(String[] args){
		HashMap<String,Integer> attr = new HashMap<>();
		
		for (String arg : args) {
			String[] argsplit = arg.split("[=,:]");
			if (argsplit.length > 0) {
				attr.put(attrAlias(argsplit[0]), valueAlias(argsplit[argsplit.length-1]));
			}
		}
		
		return attr;
	}
	
	public static HashMap<String,String> getAttributesStrings(String[] args){
		HashMap<String,String> attr = new HashMap<>();
		
		for (String arg : args) {
			String[] argsplit = arg.split("[=,:]");
			if (argsplit.length > 0) {
				attr.put(attrAlias(argsplit[0]), stringValueAlias(argsplit[argsplit.length-1]));
			}
		}
		
		return attr;
	}
	
	public static HashMap<String,Double> getAttributesDoubles(String[] args){
		HashMap<String,Double> attr = new HashMap<>();
		
		for (String arg : args) {
			String[] argsplit = arg.split("[=,:]");
			if (argsplit.length > 0) {
				String alias = attrAlias(argsplit[0]);
				try {
					double d = Double.parseDouble(argsplit[argsplit.length-1]);
					
					if (argsplit[0].equalsIgnoreCase("down") || argsplit[0].equalsIgnoreCase("west") || argsplit[0].equalsIgnoreCase("north")) d*=-1;
					
					attr.put(alias, d);
				} catch (Exception ex) {
					try {
						attr.put(alias, (double) valueAlias(argsplit[argsplit.length-1]));
					} catch (Exception ex2) {
						
					}
				}
			}
		}
		
		return attr;
	}
	
	public static List<String> getFlags(String[] args){
		List<String> flags = new ArrayList<String>();
		for (String arg : args) {
			if (arg.startsWith("-")) flags.add(arg.toLowerCase().replaceFirst("-", ""));
		}
		return flags;
	}
	
	public static String getDefaultAttribute(String[] args) {
		String firstarg = null;
		int i =0;
		for (String arg : args) {
			if (!arg.contains("=") && !arg.contains(":")) {
				if (i == 0) firstarg = arg.toLowerCase();
				else return arg.toLowerCase();
			}
			i++;
		}
		return firstarg;
	}
	
	public static int parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception ex) {
			return -2;
		}
	}
	
	public static BlockDisplay getClickedBlockDisplay(Player p) {
		BlockDisplay nearest = null;
		double angle = 1000;
		Vector dir = p.getLocation().getDirection();
		
		for (Entity e : p.getNearbyEntities(5, 5, 5)) {
			if (e instanceof BlockDisplay) {
				Vector disp = e.getLocation().add(0.5, 0.5, 0.5).toVector().subtract(p.getEyeLocation().toVector());
				double angle1 = dir.angle(disp);
				if ((nearest == null || angle1 < angle) && disp.length() <= 6) {
					angle = angle1;
					nearest = (BlockDisplay) e;
				}
			}
		}
				
		if (angle < 0.4) return nearest;
		else return null;
	}
	
	public static Location deserializeLocation(FileConfiguration config, String dir) {
		if (config.get(dir) == null) return null;
		
		double x = config.getDouble(dir + ".x");
		double y = config.getDouble(dir + ".y");
		double z = config.getDouble(dir + ".z");
		double yaw = config.getDouble(dir + ".yaw");
		double pitch = config.getDouble(dir + ".pitch");
		String world = config.getString(dir + ".world");
		return new Location(Bukkit.getWorld(world), x, y, z, (float) yaw, (float) pitch);
	}

}
