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

public class GravitySim extends BasicGame {

	public static int width  = 640;
	public static int height = 480;
	
	public static double inertia       = 1;
	public static double gravity       = 9.8f;
	public static int    crashDistance = 2;
	public static int    maxSelectDistance = 60;
	
	public GParticle creating;
	public int newMass;
	
	private ArrayList<GParticle> particles;
	private int nextID;
	
	private boolean shifting;
	
	private int selectedMenu;
	
	private boolean frozen;

	private float timeMultiplier;

	private boolean settingVelocity;
	private float settingVelocityPreY;
	private float settingVelocityPreX;
	
	private boolean settingPosition;
	private float settingPositionPreY;
	private float settingPositionPreX;
	
	private float offsetx;
	private float offsety;
	private float zoomLevel;
	
	private boolean grabbingScreen;
	private float origMouseX;
	private float origMouseY;
	/* ******** code ******** */
	public static void main(String[] args) throws SlickException {
		AppGameContainer app = new AppGameContainer(new GravitySim("Gravity Simulator v 0.1"));
		
		app.setDisplayMode(width, height, false);
		
		app.setTargetFrameRate(120);
		
		app.start();
	}
	
	/*******************************/
	
	public GravitySim(String title) {
		super(title);
	}


	public void init(GameContainer container) throws SlickException {
		nextID = 0;
		newMass = 10;
		selectedMenu = 10;
		frozen = false;
		settingVelocity = false;
		timeMultiplier = 1;
		settingPosition = false;
		
		offsetx = 0;
		offsety = 0;
		zoomLevel = 1;
		
		particles = new ArrayList<GParticle>();
		addParticle(100,100,10);
		addParticle(200,100,10);
	}

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
	
	public void addParticle(float px, float py, int mass) {
		addParticle(px,py,0,0,mass);
	}
	
	public void update(GameContainer container, int _delta) throws SlickException {
		int delta = (int) (_delta * timeMultiplier);
		
		Iterator<GParticle> iter = particles.iterator();
		while (iter.hasNext()) {
		    GParticle p = iter.next();
		    if(p == null) {
				iter.remove();
			}
		}
		if(!(frozen || settingVelocity || settingPosition)) {
			for(GParticle p1 : particles) {
				p1.clearAccel();
				for(GParticle p2 : particles) {
					if(!(p1.id == p2.id) ) {
						float[] f = force(p1,p2);
						
						float  fx = f[0];
						float  fy = f[1];
						p1.addForce(fx, fy, delta);
					}
				}
			}
		
		
			for(GParticle p : particles) {
				p.update(delta);
			}
		}
		
		if(!settingPosition) {
			for(GParticle p1 : particles) {
				for(GParticle p2 : particles) {
					if(!(p1.id == p2.id) ) {
						checkCrash(p1,p2);
					}
				}
			}
		}
		
		iter = particles.iterator();
		while (iter.hasNext()) {
		    GParticle p = iter.next();
		    if(p.destroyed == true) {
				iter.remove();
			}
		}
		
		for(GParticle p : particles) {
			p.applyMergePosition();
		}
		
		if(creating != null) {
			creating.vx = container.getInput().getMouseX() - creating.px;
			creating.vy = container.getInput().getMouseY() - creating.py;
		}
		
		if(settingVelocity && getSelected() != null) {
			getSelected().vx = container.getInput().getMouseX() - getSelected().px;
			getSelected().vy = container.getInput().getMouseY() - getSelected().py;
		}
		
		if(settingPosition && getSelected() != null) {
			getSelected().px = container.getInput().getMouseX();
			getSelected().py = container.getInput().getMouseY();
		}
		
	}
	
	public void render(GameContainer container, Graphics g) throws SlickException {
		GParticle inspectParticle = null;
		
		float offsetxdraw = offsetx + ( grabbingScreen ? container.getInput().getMouseX() - origMouseX : 0);
		float offsetydraw = offsety + ( grabbingScreen ? container.getInput().getMouseY() - origMouseY : 0);
		
		
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
		if(creating != null) {
			Color oldcolor = g.getColor();
			g.setColor(Color.green);
			if(creating.isStatic) g.setColor(Color.red);
			creating.draw(g, zoomLevel, offsetxdraw, offsetydraw);
			g.setColor(oldcolor);
		}
		
		g.drawString(String.format(
				"Mass: %d, Static: %s \n%.2fx speed, %.2fx zoom \nParticles: %d",
				newMass,
				( creating != null ? "" + creating.isStatic : "N/A" ),
				timeMultiplier,
				zoomLevel,
				particles.size()
				), 10, 25);
		
		
		if(inspectParticle != null) drawInspector(g,inspectParticle);
		drawMenu(g);
	}
	
