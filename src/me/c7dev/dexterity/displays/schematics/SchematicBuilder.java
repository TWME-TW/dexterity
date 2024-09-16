package me.c7dev.dexterity.displays.schematics;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.displays.schematics.nbt.CharNBT;
import me.c7dev.dexterity.displays.schematics.nbt.DoubleKey;
import me.c7dev.dexterity.displays.schematics.nbt.DoubleNBT;
import me.c7dev.dexterity.displays.schematics.nbt.NBT;
import me.c7dev.dexterity.displays.schematics.nbt.NBT.NBTType;
import me.c7dev.dexterity.displays.schematics.nbt.StringKey;
import me.c7dev.dexterity.displays.schematics.nbt.StringNBT;
import me.c7dev.dexterity.util.BinaryTag;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexUtils;

public class SchematicBuilder {
	
	public static final int SCHEMA_VERSION = 1;
	
	private Vector center;
	private Dexterity plugin;
	
	private HashMap<DoubleKey, DoubleNBT> double_map = new HashMap<>();
	private HashMap<StringKey, StringNBT> string_map = new HashMap<>();
	private HashMap<Character, CharNBT> char_map = new HashMap<>();
	private HashMap<NBTType, NBT> specifier_map = new HashMap<>();
	
//	private StringBuilder unencoded_output = new StringBuilder("schema-version: 1\n");
	private List<NBT> encoded_output = new ArrayList<>();
	private HashMap<NBT,Integer> freq = new HashMap<>();
	
	/* SCHEMA FORMAT
	 * Base64 encoded:
	 * Section 1: version, author, ascii_start, [char, len, ], display_delimiter, section_delimiter, block_delimiter
	 * \n
	 * NBT Encoded:
	 * Section 3: NBT Definitions
	 * Section 4: DexBlock[] data, ordered by y, x, z coordinates
	 * Section 5 (last): DexterityDisplay metadata
	 * 
	 * <end of display token>, Repeat 4-5 for any additional sub-displays
	 * <end data token>
	 * \n
	 * Hash of prev lines
	 */
	
	public SchematicBuilder(Dexterity plugin, DexterityDisplay d) {
		if (d == null) throw new IllegalArgumentException("Display cannot be null!");
		center = d.getCenter().toVector();
		this.plugin = plugin;
		for (NBTType type : NBTType.values()) {
			NBT snbt = new NBT(type);
			specifier_map.put(type, snbt);
			freq.put(snbt, 0);
		}
		
		encodeBlocks(d);
		addNBT(specifier_map.get(NBTType.SECTION_DELIMITER));
		assignTags();
		
//		int len = 0;
//		for (NBT nbt : encoded_output) {
//			len += nbt.getTag().length;
//		}
//		Bukkit.broadcastMessage("Uses " + len + " bits, or " + (len / 8.0) + " bytes");
	}
	
