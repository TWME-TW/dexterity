package me.c7dev.dexterity.displays.schematics;

import java.util.LinkedList;

import org.bukkit.Bukkit;

import me.c7dev.dexterity.displays.schematics.token.Token;
import me.c7dev.dexterity.util.BinaryTag;
import me.c7dev.dexterity.util.DexterityException;

public class HuffmanTree {
	
	private int freq = 1;
	private LinkedList<Boolean> tag = new LinkedList<>(); //easier to add vals
	private HuffmanTree left, right;
	private Token leaf;
	
	public HuffmanTree(HuffmanTree left, HuffmanTree right) {
		this.left = left;
		this.right = right;
		if (left != null) freq += left.getFrequency();
		if (right != null) freq += right.getFrequency();
		if (freq < 1) freq = 1;
	}
	
	public HuffmanTree(Token leaf, int freq) {
		this.leaf = leaf;
		this.freq = freq;
		if (this.freq < 1) this.freq = 1;
	}
	
	public int getFrequency() {
		return freq;
	}
	
	public HuffmanTree getLeft() {
		return left;
	}
	
	
	public void setLeft(HuffmanTree l) {
		if (leaf != null) {
			Bukkit.broadcastMessage("Occupying node: " + leaf.toString() + ", tag=" + leaf.getTag().toString());
			throw new DexterityException("Invalid tag: " + leaf.toString());
		}
		if (left != null) throw new DexterityException("Cannot reset left when already defined!");
		left = l;
	}
	
	public void setRight(HuffmanTree r) {
		if (leaf != null) throw new DexterityException("Invalid tag: " + leaf.toString());
		if (right != null) throw new DexterityException("Cannot reset right when already defined!");
		right = r;
	}
	
	public void setLeaf(Token leaf) {
		if (leaf != null && (left != null || right != null)) throw new DexterityException("Invalid tag: " + leaf.toString());
		this.leaf = leaf;
	}
	
	public HuffmanTree getRight() {
		return right;
	}
	
	public void setFrequency(int f) {
		freq = f;
	}
	
	public Token getLeafToken() {
		return leaf;
	}
	
	public LinkedList<Boolean> getTag() {
		return tag;
	}
	
	public void addBit(boolean b) {
		tag.addFirst(b);
		if (left != null) left.addBit(b);
		if (right != null) right.addBit(b);
	}
	
	public void assignTags() {
		if (leaf == null) {
			if (left != null) left.assignTags();
			if (right != null) right.assignTags();
		} else {
			BinaryTag binary = getTagBinary();
			leaf.setTag(binary);
		}
	}
	
	public BinaryTag getTagBinary() {
		BinaryTag r = new BinaryTag(tag.size());
		Boolean b = tag.poll();
		int i = 0;
		LinkedList<Boolean> tagclone = new LinkedList<>();

		while(b != null) {
			r.bits.set(i, b);
			tagclone.addLast(b);
			i++;
			b = tag.poll();
		}
		tag = tagclone;
		return r;
	}

}
