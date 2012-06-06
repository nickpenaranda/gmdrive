package edu.gmu.gmdrive;

public class Vec2 {
	float x,y;
	
	public Vec2() {
		x = y = 0.0f;
	}
	
	public Vec2(int x,int y) {
		this.x = (float)x;
		this.y = (float)y;
	}
	
	public Vec2(double x,double y) {
		this.x = (float)x;
		this.y = (float)y;
	}
	
	public Vec2(float x,float y) {
		this.x = x;
		this.y = y;
	}
	
	public Vec2 add(Vec2 other) {
		return(new Vec2(this.x + other.getX(),this.y + other.getY()));
	}
	
	public Vec2 sub(Vec2 other) {
		return(new Vec2(this.x - other.getX(),this.y - other.getY()));
	}
	
	public Vec2 mult(float k) {
		return(new Vec2(this.x * k, this.y * k));
	}
	
	public Vec2 div(float k) {
		return(new Vec2(this.x / k, this.y / k));
	}

	public float dot(Vec2 other) {
		return(this.x * other.getX() + this.y * other.getY());
	}
	
	public float cross(Vec2 other) {
		return(this.x * other.getY() - this.y * other.getX());
	}
	
	public float length() {
		return((float)Math.sqrt(x * x + y * y));
	}
	
	public float getX() { return x; }
	public float getY() { return y; }
}
