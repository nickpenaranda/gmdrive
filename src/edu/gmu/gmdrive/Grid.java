package edu.gmu.gmdrive;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Vector2f;

public class Grid {
	private static final Vector2f ORIGIN = new Vector2f(0.0f, 0.0f);
	private static final Vector2f MAJOR_SIZE = new Vector2f(1609.3f, 1609.3f); // 1 mile x 1 mile
	private static final Vector2f MINOR_FACTOR = new Vector2f(0.25f, 0.25f);
	
	public Grid() {
		
	}
	
	public void render(GameContainer gc, Graphics g) {
		Rectangle worldRect = g.getWorldClip();
		g.pushTransform();
			g.translate(ORIGIN.getX(), ORIGIN.getY());
			float curX = ORIGIN.getX();
			while(curX < worldRect.getMaxX()) {
				g.setColor(Color.gray);
				g.drawLine(curX,worldRect.getMinY(),curX,worldRect.getMaxY());
				g.setColor(Color.darkGray);
				for(float minorX = MAJOR_SIZE.getX() * MINOR_FACTOR.getX(); minorX < MAJOR_SIZE.getX(); minorX += MAJOR_SIZE.getX() * MINOR_FACTOR.getX()) {
					g.drawLine(curX + minorX,worldRect.getMinY(),curX + minorX,worldRect.getMaxY());
				}
				curX += MAJOR_SIZE.getX();
			}
			float curY = ORIGIN.getY();
			while(curY < worldRect.getMaxY()) {
				g.setColor(Color.gray);
				g.drawLine(worldRect.getMinX(),curY,worldRect.getMaxX(),curY);
				g.setColor(Color.darkGray);
				for(float minorY = MAJOR_SIZE.getY() * MINOR_FACTOR.getY(); minorY < MAJOR_SIZE.getY(); minorY += MAJOR_SIZE.getY() * MINOR_FACTOR.getY()) {
					g.drawLine(worldRect.getMinX(),curY + minorY,worldRect.getMaxX(),curY + minorY);
				}
				curY += MAJOR_SIZE.getY();
			}
		g.popTransform();
	}
}
