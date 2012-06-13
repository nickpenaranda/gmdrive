package edu.gmu.gmdrive;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

public class Waypoint extends PointSound {
	private static final float VISUAL_RADIUS = 100; 
	private int mPingInterval,mPingAccumulator;
	
	//private FloatBuffer mSourcePos, mSourceVel;
	
	public Waypoint() throws SlickException {
		super("waypoint.wav");
		
		mPingInterval = 0;
		mPingAccumulator = 0;
	}
	
	public void update(int delta) {
		mPingAccumulator += delta;
		if(mPingAccumulator > mPingInterval) {
			this.play();
			mPingAccumulator = 0;
		}
	}
	
	public void render(GameContainer container, Graphics g) {
		Vector2f pos = this.getPos();
		g.setColor(Color.magenta);
		g.pushTransform();
			g.translate(pos.getX(), pos.getY());
			g.drawOval(-VISUAL_RADIUS,-VISUAL_RADIUS,VISUAL_RADIUS * 2,VISUAL_RADIUS * 2);
		g.popTransform();
	}

	public void setPingInterval(int interval) {
		mPingInterval = interval; 
	}
	
}
