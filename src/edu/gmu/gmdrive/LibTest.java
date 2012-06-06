package edu.gmu.gmdrive;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public class LibTest {
	public void start() {
		try {
			Display.setDisplayMode(new DisplayMode(640,480));
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		while(!Display.isCloseRequested()) {
			Display.update();
		}
		
		Display.destroy();
	}
	
	public static void main(String[] args) {
		LibTest test = new LibTest();
		test.start();
	}
}
