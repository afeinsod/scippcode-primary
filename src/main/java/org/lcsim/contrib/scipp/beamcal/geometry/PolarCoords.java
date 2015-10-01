/**
 * PolarCoords.java.
 *
 * Created on May 05 2014, 04:50 AM
 *
 * @author Christopher Milke
 * @version 1.0 
 * 
 * This is a simple class designed to define polar coordinates 
 * for the use of the tile geometry. The coordinate "r" is (of course)
 * defined as 0 at the origin, and increases away from the origin as a
 * function of x and y ( r^2 = x^2 + y^2   as is standard). The coordinate
 * "phi" is defined as the counter-clockwise angle (in radians) with the 
 * x-axis as phi=0 (and 2pi,4pi,etc). 
 * 
 */
package org.lcsim.contrib.scipp.beamcal.geometry;

import java.lang.Math;

public class PolarCoords {
    //converts x,y cartesian coordinates to r,phi polar coordinates
    public static double[] CtoP(double x, double y) {
        double[] polars = new double[2];

        double radius = Math.hypot(x,y);
        
        double phi = 0;

        if (y==0 && x>=0) phi = 0;
        else if (y==0 && x<0) phi = Math.PI;
        
        else if (x==0 && y>0) phi = Math.PI/2;
        else if (x==0 && y<0) phi = 3*Math.PI/2;

        else if (x>0 && y>0) phi = Math.atan(y/x);
        else if (x>0 && y<0) phi = Math.atan(y/x) + 2*Math.PI;
        else if (x<0 && y>0) phi = Math.atan(y/x) + Math.PI;
        else if (x<0 && y<0) phi = Math.atan(y/x) + Math.PI;
    
        else {
            System.out.println("Something has gone horribly wrong at PolarCoords.java:cartesian_polar");
            System.out.println("x = " + x + ", y = " + y);
            System.exit(1);
        }    
        
        
        polars[0] = radius;
        polars[1] = phi;

        return polars;
    }
    
    
    //converts r,phi polar coordinates to x,y cartesian coordinates
    public static double[] PtoC(double radius, double phi) {
        double[] cartesians = new double[2];

        double x = radius * Math.cos(phi);
        double y = radius * Math.sin(phi);

        cartesians[0] = x;
        cartesians[1] = y;

        return cartesians;
    }


    public static double[] ZtoBeamOut(double x, double y, double z) {
        double[] rotated_coords = new double[3];

        rotated_coords[0] = x - Math.abs(z*BeamOutAngle);
        rotated_coords[1] = y;
        rotated_coords[2] = z;

        return rotated_coords;
    }

    final static double BeamOutAngle = 0.007;

}
