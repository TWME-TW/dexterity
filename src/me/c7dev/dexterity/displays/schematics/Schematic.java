package me.c7dev.dexterity.displays.schematics;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.displays.schematics.token.CharToken;
import me.c7dev.dexterity.displays.schematics.token.DoubleToken;
import me.c7dev.dexterity.displays.schematics.token.StringToken;
import me.c7dev.dexterity.displays.schematics.token.Token;
import me.c7dev.dexterity.displays.schematics.token.Token.TokenType;
import me.c7dev.dexterity.util.BinaryTag;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexBlockState;
import me.c7dev.dexterity.util.DexTransformation;
import me.c7dev.dexterity.util.DexterityException;

public class Schematic {
	
	private String author = null, file_name;
	private int version = 0;
	private HuffmanTree root = new HuffmanTree(null, null);
	private List<Token> data = new ArrayList<>();
	private boolean loaded = false;
	private LinkedList<SimpleDisplayState> displays = new LinkedList<>();

	public Schematic(Dexterity plugin, String file_name) {
		if (!file_name.endsWith("\\.dex")) file_name += ".dex";
		this.file_name = file_name;
		try {
			File f = new File(plugin.getDataFolder().getAbsolutePath() + "/schematics/" + file_name);
			if (f.exists()) {
				
				String[] req_sections = {"schema-version", "author", "charset", "objects", "data"};
				YamlConfiguration schem = YamlConfiguration.loadConfiguration(f);
				
				for (String req : req_sections) {
					if (!schem.contains(req)) throw new DexterityException("Schematic must include '" + req + "' section!");
				}
				
				version = schem.getInt("schema-version");
				author = schem.getString("author");
				
				load(schem);
				
			} else {
				throw new DexterityException(file_name + " does not exist in schematics folder!");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public String getAuthor() {
		return author;
	}
	
	public int getSchemaVersion() {
		return version;
	}
	
	private HuffmanTree search(Token token, int i, HuffmanTree node) {
		HuffmanTree next;
		
		if (i >= token.getTag().length) return node;
		
		if (token.getTag().bits.get(i)) {
			next = node.getLeft(); //to left
			if (next == null) {
				next = new HuffmanTree(null, null);
				node.setLeft(next);
			}
		}
		else {
			next = node.getRight();
			if (next == null) {
				next = new HuffmanTree(null, null);
				node.setRight(next);
			}
		}
		
		return search(token, i+1, next);
	}
	
	private void addToken(Token token) {
		HuffmanTree node = search(token, 0, root);
		node.setLeaf(token);
//		Bukkit.broadcastMessage("added token " + token.toString() + " with tag " + token.getTag().toString());
	}
	
	private boolean bit(byte in, int index) {
		return ((in >> index) & 1) == 1;
	}
	
	public List<Token> decode(byte[] data) {
		List<Token> out = new ArrayList<>();
		if (data.length == 0) return out;

		int index = 0, data_index = 0;
		byte buffer = data[0];
		HuffmanTree curr = root;
		
		while(true) {
			
			if (curr == null) { //TODO: Add to the tree, since we are defining the tag of a new token. Need to either store length of bit tag or search for delimiter
				throw new DexterityException("Could not load schematic: Received invalid data or object codes");
			}
			
			if (curr.getLeafToken() != null) {
				out.add(curr.getLeafToken());
				Bukkit.broadcastMessage("added data token: " + curr.getLeafToken().toString());
				if (curr.getLeafToken().getType() == TokenType.DATA_END) break; //eof
				curr = root;
				continue;
			}
			
			if (index >= 8) {
				index = 0;
				data_index++;
				if (data_index == data.length) break;
				else buffer = data[data_index];
			}
			
			boolean b = bit(buffer, 7-index);
			index++;
			if (b) curr = curr.getLeft();
			else curr = curr.getRight();
			
		}
		
		return out;
	}
	
	public void decodeObjects(byte[] data) {
		if (data.length == 0) return;

		int index = 0, data_index = 0;
		byte buffer = data[0];
		HuffmanTree curr = root;
		
		//state 0: Seek tag length, 1: Seek type (or more len digits), 2: Get tag, 3: Seek value until delimiter
		int state = -1, taglen = 0, tagread_index = 0;
		TokenType type = null;
		StringBuilder val = new StringBuilder();
		BinaryTag tagread = null;
		boolean b = false;
		
		while(true) { //Encoder format (v1, base 10): [<tag len>] <type> [<val chars>] [tag] <block delimiter>
			
			if (state == -1) state = 0; //first bit - need to run code later
			else {

				if (state == 2) { //read binary tag
					if (b) tagread.bits.set(tagread_index);
					tagread_index++;
					if (tagread_index == taglen) {
						Bukkit.broadcastMessage("finished tag: " + tagread.toString());
						state = 3; //read value
					}
				} else {

					if (curr == null) {
						throw new DexterityException("Malformed objects header: Invalid sequence");
					}

					if (curr.getLeafToken() != null) {
						Token token = curr.getLeafToken();
						Bukkit.broadcastMessage("popped " + token.toString());

						if (token.getType() == TokenType.ASCII) {
							CharToken ctk = (CharToken) token;
							if (state == 0 || state == 1) { //tag length int
								if (ctk.getCharValue() < 48 || ctk.getCharValue() > 57) throw new DexterityException("Malformed objects header: Expected tag length to be number");
								taglen = (10*taglen) + (ctk.getCharValue() - 48);
								Bukkit.broadcastMessage("new taglen = " + taglen);
								state = 1; //allow 'type' token
							}
							else if (state == 3) {
								val.append(ctk.getCharValue());
							}
						}
						else if (token.getType() == TokenType.BLOCK_DELIMITER) {
							if (state == 3) {
								Token t = Token.createToken(type, val.toString());
								t.setTag(tagread);
								addToken(t);
								Bukkit.broadcastMessage("Added token " + t.toString() + " with tag " + tagread.toString());
							}

							//reset state
							state = taglen = tagread_index = 0;
							type = null;
							tagread = null;
							val = new StringBuilder();
						}
						else if (token.getType() == TokenType.DATA_END) return;
						else if (state == 1) {
							type = token.getType();
							tagread = new BinaryTag(taglen);
							tagread_index = 0;
							state = 2;

						}
						else throw new DexterityException("Malformed objects header: Token sequencing is incorrect for object definition (" + state + ")");

						curr = root;
						continue;
					} else {
						if (b) curr = curr.getLeft();
						else curr = curr.getRight();
					}
				}
			}
			
			if (index >= 8) {
				index = 0;
				data_index++;
				if (data_index == data.length) break;
				else buffer = data[data_index];
			}
			b = bit(buffer, 7-index);
			index++;
		}
		
	}
	
	private void load(YamlConfiguration schem) {
		if (loaded) throw new DexterityException("This schematic is already loaded!");
		
		String[] charset = schem.getString("charset").split(";");
		int index = 0;
		boolean found_end_token = false;
		TokenType[] typevals = TokenType.values();
		
		//load charset - used to define objects
		for (String charstr : charset) {
			String[] charstr_split = charstr.split(":");
			if (charstr_split.length > 1) {
				index = Integer.parseInt(charstr_split[0]);
				charstr = charstr_split[1];
			}
			
			Token token;
			if (index <= 255) token = new CharToken((char) index);
			else {
				TokenType type = typevals[index-256];
				token = new Token(type);
				if (type == TokenType.DATA_END) found_end_token = true;
			}
			token.setTag(new BinaryTag(charstr));
			addToken(token);
			index++;
		}
		
		if (!found_end_token) throw new DexterityException("Data does not contain required tokens!");
		
		//load objects header - this uses the ascii tokens to define more tokens that each have a type and value
		String objectstr = schem.getString("objects");
		decodeObjects(Base64.getDecoder().decode(objectstr));
		
		//load blocks of schematic
		String datastr = schem.getString("data");
		data = decode(Base64.getDecoder().decode(datastr));
		
		Bukkit.broadcastMessage("decoded " + data.size() + " tokens for data section");
		
		loaded = true;
		reloadBlocks(data);
		
		//free mem
		data.clear();
		root = null;
	}
	
	private DexBlockState newState(World w) {
		return new DexBlockState(new Location(w, 0, 0, 0), null, DexTransformation.newDefaultTransformation(), null, null, 0f, null);
	}
	
	/**
	 * Interprets the token sequence to translate back into {@link DexBlock} states
	 * @param data
	 */
	public void reloadBlocks(List<Token> data) { //block data interpreter
		displays.clear();
		SimpleDisplayState w = new SimpleDisplayState(file_name);
		World world = Bukkit.getWorlds().get(0); //placeholder
		DexBlockState state = newState(world);
		
		for (Token t : data) {
			switch(t.getType()) {
			case DISPLAY_DELIMITER:
				w.addBlock(state); //won't add if data not set
				if (w.getBlocks().size() > 0) {
					displays.addLast(w);
					w = new SimpleDisplayState(file_name);
					state = newState(world);
				}
				continue;
				
			case DATA_END:
				w.addBlock(state);
				if (w.getBlocks().size() > 0) displays.addLast(w);
				return;
				
			case BLOCK_DELIMITER:
				w.addBlock(state);
				state = newState(world);
				continue;
			default:
			}

			if (t instanceof DoubleToken) {
				DoubleToken dt = (DoubleToken) t;

				switch(t.getType()) {
				case DX:
					state.getLocation().setX(dt.getDoubleValue());
					break;
				case DY:
					state.getLocation().setY(dt.getDoubleValue());
					break;
				case DZ:
					state.getLocation().setZ(dt.getDoubleValue());
					break;
				default:
				}
			}
			else if (t instanceof StringToken) {
				StringToken st = (StringToken) t;
				switch(t.getType()) {
				case BLOCKDATA:
					state.setBlock(Bukkit.createBlockData(st.getStringValue()));
					break;
				case LABEL:
					w.setLabel(st.getStringValue());
					break;
				default:
				}
			}
			
		}
	}
	
}
