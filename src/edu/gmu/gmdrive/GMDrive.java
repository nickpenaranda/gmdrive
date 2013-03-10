package edu.gmu.gmdrive;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.slick2d.NiftyOverlayBasicGame;

public class GMDrive extends NiftyOverlayBasicGame {
	ArrayList<RigidBody> mRigidBodies;
	Vehicle mDriverVehicle;
	//ArrayList<Waypoint> mWaypoints;
	Waypoint mNextWaypoint;
	Route mRoute;
	
	/*
	 * CONVERSION CONSTANTS
	 */
	public static final float MILE_TO_METER = 1609.3f;
	public static final float METER_TO_MILE = 1 / MILE_TO_METER;
	
	public static final float MPH_TO_MPS = 0.447f;
	public static final float MPS_TO_MPH = 1 / MPH_TO_MPS;
	
	/*
	 * CHANGE THESE VALUES TO REFLECT SYSTEM CONFIG
	 */
	public static final int JOY_INDEX = 0;
	public static final int AXIS_ACC = 2;
	public static final int AXIS_BRAKE = 3;
	public static final int AXIS_STEER = 0;
	public static final int SCREEN_W = 800;
	public static final int SCREEN_H = 800;
	
	public static final boolean USE_JOYSTICK = false;
	public static final boolean TESTING_MODE = true;
	public static final boolean RECORD_DATA = true;
	
	/*
	 * EXPERIMENT PARAMETERS
	 */
	public static final Vector2f START_POSITION = new Vector2f(MILE_TO_METER * 1.0f, MILE_TO_METER * 1.0f);
	
	public static final int PATH_INTERVAL = 250;
	ArrayList<Vector2f> mPath;
	int mPathTick;
	
	MessageWindow mMessageWindow;
	Grid mGrid;
	
	Input mInput;
	float steeringInput, accInput, brakeInput;
	int startTime,simTime;
	
	FileWriter outputFile;
	
	public static final float WORLD_SCALE = SCREEN_H / MILE_TO_METER / 2;
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
		
		mPath = new ArrayList<Vector2f>();
		mRigidBodies = new ArrayList<RigidBody>();
		mInput = container.getInput();
		container.getGraphics().setWorldClip(0.0f,0.0f,(float)container.getWidth() / WORLD_SCALE,(float)container.getHeight() / WORLD_SCALE);
		
		if(RECORD_DATA) {
			try {
				outputFile = new FileWriter( new SimpleDateFormat("ddMMyy-hhmmss").format(new Date()) + ".csv");
				outputFile.append("Time (ms),OwnX,OwnY,Accelerator,Brake,Steering,Speed (mph),TargetX,TargetY\n");
			} catch(IOException e) {
				e.printStackTrace();
				container.exit();
			}
			
		}
		/* Set up GUI */
		mMessageWindow = new MessageWindow();
		mGrid = new Grid();
		
