package com.piercecorcoran.grav;
// com.piercecorcoran.grav.GravitySim
import java.util.ArrayList;
import java.util.Iterator;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

public class GravitySim extends BasicGame {

	/**
	 * Width and height of window
	 */
	public static int width  = 640, height = 480;
	
	/**
	 * inertial constant
	 */
	public static double inertia       = 1;
	
	/**
	 * gravitational constant
	 */
	public static double gravity       = 1f;
	
	/**
	 * The distance two particles can be from each other before merging
	 */
	public static int    crashDistance = 2;
	
	/**
	 * The max distance the cursor can be from a particle to select it
	 */
	public static int    maxSelectDistance = 60;
	
	/**
	 * The particle that is currently being placed into the world
	 */
	public GParticle creating;
	
	/**
	 * The mass any new particles placed into the world will have
	 */
	public int newMass;
	
	/**
	 * The particles in the world
	 */
	private ArrayList<GParticle> particles;
	
	/**
	 * The next particle ID
	 */
	private int nextID;
	
	/**
	 * Is the user pressing shift
	 */
	private boolean shifting;
	
	/**
	 * The currently selected mouse wheel menu
	 */
	private int selectedMenu;
	
	/**
	 * Is time frozen
	 */
	private boolean frozen;

	/**
	 * Time speed, multiplied by delta on each update
	 */
	private float timeMultiplier;

	/**
	 * Is the user setting the velocity
	 */
	private boolean settingVelocity;
	
	/**
	 * The velocity of the particle before setting it's velocity, used when ESC is pressed
	 */
	private float settingVelocityPreY, settingVelocityPreX;
	
	/**
	 * Is the user setting the position
	 */
	private boolean settingPosition;
	
	/**
	 * The position of the particle before setting it's position, used when ESC is pressed
	 */
	private float settingPositionPreY, settingPositionPreX;
	
	/**
	 * Is the user panning the screen with the middle mouse button
	 */
	private boolean grabbingScreen;
	
	/**
	 * The mouse position before grabbing started, used to calculate the offset of the screen from the current offset x/y
	 */
	private float origMouseX, origMouseY;
	
	/**
	 * The offset of the screen, for panning, added to all particle coordinates when drawing
	 */
	private float offsetx, offsety;
	
	/**
	 * The zoom level of the screen, all particle coordinates are multiplied by this
	 */
	private float zoomLevel;
	
	/* ******** code ******** */
	
	/**
	 * main startup function
	 * @param args does nothing at this point
	 * @throws SlickException
	 */
	public static void main(String[] args) throws SlickException {
		AppGameContainer app = new AppGameContainer(new GravitySim("Gravity Simulator v 0.1"));
		
		app.setDisplayMode(width, height, false);
		
		app.setTargetFrameRate(120);
		
		app.start();
	}
	
	/* ******** Game code ******** */
	
	/**
	 * Create a new GravitySim game instance
	 * @param title The title to show on the window
	 */
	public GravitySim(String title) {
		super(title);
	}

	/**
	 * Initialize the game
	 */
	public void init(GameContainer container) throws SlickException {
		// first id = 0
		nextID = 0;
		// default mass = 10kg
		newMass = 10;
		// by default mouse wheel modifies new particle mass
		selectedMenu = 10;
		// the screen is not frozen by default
		frozen = false;
		// not setting velocity
		settingVelocity = false;
		// not setting position
		settingPosition = false;
		// going realtime
		timeMultiplier = 1;
		
		// no offset and normal zoom level
		offsetx = 0;
		offsety = 0;
		zoomLevel = 1;
		
		// Particles is empty
		particles = new ArrayList<GParticle>();
		// stress test with ~1,500 particles
		/*
		for(int x = 0; x < width; x += 15) {
			for(int y = 0; y < height; y += 15) {
				addParticle(x, y, 10);
			}
		}
		/**/
		
		GParticle p = new GParticle(100);
		p.px = 300;
		p.py = 300;
		p.isStatic = true;
		p.mass = 100;
		
		particles.add(p);
		
		p = new GParticle(101);
		p.px = 300;
		p.py = 100;
		p.mass = 10;
		p.vx = (float) Math.sqrt(gravity * 100 * 10 / ( distance(particles.get(0), p)) );
		
		particles.add(p);
	}

