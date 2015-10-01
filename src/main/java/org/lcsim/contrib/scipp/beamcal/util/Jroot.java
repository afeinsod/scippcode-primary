/**
 * Jroot.java.
 *
 * Created on March 12, 2015, 4:24 PM
 *
 * @author Christopher Milke
 * @version 1.0 
 * 
 */
package org.lcsim.contrib.scipp.beamcal.util;

import java.lang.String;
import java.lang.StringBuilder;
import java.io.FileWriter;
import java.io.PrintWriter;


public class Jroot {
    
    public Jroot(String path, String mode) throws java.io.IOException {
        String title;
        if ( path.contains("/") ) {
            String[] temp = path.trim().split("/");
            title = temp[temp.length-1];
        }
        else {
            title = path;
        }
        
        String init = "";
        init += "void " + title + "() {\n";
        init += "    TFile f( \""+path+".root\" , \""+mode+"\" );\n";
        
        
        //initialize output file
        output = new FileWriter(path+".cxx",false); //the "true" sets the "append" flag
        output.write(init);
        
    }
    
    
    
    //process commands and dump string to file if necessary
    public void proc(String process) throws java.io.IOException {
        String temp = "    " + process + ";\n";
        output.write(temp);
    }
    


    //process commands and dump string to file if necessary
    public void init(String ptype, String name, String id, String title, 
            double param1, double param2, double param3) 
            throws java.io.IOException {
        
        String temp = ptype+" *"+name+" = new "+ptype+"(\""+id+"\",\""+title+"\",";
        temp += param1+","+param2+","+param3+")";
        this.proc(temp);
    }



    public void init(String ptype, String name, String id, String title, 
            double param1, double param2, double param3, 
            double param4) 
            throws java.io.IOException {
        
        String temp = ptype+" *"+name+" = new "+ptype+"(\""+id+"\",\""+title+"\",";
        temp += param1+","+param2+","+param3+","+param4+")";
        this.proc(temp);
    }
    


    public void init(String ptype, String name, String id, String title, 
            double param1, double param2, double param3, 
            double param4, double param5) 
            throws java.io.IOException {
        
        String temp = ptype+" *"+name+" = new "+ptype+"(\""+id+"\",\""+title+"\",";
        temp += param1+","+param2+","+param3+","+param4+","+param5+")";
        this.proc(temp);
    }



    public void init(String ptype, String name, String id, String title, 
            double param1, double param2, double param3, 
            double param4, double param5, double param6) 
            throws java.io.IOException {
        
        String temp = ptype+" *"+name+" = new "+ptype+"(\""+id+"\",\""+title+"\",";
        temp += param1+","+param2+","+param3+","+param4+","+param5+","+param6+")";
        this.proc(temp);
    }



    public void init(String ptype, String name, String id, String title, 
            double param1, double param2, double param3, 
            double param4, double param5, double param6,
            double param7) 
            throws java.io.IOException {
        
        String temp = ptype+" *"+name+" = new "+ptype+"(\""+id+"\",\""+title+"\",";
        temp += param1+","+param2+","+param3+","+param4+","+param5+","+param6+","+param7+")";
        this.proc(temp);
    }



    public void init(String ptype, String name, String id, String title, 
            double param1, double param2, double param3, 
            double param4, double param5, double param6,
            double param7, double param8) 
            throws java.io.IOException {
        
        String temp = ptype+" *"+name+" = new "+ptype+"(\""+id+"\",\""+title+"\",";
        temp += param1+","+param2+","+param3+","+param4+","+param5+","+param6+","+param7+","+param8+")";
        this.proc(temp);
    }



    public void init(String ptype, String name, String id, String title, 
            double param1, double param2, double param3, 
            double param4, double param5, double param6,
            double param7, double param8, double param9) 
            throws java.io.IOException {
        
        String temp = ptype+" *"+name+" = new "+ptype+"(\""+id+"\",\""+title+"\",";
        temp += param1+","+param2+","+param3+","+param4+","+param5+","+param6+","+param7+","+param8+","+param9+")";
        this.proc(temp);
    }



    public void fill(String name, double param1) 
            throws java.io.IOException {
        
        String temp = name+"->Fill(";
        temp += param1+")";
        this.proc(temp);
    }

   
    
    public void fill(String name, double param1, double param2) 
            throws java.io.IOException {
        
        String temp = name+"->Fill(";
        temp += param1+","+param2+")";
        this.proc(temp);
    }



    public void fill(String name, double param1, double param2, double param3) 
            throws java.io.IOException {
        
        String temp = name+"->Fill(";
        temp += param1+","+param2+","+param3+")";
        this.proc(temp);
    }



    public void fill(String name, double param1, double param2, double param3, double param4) 
            throws java.io.IOException {
        
        String temp = name+"->Fill(";
        temp += param1+","+param2+","+param3+","+param4+")";
        this.proc(temp);
    }



    public void end() throws java.io.IOException {
        String temp = "    f.Write();\n}";
        output.write(temp);
        output.close();
    }
 
 
    
    private FileWriter output;
}
