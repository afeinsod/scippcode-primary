/**
 * PhiTileParameters.java.
 *
 * Created on Feb 08 2014, 02:20 PM
 *
 * @author Christopher Milke
 * @version 1.0 
 * 
 * This class handles all of the coordinate changing, tile parameters,
 * and tile labeling needed for the radial tiling schemeIt allows users
 * to set the radius of individual rings of the detector, and to control
 * the ANGLE RANGE (really by controlling the number of arcs per ring)
 * of the tiles on a per ring basis.
 * 
 */
package org.lcsim.contrib.scipp.beamcal.geometry;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;

import java.lang.String;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;

public class PhiTileParameters implements TileParameters{
    
    public PhiTileParameters(float rOff, float rMult, float rOrd, float pOff, float pMult, float pOrd) {
    radOffset     = rOff;   
    radMultiplier = rMult;
    radOrder      = rOrd;

    phiOffset     = pOff;
    phiMultiplier = pMult;
    phiOrder      = pOrd;
    }
    
    
    
    public float[] getCornerPolar(int ring, int arc) {
        float[] corner = new float[2];
        
        double radius;
        if (ring < 0) radius = 0;
        else radius = this.radMultiplier * Math.pow(ring,this.radOrder) + this.radOffset;

        int N = getArcsInRing(ring);
        double theta = (2*Math.PI)/N;
        double phi;
        
        int last_arc = N - 1;
        if      (arc < 0)        phi = (arc+N)*theta;
        else if (arc > last_arc) phi = (arc-N)*theta;
        else                     phi =   arc  *theta;

        corner[0] = (float) radius;
        corner[1] = (float) phi;
    
        return corner;
      
    }
    
    
    public float[] getCorner(int ring, int arc) {
        float[] xycorner = new float[2];

        float[] corner = getCornerPolar(ring, arc);
        double[] convert = PolarCoords.PtoC(corner[0],corner[1]);

        xycorner[0] = (float) convert[0];
        xycorner[1] = (float) convert[1];

        return xycorner;
    }
    
    
    public int getArcsInRing(int ring) {
        return (int) ( this.phiMultiplier * Math.pow(ring,this.phiOrder) + this.phiOffset );
    }
    
    
    public short getArc(int ring, double phi) {
        if (phi < 0)         phi += 2*Math.PI;
        if (phi > 2*Math.PI) phi -= 2*Math.PI;
        
        double N = getArcsInRing(ring);
        double theta = (2*Math.PI)/N;
        double arcFraction = phi / theta;

        short arc = (short) arcFraction;
        
        return arc;
    }
    
    
    public short[] getIDpolar(double radius, double phi) {
        short[] ID = new short[2];

        double deOffset     = radius - this.radOffset;
        double deMult       = deOffset / this.radMultiplier;
        double recipOrder   = 1 / this.radOrder;
        double ringFraction = Math.pow(deMult,recipOrder) + 1; //1 added to ensure proper ring when rounding down

        short ring = (short) ringFraction;
        short arc =  (short) getArc(ring,phi);
        
        ID[0] = ring;
        ID[1] = arc;

        return ID;
    }
    
    
    public short[] getID(double x, double y) {
        double[] convert = PolarCoords.CtoP(x,y);
        return getIDpolar(convert[0],convert[1]);
    }
    
    
    //returns the radial edge of the detector
    public float getEdge() {
        return geometryEdge;
    }
    
    
    //returns the edge of the detector in the form of the outermost ring
    public short getLastRing() {
        return getIDpolar(geometryEdge,0)[0];
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
    
    
    //for debugging purposes
    public String toString() {
        String paramString = new String();
        
        paramString = this.radOffset+","+this.radMultiplier+","+this.radOrder+";"+this.phiOffset+","+this.phiMultiplier+","+this.phiOrder;

        return paramString;
    }
    
    
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


    /*These six variables determine the distance of a specific ring or arc
     * from their origin through these equations:
     *    radius = radMultiplier * [ (ring)^radOrder ] + radOffset
     *
     *    phi = arc * 2*PI / {phiMultiplier * [ (ring)^phiOrder ] + phiOffset}
     *                                        this ^ is not a typo. Phi varies per ring.
     */
    private float radOffset;
    private float radMultiplier;
    private float radOrder;

    private float phiOffset;
    private float phiMultiplier;
    private float phiOrder;
    
    private float geometryEdge = 200;
}
