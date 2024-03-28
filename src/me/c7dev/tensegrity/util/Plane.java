package me.c7dev.tensegrity.util;

import org.bukkit.util.Vector;

public enum Plane {
	
	XZ(new Vector(0, 1, 0)),
	XY(new Vector(0, 0, 1)),
	ZY(new Vector(1, 0, 0));

	private Vector normal;
	
	Plane(Vector normal_){
		normal = normal_;
	}
	
	public Vector getNormal() {
		return normal;
	}
}