		initScenario("route4.xml");
	}

	private void initScenario(String filename) {
		mMessageWindow.add("GMDrive is loading " + filename + "...");
		mRoute =  new Route(filename);
		
		steeringInput = accInput = brakeInput = 0;
		startTime = simTime = 0;
		mPathTick = 0;
		
		/* Init driver vehicle and set position to first waypoint */
		mDriverVehicle = new Vehicle();
		Vector2f pos = mRoute.getNext().getPos();
		mDriverVehicle.setLocation(pos, (float)Math.PI);
		mRigidBodies.add(mDriverVehicle);
		
		try {
			mNextWaypoint = new Waypoint();
		} catch (SlickException e) {
			e.printStackTrace();
		}
		mNextWaypoint.setLoc(mRoute.getNext().getPos(), new Vector2f(0.0f, 0.0f));
		
		mMessageWindow.add("Ready.");
	}
	@Override
	public void renderGame(GameContainer container, Graphics g) throws SlickException {
		g.setFont(mMessageWindow.getFont());
		g.drawString("Steering: " + steeringInput, 0, 0);
		g.drawString("Throttle: " + accInput, 0, 16);
		g.drawString("  Brakes: " + brakeInput, 0, 32);
		
		float vehSpeed = mDriverVehicle.getSpeedometer();
		g.drawString("   Speed: " + String.format("%.2f", vehSpeed * 2.2369) + " mph", 0, 48);
				
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

			if(mNextWaypoint != null) mNextWaypoint.render(container, g);
			
			for(RigidBody rb : mRigidBodies) {
				rb.render(container, g);
			}
		g.popTransform();
		
		mMessageWindow.render(container, g);
	}

	@Override
	public void updateGame(GameContainer container, int delta) throws SlickException {
		if(RECORD_DATA)
				recordData();
		
	  simTime += delta;
		mMessageWindow.update(container, delta);
		
		if(mInput.isKeyDown(Input.KEY_ESCAPE))
			container.exit();
		
		if(USE_JOYSTICK) {
	  		steeringInput = mInput.getAxisValue(JOY_INDEX, AXIS_STEER);
	  		brakeInput = -(mInput.getAxisValue(JOY_INDEX, AXIS_BRAKE) - 1.0f) / 2;
	  		accInput = -(mInput.getAxisValue(JOY_INDEX, AXIS_ACC) - 1.0f) / 2;
	
			if(Math.abs(accInput) < 0.1f) accInput = 0f;
			if(Math.abs(brakeInput) < 0.1f) brakeInput = 0f;
		} else {
			/* Steering */
			if(mInput.isKeyDown(Input.KEY_A)) steeringInput = -1.0f;
			else if(mInput.isKeyDown(Input.KEY_D)) steeringInput = 1.0f;
			else steeringInput = 0.0f;

			/* Accelerator */
			if(mInput.isKeyDown(Input.KEY_W)) accInput = 1.0f;
			else accInput = 0.0f;

			/* Brakes */
			if(mInput.isKeyDown(Input.KEY_S)) brakeInput = 1.0f;
			else brakeInput = 0.0f;
		}

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
		
		/* Update waypoint */
		if(mNextWaypoint != null) {
			float dist = driverPos.distance(mNextWaypoint.getPos());
			int roundDist = Math.round(dist);
			if(roundDist < 100)
				mNextWaypoint = mRoute.getNext();
			if(mNextWaypoint != null) { 
				mNextWaypoint.setPingInterval((roundDist * 2) < 200 ? 200 : roundDist * 2);
				mNextWaypoint.update(delta);
			}
		}
		/* Path saving */
		mPathTick += delta;
		if(mPathTick > PATH_INTERVAL) {
			mPathTick -= PATH_INTERVAL;
			mPath.add(driverPos.copy());
		}
	}
	
	private void recordData() {
		String row = 
				String.format("%d,%f,%f,%f,%f,%f,%f,%f,%f\n",
						simTime,
						mDriverVehicle.getLocation().x,
						mDriverVehicle.getLocation().y,
						accInput,
						brakeInput,
						steeringInput,
						mDriverVehicle.getSpeedometer() * 2.2369,
						mNextWaypoint.getPos().x,
						mNextWaypoint.getPos().y);
		//outputFile.append("Time,OwnX,OwnY,Accelerator,Brake,Steering,TargetX,TargetY\n");
		try {
			outputFile.append(row);
			outputFile.flush();
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String[] args) {
		try {
			AppGameContainer gameContainer = new AppGameContainer(new GMDrive());
			gameContainer.setUpdateOnlyWhenVisible(false);
			gameContainer.setAlwaysRender(true);
			gameContainer.setMaximumLogicUpdateInterval(20);
			gameContainer.setMinimumLogicUpdateInterval(10);
			gameContainer.setDisplayMode(SCREEN_W, SCREEN_H, false);
			gameContainer.setTargetFrameRate(60);
			gameContainer.setShowFPS(false);
			gameContainer.start();
		} catch (SlickException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	

	@Override
	public boolean closeRequested() {
		if(RECORD_DATA) {
			try {
				outputFile.flush();
				outputFile.close();
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		return super.closeRequested();
	}

	@Override
	protected void prepareNifty(Nifty nifty) {
		//nifty.fromXml("gui.xml", "start");
	}

}