	public boolean save(String file_name, String author) {
		if (file_name == null || file_name.length() == 0) throw new IllegalArgumentException("Directory cannot be null!");
		if (author == null || author.length() == 0) throw new IllegalArgumentException("Must provide an author!");
		if (author.contains(";")) throw new IllegalArgumentException("Author name cannot have ; in it!");

		try {
			File f = new File(plugin.getDataFolder().getAbsolutePath() + "/schematics/" + file_name + ".dex");
			if (!f.exists()) {
				f.createNewFile();
				Bukkit.broadcastMessage("not exists");
			}
			FileWriter writer = new FileWriter(f);

			writer.write("schema-version: ");
			writer.write("" + SCHEMA_VERSION);
			writer.write("\nauthor: ");
			writer.write(author);
			writer.write("\ncharset: ");
			
			//define char tags
			boolean writeindex = true;
			for (int i = 32; i <= 254; i++) {
				char c = (char) i;
				CharNBT nbt = char_map.get(c);

				if (nbt != null) {
					if (writeindex) {
						writer.write(i + ":");
						writeindex = false;
					}
					writer.write(nbt.getTag().serialize() + ";");
				}
				else writeindex = true;
			}
			
			//define specifier tags
			writeindex = true;
			NBTType[] types = NBTType.values();
			for (int i = 0; i < types.length; i++) {
				NBT spec = specifier_map.get(types[i]);
				if (freq.getOrDefault(spec, 0) >= 1) {
					if (writeindex) {
						writer.write((i+256) + ":");
						writeindex = false;
					}
					writer.write(spec.getTag().serialize() + ";");
				} else writeindex = true;
			}
			
			writer.write("\ndata: ");
			NBTEncoder encoder = new NBTEncoder();
			for (NBT nbt : encoded_output) encoder.append(nbt.getTag());
			writer.write(Base64.getEncoder().encodeToString(encoder.getData()));
			writer.write("\n");

			writer.close();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	public DoubleNBT getDouble(NBTType type, double x) {
		x = DexUtils.round(x, 10);
		DoubleKey key = new DoubleKey(type, x);
		DoubleNBT r = double_map.get(key);
		if (r == null) {
			r = new DoubleNBT(type, x);
			double_map.put(key, r);
			
			//auto-increase the freq to account for double nbt's definition
			char[] stringx = ("" + x).toCharArray();
			for (char c : stringx) {
				CharNBT cnbt = char_map.get(c);
				if (cnbt == null) {
					cnbt = new CharNBT(c);
					char_map.put(c, cnbt);
				}
				freq.put(cnbt, freq.getOrDefault(cnbt, 0) + 1);
			}
			NBT specifier = specifier_map.get(type); //created on init
			freq.put(specifier, freq.getOrDefault(specifier, 0) + 1);
		}
		return r;
	}
	
	public StringNBT getString(NBTType type, String s) {
		StringKey key = new StringKey(type, s);
		StringNBT r = string_map.get(key);
		if (r == null) {
			r = new StringNBT(type, s);
			string_map.put(key, r);
			
			//auto-increase the freq to account for string nbt's definition
			for (char c : s.toCharArray()) {
				CharNBT cnbt = char_map.get(c);
				if (cnbt == null) {
					cnbt = new CharNBT(c);
					char_map.put(c, cnbt);
				}
				freq.put(cnbt, freq.getOrDefault(cnbt, 0) + 1);
			}
			NBT specifier = specifier_map.get(type); //created on init
			freq.put(specifier, freq.getOrDefault(specifier, 0) + 1);
		}
		return r;
	}
	
	private void addNBT(NBT nbt) {
		freq.put(nbt, freq.getOrDefault(nbt, 0) + 1);
		encoded_output.add(nbt);
	}

	private void encodeBlocks(DexterityDisplay d) {
		d.sortBlocks();
		double epsilon = 0.00001;
		NBT block_delimiter = specifier_map.get(NBTType.BLOCK_DELIMITER), display_delimiter = specifier_map.get(NBTType.DISPLAY_DELIMITER);
		for (DexBlock db : d.getBlocks()) {
			addNBT(getString(NBTType.BLOCKDATA, db.getEntity().getBlock().getAsString()));
			
			Vector diff = db.getLocation().toVector().subtract(center); //from root display
			if (Math.abs(diff.getX()) >= epsilon) addNBT(getDouble(NBTType.DX, diff.getX()));
			if (Math.abs(diff.getY()) >= epsilon) addNBT(getDouble(NBTType.DY, diff.getY()));
			if (Math.abs(diff.getZ()) >= epsilon) addNBT(getDouble(NBTType.DZ, diff.getZ()));
			
			double roll = db.getRoll() % 360;
			if (Math.abs(db.getEntity().getLocation().getYaw()) >= epsilon) addNBT(getDouble(NBTType.YAW, db.getEntity().getLocation().getYaw()));
			if (Math.abs(db.getEntity().getLocation().getPitch()) >= epsilon) addNBT(getDouble(NBTType.PITCH, db.getEntity().getLocation().getPitch()));
			if (Math.abs(roll) >= epsilon) addNBT(getDouble(NBTType.ROLL, roll));
			
			if (Math.abs(db.getTransformation().getScale().getX() - 1) >= epsilon) addNBT(getDouble(NBTType.SCALE_X, db.getTransformation().getScale().getX()));
			if (Math.abs(db.getTransformation().getScale().getY() - 1) >= epsilon) addNBT(getDouble(NBTType.SCALE_Y, db.getTransformation().getScale().getY()));
			if (Math.abs(db.getTransformation().getScale().getZ() - 1) >= epsilon) addNBT(getDouble(NBTType.SCALE_Z, db.getTransformation().getScale().getZ()));
			
			if (Math.abs(db.getTransformation().getDisplacement().getX() + (0.5*db.getTransformation().getScale().getX())) >= epsilon) addNBT(getDouble(NBTType.TRANS_X, db.getTransformation().getDisplacement().getX()));
			if (Math.abs(db.getTransformation().getDisplacement().getY() + (0.5*db.getTransformation().getScale().getY())) >= epsilon) addNBT(getDouble(NBTType.TRANS_Y, db.getTransformation().getDisplacement().getY()));
			if (Math.abs(db.getTransformation().getDisplacement().getZ() + (0.5*db.getTransformation().getScale().getZ())) >= epsilon) addNBT(getDouble(NBTType.TRANS_Z, db.getTransformation().getDisplacement().getZ()));
			
			if (Math.abs(db.getTransformation().getRollOffset().getX()) >= epsilon) addNBT(getDouble(NBTType.ROFFSET_X, db.getTransformation().getRollOffset().getX()));
			if (Math.abs(db.getTransformation().getRollOffset().getY()) >= epsilon) addNBT(getDouble(NBTType.ROFFSET_Y, db.getTransformation().getRollOffset().getY()));
			if (Math.abs(db.getTransformation().getRollOffset().getZ()) >= epsilon) addNBT(getDouble(NBTType.ROFFSET_Z, db.getTransformation().getRollOffset().getZ()));
			
			if (Math.abs(db.getTransformation().getLeftRotation().x) >= epsilon) addNBT(getDouble(NBTType.QUAT_X, db.getTransformation().getLeftRotation().x));
			if (Math.abs(db.getTransformation().getLeftRotation().y) >= epsilon) addNBT(getDouble(NBTType.QUAT_Y, db.getTransformation().getLeftRotation().y));
			if (Math.abs(db.getTransformation().getLeftRotation().z) >= epsilon) addNBT(getDouble(NBTType.QUAT_Z, db.getTransformation().getLeftRotation().z));
			if (Math.abs(db.getTransformation().getLeftRotation().w - 1) >= epsilon) addNBT(getDouble(NBTType.QUAT_W, db.getTransformation().getLeftRotation().w));
			
			addNBT(block_delimiter);
		}
		
		addNBT(display_delimiter);
		for (DexterityDisplay sub : d.getSubdisplays()) encodeBlocks(sub);
		
		Bukkit.broadcastMessage("Added " + encoded_output.size() + " nbts, " + freq.size() + " unique types");
	}
	
	private void assignTags() {
		if (encoded_output.size() == 0) return;
		
		NBT end_data = new NBT(NBTType.DATA_END);
		addNBT(end_data);
		
		if (encoded_output.size() == 1) {
			encoded_output.get(0).setTag(new BinaryTag(1));
			return;
		}
		LinkedList<HuffmanTree> forest = new LinkedList<>();
		
		//add data nbts
		for (Entry<NBT, Integer> freq_entry : freq.entrySet()) {
			if (freq_entry.getValue() == 0) continue;
			HuffmanTree node = new HuffmanTree(freq_entry.getKey(), freq_entry.getValue());
			forest.addLast(node);
		}
		
		//Combine forest to produce Huffman codes
		while(forest.size() > 1) {
			forest.sort((l, r) -> {
				if (l.getFrequency() == r.getFrequency()) return 0;
				return l.getFrequency() > r.getFrequency() ? 1 : -1;
			});
			
			//combine 1st and 2nd tree
			HuffmanTree left = forest.poll();
			HuffmanTree right = forest.poll();
			left.addBit(true); //1's on left
			right.addBit(false);
			
			HuffmanTree combo = new HuffmanTree(left, right);
			forest.addFirst(combo);
		}
		
		HuffmanTree root = forest.getFirst();
		root.assignTags();
		
//		for (Entry<NBT, Integer> freq_entry : freq.entrySet()) {
//			Bukkit.broadcastMessage("NBT " + freq_entry.getKey().getType() + "=" + freq_entry.getKey().getValue() + " tag = " + freq_entry.getKey().getTag().toString() + ", freq = " + freq_entry.getValue());
//		}
//		Bukkit.broadcastMessage(freq1 + " nbts are used only once");
	}
}
