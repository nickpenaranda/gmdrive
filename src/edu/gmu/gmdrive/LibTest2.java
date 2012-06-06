package edu.gmu.gmdrive;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

public class LibTest2 extends BasicGame {
	int activeControllerIndex = 1; //@HACK
	Input input;
	String[] axisLabels;
	
	public LibTest2() {
		super("LibTest2");
	}

	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException {
		int j = 0;
		for(String label : axisLabels) {
			float value = input.getAxisValue(activeControllerIndex, j);
			g.pushTransform();
				g.translate(32 + j * 160, 240);
				g.drawString(axisLabels[j], 16, 0);
				g.drawString("" + value, 0, -100);
				//g.rotate(0,0,input.getAxisValue(activeControllerIndex, j) * -180.0F);
				//g.drawLine(0, 0, 0, 100);
				g.drawRect(0, 8, 16, value * 100F);
			g.popTransform();
			++j;
		}
	}

	@Override
	public void init(GameContainer gc) throws SlickException {
		input = gc.getInput();
		/* Display controller and axis info */
		int numControllers = input.getControllerCount();
		for(int i=0;i<numControllers;++i) {
			int numAxes = input.getAxisCount(i);
			for(int j=0;j<numAxes;++j) {
				System.out.println("Controller " + i + ", Axis " + j + ": " + input.getAxisName(i, j));
			}
		}
		
		/* Populate axis labels */
		int numAxes = input.getAxisCount(activeControllerIndex);
		axisLabels = new String[numAxes];
		for(int j=0;j<numAxes;++j) {
			axisLabels[j] = input.getAxisName(activeControllerIndex, j);
		}
	}

	@Override
	public void update(GameContainer gc, int delta) throws SlickException {
	}
	
	
	public static void main(String[] args) {
		try {
			AppGameContainer gc = new AppGameContainer(new LibTest2());
			gc.setDisplayMode(640,480,false);
			gc.setTargetFrameRate(120);
			gc.start();
			
		} catch (SlickException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