	/**
	 * Update the game
	 * @param container
	 * @param _delta milliseconds since last update
	 */
	public void update(GameContainer container, int _delta) throws SlickException {
		// speeding up time
		int delta = (int) (_delta * timeMultiplier);
		
		// remove all null or destroyed particles
		Iterator<GParticle> iter = particles.iterator();
		while (iter.hasNext()) {
		    GParticle p = iter.next();
		    if(p == null || p.destroyed) {
				iter.remove();
			}
		}
		
		// don't update if time is frozen, or user is setting a particle's velocity/position
		if(!(frozen || settingVelocity || settingPosition)) {
			// apply gravitational force to all particles
			for(GParticle p1 : particles) {
				p1.clearForces();
				if(p1.isStatic) continue; // we don't need to do all these extra calculations for a static particle
				for(GParticle p2 : particles) {
					if(!(p1.id == p2.id) ) {
						float[] f = force(p1,p2);
						
						float  fx = f[0];
						float  fy = f[1];
						p1.addForce(fx, fy, delta);
					}
				}
			}
			
			for(GParticle p1 : particles) {
				for(GParticle p2 : particles) {
					if(!(p1.id == p2.id) ) {
						checkCrash(p1,p2);
					}
				}
			}
			
			for(GParticle p : particles) {
				p.update(delta);
				p.applyMergePosition();
			}
		}
		
		// creating new particle and dragging the velocity
		if(creating != null) {
			creating.vx = container.getInput().getMouseX() - creating.px;
			creating.vy = container.getInput().getMouseY() - creating.py;
		}
		
		// setting velocity
		if(settingVelocity && getSelected() != null) {
			getSelected().vx = container.getInput().getMouseX() - getSelected().px;
			getSelected().vy = container.getInput().getMouseY() - getSelected().py;
		}
		
		// setting position
		if(settingPosition && getSelected() != null) {
			getSelected().px = container.getInput().getMouseX();
			getSelected().py = container.getInput().getMouseY();
		}
		
	}
	
	/**
	 * render the world
	 * @param container GameContainer
	 * @param g Graphics to draw to
	 */
	public void render(GameContainer container, Graphics g) throws SlickException {
		// the particle selected to inspect, we don't look for it, we just set this when we find it while drawing
		GParticle inspectParticle = null;
		
		// the "real" offset of the screen, including the middle mouse button drag
		float offsetxdraw = offsetx + ( grabbingScreen ? container.getInput().getMouseX() - origMouseX : 0);
		float offsetydraw = offsety + ( grabbingScreen ? container.getInput().getMouseY() - origMouseY : 0);
		
		// draw all the particles and set the inspectParticle if the particle is selected
		for (GParticle p : particles) {
			Color oldcolor = g.getColor();
			if(p.isStatic) g.setColor(Color.red);
			if(p.selected) g.setColor(Color.blue);
			p.draw(g, zoomLevel, offsetxdraw, offsetydraw);
			if (p.selected) {
				inspectParticle = p;
			}
			g.setColor(oldcolor);
		}
		
		// draw the particle the user is placing, if any
		if(creating != null) {
			Color oldcolor = g.getColor();
			g.setColor(Color.green);
			if(creating.isStatic) g.setColor(Color.red);
			creating.draw(g, zoomLevel, offsetxdraw, offsetydraw);
			g.setColor(oldcolor);
		}
		
		// draw top left info
		g.drawString(String.format(
				"Mass: %d, Static: %s \n%.2fx speed, %.2fx zoom \nParticles: %d, distance: %.2f",
				newMass,
				( creating != null ? "" + creating.isStatic : "N/A" ),
				timeMultiplier,
				zoomLevel,
				particles.size(),
				particles.size() > 1 ? distance(particles.get(0), particles.get(1)) : 0
				), 10, 25);
		
		// if a particle is selected, show the inspector
		if(inspectParticle != null) drawInspector(g,inspectParticle);
		// draw the bottom left menu
		drawMenu(g);
	}
	
