package me.c7dev.tensegrity.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class DexUtils {
	
	public static ItemStack createItem(Material material, int amount, String name, String... lore) {
		ItemStack item = new ItemStack(material, amount);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		List<String> lore2 = new ArrayList<String>();
		for (String s : lore) lore2.add(s);
		meta.setLore(lore2);
		item.setItemMeta(meta);
		return item;
	}
	
	public static double round(double d, int decimals) {
		int a = (int) (d * Math.pow(10, decimals));
		if (decimals == 0) return a;
		return a / (double) Math.pow(10, decimals);
	}
	
	public static String locationString(Location loc, int decimals) {
		return vectorString(loc.toVector(), decimals);
	}
	
	public static String vectorString(Vector loc, int decimals) {
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
	
	public static Vector3f vector(Vector v) {
		return new Vector3f((float) v.getX(), (float) v.getY(), (float) v.getZ());
	}
	public static Vector vector(Vector3f v) {
		return new Vector(v.x, v.y, v.z);
	}
	public static Vector hadimard(Vector a, Vector b) {
		return new Vector(a.getX()*b.getX(), a.getY()*b.getY(), a.getZ()*b.getZ());
	}
	public static Location location(World w, Vector v) {
		return new Location(w, v.getX(), v.getY(), v.getZ(), 0, 0);
	}
	
	public static int maxPage(int size, int pagelen) {
		return (size/pagelen) + (size % pagelen > 0 ? 1 : 0);
	}
	
	public static void paginate(Player p, String[] strs, int page, int pagelen) {
		int maxpage = (strs.length/pagelen) + (strs.length % pagelen > 0 ? 1 : 0);
		if (page >= maxpage) page = maxpage - 1;
		int pagestart = pagelen*page;
		int pageend = Math.min(pagestart + pagelen, strs.length);
				
		for (int i = pagestart; i < pageend; i++) {
			if (strs[i] != null) p.sendMessage(strs[i]);
		}
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
	
	public static Vector getBlockSize(BlockData b) {
		Material mat = b.getMaterial();
		String m = mat.toString();
		if (m.endsWith("_SLAB") || m.endsWith("_CARPET")) return new Vector(1, 0.5, 1);
		if (m.endsWith("_TRAPDOOR")) return new Vector(1, 1.0/16, 1);
		if (m.endsWith("_BED")) return new Vector(1, 9.0/16, 1);
		
		//TODO doors, fences, gates
		
		switch(mat) {
		case SNOW:
			Snow sd = (Snow) b;
			return new Vector(1, sd.getLayers() / 8.0, 1);
		case CHAIN: return new Vector(0.2, 1, 0.2);
		default:
		}
		return new Vector(1, 1, 1);
	}
	
	public static Vector nearestPoint(Vector a, Vector b, Vector x) {
		Vector b_a = b.clone().subtract(a);
		double theta = b_a.dot(x.clone().subtract(a)) / b_a.lengthSquared();
		return a.clone().multiply(1-theta).add(b.clone().multiply(theta));
	}

}
