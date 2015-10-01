/**
 * ArcTileParameters.java.
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

import java.lang.String;
import java.lang.Short;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;

public class ArcTileParameters implements TileParameters{
    
    //CONSTRUCTOR
    public ArcTileParameters(float rOff, float rMult, float rOrd, 
                             float pOff, float pMult, float pOrd, 
                             float aOff, float aMult) {
    radOffset     = rOff;   
    radMultiplier = rMult;
    radOrder      = rOrd;

    phiOffset     = pOff;
    phiMultiplier = pMult;
    phiOrder      = pOrd;
    
    arcOffset     = aOff;
    arcMultiplier = aMult;
    }
    
    
    //Takes a particular ring and arc, identifying a specific tile,
    //and returns the r/phi point of the arc at the corner of the 
    //outer-most (larger radius) edge and the clockwise-most edge.
    public float[] getCornerPolar(int ring, int arc) {
        float[] corner = new float[2];
        
        double radius;
        if (ring < 0) radius = 0;
        else radius = this.radMultiplier * Math.pow(ring,this.radOrder) + this.radOffset;


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
    //The approximations performed in this function are made because this
    //implementation is based on defining the arc-length of the arcs. However,
    //all arcs on a ring must be the same size. So this function makes the 
    //arc-length as close as possible to what has been specified by the user,
    //while still ensuring all arcs have the same size.
    public int getArcsInRing(int ring) {
        if (ring < 0) ring = 0;
        int N;//number of arcs in the specified ring
        
        //tAL is the temporary Arc Length of the ring, and circ is the ring's circumference
        double tAL = this.phiMultiplier * Math.pow(ring,this.phiOrder) + this.phiOffset;
        double radius = this.radMultiplier * Math.pow(ring,this.radOrder) + this.radOffset;
        double circ = 2 * Math.PI * radius;
        
        if ( (circ%tAL) == 0 ) N = (int) (circ / tAL);
        else {
            double ratio = circ / tAL;
            int high_ratio = (int) (ratio + 1);//closest number of fitting arcs above ratio
            int low_ratio  = (int) ratio;//closest number of fitting arcs below ratio
            
            //determine which ratio is closest
            double high_AL = circ / high_ratio;//arc length from larger number of arcs (the smaller AL)
            double low_AL  = circ / low_ratio;//arc length from smaller number of arcs (the bigger AL)
            
            double high_dif = tAL - high_AL;
            double low_dif  = low_AL - tAL;
            
            //assign the number of arcs to the closest ratio
            N = ( low_dif < high_dif ) ? low_ratio : high_ratio;
        }
        
        return N;
    }
    
    
    //Takes a specific ring and a phi coordinate, and returns the arc number
    //associated with that phi coordinate in that ring.
    public short getArc(int ring, double phi) {
        if (phi < 0)         phi += 2*Math.PI;
        if (phi > 2*Math.PI) phi -= 2*Math.PI;
        
        
        double reduced_phi = (phi - arcOffset - arcMultiplier*ring);        
        if (reduced_phi < 0)         reduced_phi += 2*Math.PI;
        if (reduced_phi > 2*Math.PI) reduced_phi -= 2*Math.PI;
        
        int N = (int) getArcsInRing(ring);
        double theta = (2*Math.PI)/N;
        double arcFraction = reduced_phi / theta;
        int arc = (int) arcFraction;
        
        return (short)arc;
    }
    
    
    //Takes a point in r,phi coordinates, and returns the ID number
    //of the arcs that encloses the point. The ID is of the form
    // {ring number, arc number}.
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


    //Same as above, but takes a point in the form of x,y coordinates.
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
    
    
    //For debugging purposes, returns all the parameters in the form of
    //a string: radOffset,radMultiplier,radOrder;phiOffset,phiMultiplier,phiOrder;arcOffset,arcMultiplier".
    public String toString() {
        String paramString = new String();
        
        paramString = this.radOffset+","+this.radMultiplier+","+this.radOrder+";"+this.phiOffset+","+this.phiMultiplier+","+this.phiOrder+";"+this.arcOffset+","+this.arcMultiplier;

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


    /*These six variables determine the distance of a specific ring or arc
     * from their origin through these equations:
     *    
     *    radius = radMultiplier*(ring^radOrder) + radOffset
     *
     *    arcLength ~= arc * 2*PI / [phiMultiplier * (ring^phiOrder)+phiOffset]
     *               ^       
     *         this  |  is approximate because you can't have 
     *         exactly the same arc length on rings of varying radius. 
     *         But, getArcsInRing tries to make the arc length as 
     *         consistent as possible for each ring.
     */
    private float radOffset;
    private float radMultiplier;
    private float radOrder;

    private float phiOffset;
    private float phiMultiplier;
    private float phiOrder;
    
    private float arcOffset;
    private float arcMultiplier;
    
    
    private float geometryEdge = 200;
}
