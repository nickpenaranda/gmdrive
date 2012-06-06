package edu.gmu.gmdrive;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Transform;
import org.newdawn.slick.geom.Vector2f;

public class Vehicle extends RigidBody {
	//private static final float C_DRAG = 0.4257f; 
	private static final float C_DRAG = 0.09f;
	private static final float C_ROLLING_RESISTANCE = 6.8f;
	
	private Wheel[] mWheels;

	/*
	 * These values are based on a 2010 Ford Mustang
	 * Length in meters, weight in kg
	 */
	public Vehicle() { 
		super(new Vector2f(0.935f, 2.385f), 542.6f);
		
		mWheels = new Wheel[4];
		mWheels[0] = new Wheel("FL",new Vector2f(-0.791f, 1.36f), 0.4826f); // FL
		mWheels[1] = new Wheel("FR",new Vector2f(0.791f, 1.36f),0.4826f); // FR
		mWheels[2] = new Wheel("RL",new Vector2f(-0.798f,-1.36f),0.4826f); // RL
		mWheels[3] = new Wheel("RR",new Vector2f(0.798f,-1.36f),0.4826f); // RR
	}
	
	public void setSteering(float steering) {
		float steeringLock = 0.349f;
		
		mWheels[0].setSteeringAngle(steering * steeringLock);
		mWheels[1].setSteeringAngle(steering * steeringLock);
	}
	
	public void setThrottle(float throttle) {
		//float maxTorque = 325.4f; // 240 ft-lbs
		float maxTorqueFtLb = 90.0f;
		float maxTorqueNM = maxTorqueFtLb * 1.3558f;
		
		mWheels[2].addTransmissionTorque(throttle * maxTorqueNM / 2);
		mWheels[3].addTransmissionTorque(throttle * maxTorqueNM / 2);
	}
	
	public void setBrakes(float brakes) {
		float maxBrakeTorque = 50.0f;
		
		for(Wheel wheel : mWheels) {
			float speed = wheel.getWheelSpeed();
			float sign = Math.signum(speed);
			float brakeTorque = -sign * maxBrakeTorque * brakes * brakes;
			wheel.addTransmissionTorque(brakeTorque);
		}
	}
	
	public void update(int delta) {
		float timeStep = delta / 1000.0f;
		
		/* per-wheel forces */
		for(Wheel wheel : mWheels) {
			Vector2f worldWheelOffset = relToWorld(wheel.getAttachPoint());
			Vector2f worldGroundVel = pointVel(worldWheelOffset);
			Vector2f relGroundVel = worldToRel(worldGroundVel);
			Vector2f relRespForce = wheel.calculateForce(relGroundVel, timeStep);
			Vector2f worldRespForce = relToWorld(relRespForce);
			
			this.addForce(worldRespForce,worldWheelOffset);
		}
		
		super.update(delta);
		/* Rolling resistance */
		this.addForce(mVelocity.copy().scale(-C_ROLLING_RESISTANCE), new Vector2f(0,0));

		/* Linear Drag */
		this.addForce(mVelocity.copy().scale(-C_DRAG * mVelocity.length() * getMass()), new Vector2f(0,0));
	}
	
	public void render(GameContainer gc, Graphics g) {
		super.render(gc, g);
		g.pushTransform();
			g.translate(mPosition.x, mPosition.y);
			g.rotate(0, 0, mAngle * 180 / (float)Math.PI);
			for(Wheel wheel : mWheels) {
				g.setColor(Color.gray);
				float radius = wheel.getRadius();
				Vector2f wheelPos = wheel.getAttachPoint();
				g.pushTransform();
					g.translate(wheelPos.x, wheelPos.y);
					g.pushTransform();
						g.translate(wheelPos.x * 10, wheelPos.y * 10);
						g.scale(.5f, 0.5f);
					g.popTransform();
					//g.drawLine(0, 0, wheel.mForwardAxis.x*5, wheel.mForwardAxis.y*5);
					g.pushTransform();
						g.rotate(0.0f, 0.0f, wheel.mSteeringAngle * 180 / (float)Math.PI);
						g.fillRect(radius / 4f, -radius, radius / 2f, radius * 2f);
					g.popTransform();
					
//					g.setColor(Color.red);
//					g.drawLine(0.0f, 0.0f, wheel.mResponseForce.x / 10, wheel.mResponseForce.y / 10);
				g.popTransform();
			}
			g.setColor(Color.white);
		g.popTransform();
	}
	
	private class Wheel {
		private Vector2f mForwardAxis, mSideAxis;
		private Vector2f mPosition;
		private Vector2f mResponseForce;
		
		private float mWheelTorque, mWheelSpeed, mWheelInertia, mWheelRadius;
		private float mSteeringAngle;
		private String mID;
		
		public Wheel(String id, Vector2f position, float radius) {
			mID = id;
			mResponseForce = new Vector2f();
			mPosition = position;
			
			mWheelRadius = radius;
			
			setSteeringAngle(0.0f);
			
			mWheelSpeed = 0.0f;
			mWheelInertia = (1.0f / 2.0f) * 1.5f * radius * radius;
		}
		
		public String getID() {
			return mID;
		}

		public float getTorque() {
			return mWheelTorque;
		}

		public void setSteeringAngle(float newAngle) {
			Transform mat = Transform.createRotateTransform(newAngle);
			mForwardAxis = mat.transform(new Vector2f(0.0f, 1.0f));
			mSideAxis = mat.transform(new Vector2f(-1.0f, 0.0f));
			mSteeringAngle = newAngle;
		}
		
		public void addTransmissionTorque(float torque) {
			mWheelTorque += torque;
		}
		
		public Vector2f calculateForce(Vector2f relGroundSpeed, float timeStep) {
			Vector2f patchSpeed = mForwardAxis.negate().scale(mWheelSpeed * mWheelRadius);
			Vector2f velDiff = patchSpeed.add(relGroundSpeed);
			
			Vector2f responseForce = new Vector2f();
			
			Vector2f sideVel = new Vector2f();
			Vector2f forwardVel = new Vector2f();

			velDiff.projectOntoUnit(mSideAxis, sideVel);
			velDiff.projectOntoUnit(mForwardAxis, forwardVel);
			
			float forwardMag = velDiff.dot(mForwardAxis);
			
			sideVel.scale(1.5f); // Arbitrary
			
			responseForce.sub(sideVel).sub(forwardVel).scale(542.6f / 4.0f);
			
			mWheelTorque += forwardMag * mWheelRadius;
			
			float preSign = Math.signum(mWheelSpeed);
			mWheelSpeed += mWheelTorque * mWheelRadius / mWheelInertia * timeStep;
			if(preSign != 0.0f && Math.signum(mWheelSpeed) != preSign)
				mWheelSpeed = 0.0f;
			
			mWheelTorque = 0;
			mResponseForce = responseForce;
			return(responseForce);
		}
		
		public float getWheelSpeed() { return mWheelSpeed; }
		public float getRadius() { return mWheelRadius; }
		public Vector2f getAttachPoint() { return mPosition; }
	}

	public float getSpeedometer() {
		Vector2f vehForwardAxis = new Vector2f(1.0f,0.0f);
		vehForwardAxis.setTheta((mAngle * 180f / (float)Math.PI) + 90);
		return(Math.abs(mVelocity.dot(vehForwardAxis)));
	}	
}
