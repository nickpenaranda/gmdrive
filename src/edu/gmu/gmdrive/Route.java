package edu.gmu.gmdrive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class Route extends DefaultHandler {
	private Vector<Waypoint> mWaypoints;
	private String mRouteName;
	
	public Route(String filename) {
		mWaypoints = new Vector<Waypoint>();
		mRouteName = "Untitled Route";
		
		File file = new File(filename);
		InputSource src = null;
		
		try {
			src = new InputSource(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Could not load route file: " + filename + "\nRoute is empty!");
			return;
		}
		
		XMLReader reader = null;
		try {
			reader = XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			e.printStackTrace();
			System.err.println("Could not create SAX object.\nRoute is empty!");
		}
		if(reader != null) {
			reader.setContentHandler(this);
			try {
				reader.parse(src);
			} catch (IOException | SAXException e) {
				e.printStackTrace();
				System.err.println("Error reading file: " + filename + "\nRoute may have only partially loaded!");
			}
		}
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attribs) throws SAXException {
		if(localName.equals("route")) {
			mRouteName = attribs.getValue(uri,"name");
		} else if(localName.equals("waypoint")) {
			float x = Float.parseFloat(attribs.getValue(uri,"x"));
			float y = Float.parseFloat(attribs.getValue(uri,"y"));
			Waypoint tmp = null;
			try {
				tmp = new Waypoint();
				tmp.setLoc(new Vector2f(x * GMDrive.MILE_TO_METER,y * GMDrive.MILE_TO_METER), new Vector2f(0.0f, 0.0f));
			} catch (SlickException e) {
				e.printStackTrace();
			}
			if(tmp != null) mWaypoints.add(tmp);
		}
		
	}
}
