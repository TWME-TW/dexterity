package me.c7dev.dexterity.util;

import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Provides a factory for the {@link Transformation} class
 */
public class DexTransformation {
	
	private Vector disp, scale, disp2 = new Vector(0, 0, 0);
	private Quaternionf r, l;
	
	public DexTransformation() {
		disp = new Vector(0, 0, 0);
		scale = new Vector(1, 1, 1);
		r = new Quaternionf();
		l = new Quaternionf();
	}
	
	public DexTransformation(Transformation trans) {
		disp = DexUtils.vector(trans.getTranslation());
		scale = DexUtils.vector(trans.getScale());
		r = trans.getRightRotation();
		l = trans.getLeftRotation();
	}
	
	public DexTransformation clone() {
		DexTransformation ret = new DexTransformation();
		ret.setDisplacement(disp.clone());
		ret.setLeftRotation(new Quaternionf(l.x, l.y, l.z, l.w));
		ret.setRightRotation(new Quaternionf(r.x, r.y, r.z, r.w));
		ret.setScale(scale.clone());
		ret.setRollOffset(disp2.clone());
		return ret;
	}
	
	/**
	 * Returns a transformation set so that the scale is 1 and location is in the block center
	 * @return
	 */
	public static DexTransformation newDefaultTransformation() {
		return new DexTransformation(new Transformation(
				new Vector3f(-0.5f, -0.5f, -0.5f), //translation
				new AxisAngle4f(0f, 0f, 0f, 1f), //l rotation
				new Vector3f(1, 1, 1), //scale
				new AxisAngle4f(0f, 0f, 0f, 1f))); //r rotation
	}
	
	/**
	 * Returns a transformation set so that the scale is 1 and translation is 0 (the block display's corner)
	 * @return
	 */
	public static DexTransformation newEmptyTransformation() {
		return new DexTransformation(new Transformation(
				new Vector3f(0f, 0f, 0f),
				new AxisAngle4f(0f, 0f, 0f, 1f),
				new Vector3f(1, 1, 1),
				new AxisAngle4f(0f, 0f, 0f, 1f)));
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
	
	public Vector getRollOffset() {
		return disp2;
	}
	
	/**
	 * Sets the scale to the input Vector and resets the displacement
	 * Creates an easy way to set scale and keep the block display centered
	 * @param s
	 * @return
	 */
	public DexTransformation rescale(Vector s) {
		scale = s;
		disp = s.clone().multiply(-0.5);
		return this;
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
	
	public DexTransformation setRollOffset(Vector v) {
		disp2 = v;
		return this;
	}
	
	public Transformation build() {
		return new Transformation(DexUtils.vector(disp.clone().add(disp2)), l, DexUtils.vector(scale), r);
	}

}
