package edu.gmu.gmdrive;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.slick2d.NiftyOverlayBasicGame;

public class GMDrive extends NiftyOverlayBasicGame {
	ArrayList<RigidBody> mRigidBodies;
	Vehicle mDriverVehicle;
	ArrayList<Waypoint> mWaypoints;
	
	public static final int JOY_INDEX = 0;

	public static final int AXIS_ACC = 2;
	public static final int AXIS_BRAKE = 3;
	public static final int AXIS_STEER = 0;
	
	public static final int PATH_INTERVAL = 250;
	ArrayList<Vector2f> mPath;
	int mPathTick;
	
	
	MessageWindow mMessageWindow;
	Grid mGrid;
	
	Input mInput;
	float steeringInput, accInput, brakeInput;
	
	public static final float WORLD_SCALE = 0.5f;
	public GMDrive() {
		super("GMDrive");
	}

	@Override
	public void initGameAndGUI(GameContainer container) throws SlickException {
		/* Audio init */
		
		initNifty(container);
		
		try {
			AL.destroy();
			AL.create("DirectSound3D", 44100, 60, false);
			AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE_CLAMPED);
		} catch (LWJGLException e) {
			e.printStackTrace();
			container.exit();
		}
		
		/* Set up GUI */
		
		
		mMessageWindow = new MessageWindow();
		mGrid = new Grid();
		mWaypoints = new ArrayList<Waypoint>();
		
		Waypoint sound;
		sound = new Waypoint();
		sound.setLoc(new Vector2f(1280.0f, 960.0f), new Vector2f(0.0f, 0.0f));
		sound.play();
		
		mWaypoints.add(sound);

		AL10.alListener3f(AL10.AL_POSITION, 0f, 0f, 0f);
		AL10.alListener3f(AL10.AL_VELOCITY, 0f, 0f, 0f);		
		FloatBuffer listenerOri =
				BufferUtils.createFloatBuffer(6).put(new float[] { 0.0f, 0.0f, -1.0f,  0.0f, 1.0f, 0.0f });
		listenerOri.rewind();
		AL10.alListener(AL10.AL_ORIENTATION, listenerOri);
		

		mInput = container.getInput();
		steeringInput = accInput = brakeInput = 0;
		
		mRigidBodies = new ArrayList<RigidBody>();
		
		mDriverVehicle = new Vehicle();
		mDriverVehicle.setLocation(new Vector2f(50.0f, 50.0f), 0);
		
		mRigidBodies.add(mDriverVehicle);
		
		mPath = new ArrayList<Vector2f>();
		mPathTick = 0;
		
		mMessageWindow.add("GMDrive is loading...");
		mMessageWindow.add("Ready.");
		
		container.getGraphics().setWorldClip(0.0f,0.0f,(float)container.getWidth() / WORLD_SCALE,(float)container.getHeight() / WORLD_SCALE);
	}

	@Override
	public void renderGame(GameContainer container, Graphics g) throws SlickException {
		g.setFont(mMessageWindow.getFont());
		g.drawString("Steering: " + steeringInput, 0, 32);
		g.drawString("Throttle: " + accInput, 0, 48);
		g.drawString("  Brakes: " + brakeInput, 0, 64);
		
		float vehSpeed = mDriverVehicle.getSpeedometer();
		g.drawString("   Speed: " + String.format("%.2f", vehSpeed * 2.2369) + " mph", 0, 80);
				
		/* World coordinates */
		g.pushTransform();
			g.scale(WORLD_SCALE, WORLD_SCALE);
			mGrid.render(container,g);
			
			Vector2f lastNode = null;
			for(Vector2f pathNode : mPath) {
				if(lastNode != null) {
					g.setColor(Color.blue);
					g.drawLine(pathNode.x, pathNode.y, lastNode.x, lastNode.y);
				}
				g.setColor(Color.cyan);
				g.fillRect(pathNode.x - 0.5f, pathNode.y - 0.5f, 1.0f, 1.0f);
				lastNode = pathNode;
			}
			for(Waypoint sound : mWaypoints) {
				sound.render(container, g);
			}
			for(RigidBody rb : mRigidBodies) {
				rb.render(container, g);
			}
		g.popTransform();
		
		mMessageWindow.render(container, g);
	}

	@Override
	public void updateGame(GameContainer container, int delta) throws SlickException {
		mMessageWindow.update(container, delta);
		
		if(mInput.isKeyDown(Input.KEY_ESCAPE))
			container.exit();
		
		steeringInput = mInput.getAxisValue(JOY_INDEX, AXIS_STEER);
		brakeInput = -(mInput.getAxisValue(JOY_INDEX, AXIS_BRAKE) - 1.0f) / 2;
		accInput = -(mInput.getAxisValue(JOY_INDEX, AXIS_ACC) - 1.0f) / 2;

		//if(Math.abs(steeringInput) < 0.1f) steeringInput = 0f;
		if(Math.abs(accInput) < 0.1f) accInput = 0f;
		if(Math.abs(brakeInput) < 0.1f) brakeInput = 0f;

		mDriverVehicle.setSteering(steeringInput);
		mDriverVehicle.setBrakes(brakeInput);
		mDriverVehicle.setThrottle(accInput);
		
		for(RigidBody rb : mRigidBodies) {
			rb.update(delta);
		}
		
		/* Update OpenAL listener state */
		Vector2f driverPos = mDriverVehicle.getLocation();
		Vector2f driverVel = mDriverVehicle.pointVel(new Vector2f(0.0f,0.0f));
		Vector2f driverAt = new Vector2f(1.0f,0.0f);
		driverAt.setTheta(mDriverVehicle.mAngle * 180 / Math.PI);
		
		AL10.alListener3f(AL10.AL_POSITION, driverPos.getX(), driverPos.getY(), 0f);
		AL10.alListener3f(AL10.AL_VELOCITY, driverVel.getX(), driverVel.getY(), 0f);
		FloatBuffer listenerOri =
				BufferUtils.createFloatBuffer(6).put(new float[] { -driverAt.getY(), driverAt.getX(), 0.0f,  0.0f, 0.0f, -1.0f });
		listenerOri.rewind();
		AL10.alListener(AL10.AL_ORIENTATION, listenerOri);
		
		/* Update ping rates for waypoints */
		for(Waypoint ps : mWaypoints) {
			float dist = driverPos.distance(ps.getPos());
			int roundDist = Math.round(dist) * 2;
			ps.setPingInterval(roundDist < 200 ? 200 : roundDist);
			ps.update(delta);
		}
		
		/* Path saving */
		mPathTick += delta;
		if(mPathTick > PATH_INTERVAL) {
			mPathTick -= PATH_INTERVAL;
			mPath.add(driverPos.copy());
		}
	}
	
	public static void main(String[] args) {
		try {
			AppGameContainer gameContainer = new AppGameContainer(new GMDrive());
			gameContainer.setUpdateOnlyWhenVisible(false);
			gameContainer.setAlwaysRender(true);
			gameContainer.setMaximumLogicUpdateInterval(20);
			gameContainer.setMinimumLogicUpdateInterval(10);
			gameContainer.setDisplayMode(1680, 1050, true);
			gameContainer.setTargetFrameRate(60);
			gameContainer.start();
		} catch (SlickException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	protected void prepareNifty(Nifty nifty) {
		//nifty.fromXml("gui.xml", "start");
	}

}
