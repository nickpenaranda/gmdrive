package edu.gmu.gmdrive;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

public class PointSound {
	private static final float VISUAL_RADIUS = 16; 
	
	private IntBuffer mBuffer;
	private IntBuffer mSource;
	private Vector2f mPos, mVel;
	
	public PointSound(String filename) throws SlickException {
		mBuffer = BufferUtils.createIntBuffer(1);
		mSource = BufferUtils.createIntBuffer(1);
		
		/* Produce buffer */
		AL10.alGenBuffers(mBuffer);
		
		if(AL10.alGetError() != AL10.AL_NO_ERROR)
			throw new SlickException("AL10 Failure: Could not generate buffer handle");

		/* Produce source */
		AL10.alGenSources(mSource);
		
		if(AL10.alGetError() != AL10.AL_NO_ERROR)
			throw new SlickException("AL10 Failure: Could not generate source handle");

		WaveData waveData = null;

		try {
			waveData = WaveData.create(new BufferedInputStream(new FileInputStream(filename)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if(waveData == null)
			throw new SlickException(String.format("AL10 Failure: File Not Found (%s)",filename));
		
		AL10.alBufferData(mBuffer.get(0), waveData.format, waveData.data, waveData.samplerate);
		waveData.dispose();
		
		AL10.alSourcei(mSource.get(0), AL10.AL_BUFFER, mBuffer.get(0)); // Bind buffer to this source

		AL10.alSourcef(mSource.get(0), AL10.AL_REFERENCE_DISTANCE, 100.0f);
		AL10.alSourcef(mSource.get(0), AL10.AL_MAX_DISTANCE, 1000.0f);
		
		AL10.alSourcei(mSource.get(0), AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);

		AL10.alSourcef(mSource.get(0), AL10.AL_PITCH, 1.0f);
		AL10.alSourcef(mSource.get(0), AL10.AL_GAIN, 0.5f);
		
		AL10.alSource3f(mSource.get(0), AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
		AL10.alSource3f(mSource.get(0), AL10.AL_VELOCITY, 0.0f, 0.0f, 0.0f);
		
		AL10.alSourcei(mSource.get(0), AL10.AL_LOOPING, AL10.AL_FALSE);
	}
	
	public void update(int delta) {

	}
	
	public void setLoc(Vector2f pos, Vector2f vel) {
		mPos = pos;
		mVel = vel;
		
		AL10.alSource3f(mSource.get(0), AL10.AL_POSITION, mPos.x, mPos.y, 0.0f);
		AL10.alSource3f(mSource.get(0), AL10.AL_VELOCITY, mVel.x, mVel.y, 0.0f);
	}
	
	public Vector2f getPos() {
		return(mPos);
	}

	public Vector2f getVel() {
		return(mVel);
	}

	public void play() {
		AL10.alSourcePlay(mSource);
	}
	
	public void stop() {
		AL10.alSourceStop(mSource);
	}

	public void destroy() {
		AL10.alDeleteBuffers(mBuffer);
		AL10.alDeleteSources(mSource);
	}

	public void render(GameContainer container, Graphics g) {

	}
}
