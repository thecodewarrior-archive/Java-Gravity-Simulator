package com.piercecorcoran.grav;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

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
	
	public float fx;
	public float fy;

	public int mass;

	public int id;
	
	public boolean isStatic;
	public boolean selected;
	
	private float mergeX;
	private float mergeY;
	private int   mergeM;
	private float mergeVX;
	private float mergeVY;

	public double lastdelta;
	
	/**
	 * Create a particle with the specified ID
	 * @param pid The particle id
	 */
	public GParticle(int pid) {
		vx = vy = px = py = fx = fy = 0;
		mergeM = -1;
		mergeX = mergeY = mergeVX = mergeVY = -1;
		mass = 1;
		id = pid;
		destroyed = false;
		isStatic = false;
	}
	
	/**
	 * Sterilize the particle, creating a string that can be
	 * turned back into the particle when loading from a save
	 * @return Sterilized particle
	 */
	public String sterilize() {
		return "(" + id + ";" + mass + ";" + px + ";" + py + ";" + vx + ";" + vx + ")";
	}
	
	/**
	 * Get the radius of the particle (particle is a sphere)
	 * @return The radius of the particle
	 */
	public float getRadius() {
		// Area = 4/3 * pi * r^3
		// cuberoot(area/ (4/3 * pi)) = radius
		
		double rad = (double) Math.pow(mass/(4/3 * Math.PI), 1.0/3);
		if(rad < 0.5) { rad = 0.5f; }
		return (float)rad;
	}
	
	/**
	 * Set acceleration to 0
	 */
	public void clearForces() {
		fx = 0;
		fy = 0;
	}
	
	/**
	 * Apply force in newtons to particle
	 * @param x Force on the X axis
	 * @param y Force on the Y axis
	 * @param delta Milliseconds since last update
	 */
	public void addForce(double x, double y, int delta) {
		// you can't accelerate a static particle
		if(isStatic) { return; }
		fx += x;
		fy += y;
	}
	
	/**
	 * Merge particle with another
	 * @param x Other particle's x position
	 * @param y Other particle's y position
	 * @param _mass Other particle's mass
	 * @param velx Other particle's x velocity
	 * @param vely Other particle's y velocity
	 */
	public void mergeWith(float x, float y, int _mass, float velx, float vely) {
		// if merge has been applied, initialize the merge values
		if(mergeM == -1) {
			mergeX  = px;
			mergeY  = py;
			mergeM  = mass;
			mergeVX = vx;
			mergeVY = vy;
		}
		if(isStatic) {
			// Static particles shouldn't change position, only mass
			mergeM += _mass;
			return;
		}
		// get the center of mass of the two particles
		float[] m = GravitySim.centerOfMass(mergeX,  mergeY,  mergeM, x,    y,    _mass);
		mergeX = m[0];
		mergeY = m[1];
		// get the center of mass on the velocity coordinate plane, not position
		float[] v = GravitySim.centerOfMass(mergeVX, mergeVY, mergeM, velx, vely, _mass);
		mergeVX = v[0];
		mergeVY = v[1];
		// merge the masses
		mergeM = mergeM + _mass;
	}
	
	/**
	 * Apply the current merge variables to their real counterparts
	 */
	public void applyMergePosition() {
		// Don't merge if already merged
		if(mergeM == -1) { return; }
		// set real values to merge values
		px = mergeX;
		py = mergeY;
		mass = mergeM;
		vx = mergeVX;
		vy = mergeVY;
		// set mergeM to -1, showing that the merge has been applied
		mergeM = -1;
	}
	
	/**
	 * Update position
	 * @param delta milliseconds since last update
	 */
	public void update(int deltaMillis) {
		double delta = deltaMillis/1000.0;
		// don't update a static particle
		if(isStatic) { return; }
		// increase the velocity using the applied force
		vx += (fx/mass) * 1000/delta;// the 1000/delta is in the old code, put here out of desperation
		vy += (fy/mass) * 1000/delta;//
		lastdelta = delta;
		px += vx*delta;
		py += vy*delta;
	}
	
	
	/**
	 * Draw particle to screen
	 * @param g Graphics to draw particle to
	 * @param scale Scale of the screen
	 * @param offsetx X offset of screen
	 * @param offsety Y offset of screen
	 */
	public void draw(Graphics g, float scale, float offsetx, float offsety) {
		float rad = getRadius()*scale;
				
		float apx = (px * scale) + offsetx;
		float apy = (py * scale) + offsety;
				
		g.fillOval(apx-rad, apy-rad, rad*2, rad*2);
		
		if(isStatic) { return; }
		
		g.drawLine(apx, apy, apx+(vx*scale), apy+(vy*scale));
		
		g.drawLine(apx, apy, apx+(fx*scale), apy+(fy*scale));
	}
	
}