	public void drawInspector(Graphics g, GParticle p) {
		g.drawString(
				String.format("Particle #%d, Mass:%d m³", p.id, p.mass) + (p.isStatic ? " Static" : "") +"\n" +
				String.format("Velocity: %.3fm/s  X, %.3fm/s  Y", p.vx, p.vy     ) + "\n" +
				String.format("Position: %.3fm  X, %.3fm  Y", p.px, p.py     ) + "\n" +
				String.format("Accel   : %.3fm/s² X, %.3fm/s²  Y", p.ax, p.ay     ) + "\n"
				
				, 300, 10);
		
	}
	
	public void drawMenu(Graphics g) {
		drawMenuItem(g, 10, "New Particle mass");
		drawMenuItem(g, 9,  "Selected Particle mass");
		drawMenuItem(g, 8,  "Zoom");
	}
	
	public void drawMenuItem(Graphics g, int num, String name) {
		Color oldcolor = g.getColor();
		int selectedBump = 0;
		g.setColor(Color.gray);
		if(selectedMenu == num) {
			g.setColor(Color.white);
			selectedBump = 5;
		}
		g.drawString("" + num + ". " + name, 10 + selectedBump, height - 15 - ((11 - num)*15) );
		g.setColor(oldcolor);
	}
	
	@Override
	public void mouseWheelMoved(int change) {
		int clicks = -change / 120;
		if (shifting) clicks = clicks * 10;
		switch(selectedMenu){
		case 10:
			newMass += clicks;
			break;
		case 9:
			GParticle p = getSelected();
			if(p!=null) p.mass += clicks;
			break;
		case 8:
			zoomLevel += clicks/10.0;
		}
		if(creating != null) {
			creating.mass = newMass;
		}
		
	}
	
	public GParticle getSelected() {
		for(GParticle p : particles) {
			if (p.selected) return p;
		}
		return null;
	}
	
	@Override
    public void mousePressed(int button, int x, int y) {
       if (button == Input.MOUSE_LEFT_BUTTON) {
    	   if(settingVelocity) {
    		   settingVelocity = false;
    	   } else if(settingPosition) {
    		   settingPosition = false;
    	   } else {
	    	   creating = new GParticle(nextID);
	    	   nextID += 1;
	           creating.px = x;
	           creating.py = y;
	           creating.mass = newMass;
	           creating.isStatic = shifting;
    	   }
       } else if ( button == Input.MOUSE_RIGHT_BUTTON ) {
    	   GParticle bestp = null;
    	   float bestd = 1000000000;
    	   for(GParticle p : particles) {
    		   p.selected = false;
    		   float d = distance((x/zoomLevel)-offsetx, (y/zoomLevel)-offsety, p.px, p.py);
    		   if(d < bestd) {
    			   bestd = d;
    			   bestp = p;
    		   }
    	   }
    	   if(bestp != null && bestd < maxSelectDistance/zoomLevel) {
    		   bestp.selected = true;
    	   }
       } else if (button == Input.MOUSE_MIDDLE_BUTTON) {
    	   grabbingScreen = true;
    	   origMouseX = x;
    	   origMouseY = y;
       }
       
    }

    @Override
    public void mouseReleased(int button, int x, int y) {
       if(button == Input.MOUSE_LEFT_BUTTON) {
    	   if(creating != null) {
    		   particles.add(creating);
               creating = null;
    	   }
       } else if (button == Input.MOUSE_RIGHT_BUTTON) {
    	   
       } else if (button == Input.MOUSE_MIDDLE_BUTTON) {
    	   grabbingScreen = false;
    	   offsetx += x - origMouseX;
    	   offsety += y - origMouseY;
    	   origMouseX = 0;
    	   origMouseY = 0;
       }
    }
    
