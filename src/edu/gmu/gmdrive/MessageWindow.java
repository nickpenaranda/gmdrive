package edu.gmu.gmdrive;

import java.awt.Color;
import java.util.ArrayList;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;

public class MessageWindow {
	private static final int MSG_DURATION = 5000;
	private static final int FONT_SIZE = 12;
	private static final int VERT_PADDING = 2;
	
	ArrayList<Message> mMessages;
	UnicodeFont mFont;
	
	public MessageWindow() {
		try {
			mFont = new UnicodeFont("res/VeraMono.ttf",FONT_SIZE,false,false);
			mFont.addAsciiGlyphs();
			mFont.getEffects().add(new ColorEffect(Color.WHITE));
			mFont.loadGlyphs();
		} catch (SlickException e) {
			e.printStackTrace();
		}
		mMessages = new ArrayList<Message>();
	}
	
	public void update(GameContainer gc, int delta) {
		ArrayList<Message> removeList = new ArrayList<Message>();
		for(Message msg : mMessages) {
			if(!msg.updateDuration(delta)) {
				removeList.add(msg);
			}
		}
		mMessages.removeAll(removeList);
	}
	
	public void render(GameContainer gc,Graphics g) {
		int n = 1;
		for(Message msg : mMessages) {
			int y = gc.getHeight() - (n++ * (FONT_SIZE + VERT_PADDING));
			mFont.drawString(0.0f, y, msg.getText());
		}
	}
	
	public void add(String text) {
		mMessages.add(0,new Message(text,MSG_DURATION));
	}
	
	/*
	 * HACK!
	 */
	public UnicodeFont getFont() {
		return mFont;
	}

	class Message {
		private String mText;
		private int mDuration;
		
		public Message(String text, int duration) {
			mText = text;
			mDuration = duration;
		}
		
		/*
		 * Returns false if duration falls below 0 (needs to be removed from message list)
		 */
		public boolean updateDuration(int delta) {
			mDuration -= delta;
			return (mDuration > 0) ? true : false;
		}
		
		public int getDuration() {
			return mDuration;
		}
		
		public String getText() {
			return mText;
		}
	}
}
