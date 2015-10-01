/*
 * ArcReconstructionAnalysis.java
 *
 * Created on Mar 27, 2014, 10:05 PM
 * @author Christopher Milke
 *
 */
package org.lcsim.contrib.scipp.drivers;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTiler;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTiler;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterPallet;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterPallet;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterScanner;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterScanner;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterCylinder;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterCylinder;

import org.lcsim.contrib.scipp.beamcal.database.TileDataBaseReader;
import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.CylinderDataBaseReader;
import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterCylinder;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;

import org.lcsim.contrib.scipp.beamcal.util.Jroot;
import org.lcsim.contrib.scipp.beamcal.util.LCIOFileManager;
import org.lcsim.contrib.scipp.beamcal.util.ScippUtils;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;

import java.lang.String;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Math;

public class ReconstructionAnalysis extends Driver {

    //DEFINE XML FUNCTIONS
    //These functions are specially fomatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT

    //public void setVariablename(variable type) { //the first letter after "set" must be uppercase
    //                                              //but can (must?) be lowercase in xml file
    //    set variable here; 
    //}
    *******************************************************************************************/

    public void setInput1(String s) {
        this.lcioFileMNGR.setFilelist(s);
    }

    public void setOutputfile(String s) {
        this.jrootFile = s;
    }
    
    
    public void setDbtilefile(String s) { 
        this.DBtileFile = s;
    }
    
    
    public void setDbcylfile(String s) {
        this.DBcylFile = s;
    }
    
    
    public void setPalletsize(int n) {
        prad = n;
    }
    
    
    public void setParams(String parameter_string) { 
        params = ScippUtils.generateParams(parameter_string);
    }
    
    
    public void setCellsize(double size) { 
        cellSize = size;
    }
    
    
    public void setSpreadfactor(int spread) { 
        spreadFactor = spread;
    }
    
    
    public void setNegativeremoval(boolean rmn) { 
        removeNegative = rmn;
    }

    public void setBeamOutAligned(boolean algn) {
        aligned = algn;
    }

    public void setSigma(double s) { 
        sigma = s;
    }
    //END DEFINE XML FUNCTIONS

