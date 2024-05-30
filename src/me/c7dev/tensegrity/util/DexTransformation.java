package me.c7dev.tensegrity.util;

import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DexTransformation {
	
	private Vector3f disp, scale;
	private Quaternionf r, l;
	
	public DexTransformation(Transformation trans) {
		disp = trans.getTranslation();
		scale = trans.getScale();
		r = trans.getRightRotation();
		l = trans.getLeftRotation();
	}
	
	public Vector3f getScale() {
		return scale;
	}
	
	public Vector3f getDisplacement() {
		return disp;
	}
	
	public Quaternionf getLeftRotation() {
		return l;
	}
	
	public Quaternionf getRightRotation() {
		return r;
	}
	
	public DexTransformation setScale(Vector3f s) {
		scale = s;
		return this;
	}
	
	public DexTransformation setScale(float x, float y, float z) {
		scale = new Vector3f(x, y, z);
		return this;
	}
	
	public DexTransformation setDisplacement(Vector3f d) {
		disp = d;
		return this;
	}
	
	public DexTransformation setDisplacement(float x, float y, float z) {
		disp = new Vector3f(x, y, z);
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
		return new Transformation(disp, l, scale, r);
	}

}
