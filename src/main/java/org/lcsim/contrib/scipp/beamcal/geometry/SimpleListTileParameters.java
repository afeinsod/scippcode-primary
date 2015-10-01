/**
 * SimpleListTileParameters.java.
 *
 * Created on June 13 2014, 12:20 AM
 * Edited  on June 30 2014, 03:30 PM
 *
 * @author Christopher Milke
 * @version 1.1
 * 
 * This class handles all of the coordinate changing, tile parameters,
 * and tile labeling needed for the radial tiling scheme. It allows users
 * to set the radius of individual rings of the detector, and to control
 * the ARC LENGTH of the tiles on a per ring basis.
 *
 * Edited to allow the "crack" at phi=0 to be removable.
 *
 */
 
package org.lcsim.contrib.scipp.beamcal.geometry;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;

import de.schlichtherle.io.File;

import java.lang.String;
import java.lang.Short;
import java.lang.Math;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;


public class SimpleListTileParameters implements TileParameters{
    
    //CONSTRUCTOR
    public SimpleListTileParameters(String Gear) {
        gear_file = Gear;
        readGearFile();
        makeTables();
    }
    
    
    private void readGearFile() {    
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder(); 
            Document doc = db.parse( new File(gear_file) );
            
            doc.getDocumentElement().normalize();
            
            
            
            String ring_edges = doc.getElementsByTagName("ring_edges").item(0).getTextContent();
            String arc_count = doc.getElementsByTagName("arc_count").item(0).getTextContent();
            String offset = doc.getElementsByTagName("offset").item(0).getTextContent();
            String multiplier = doc.getElementsByTagName("multiplier").item(0).getTextContent();
            
            double degToRad = 2 * Math.PI / 360; 
            double convert = Double.parseDouble(offset) * degToRad;
            arcOffset = (float) convert;
            arcMultiplier = Float.parseFloat(multiplier);
            
            
            String[] ring_edge_list = ring_edges.trim().split(",");
            String[] arc_count_list = arc_count.trim().split(",");
            
            LastRing = ring_edge_list.length - 1;
            if ( LastRing != (arc_count_list.length-1) ) {
                throw new Exception("arc parameter does not match ring parameter number!");
            }
            
            RingToRadTable = new float[LastRing+1];
            for (int i = 0; i <= LastRing; i++) {
                RingToRadTable[i] = Float.parseFloat( ring_edge_list[i].trim() );
            }
            
            
            ArcCountTable = new short[LastRing+1];
            for (int i = 0; i <= LastRing; i++) {
                ArcCountTable[i] = Short.parseShort( arc_count_list[i].trim() );
            }

        
        }catch (Exception e) {
            e.printStackTrace();
        }        
    }
    
    
    //if tiles are ever made thinner than 1mm this will need to be modified
    //maybe try doubling everything
    private void makeTables() {
        float radEdge = RingToRadTable[LastRing];
        
        RadToRingTable = new short[ (int)radEdge ];
        
        short ring = 0;
        int radius = 0;
        
        while (ring <= LastRing) {
            RadToRingTable[ radius ] = ring;
            radius += incrementation;
            if ( radius >= RingToRadTable[ring] ) ++ring;
        }
    }
    
    
    
    //Takes a particular ring and arc, identifying a specific tile,
    //and returns the r/phi point of the arc at the corner of the 
    //outer-most (larger radius) edge and the clockwise-most edge.
    public float[] getCornerPolar(int ring, int arc) {
        float[] corner = new float[2];
        
        double radius = RingToRadTable[ring];


        int N = getArcsInRing(ring);
        double theta = (2*Math.PI) / N;
        double phi;
        
        int last_arc = N - 1;
        if      (arc < 0)        phi = (arc+N)*theta;
        else if (arc > last_arc) phi = (arc-N)*theta;
        else                     phi =   arc  *theta;
        
        
        //remove the "crack" at phi=0 using an offset. 
        phi = phi + arcOffset + ring*arcMultiplier;
        if (phi < 0)         phi += 2*Math.PI;
        if (phi > 2*Math.PI) phi -= 2*Math.PI;


        corner[0] = (float) radius;
        corner[1] = (float) phi;
    
        return corner;
      
    }


    //Does the same as the above, but returns the corner point
    //in x,y coordinates. 
    public float[] getCorner(int ring, int arc) {
        float[] xycorner = new float[2];

        float[] corner = getCornerPolar(ring, arc);
        double[] convert = PolarCoords.PtoC(corner[0],corner[1]);

        xycorner[0] = (float) convert[0];
        xycorner[1] = (float) convert[1];

        return xycorner;
    }
    
    
    //Takes a specific ring, and returns the number of arcs in that ring.
    public int getArcsInRing(int ring) {
        return ArcCountTable[ring];
    }
    
    
    private int getRing(double radius) {
        short tempRing = RadToRingTable[(int)radius];
        
        if (radius <= RingToRadTable[tempRing]) return tempRing;
        else return tempRing+1;
    }
    
    
    //Takes a specific ring and a phi coordinate, and returns the arc number
    //associated with that phi coordinate in that ring.
    public short getArc(int ring, double phi) {
        //account for fact that radial coordinates are cyclic
        if (phi < 0)         phi += 2*Math.PI;
        if (phi > 2*Math.PI) phi -= 2*Math.PI;
        
        
        double reduced_phi = (phi - arcOffset - arcMultiplier*ring);        
        if (reduced_phi < 0)         reduced_phi += 2*Math.PI;
        if (reduced_phi > 2*Math.PI) reduced_phi -= 2*Math.PI;
        
        
        int N = (int) getArcsInRing(ring);
        double delta_phi = (2*Math.PI)/N;
        double arcFraction = reduced_phi / delta_phi;
        int arc = (int) arcFraction;
        
        return (short)arc;
    }
    
    
    //Takes a point in r,phi coordinates, and returns the ID number
    //of the arcs that encloses the point. The ID is of the form
    // {ring number, arc number}.
    public short[] getIDpolar(double radius, double phi) {
        short[] ID = new short[2];

        short ring = (short) getRing(radius);
        short arc =  (short) getArc(ring,phi);
        
        ID[0] = ring;
        ID[1] = arc;

        return ID;
    }


    //Same as above, but takes a point in the form of x,y coordinates.
    public short[] getID(double x, double y) {
        double[] convert = PolarCoords.CtoP(x,y);
        return getIDpolar(convert[0],convert[1]);
    }
    
    
    //returns the radial edge of the detector
    public float getEdge() {
        return RingToRadTable[LastRing];  
    }
    
    
    //returns the edge of the detector in the form of the outermost ring
    public short getLastRing() {
        return (short)LastRing;
    }
    
    
    //returns whether or not the given radius is within the boundaries of the detector
    public boolean contains(double radius) {
        if ( 0 > radius || radius > getEdge() ) return false;
        else return true;
    }
    
    
    //returns whether or not the given ring is within the boundaries of the detector
    public boolean contains(int ring) {
        if ( 0 > ring || ring > getLastRing() ) return false;
        else return true;  
    }
    
    
    //For debugging purposes, returns all the parameters in the form of
    //a string: radOffset,radMultiplier,radOrder;phiOffset,phiMultiplier,phiOrder;arcOffset,arcMultiplier".
    public String toString() {
        String paramString = new String();
        
        paramString = gear_file + ";" + arcOffset + ";" + arcMultiplier ;

        return paramString;
    }
    
    
    //Takes the ID of a particular arc, identified by its ring and arc,
    //and returns that ID in a string of the form: "aaabbb",
    //where 'aaa' is the ring number, and 'bbb' is the arc number.
    public String IDtoString(int coord1, int coord2) {
        return String.format("%03d%03d",coord1,coord2);
    }
    
    public short[] StringtoID(String ID) {
        String ringString = ID.substring(0,3);
        String arcString  = ID.substring(3,6);
        
        short ring = Short.parseShort(ringString);
        short arc  = Short.parseShort(arcString);
        
        short[] newID = new short[2];
        newID[0] = ring;
        newID[1] = arc;
        
        return newID;
    }


    /*These seven variables determine the distance of a specific ring or arc
     * from their origin through these equations:
     *    
     *    radius = radZero + radOne*R^1 + radTwo*R^2 + radThree*R^3
     *
     *    arcLength = arc * 2*PI / phiOffset
     *    
     */
    /*private float radZero;
    private float radOne;
    private float radTwo;
    private float radThree;
    private float radEdge;
    
    private int phiCount;
    private float arcOffset;
    private float arcMultiplier;
    */
    private int incrementation = 1; //scale this if tiles are ever thinner than 1mm 
    
    private String gear_file;
    
    private int LastRing;
    private float arcOffset;
    private float arcMultiplier;
    private float[] RingToRadTable;
    private short[] RadToRingTable;
    private short[] ArcCountTable;
}
