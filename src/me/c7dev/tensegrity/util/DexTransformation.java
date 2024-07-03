package me.c7dev.tensegrity.util;

import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DexTransformation {
	
	private Vector disp, scale;
	private Quaternionf r, l;
	
	public DexTransformation(Transformation trans) {
		disp = DexUtils.vector(trans.getTranslation());
		scale = DexUtils.vector(trans.getScale());
		r = trans.getRightRotation();
		l = trans.getLeftRotation();
	}
	
	public Vector getScale() {
		return scale;
	}
	
	public Vector getDisplacement() {
		return disp;
	}
	
	public Quaternionf getLeftRotation() {
		return l;
	}
	
	public Quaternionf getRightRotation() {
		return r;
	}
	
	public DexTransformation setScale(Vector s) {
		scale = s;
		return this;
	}
	
	public DexTransformation setScale(float x, float y, float z) {
		scale = new Vector(x, y, z);
		return this;
	}
	
	public DexTransformation setDisplacement(Vector d) {
		disp = d;
		return this;
	}
	
	public DexTransformation setDisplacement(float x, float y, float z) {
		disp = new Vector(x, y, z);
		return this;
	}
	
	public DexTransformation setLeftRotation(Quaternionf lr) {
		l = lr;
		return this;
	}
	
	public DexTransformation setRightRotation(Quaternionf rr) {
		r = rr;
		return this;
	}
	
	public Transformation build() {
		return new Transformation(DexUtils.vector(disp), l, DexUtils.vector(scale), r);
	}

}
