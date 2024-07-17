package me.c7dev.tensegrity.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Snow;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.joml.Matrix3d;
import org.joml.Vector3d;
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
	
	public static List<String> getDefaultAttributes(String[] args) {
		List<String> r = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			String arg = args[i];
			if (!arg.contains("=") && !arg.contains(":") && !arg.startsWith("-")) {
				r.add(arg.toLowerCase());
			}
		}
		return r;
	}
	
	public static double faceToDirection(BlockFace face, Vector scale) {
		switch(face) {
		case UP: return scale.getY();
		case DOWN: return -scale.getY();
		case EAST: return scale.getX();
		case WEST: return -scale.getX();
		case SOUTH: return scale.getZ();
		case NORTH: return -scale.getZ();
		default: return 1;
		}
	}
	public static double faceToDirectionAbs(BlockFace face, Vector scale) {
		switch(face) {
		case UP: 
		case DOWN: return scale.getY();
		case EAST: 
		case WEST: return scale.getX();
		case SOUTH: 
		case NORTH: return scale.getZ();
		default: return 1;
		}
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
	public static Vector3d vectord(Vector v) {
		return new Vector3d(v.getX(), v.getY(), v.getZ());
	}
	public static Vector vector(Vector3f v) {
		return new Vector(v.x, v.y, v.z);
	}
	public static Vector vector(Vector3d v) {
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
	
	public static Vector getBlockDimensions(BlockData b) {
		Material mat = b.getMaterial();
		String m = mat.toString();
		if (m.endsWith("_SLAB")) return new Vector(1, 0.5, 1);
		if (m.endsWith("_TRAPDOOR")) return new Vector(1, 3.0/16, 1);
		if (m.endsWith("_FENCE")) return new Vector(0.25, 1, 0.25);
		if (m.endsWith("_BED")) return new Vector(1, 9.0/16, 1);
		if (m.endsWith("TORCH")) return new Vector(0.125, 0.625, 0.125);
		if (m.endsWith("LANTERN")) return new Vector(0.375, 11.0/16, 0.375);
		if (m.endsWith("_WALL")) return new Vector(0.5, 1, 0.5);
		if (m.endsWith("_CARPET") || m.endsWith("_PRESSURE_PLATE")) return new Vector(1, 1.0/16, 1);
		if (m.startsWith("POTTED")) {
			if (m.endsWith("_SAPLING")) return new Vector(0.625, 1, 0.625);
		}
		if (m.endsWith("CANDLE")) return new Vector(0.125, 7.0/16, 0.125);
		if (m.endsWith("RAIL")) return new Vector(1, 0.125, 1);
		
		//TODO doors, fences, gates
		BlockFace facing = null;
		if (b instanceof Directional) {
			Directional bd = (Directional) b;
			facing = bd.getFacing();
		}

		switch(mat) {
		case SNOW:
			Snow sd = (Snow) b;
			return new Vector(1, sd.getLayers() / 8.0, 1);
		case END_ROD:
		case LIGHTNING_ROD:
		case CHAIN: return new Vector(0.25, 1, 0.25);
		case IRON_BARS: return new Vector(0.125, 1, 0.125);
		case END_PORTAL_FRAME: return new Vector(1, 13.0/16, 1);
		case FLOWER_POT: return new Vector(0.375, 0.375, 0.357);
		case POTTED_BROWN_MUSHROOM:
		case POTTED_RED_MUSHROOM: return new Vector(0.375, 9.0/16, 0.375);
		case POTTED_CACTUS: return new Vector(0.375, 1, 0.375);
		case NETHER_PORTAL: return new Vector(1, 1, 0.25);
		case PINK_PETALS: return new Vector(1, 3.0/16, 1);
		case DAYLIGHT_DETECTOR: return new Vector(1, 0.375, 1);
		case BELL:
			if (facing == BlockFace.WEST || facing == BlockFace.EAST) return new Vector(0.25, 1, 1);
			else return new Vector(1, 1, 0.25);
		case REPEATER:
		case COMPARATOR: return new Vector(1, 7.0/16, 1);
		default: return new Vector(1, 1, 1);
		}
	}
	
	public static Matrix3d rotMatDeg(double xdeg, double ydeg, double zdeg) {
		return rotMat(Math.toRadians(xdeg), Math.toRadians(ydeg), Math.toRadians(zdeg));
	}
	
	public static Matrix3d rotMat(double xrad, double yrad, double zrad) {
		double sinx = Math.sin(xrad), siny = -Math.sin(yrad), sinz = Math.sin(zrad);
		double cosx = Math.cos(xrad), cosy = Math.cos(yrad), cosz = Math.cos(zrad);
//		return new Matrix3d( //(Rz)(Ry)(Rx)
//				cosz*cosy, sinz*cosy, -siny,
//				(cosz*siny*sinx) - (sinz*cosx), (sinz*siny*sinx) + (cosz*cosx), cosy*sinx,
//				(cosz*siny*cosx) + (sinz*siny), (sinz*siny*cosx) - (cosz*sinx), cosy*cosx
//				);
		return new Matrix3d( //(Rz)(Rx)(Ry)
				cosy*cosz - sinx*siny*sinz, sinx*siny*cosz + cosy*sinz, -cosx*siny,
				-cosx*sinz, cosx*cosz, sinx,
				sinx*cosy*sinz + siny*cosz, siny*sinz - sinx*cosy*cosz, cosx*cosy
				);
	}
	
	public static Vector nearestPoint(Vector a, Vector b, Vector x) { //nearest point to x on line defined by a, b
		Vector b_a = b.clone().subtract(a);
		double theta = b_a.dot(x.clone().subtract(a)) / b_a.lengthSquared();
		return a.clone().multiply(1-theta).add(b.clone().multiply(theta));
	}

}