    //This function is called when the program is first started
    //and initializes all persistant data
    public void startOfData() {
        tiler = new BaseBeamCalorimeterTiler(params,cellSize,spreadFactor,removeNegative,aligned);
        scanner = new BaseBeamCalorimeterScanner(params,1,prad);
        runcount = 0;
        averageEnergy = 0;
        totalEnergy = 0;
        eventCount = 0;
        tileCount = 0;
        outOfBounds = 0;
        
        try {
            root = new Jroot(jrootFile,"NEW");
                
            root.init("TH2D","EandP","EandP", 
                "Reconstruction Accuracy",180,0,180,400,-2,2);
            
            root.init("TH1D","PosD","PosD", 
                "Reconstruction XY Position Accuracy",40,0,40);
                
            root.init("TH1D","PosX","PosX", 
                "Reconstruction X Position Accuracy",80,-40,40);
                
            root.init("TH1D","PosY","PosY", 
                "Reconstruction Y Position Accuracy",80,-40,40);
                
            root.init("TH2D","PosD_R","PosD_R", 
                "Reconstruction XY Position Accuracy as a Function of Radius",
                180,0,180,40,0,40);
                
            root.init("TH2D","PosX_R","PosX_R", 
                "Reconstruction X Position Accuracy as a Function of Radius",
                180,0,180,80,-40,40);
                
            root.init("TH2D","PosY_R","PosY_R", 
                "Reconstruction Y Position Accuracy as a Function of Radius",
                180,0,180,80,-40,40);
                
            root.init("TH1D","Ener","Ener", 
                "Reconstruction Energy Accuracy",16,-2,2);
                
            root.init("TH2D","Ener_R","Ener_R", 
                "Reconstruction Energy Accuracy as a Function of Radius",
                180,0,180,400,-2,2);
                
            root.init("TH1D","Res","Res", 
                "Reconstruction Energy Resolution",200,-5,5);
                
            root.init("TProfile","radeff","radeff",
                "GeV", 10, 0, 100, 0.0, 1.2);
            
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

    //This function is called after all file runs have finished,
    // and closes any necessary data
    public void endOfData(){
        try {
            root.end();
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
        super.process( event );
        EventHeader signalEvent = ScippUtils.getProperEvent(lcioFileMNGR);
        
        MCParticle electron = ScippUtils.getElectron(signalEvent);
        
        double[] electron_position = electron.getEndPoint().v();
        double   electron_energy   = electron.getEnergy();
        double   electron_radius   = Math.hypot(electron_position[0],electron_position[1]);
        int layer = 9;

        //process signal and background files to extract the locations of hits 
        // and remove those hits that we don't care about
        List<SimCalorimeterHit> signalhits = signalEvent.get(SimCalorimeterHit.class, "BeamCalHits");
        List<SimCalorimeterHit> bghits     = event.get(SimCalorimeterHit.class, "BeamCalHits");
        
        try {
            //apply hits to the geometry of the detector. background hits are 
            // processed specially so they are overlayed on signal hits
            this.tiler.clear();
            this.tiler.process(signalhits);
            this.tiler.process(bghits);
            
            TileDataBaseReader dbreader1 = new TileDataBaseReader(DBtileFile);
            
            //look over all possible pallets and order them by energy
            this.scanner.scanDB( this.tiler.getDBTiles_onLayer(layer,dbreader1) );
            List<BeamCalorimeterPallet> seedPallets = new ArrayList();
            seedPallets.addAll( this.scanner.getTopPalletsNoOverlap(50) );
            dbreader1.close();
            
            
            //extend pallets backwards to form cylinders
            List< HashMap<String, BeamCalorimeterTile> > tileList = tiler.getTiles_betweenLayers(10,40);
            BeamCalorimeterCylinder[] cylinders = new BeamCalorimeterCylinder[seedPallets.size()];
            
            for (int l = 0; l < cylinders.length; l++) {
                cylinders[l] = new BaseBeamCalorimeterCylinder( seedPallets.get(l) );
                cylinders[l].addTileList(tileList);
            }
            
            
            
            //subtract background average from all cylinders and sort them
            CylinderDataBaseReader dbreader2 = new CylinderDataBaseReader(DBcylFile);
            dbreader2.close();
            
            System.out.println("Sorting Cylinders");
            DataBaseBeamCalorimeterCylinder dbc = null;
            List<DataBaseBeamCalorimeterCylinder> dbcyls = new ArrayList();
            System.out.println("filling cylinders with database");
            for (int l = 0; l < cylinders.length; l++) {
                dbreader2.open(DBcylFile);
                dbc = new DataBaseBeamCalorimeterCylinder(cylinders[l]);
                dbc.setDBInfo(dbreader2);
                if ( dbc.hasDBInfo() ) {
                    dbcyls.add(dbc);
                }
                if (dbcyls.size() >= 50) break;
                dbreader2.close();
                dbc = null;
            }
            Collections.sort( dbcyls, Collections.reverseOrder() );
            
            
            //performs a multitude of tests
            boolean testPassed = false;
            int eff = 0;
            for (DataBaseBeamCalorimeterCylinder sig_cyl : dbcyls) {
                if ( sig_cyl.getSubtractedEnergy() > sigma*sig_cyl.getSTDev() ) {
                    double[] sig_position = this.calculate_position(sig_cyl,dbreader1);
                    double   sig_energy   = this.getReconstructedEnergy(sig_cyl,dbreader1);
                    
                    double x_dif = sig_position[0] - electron_position[0];
                    double y_dif = sig_position[1] - electron_position[1];
                    
                    double pos_dif = Math.hypot(x_dif,y_dif);
                    double energy_dif = sig_energy - electron_energy;
                    
                    
                    double energy_fract = energy_dif / electron_energy;

                    System.out.println("electron X = "+electron_position[0]);
                    System.out.println("electron Y = "+electron_position[1]);
                    System.out.println("pos_dif = " + pos_dif);
                    System.out.println("energy = " + sig_energy);
                    System.out.println("energy_fract = " + energy_fract);
                    
                    root.fill ("EandP",pos_dif,energy_fract);
                    root.fill  ("PosD",pos_dif);
                    root.fill  ("PosX",x_dif);
                    root.fill  ("PosY",y_dif);
                    root.fill("PosD_R",electron_radius,pos_dif);
                    root.fill("PosX_R",electron_radius,x_dif);
                    root.fill("PosY_R",electron_radius,y_dif);
                    root.fill  ("Ener",energy_fract);
                    root.fill("Ener_R",electron_radius,energy_fract);
                    
                    
                    totalEnergy += sig_energy;
                    energyList.add( new Double(sig_energy) );
                    eventCount++;
                    testPassed = true;
                    eff = 1;
                    break;
                }
            }
            root.fill("radeff",electron_radius, eff);
            
            if (!testPassed) System.out.println("                    EVENT FAILED TEST");
            
            System.out.println("      RUNCOUNT AT " + (++runcount) + " and outOfBounds at " + outOfBounds );
            
            
        }catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }catch (java.sql.SQLException e) {
            System.out.println(e);
            System.exit(1);
        }

    }//End Process
    
    
    
    
    //calculates the center of a cylinder. Currently not built in to any
    //classes because this may be a temporary (and faulty) way of reconstructing
    //the electron's position.
    private double[] calculate_position(DataBaseBeamCalorimeterCylinder cyl, 
    TileDataBaseReader dbreader1) throws java.sql.SQLException {
        
        double[] Position = new double[2];
        double TotalEnergy = 0;
        int temp_layer = 15;//layer of highest signal energy
        Position[0] = 0;
        Position[1] = 0;
        
        dbreader1.open(DBtileFile);
        Map<String, DataBaseBeamCalorimeterTile> tileMap;
        tileMap = tiler.getDBTiles_onLayer(temp_layer,dbreader1);
        dbreader1.close();
        
        for ( String ID : cyl.getIDs() ) {
            if ( tileMap.containsKey(ID) ) {
                double energy = tileMap.get(ID).getSubtractedEnergy();
                
                short[] tileID = params.StringtoID(ID);
                double[] tileCenter = calculate_center(tileID[0],tileID[1]);
                
                Position[0] += tileCenter[0]*energy;
                Position[1] += tileCenter[1]*energy;
                TotalEnergy += energy;
                
                System.out.println("X Pos = "+tileCenter[0]);
                System.out.println("Y Pos = "+tileCenter[1]);
                ///System.out.println("tile energy = "+energy);
            }
        }
        ///System.out.println("tiles energy = "+TotalEnergy);
        Position[0] = Position[0] / TotalEnergy;
        Position[1] = Position[1] / TotalEnergy;
        
        dbreader1.close();
        return Position;
    }
    
    
    //determines the center of a tile
    private double[] calculate_center(int ring, int arc) {
        double[] center;
        
        double outer_edge = params.getCornerPolar(ring,arc)[0];
        double inner_edge = params.getCornerPolar(ring-1,arc)[0];
        double radial_center = (outer_edge + inner_edge) / 2;
        
        double clockwise_edge = params.getCornerPolar(ring,arc)[1];
        double counter_edge   = params.getCornerPolar(ring,arc+1)[1];
        double inv_counter    = 2*Math.PI - counter_edge;
        
        double phi_diff1 = Math.abs(clockwise_edge - counter_edge);
        double phi_diff2 = Math.abs(clockwise_edge - inv_counter);
        
        
        double phi_center;
        if (phi_diff1 < phi_diff2)
            phi_center = (clockwise_edge + counter_edge) / 2;
        else
            phi_center = (clockwise_edge + inv_counter) / 2;
        

        System.out.println("                                          inner pos = "+inner_edge);
        System.out.println("                                          outer pos = "+outer_edge);
        System.out.println("                                          clock pos = "+clockwise_edge);
        System.out.println("                                          count pos = "+counter_edge);
        
        System.out.println("                 Rad center = "+radial_center);
        System.out.println("                 Phi Pos = "+phi_center);
        
        center = PolarCoords.PtoC(radial_center,phi_center);
        return center;
    }
    
    
    
    private double getReconstructedEnergy(DataBaseBeamCalorimeterCylinder sig_cyl, TileDataBaseReader dbreader1) throws java.sql.SQLException{
        short[] ref = sig_cyl.getRef();
        
        BaseBeamCalorimeterPallet pal;
        pal = new BaseBeamCalorimeterPallet(params,ref[0],ref[1],0,prad+1);
        
        BeamCalorimeterCylinder expanded_cyl;
        expanded_cyl = new BaseBeamCalorimeterCylinder(pal);
        
        List< HashMap<String, BeamCalorimeterTile> > mapList;
        mapList = tiler.getTiles_betweenLayers(0,50);
        
        dbreader1.open(DBtileFile);
        expanded_cyl.addBGTileList(mapList,dbreader1);
        dbreader1.close();
        
        double energy = expanded_cyl.getEnergy();
        
        return energy;
    }
    
    

    /*here all the classwide variables are declared*/

    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;
    
    //miscellenous variables
    private String DBtileFile;
    private String DBcylFile;
    private TileParameters params;
    private BaseBeamCalorimeterTiler tiler;
    private BaseBeamCalorimeterScanner scanner;
    private int prad;
    private double cellSize;
    private int spreadFactor;
    private boolean removeNegative;
    private boolean aligned;
    private double sigma;
    private double tileDensity[] = new double[6];
    private int    tileCount;
    
    private int runcount;
    private double averageEnergy;
    private double totalEnergy;
    private int eventCount;
    private int outOfBounds;
    List<Double> energyList = new ArrayList<Double>();
}