    @Override
    public void keyPressed(int key, char c) {
    	if(key == Input.KEY_LSHIFT) { shifting = true; }
    	if(creating != null) {
    		creating.isStatic = shifting;
    	}
    	
    	GParticle p;
    	
    	switch(key) {
    	case Input.KEY_C:
    		particles = new ArrayList<GParticle>();
    		break;
    	case Input.KEY_BACK:
    		p = getSelected();
			if(p!=null) p.destroyed = true;
    		break;
    		
    	case Input.KEY_S:
    		p = getSelected();
			if(p!=null) {
				p.isStatic = !p.isStatic;
				p.vx = 0;
				p.vy = 0;
			}
			break;
    		
    	case Input.KEY_F:
    		frozen = !frozen;
    		break;
    		
    	case Input.KEY_V:
    		p = getSelected();
			if(p!=null) {
	    		settingVelocity = !settingVelocity;
				settingVelocityPreX = p.vx;
				settingVelocityPreY = p.vy;
			}
    		break;
			
    	case Input.KEY_ESCAPE:
    		if(settingVelocity) {
    			p = getSelected();
    			if(p!=null) {
    				p.vx = settingVelocityPreX;
    				p.vy = settingVelocityPreY;
    			}
    			settingVelocity = false;
    		}
    		if(settingPosition) {
    			p = getSelected();
    			if(p!=null) {
    				p.px = settingPositionPreX;
    				p.py = settingPositionPreY;
    			}
    			settingPosition = false;
    		}
    		break;
    		
    	case Input.KEY_M:
    		p = getSelected();
			if(p!=null) {
				settingPosition = !settingPosition;
				settingPositionPreX = p.px;
				settingPositionPreY = p.py;
			}
 
    		break;
    		
    	case Input.KEY_EQUALS:
    		timeMultiplier = timeMultiplier * 2;
    		break;
    	case Input.KEY_MINUS:
    		timeMultiplier = timeMultiplier / 2;
    		break;
    		
    		
    	case Input.KEY_LEFT:
    		offsetx += 5; break;
    	case Input.KEY_RIGHT:
    		offsetx -= 5; break;
    	case Input.KEY_UP:
    		offsety += 5; break;
    	case Input.KEY_DOWN:
    		offsety -= 5; break;
    		
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
    	if(key == Input.KEY_LSHIFT) { shifting = false; }
    	if(creating != null) {
    		creating.isStatic = shifting;
    	}
    }

	/**************** Game Logic ****************/
	
	public void checkCrash(GParticle p1, GParticle p2) {
		
		if ( distance(p1,p2) - GravitySim.crashDistance <= p1.getRadius() + p2.getRadius()) {
			crashParticles(p1,p2);
		}
	}
	
	public float distance(GParticle p1, GParticle p2) {
		
		return distance(p1.px, p1.py, p2.px, p2.py);
	}
	
	public float distance(float x, float y, float x2, float y2) {
		float dx = x2 - x;
		float dy = y2 - y;

		float d  = (float) Math.sqrt(
				Math.pow(Math.abs(dx),2) + 
				Math.pow(Math.abs(dy),2)
				);
		return d;
	}
	
	public float[] force(GParticle p1, GParticle p2) {
		float dx = p2.px - p1.px;
		float dy = p2.py - p1.py;

		//System.out.println("------------------------------");		
		//System.out.println("p1:" + p1.id + " p2:" + p2.id);
		//System.out.println("p1x:" + p1.px + " p2x:" + p2.px);
		//System.out.println("p1y:" + p1.py + " p2y:" + p2.py);
		
		//System.out.println("dx:" + dx    + " dy:" + dy   );
		//System.out.println("total distance: " + d);

		float rSquared = (dx*dx) + (dy*dy);
		float r = (float) Math.sqrt(rSquared);
		float forceMagnitude = (float) (gravity * p1.mass * p2.mass) / rSquared;
		
		float dxNormalizedScaled = (float) ((dx / r) * forceMagnitude / 1000.0);
		float dyNormalizedScaled = (float) ((dy / r) * forceMagnitude / 1000.0);
		
		//System.out.println("Force on " + p1.id + " by " + p2.id + " is X:" + dxNormalizedScaled + " Y:" + dyNormalizedScaled);
		//System.out.println("------------------------------");
		
		float[] ret = {dxNormalizedScaled, dyNormalizedScaled};
		return ret;
		
	}
	
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
