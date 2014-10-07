package com.piercecorcoran.grav;

import java.util.ArrayList;

import org.newdawn.slick.Graphics;

public class ProtoDiskGenerator extends ParticleGenerator{

	private float x;
	private float y;
	
	public ProtoDiskGenerator() {
		x = 0;
		y = 0;
	}
	
	@Override
	public ArrayList<GParticle> generate(float x, float y, float vx, float vy) {
		
		return null;
	}

	@Override
	public void draw(Graphics g, float scale, float offsetx, float offsety) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(float toX, float toY) {
		// TODO Auto-generated method stub
		
	}
	
	private float orbitalVelocity(int centermass, float distance) {
		
		float vel = (float) Math.sqrt( (GravitySim.gravity * centermass)/distance );
		
		return vel;
	}

}
