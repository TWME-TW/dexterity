package me.c7dev.dexterity.displays.schematics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Color;
import org.bukkit.util.Vector;

import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.displays.schematics.token.CharToken;
import me.c7dev.dexterity.displays.schematics.token.DoubleKey;
import me.c7dev.dexterity.displays.schematics.token.DoubleToken;
import me.c7dev.dexterity.displays.schematics.token.StringKey;
import me.c7dev.dexterity.displays.schematics.token.StringToken;
import me.c7dev.dexterity.displays.schematics.token.Token;
import me.c7dev.dexterity.displays.schematics.token.Token.TokenType;
import me.c7dev.dexterity.util.BinaryTag;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.DexterityException;

/**
 * Creates and exports a schematic file
 */
public class SchematicBuilder {
	
	public static final int SCHEMA_VERSION = 1, DECIMAL_PRECISION = 10;
	
	private Vector center;
	private Dexterity plugin;
	
	private HashMap<DoubleKey, DoubleToken> double_map = new HashMap<>();
	private HashMap<StringKey, StringToken> string_map = new HashMap<>();
	private HashMap<Character, CharToken> char_map = new HashMap<>();
	private HashMap<TokenType, Token> specifier_map = new HashMap<>();
	
	private List<Token> encoded_output = new ArrayList<>();
	private HashMap<Token,Integer> freq = new HashMap<>();
	private boolean assigned_tags = false;
	private MessageDigest sha256;
	private String hash_progress = "";
	private StringBuilder hash_line = new StringBuilder("NaCl, why not");
	
	/* SCHEMA FORMAT
	 * schema-version: <version>
	 * author: <author>
	 * charset: [index: char, len], [index: specifier, len
	 * 
	 * Huffman Encoded:
	 * objects: Codes definitions [type chars_val]
	 * <end data>
	 * 
	 * data:
	 * DexBlock[] data, ordered by y, x, z coordinates, DexterityDisplay metadata
	 * 
	 * <end of display token>, Repeat data for any additional sub-displays
	 * <end data token>
	 * 
	 * hash
	 * signature
	 */
	
	public SchematicBuilder(Dexterity plugin, DexterityDisplay d) {
		if (d == null) throw new IllegalArgumentException("Display cannot be null!");
		center = d.getCenter().toVector();
		this.plugin = plugin;
		
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		plugin.api().unTempHighlight(d);
		for (TokenType type : TokenType.values()) {
			Token stoken = new Token(type);
			specifier_map.put(type, stoken);
			freq.put(stoken, 0);
		}
		
		for (char i = 48; i <= 57; i++) { //define all digits because used afterward encoding for obj tag lengths
			CharToken ctoken = new CharToken(i);
			char_map.put(i, ctoken);
			freq.put(ctoken, 0);
		}
		
		encodeBlocks(d);
		encodeMetadata(d);
		addToken(TokenType.DATA_END);
		assignTags();
	}
	
	private String hash(String s) {
		return hash(s.getBytes());
	}
	
	private String hash(byte[] data) {
		return DexUtils.bytesToHex(sha256.digest(data));
	}
	
	public int save(String file_name, String author, boolean override) {
		if (file_name == null || file_name.length() == 0) throw new IllegalArgumentException("File name cannot be null!");
		if (author == null || author.length() == 0) throw new IllegalArgumentException("Must provide an author!");
		if (author.contains(";")) throw new IllegalArgumentException("Author name cannot have ; in it!");
		try {
			File f = new File(plugin.getDataFolder().getAbsolutePath() + "/schematics/" + file_name + ".dexterity");
			if (f.exists()) {
				if (!override) return 1;
			} else f.createNewFile();
			
			save(f, author);
			
			return 0;
		} catch (Exception ex) {
			ex.printStackTrace();
			return -1;
		}
	}
	
