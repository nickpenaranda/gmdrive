package edu.gmu.gmdrive;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

public class SoundTest extends BasicGame {
	IntBuffer buffer;
	IntBuffer source;
	
	public SoundTest() {
		super("Sound Test");
	}
	
	@Override
	public void render(GameContainer container, Graphics g)
			throws SlickException {
	}

	@Override
	public void init(GameContainer container) throws SlickException {
		try {
			AL.destroy();
			AL.create("DirectSound3D", 44100, 60, false);
		} catch (LWJGLException e) {
			e.printStackTrace();
			container.exit();
		}
		
		if(AL10.alGetError() != AL10.AL_NO_ERROR) {
			System.out.println("Error creating AL context");
			container.exit();
		}
		
		WaveData waveData = null;
		
		try {
			waveData = WaveData.create(new BufferedInputStream(new FileInputStream("SONAR.wav")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			container.exit();
		}

		buffer = BufferUtils.createIntBuffer(1);
		source = BufferUtils.createIntBuffer(1);
		
		AL10.alGenBuffers(buffer);
		AL10.alGenSources(source);
		
		int buf = buffer.get(0);
		int src = source.get(0);

		AL10.alBufferData(buf, waveData.format, waveData.data, waveData.samplerate);
		waveData.dispose();
		
		AL10.alSourcei(src, AL10.AL_BUFFER, buf); // Bind buffer to this source
		AL10.alSourcef(src, AL10.AL_PITCH, 1.0f);
		AL10.alSourcef(src, AL10.AL_GAIN, 0.5f);
		AL10.alSource3f(src, AL10.AL_POSITION, 0f, 0f, 0f);
		AL10.alSource3f(src, AL10.AL_VELOCITY, 0f, 0f, 0f);
		AL10.alSourcei(src, AL10.AL_LOOPING, AL10.AL_TRUE);
		
		AL10.alListener3f(AL10.AL_POSITION, 0f,0f,0f);
		AL10.alListener3f(AL10.AL_VELOCITY, 0f,0f,0f);
		
		FloatBuffer listenerOri = BufferUtils.createFloatBuffer(6)
				.put(new float[] {0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f});
		listenerOri.rewind();
		AL10.alListener(AL10.AL_ORIENTATION, listenerOri);
		AL10.alSourcePlay(src);
	}

	@Override
	public void update(GameContainer container, int delta)
			throws SlickException {
		int src = source.get(0);
		
		AL10.alSource3f(src, AL10.AL_POSITION, 20f, 1f, 0f);
		AL10.alSourcef(src, AL10.AL_GAIN, 1f);
		
		AL10.alListener3f(AL10.AL_POSITION, 0f, 0f, 0f);
	}

	
	@Override
	public boolean closeRequested() {
		System.out.println("Exiting...");
		AL.destroy();
		return true;
	}

	public static void main(String[] args) {
		try {
			AppGameContainer gc = new AppGameContainer(new SoundTest());
			gc.setDisplayMode(640, 480, false);
			gc.start();
		} catch(SlickException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
