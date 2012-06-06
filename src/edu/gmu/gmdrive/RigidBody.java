package edu.gmu.gmdrive;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Transform;
import org.newdawn.slick.geom.Vector2f;

public class RigidBody {
	/* 
	 * http://www.gamedev.net/topic/470497-2d-car-physics-tutorial/
	 * thank you bzroom
	*/
	protected Vector2f mPosition;
	protected Vector2f mVelocity;
	private Vector2f mForces;
	
	private float mMass;
	private Vector2f mHalfSize;
	
	protected float mAngle;
	protected float mAngularVelocity;
	private float mTorque;
	private float mInertia;
	
	public RigidBody(Vector2f halfSize, float mass) {
		mPosition = new Vector2f();
		mVelocity = new Vector2f();
		mForces = new Vector2f();
		mHalfSize = halfSize;
		mMass = mass;
		
		mInertia = (1.0f / 12.0f) * mMass * ((4 * mHalfSize.x * mHalfSize.x) + (4 * mHalfSize.y * mHalfSize.y)) * 1.2f;
	}
		
	public void update(int delta) {
		float timeStep = delta / 1000f;
		/* Linear */
		Vector2f acceleration = mForces.scale(1/mMass);
		mVelocity.add(acceleration.scale(timeStep));
		mPosition.add(mVelocity.copy().scale(timeStep));
		mForces.set(0.0f, 0.0f);
		
		/* Angular */
		float angAcc = mTorque / mInertia;
		mAngularVelocity += angAcc * timeStep;
		mAngle += mAngularVelocity * timeStep;
		mTorque = 0.0f;
	}
	
	public void render(GameContainer gc, Graphics g) {
		g.pushTransform();
			g.translate(mPosition.x,mPosition.y);
			g.rotate(0.0f, 0.0f, mAngle * 180 / (float)Math.PI);
			g.setColor(Color.white);
			g.drawRect(-mHalfSize.x, -mHalfSize.y, 
						mHalfSize.x * 2, mHalfSize.y * 2);
		g.popTransform();
		
		
	}
	
	public void setLocation(Vector2f position, float angle) {
		mPosition = position;
		mAngle = angle;
	}
	
	public Vector2f getLocation() { return(mPosition); }
	
	public Vector2f relToWorld(Vector2f relative) {
		Transform mat = Transform.createRotateTransform(mAngle);
		return(mat.transform(relative));
	}
	
	public Vector2f worldToRel(Vector2f world) {
		Transform mat = Transform.createRotateTransform(-mAngle);
		return(mat.transform(world));
	}
	
	public Vector2f pointVel(Vector2f worldOffset) {
		Vector2f tangent = new Vector2f(-worldOffset.y,worldOffset.x);
		return(tangent.scale(mAngularVelocity).add(mVelocity));
	}
	
	public void addForce(Vector2f worldForce, Vector2f worldOffset) {
		mForces.add(worldForce);
		mTorque += (worldOffset.x * worldForce.y) - (worldOffset.y * worldForce.x);
	}
	
	public float getMass() {
		return mMass;
	}
}