	public boolean save(File f, String author) {
		if (f == null) throw new IllegalArgumentException("File cannot be null!");
		if (author == null || author.length() == 0) throw new IllegalArgumentException("Must provide an author!");
		if (author.contains(";")) throw new IllegalArgumentException("Author name cannot have ; in it!");
		if (!assigned_tags) throw new DexterityException("Did not assign tags to tokens, cannot save!");
		
		List<Token> definitions = createObjectTokens(true); //create definitions token list, dependent on created tags

		try {
			if (!f.exists()) f.createNewFile();
			FileWriter writer = new FileWriter(f);

			writer.write("#DO NOT MODIFY");
			write("\n", writer);
			write("schema-version: ", writer);
			write("" + SCHEMA_VERSION, writer);
			write("\n", writer);
			write("author: ", writer);
			write(author, writer);
			write("\n", writer);
			write("charset: ", writer);
			
			//define char tags
			boolean writeindex = true;
			for (int i = 32; i <= 254; i++) {
				char c = (char) i;
				CharToken token = char_map.get(c);

				if (token != null) {
					if (writeindex) {
						write(i + ":", writer);
						writeindex = false;
					}
					if (token.getTag() == null) {
						writer.close();
						throw new DexterityException("Tag is undefined: " + token.toString());
					}
					write(token.getTag().serialize() + ";", writer);
				}
				else writeindex = true;
			}
			
			//define specifier tags
			writeindex = true;
			TokenType[] types = TokenType.values();
			for (int i = 0; i < types.length; i++) {
				Token spec = specifier_map.get(types[i]);
				if (spec.getTag() != null && freq.getOrDefault(spec, 0) >= 1) {
					if (writeindex) {
						write((i+256) + ":", writer);
						writeindex = false;
					}
					write(spec.getTag().serialize() + ";", writer);
				} else writeindex = true;
			}
			write("\n", writer);
			
			//define object tokens
			write("objects: ", writer);
			TokenEncoder obj_encoder = new TokenEncoder();
			for (Token token : definitions) {
				obj_encoder.append(token.getTag());
			}
			write(Base64.getEncoder().encodeToString(obj_encoder.getData()), writer);
			write("\n", writer);
			
			
			write("data: ", writer);
			TokenEncoder encoder = new TokenEncoder();
			for (Token token : encoded_output) encoder.append(token.getTag());
			write(Base64.getEncoder().encodeToString(encoder.getData()), writer);
			write("\n", writer);
			
			writer.write("hash: ");
			writer.write(hash_progress);
			writer.write("\n");

			writer.close();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	private void write(String s, FileWriter writer) throws IOException {
		writer.write(s);
		
		if (s.equals("\n")) {
			hash_progress = hash(hash_line.toString() + hash_progress);
			hash_line = new StringBuilder();
		}
		else hash_line.append(s);
	}
	
	public DoubleToken getDouble(TokenType type, double x) {
		DoubleKey key = new DoubleKey(type, x);
		DoubleToken r = double_map.get(key);
		if (r == null) {
			r = new DoubleToken(type, x);
			double_map.put(key, r);
			
			//auto-increase the freq to account for double token's definition
			char[] stringx = ("" + x).toCharArray();
			for (char c : stringx) charToken(c); //freq
			Token specifier = specifier_map.get(type); //created on init
			freq.put(specifier, freq.getOrDefault(specifier, 0) + 1);
		}
		return r;
	}
	
	public StringToken getString(TokenType type, String s) {
		StringKey key = new StringKey(type, s);
		StringToken r = string_map.get(key);
		if (r == null) {
			r = new StringToken(type, s);
			string_map.put(key, r);
			
			//auto-increase the freq to account for string token's definition
			for (char c : s.toCharArray()) charToken(c); //freq
			Token specifier = specifier_map.get(type); //created on init
			freq.put(specifier, freq.getOrDefault(specifier, 0) + 1);
		}
		return r;
	}
	
	private CharToken charToken(char c) {
		CharToken ctoken = char_map.get(c);
		if (ctoken == null) {
			ctoken = new CharToken(c);
			char_map.put(c, ctoken);
		}
		freq.put(ctoken, freq.getOrDefault(ctoken, 0) + 1);
		return ctoken;
	}
	
	private void addToken(TokenType specifier) {
		addToken(specifier, encoded_output);
	}
	
	private void addToken(TokenType specifier, List<Token> list) {
		addToken(specifier_map.get(specifier), list);
	}
	
	private void addToken(Token token) {
		freq.put(token, freq.getOrDefault(token, 0) + 1);
		addToken(token, encoded_output);
	}
	
	private void addToken(Token token, List<Token> list) {
		freq.put(token, freq.getOrDefault(token, 0) + 1);
		if (list != null) list.add(token);
	}
	
	private void addString(String s, List<Token> list) {
		for (char c : s.toCharArray()) {
			Token token = charToken(c);
			if (list != null) list.add(token);
		}
	}

	private void encodeBlocks(DexterityDisplay d) {
		d.sortBlocks();
		double epsilon = 0.00001;
		Token block_delimiter = specifier_map.get(TokenType.BLOCK_DELIMITER), display_delimiter = specifier_map.get(TokenType.DISPLAY_DELIMITER);
		for (DexBlock db : d.getBlocks()) {
			addToken(getString(TokenType.BLOCKDATA, db.getEntity().getBlock().getAsString().replaceFirst("minecraft:", "")));
			
			Vector diff = db.getLocation().toVector().subtract(center); //from root display
			if (Math.abs(diff.getX()) >= epsilon) addToken(getDouble(TokenType.DX, diff.getX()));
			if (Math.abs(diff.getY()) >= epsilon) addToken(getDouble(TokenType.DY, diff.getY()));
			if (Math.abs(diff.getZ()) >= epsilon) addToken(getDouble(TokenType.DZ, diff.getZ()));
			
			double roll = db.getRoll() % 360;
			if (Math.abs(db.getEntity().getLocation().getYaw()) >= epsilon) addToken(getDouble(TokenType.YAW, db.getEntity().getLocation().getYaw()));
			if (Math.abs(db.getEntity().getLocation().getPitch()) >= epsilon) addToken(getDouble(TokenType.PITCH, db.getEntity().getLocation().getPitch()));
			if (Math.abs(roll) >= epsilon) addToken(getDouble(TokenType.ROLL, roll));
			
			if (Math.abs(db.getTransformation().getScale().getX() - 1) >= epsilon) addToken(getDouble(TokenType.SCALE_X, db.getTransformation().getScale().getX()));
			if (Math.abs(db.getTransformation().getScale().getY() - 1) >= epsilon) addToken(getDouble(TokenType.SCALE_Y, db.getTransformation().getScale().getY()));
			if (Math.abs(db.getTransformation().getScale().getZ() - 1) >= epsilon) addToken(getDouble(TokenType.SCALE_Z, db.getTransformation().getScale().getZ()));
			
			if (Math.abs(db.getTransformation().getDisplacement().getX() + (0.5*db.getTransformation().getScale().getX())) >= epsilon) addToken(getDouble(TokenType.TRANS_X, db.getTransformation().getDisplacement().getX()));
			if (Math.abs(db.getTransformation().getDisplacement().getY() + (0.5*db.getTransformation().getScale().getY())) >= epsilon) addToken(getDouble(TokenType.TRANS_Y, db.getTransformation().getDisplacement().getY()));
			if (Math.abs(db.getTransformation().getDisplacement().getZ() + (0.5*db.getTransformation().getScale().getZ())) >= epsilon) addToken(getDouble(TokenType.TRANS_Z, db.getTransformation().getDisplacement().getZ()));
			
			if (Math.abs(db.getTransformation().getRollOffset().getX()) >= epsilon) addToken(getDouble(TokenType.ROFFSET_X, db.getTransformation().getRollOffset().getX()));
			if (Math.abs(db.getTransformation().getRollOffset().getY()) >= epsilon) addToken(getDouble(TokenType.ROFFSET_Y, db.getTransformation().getRollOffset().getY()));
			if (Math.abs(db.getTransformation().getRollOffset().getZ()) >= epsilon) addToken(getDouble(TokenType.ROFFSET_Z, db.getTransformation().getRollOffset().getZ()));
			
			if (Math.abs(db.getTransformation().getLeftRotation().x) >= epsilon) addToken(getDouble(TokenType.QUAT_X, db.getTransformation().getLeftRotation().x));
			if (Math.abs(db.getTransformation().getLeftRotation().y) >= epsilon) addToken(getDouble(TokenType.QUAT_Y, db.getTransformation().getLeftRotation().y));
			if (Math.abs(db.getTransformation().getLeftRotation().z) >= epsilon) addToken(getDouble(TokenType.QUAT_Z, db.getTransformation().getLeftRotation().z));
			if (Math.abs(db.getTransformation().getLeftRotation().w - 1) >= epsilon) addToken(getDouble(TokenType.QUAT_W, db.getTransformation().getLeftRotation().w));
			
			if (db.getEntity().isGlowing()) {
				Color glow = db.getEntity().getGlowColorOverride();
				if (glow == null) glow = Color.WHITE;
				addToken(getDouble(TokenType.GLOW_ARGB, glow.asARGB()));
			}
			
			addToken(block_delimiter);
		}
		
		addToken(display_delimiter);
		for (DexterityDisplay sub : d.getSubdisplays()) encodeBlocks(sub);
	}
	
	private void encodeMetadata(DexterityDisplay d) {
		if (d.getLabel() != null) addToken(getString(TokenType.LABEL, d.getLabel())); //TODO improve token interpreter to not have to put this in the objects header
	}

	private List<Token> createObjectTokens(boolean create_list) {
		List<Token> defs = create_list ? new ArrayList<>() : null;
		Token delimiter = specifier_map.get(TokenType.BLOCK_DELIMITER);
		
		//add token definitions to freq map for doubles
		for (Entry<DoubleKey,DoubleToken> entry : double_map.entrySet()) {
			DoubleToken token = entry.getValue();
			String valstr = "" + DexUtils.round(token.getDoubleValue(), DECIMAL_PRECISION);
			
			if (create_list) addString("" + token.getTag().length, defs);
			addToken(token.getType(), defs);
			addToken(token, defs);
			addString(valstr, defs);
			addToken(delimiter, defs);
		}
		//add token defs to freq map for strings
		for (Entry<StringKey,StringToken> entry : string_map.entrySet()) {
			StringToken token = entry.getValue();
			
			if (create_list) addString("" + token.getTag().length, defs);
			addToken(token.getType(), defs);
			addToken(token, defs);
			addString(token.getStringValue(), defs);
			addToken(delimiter, defs);
		}
		
		addToken(TokenType.DATA_END, defs); //end objects token definition
		return defs;
	}
	
	private void assignTags() {
		if (encoded_output.size() == 0) return;
		
		Token end_data = new Token(TokenType.DATA_END);
		addToken(end_data);
		
		if (encoded_output.size() == 1) {
			encoded_output.get(0).setTag(new BinaryTag(1));
			return;
		}
		LinkedList<HuffmanTree> forest = new LinkedList<>();
		
		createObjectTokens(false); //count the planned tokens into frequency map for encoding
		
		//init forest
		for (Entry<Token, Integer> freq_entry : freq.entrySet()) {
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
		assigned_tags = true;
	}
}
