/*
 * TileParameters.java
 *
 * Created on Apr 05 2014, 2:43 PM
 * 
 * @author Christopher Milke
 * @version 1.0
 *
 * The core class to the radial tiling geometry.
 * Originally designed only as a wrapper class for the numerous
 * parameters used to adjust the tile geometry, a few additional
 * functions allowed for the entire geometry of the detector
 * to be localized to this one class. As such, any arbitrary
 * tile geometry, within the confines of a radial tiling scheme,
 * should be producable simply by creating a new implementation 
 * of this interface (and possibly modification of this interface).
 * The remainder of the files in the program should thus not have to
 * be changed in any way. 
 * 
 * Notes on current geometry:
 * The current geometry is based around a series of concentric "rings"
 * centered around the middle of the detector. Each ring is then divided
 * into subsections dubbed "arcs". The rings and arcs can vary in whatever 
 * way the implementing class defines. 
 * 
 */
package org.lcsim.contrib.scipp.beamcal;


public interface TileParameters
{
    //Takes a particular ring and arc, identifying a specific tile,
    //and returns the r/phi point of the arc at the corner of the 
    //outer-most (larger radius) edge and the clockwise-most edge. 
    public float[] getCornerPolar(int ring, int arc);
    
    //Does the same as the above, but returns the corner point
    //in x,y coordinates. 
    public float[] getCorner(int ring, int arc);
    
    //Takes a specific ring, and returns the number of arcs in that ring
    public int getArcsInRing(int ring);
    
    //Takes a specific ring and a phi coordinate, and returns the arc number
    //associated with that phi point in that arc.
    public short getArc(int ring, double phi);
    
    //Takes a point in r,phi coordinates, and returns the ID number
    //of the arc that encloses the point. The ID is of the form
    // {ring number, arc number}.
    public short[] getIDpolar(double radius, double phi);
    
    //Same as above, but takes a point in the form of x,y coordinates.
    public short[] getID(double x, double y);
    
    //returns the radial edge of the detector
    public float getEdge();
    
    //returns the edge of the detector in the form of the outermost ring
    public short getLastRing();
    
    //returns whether or not the given radius is within the boundaries of the detector
    public boolean contains(double radius);
    
    //returns whether or not the given ring is within the boundaries of the detector
    public boolean contains(int ring);
    
    //For debugging purposes, returns all the parameters in the form of
    //a string, in whatever way the implementation determines.
    public String toString();
    
    //Takes the ID of a particular arc, identified by its arc number and ring number,
    //and returns that ID in a string of a form specific to the implementation.
    public String IDtoString(int ring, int arc);
    
    //Inverts the above function.
    public short[] StringtoID(String ID);

}