	/**
	 * Inspect the particle
	 * @param g Graphics to draw on
	 * @param p Particle to inspect
	 */
	public void drawInspector(Graphics g, GParticle p) {
		g.drawString(
				String.format("Particle #%d, Mass:%d m³", p.id, p.mass) + (p.isStatic ? " Static" : "") +"\n" +
				String.format("Velocity: %.3fm/s  X, %.3fm/s  Y", p.vx, p.vy     ) + "\n" +
				String.format("Position: %.3fm  X, %.3fm  Y", p.px, p.py     ) + "\n" +
				String.format("Force   : %.3fm/s² X, %.3fm/s²  Y", p.fx, p.fy     ) + "\n" +
				String.format("Last ∆: %.5f, Adjusted accel:", p.lastdelta) + "\n" +
				String.format("X: %.4f Y: %.4f", p.fx/p.mass, p.fy/p.mass)
				, 300, 10);
		
	}
	
	/**
	 * Draw the bottom left menu
	 * @param g Graphics to draw on
	 */
	public void drawMenu(Graphics g) {
		drawMenuItem(g, 10, "New Particle mass");
		drawMenuItem(g, 9,  "Selected Particle mass");
		drawMenuItem(g, 8,  "Zoom");
	}
	
	/**
	 * Draw a menu item
	 * @param g Graphics to draw to
	 * @param num Item number, used for placement and selection style
	 * @param name Text to draw
	 */
	public void drawMenuItem(Graphics g, int num, String name) {
		Color oldcolor = g.getColor(); // get old color so we can reset it later
		int selectedBump = 0; // Initialize the offset to the left, used so the selected menu is offset out from the rest
		g.setColor(Color.gray); // unselected menus are gray
		if(selectedMenu == num) { // if the current menu is selected
			g.setColor(Color.white); // selected menu is white
			selectedBump = 5; // selected menu is offset 5 pixels to the right
		}
		g.drawString("" + num + ". " + name, 10 + selectedBump, height - 15 - ((11 - num)*15) ); // draw menu
		g.setColor(oldcolor); // reset color
	}

	@Override
	public void mouseWheelMoved(int change) {
		int clicks = -change / 120; // number of "clicks", the up/down is reversed so we negate it
		
		if (shifting) clicks = clicks * 10; // multiply by 10 if user is pressing shift
		
		switch(selectedMenu){
		case 10:
			newMass += clicks;
			break;
		case 9:
			GParticle p = getSelected();
			if(p!=null) p.mass += clicks;
			break;
		case 8:
			if(clicks > 0) {
				zoomLevel = zoomLevel * 2;
			} else if (clicks < 0) {
				zoomLevel = zoomLevel / 2;
			}
		}
		if(creating != null) {
			creating.mass = newMass;
		}
		
	}
	
	@Override
    public void mousePressed(int button, int x, int y) {
       if (button == Input.MOUSE_LEFT_BUTTON) {
    	   if(settingVelocity) {
    		   settingVelocity = false; // commit to setting velocity
    	   } else if(settingPosition) {
    		   settingPosition = false; // commit to setting position
    	   } else {
	    	   creating = new GParticle(nextID); // create a new particle
	    	   nextID += 1;
	           creating.px = x; // set it's position to the cursor position
	           creating.py = y;
	           creating.mass = newMass; 
	           creating.isStatic = shifting; // make particle static if user is holding shift
    	   }
       } else if ( button == Input.MOUSE_RIGHT_BUTTON ) {
    	   GParticle bestp = null; // the closest particle
    	   // only select particles that are less then the maxselectdistance away from the cursor
    	   float bestd = maxSelectDistance/zoomLevel;
    	   // loop over all the particles
    	   for(GParticle p : particles) {
    		   // deselect the particle
    		   p.selected = false; 
    		   // figure out the distance to the particle
    		   float d = distance((x/zoomLevel)-offsetx, (y/zoomLevel)-offsety, p.px, p.py);
    		   // if the particle is closer then the current closest particle
    		   if(d < bestd) {
    			   bestd = d; // set the best distance to the distance for this particle
    			   bestp = p; // set the closest particle to the current particle
    		   }
    	   }
    	   if(bestp != null) { // if there is a particle in range
    		   bestp.selected = true; // select it
    	   }
       } else if (button == Input.MOUSE_MIDDLE_BUTTON) {
    	   grabbingScreen = true; // start grabbing the screen
    	   // store the mouse x and y to calculate offset later
    	   origMouseX = x;
    	   origMouseY = y;
       }
       
    }

