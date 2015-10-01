/**
 * ScippUtils.java.
 *
 * Created on Aug 28 2014, 01:50 AM
 *
 * @author Christopher Milke
 * @version 1.0 
 * 
 */
package org.lcsim.contrib.scipp.beamcal.util;
import org.lcsim.contrib.scipp.beamcal.util.LCIOFileManager;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;
import org.lcsim.contrib.scipp.beamcal.geometry.PhiTileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.ArcTileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.CubicTileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.SimpleListTileParameters;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

public class ScippUtils {
    
    //Get electron signal event and ensure it actually hits the detector
    public static EventHeader getProperEvent(LCIOFileManager mngr) {
        EventHeader signalEvent = mngr.nextEvent();

        if (signalEvent == null)
            throw new java.lang.RuntimeException("There are no signal events left. Process ending");

        MCParticle electron = getElectron(signalEvent);

        if (electron == null) {
            throw new NextEventException();
        }

        while ( !hitBeamCal(electron) ) {
            signalEvent = mngr.nextEvent();
            if (signalEvent == null)
            throw new java.lang.RuntimeException("There are no signal events left. Process ending");

            electron = getElectron(signalEvent);
            if (electron == null) {
                throw new NextEventException();
            }
        }
        
        return signalEvent;
    }



    public static EventHeader getEvent(LCIOFileManager mngr) {
        EventHeader signalEvent = mngr.nextEvent();

        if (signalEvent == null) throw new java.lang.RuntimeException("There are no signal events left. Process ending");
        MCParticle electron = getElectron(signalEvent);
        if (electron == null) throw new NextEventException();

        return signalEvent;
    }
    
    
    
    //extracts electron data from signal file
    public static MCParticle getElectron(EventHeader event) {
        MCParticle mcp = null;
        for (MCParticle p : event.getMCParticles()) {
            if (Math.abs(p.getPDGID()) == 11 && p.getGeneratorStatus() == MCParticle.FINAL_STATE) {
                mcp = p;
                break;
            }
        }
        return mcp;
    }
    
    
    
    //ensures the electron hits the detector
    private static boolean hitBeamCal(MCParticle electron) {
        double[] vec = electron.getEndPoint().v();
        double   rorg = Math.hypot(vec[0], vec[1]);

        //below values are VERY approximate and provide a wide range that should usually work
        double beamCalFront = 2000;
        double beamCalRear = 4000;

        //Reject electrons that do not decay at the front of the beamcal
        return (beamCalFront < vec[2] && vec[2] < beamCalRear);
    }

    public static TileParameters generateParams(String parameter_string) {
        TileParameters params = null;

        String[] parameters = parameter_string.split(",");
        String paramType = parameters[0];
        
        if ( paramType.equals("Arc") ) {
            int pn = 9;
            float[] p = new float[pn];
            for (int i=1;i<pn;i++) p[i]=Float.parseFloat(parameters[i]);
            params = new ArcTileParameters(p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8]);

        } else if ( paramType.equals("Cubic") ) {
            int pn = 9;
            float[] p = new float[pn];
            for (int i=1;i<pn;i++) p[i]=Float.parseFloat(parameters[i]);
            params = new CubicTileParameters(p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8]);

        } else if ( paramType.equals("Phi") ) {
            int pn = 7;
            float[] p = new float[pn];
            for (int i=1;i<pn;i++) p[i]=Float.parseFloat(parameters[i]);
            params = new PhiTileParameters(p[1],p[2],p[3],p[4],p[5],p[6]);

        } else if ( paramType.equals("SimpleList") ) {
            params = new SimpleListTileParameters(parameters[1]);

        } else {
            System.out.println("no matching parameter type to: \"" + paramType + "\"");
            System.exit(1);
        }

        return params;
        
        
    } 
    
}
