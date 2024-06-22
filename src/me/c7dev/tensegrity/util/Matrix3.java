package me.c7dev.tensegrity.util;

import org.bukkit.util.Vector;

public class Matrix3 {
	
	private double[][] vals = {
			{0, 0, 0},
			{0, 0, 0},
			{0, 0, 0}
	};
	
	public Matrix3(double[][] vals) {
		if (vals.length != 3 || vals[0].length != 3) return;
		this.vals = vals;
	}
	
	public Vector mult(Vector v) {
		return new Vector(
				(vals[0][0]*v.getX()) + (vals[0][1]*v.getY()) + (vals[0][2]*v.getZ()),
				(vals[1][0]*v.getX()) + (vals[1][1]*v.getY()) + (vals[1][2]*v.getZ()),
				(vals[2][0]*v.getX()) + (vals[2][1]*v.getY()) + (vals[2][2]*v.getZ())
				);
	}
	
	public Matrix3 mult(double f) {
		vals[0][0]*= f;
		vals[0][1]*=f;
		vals[0][2]*=f;
		vals[1][0]*=f;
		vals[1][1]*=f;
		vals[1][2]*=f;
		vals[2][0]*=f;
		vals[2][1]*=f;
		vals[2][2]*=f;
		return this;
	}
	
	public double det() {
		double a = vals[0][0];
		double f = vals[0][0];
		double b = vals[0][1];
		double e = vals[0][1];
		double c = vals[0][2];
		double d = vals[0][2];
		
		c*=vals[1][0];
		e*=vals[1][0];
		a*=vals[1][1];
		d*=vals[1][1];
		b*=vals[1][2];
		f*=vals[1][2];
		
		b*=vals[2][0];
		d*=vals[2][0];
		c*=vals[2][1];
		f*=vals[2][1];
		a*=vals[2][2];
		e*=vals[2][2];
		
		return a+b+c-d-e-f;
	}
	
	public double adbc(double a, double b, double c, double d) {
		return (a*d)-(b*c);
	}
	
	public Matrix3 inverse() {
		double d = det();
		if (d == 0) return null;
		return new Matrix3(new double[][] {
			{adbc(vals[1][1], vals[1][2], vals[2][1], vals[2][2]), 
				-adbc(vals[0][1], vals[0][2], vals[2][1], vals[2][2]), 
				adbc(vals[0][1], vals[0][2], vals[1][1], vals[1][2])},
			{-adbc(vals[1][0], vals[1][2], vals[2][0], vals[2][2]),
					adbc(vals[0][0], vals[0][2], vals[2][0], vals[2][2]),
					-adbc(vals[0][0], vals[0][2], vals[1][0], vals[1][2])
			},
			{adbc(vals[1][0], vals[1][1], vals[2][0], vals[2][1]),
				-adbc(vals[0][0], vals[0][1], vals[2][0], vals[2][1]),
				adbc(vals[0][0], vals[0][1], vals[1][0], vals[1][1])}
		}).mult(1/d);
	}
	
	public static Vector vecFromDoubles(double[] f) {
		return new Vector(f[0], f[1], f[2]);
	}
	
	public String toString() {
		String r = "[";
		for (int i = 0; i < 3; i++) {
			r += "[";
			for (int j = 0; j < 3; j++) {
				r += vals[i][j] + (j < 2 ? ", " : "");
			}
			r += "]" + (i < 2 ? "\n" : "");
		}
		return r + "]";
	}
	

}