    @Override
    public void mouseReleased(int button, int x, int y) {
       if(button == Input.MOUSE_LEFT_BUTTON) {
    	   if(creating != null) { // commit to creating particle
    		   particles.add(creating); // add particle to array
               creating = null;
    	   }
       } else if (button == Input.MOUSE_RIGHT_BUTTON) {
    	   // no rmb release yet
       } else if (button == Input.MOUSE_MIDDLE_BUTTON) {
    	   grabbingScreen = false; // stop grabbing the screen
    	   // add the distance moved to the offset
    	   offsetx += x - origMouseX;
    	   offsety += y - origMouseY;
    	   // reset origMouseX and Y
    	   origMouseX = 0;
    	   origMouseY = 0;
       }
    }
    
    @Override
    public void keyPressed(int key, char c) {
    	if(key == Input.KEY_LSHIFT) { shifting = true; } // user is pressing shift
    	
    	if(creating != null) { // set static flag to shifting if user is creating particle
    		creating.isStatic = shifting;
    	}
    	
    	GParticle p = getSelected(); // set p to currently selected particle, if any
    	
    	switch(key) {
    	case Input.KEY_C: // clear screen
    		particles = new ArrayList<GParticle>();
    		break;
    		
    	case Input.KEY_BACK: // delete selected particle
			if(p!=null) p.destroyed = true; // particle is sceduled to be destroyed
    		break;
    		
    	case Input.KEY_S: // invert selected particle's static flag
			if(p!=null) {
				p.isStatic = !p.isStatic; // invert isStatic flag
				p.vx = 0; // set velocity to 0, so the user can press S-S to reset velocity
				p.vy = 0; // ^^
			}
			break;
    		
    	case Input.KEY_F: // invert frozen flag
    		frozen = !frozen;
    		break;
    		
    	case Input.KEY_V: // start/stop setting velocity
			if(p!=null) {
	    		settingVelocity = !settingVelocity;
				settingVelocityPreX = p.vx;
				settingVelocityPreY = p.vy;
			}
    		break;
			
    	case Input.KEY_ESCAPE: // stop setting velocity/position
    		if(settingVelocity) {
    			if(p!=null) {
    				p.vx = settingVelocityPreX; 
    				p.vy = settingVelocityPreY;
    			}
    			settingVelocity = false;
    		}
    		if(settingPosition) {
    			if(p!=null) {
    				p.px = settingPositionPreX;
    				p.py = settingPositionPreY;
    			}
    			settingPosition = false;
    		}
    		break;
    		
    	case Input.KEY_M: // start moving particle
			if(p!=null) {
				settingPosition = !settingPosition;
				settingPositionPreX = p.px;
				settingPositionPreY = p.py;
			}
 
    		break;
    		
    	case Input.KEY_EQUALS: // equals is the plus key
    		timeMultiplier = timeMultiplier * 2;
    		break;
    	case Input.KEY_MINUS: // minus is the... minus key
    		timeMultiplier = timeMultiplier / 2;
    		break;
    		
    		// move screen
    	case Input.KEY_LEFT:
    		offsetx += 5; break;
    	case Input.KEY_RIGHT:
    		offsetx -= 5; break;
    	case Input.KEY_UP:
    		offsety += 5; break;
    	case Input.KEY_DOWN:
    		offsety -= 5; break;
    		
    		// menu selection
    	case Input.KEY_1:
    		selectedMenu = 1; break;
    	case Input.KEY_2:
    		selectedMenu = 2; break;
    	case Input.KEY_3:
    		selectedMenu = 3; break;
    	case Input.KEY_4:
    		selectedMenu = 4; break;
    	case Input.KEY_5:
    		selectedMenu = 5; break;
    	case Input.KEY_6:
    		selectedMenu = 6; break;
    	case Input.KEY_7:
    		selectedMenu = 7; break;
    	case Input.KEY_8:
    		selectedMenu = 8; break;
    	case Input.KEY_9:
    		selectedMenu = 9; break;
    	case Input.KEY_0:
    		selectedMenu = 10; break;
    	}
    	
    	
    }
    
