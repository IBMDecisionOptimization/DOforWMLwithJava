package com.ibm.ml.cp;

/* ------------------------------------------------------------

Problem Description
-------------------

The problem involves choosing colors for the countries on a map in 
such a way that at most four colors (blue, white, yellow, green) are 
used and no neighboring countries are the same color. In this exercise, 
you will find a solution for a map coloring problem with six countries: 
Belgium, Denmark, France, Germany, Luxembourg, and the Netherlands. 

------------------------------------------------------------ */

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.cp.*;
import ilog.concert.*;
import ilog.cp.WmlCP;

public class Color {
    public static String[] Names = {"blue", "white", "yellow", "green"}; 
    public static void main(String[] args) {
        try {
            //IloCP cp = new IloCP();
            IloCP cp = new WmlCP(
                    Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
                    Connector.Runtime.DO_20_1,
                    Connector.TShirtSize.M,
                    1);
            IloIntVar Belgium = cp.intVar(0, 3);
            IloIntVar Denmark = cp.intVar(0, 3);
            IloIntVar France = cp.intVar(0, 3);
            IloIntVar Germany = cp.intVar(0, 3);
            IloIntVar Luxembourg = cp.intVar(0, 3);
            IloIntVar Netherlands = cp.intVar(0, 3);
            
            cp.add(cp.neq(Belgium , France)); 
            cp.add(cp.neq(Belgium , Germany)); 
            cp.add(cp.neq(Belgium , Netherlands));
            cp.add(cp.neq(Belgium , Luxembourg));
            cp.add(cp.neq(Denmark , Germany)); 
            cp.add(cp.neq(France , Germany)); 
            cp.add(cp.neq(France , Luxembourg)); 
            cp.add(cp.neq(Germany , Luxembourg));
            cp.add(cp.neq(Germany , Netherlands)); 
            
            if (cp.solve())
                {    
                   System.out.println();
                   System.out.println( "Belgium:     " + Names[(int)cp.getValue(Belgium)]);
                   System.out.println( "Denmark:     " + Names[(int)cp.getValue(Denmark)]);
                   System.out.println( "France:      " + Names[(int)cp.getValue(France)]);
                   System.out.println( "Germany:     " + Names[(int)cp.getValue(Germany)]);
                   System.out.println( "Luxembourg:  " + Names[(int)cp.getValue(Luxembourg)]);
                   System.out.println( "Netherlands: " + Names[(int)cp.getValue(Netherlands)]);
                }
        } catch (IloException e) {
            System.err.println("Error " + e);
        }
    }
}

