package com.piercecorcoran.grav;

import java.util.ArrayList;

public abstract class ParticleGenerator extends Drawable {

	public abstract void move(float x, float y);
	
	public abstract ArrayList<GParticle> generate(float x, float y, float vx, float vy);
	
}