    @Override
    public void keyReleased(int key, char c) {
    	if(key == Input.KEY_LSHIFT) { shifting = false; } // set shifting to false if user released shift key
    	if(creating != null) {
    		creating.isStatic = shifting; // update creating isStatic
    	}
    }

	/* *************** Game Logic *************** */
	
    /**
     * Add particle to the world
     * @param px X position of particle
     * @param py Y position of particle
     * @param vx X velocity of particle
     * @param vy Y velocity of particle
     * @param mass Mass of particle
     */
	public void addParticle(float px, float py, float vx, float vy, int mass) {
		GParticle particle = new GParticle(nextID);
		nextID += 1;
		particle.px   = px;
		particle.py   = py;
		particle.vx   = vx;
		particle.vy   = vy;
		particle.mass = mass;
		
		particles.add(particle);
	}
	
	/**
	 * Add particle to the world with no velocity
	 * @param px X position of particle
	 * @param py Y position of particle
	 * @param mass Mass of particle
	 */
	public void addParticle(float px, float py, int mass) {
		addParticle(px,py,0,0,mass);
	}
    
	/**
	 * Get selected particle
	 * @return selected particle
	 */
    public GParticle getSelected() {
		for(GParticle p : particles) {
			if (p.selected) return p;
		}
		return null;
	}
    
    /**
     * Check if two particles are crashing
     * @param p1 first particle
     * @param p2 second particle
     */
	public void checkCrash(GParticle p1, GParticle p2) {
		
		if ( distance(p1,p2) - GravitySim.crashDistance <= p1.getRadius() + p2.getRadius()) {
			crashParticles(p1,p2);
		}
	}
	
	/**
	 * Get distance between two particles
	 * @param p1 first particle
	 * @param p2 second particle
	 * @return pixels between particles
	 */
	public float distance(GParticle p1, GParticle p2) {
		
		return distance(p1.px, p1.py, p2.px, p2.py);
	}
	
	/**
	 * Get distance between two coordinates
	 * @param x X position 1
	 * @param y Y position 1
	 * @param x2 X position 2
	 * @param y2 Y position 2
	 * @return pixels between positions
	 */
	public float distance(float x, float y, float x2, float y2) {
		float dx = x2 - x;
		float dy = y2 - y;

		float d  = (float) Math.sqrt(
				Math.pow(Math.abs(dx),2) + 
				Math.pow(Math.abs(dy),2)
				);
		return d;
	}
	
	/**
	 * Get gravitational force between particles
	 * @param p1 particle 1
	 * @param p2 particle 2
	 * @return Newtons of force between particles
	 */
	public float[] force(GParticle p1, GParticle p2) {
		float dx = p2.px - p1.px; // y distance
		float dy = p2.py - p1.py; // x distance
		
		float rSquared = (dx*dx) + (dy*dy); // calculate r
		float r = (float) Math.sqrt(rSquared);
		float force = (float) (gravity * p1.mass * p2.mass)/rSquared;
		
		float forceX = (float) ((dx/r) * force );
		float forceY = (float) ((dy/r) * force );
		
		float[] ret = {forceX, forceY};
		return ret;
		
	}
	
	/**
	 * Crash particles together
	 * @param p1 first particle
	 * @param p2 second particle
	 */
	public void crashParticles(GParticle p1, GParticle p2) {
		if (p2.isStatic) {
			crashParticles(p2, p1);
			return;
		}
		if(p1.destroyed) { return; }
		System.out.println("CRASH!");
		p2.destroyed = true;
		p1.mergeWith(p2.px, p2.py, p2.mass, p2.vx, p2.vy);

	}
	
	public static float[] centerOfMass(float x1, float y1, float m1, float x2, float y2, float m2) {
		float cx = ( (m1 * x1) + (m2 * x2) ) / (m1 + m2);
		float cy = ( (m1 * y1) + (m2 * y2) ) / (m1 + m2);
		
		float[] ret = {cx,cy};
		return ret;
	}

}
