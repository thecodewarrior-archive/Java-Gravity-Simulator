package com.piercecorcoran.grav;

import org.newdawn.slick.Graphics;

public class GParticle extends Drawable {

	/*
	 * Units:
	 * px   = m
	 * mass = kg
	 * 
	 */
	
	public boolean destroyed;
	
	public float vx;
	public float vy;

	public float px;
	public float py;
	
	public float ax;
	public float ay;

	public int mass;

	public int id;
	
	public boolean isStatic;
	public boolean selected;
	
	private float mergeX;
	private float mergeY;
	private int   mergeM;
	private float mergeVX;
	private float mergeVY;
		
	public GParticle(int pid) {
		vx = vy = px = py = ax = ay = 0;
		mergeM = -1;
		mergeX = mergeY = mergeVX = mergeVY = -1;
		mass = 1;
		id = pid;
		destroyed = false;
		isStatic = false;
	}
	
	public String sterilize() {
		return "(" + id + ";" + mass + ";" + px + ";" + py + ";" + vx + ";" + vx + ")";
	}
	
	public float getRadius() {
		// Area = 4/3 * pi * r^3
		// cuberoot(area/ (4/3 * pi)) = radius
		
		double rad = (double) Math.pow(mass/(4/3 * Math.PI), 1.0/3);
		if(rad < 0.5) { rad = 0.5f; }
		return (float)rad;
	}
	
	public void clearAccel() {
		ax = 0;
		ay = 0;
	}
	
	public void addForce(double x, double y, int delta) {
		if(isStatic) { return; }
		ax += x*1000/delta*GravitySim.inertia;
		ay += y*1000/delta*GravitySim.inertia;
	}
	
	public void mergeWith(float x, float y, int _mass, float velx, float vely) {
		if(mergeM == -1) {
			mergeX  = px;
			mergeY  = py;
			mergeM  = mass;
			mergeVX = vx;
			mergeVY = vy;
		}
		float[] m = GravitySim.centerOfMass(mergeX,  mergeY,  mergeM, x,    y,    _mass);
		mergeX = m[0];
		mergeY = m[1];
		float[] v = GravitySim.centerOfMass(mergeVX, mergeVY, mergeM, velx, vely, _mass); // same as COM, just on a velocity plane instead of a coordinate plane
		mergeVX = v[0];
		mergeVY = v[1];
		mergeM = mergeM + _mass;
		System.out.println("merge " + id );
	}
	
	public void applyMergePosition() {
		if(mergeM == -1) { return; }
		px = mergeX;
		py = mergeY;
		mass = mergeM;
		vx = mergeVX;
		vy = mergeVY;
		mergeM = -1;
		System.out.println("apply merge vx=" + vx);
	}
		
	public void update(int delta) {
		if(isStatic) { return; }
		vx += ax*delta/1000;
		vy += ay*delta/1000;
		
		px += vx*delta/1000;
		py += vy*delta/1000;
	}
	
	public void draw(Graphics g, float scale, float offsetx, float offsety) {
		float rad = getRadius()*scale;
		
		float apx = (px * scale) + offsetx;
		float apy = (py * scale) + offsety;
		
		float avx = (vx * scale);
		float avy = (vy * scale);
		
		float aax = (ax * scale);
		float aay = (ay * scale);
		
		g.fillOval(apx-rad, apy-rad, rad*2, rad*2);
		
		if(isStatic) { return; }
		
		g.drawLine(apx, apy, apx+vx, apy+vy);
		
		g.drawLine(apx, apy, apx+ax, apy+ay);
	}
	
}
