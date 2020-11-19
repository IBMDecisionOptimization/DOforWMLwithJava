package com.ibm.ml.mp.others;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.concert.*;
import ilog.cplex.*;

public class InOut1 {
   static int _nbProds = 3;
   static int _nbResources = 2;
   static double[][] _consumption = {{0.5, 0.4, 0.3},
                                     {0.2, 0.4, 0.6}};
   static double[] _demand = {100.0, 200.0, 300.0};
   static double[] _capacity = {20.0, 40.0};
   static double[] _insideCost = {0.6, 0.8, 0.3};
   static double[] _outsideCost = {0.8, .09, 0.4};
   
   static void displayResults(IloCplex cplex,
                              IloNumVar[] inside,
                              IloNumVar[] outside) throws IloException {
      System.out.println("cost: " + cplex.getObjValue());
      
      for(int p = 0; p < _nbProds; p++) {
         System.out.println("P" + p);
         System.out.println("inside:  " + cplex.getValue(inside[p]));
         System.out.println("outside: " + cplex.getValue(outside[p]));
      }
   }
   
   public static void main( String[] args ) {
      try (IloCplex cplex = new WmlCplex(
              Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
              Connector.Runtime.DO_12_10,
              Connector.TShirtSize.M,
              1)) {
         IloNumVar[]  inside = new IloNumVar[_nbProds];
         IloNumVar[] outside = new IloNumVar[_nbProds];
       
         IloObjective obj = cplex.addMinimize();
       
         // Must meet demand for each product
       
         for(int p = 0; p < _nbProds; p++) {
            IloRange demRange = cplex.addRange(_demand[p], _demand[p]);
            inside[p] = cplex.numVar(cplex.column(obj, _insideCost[p]).and(
                                     cplex.column(demRange, 1.)),
                                     0., Double.MAX_VALUE);
            
            outside[p] = cplex.numVar(cplex.column(obj, _outsideCost[p]).and(
                                      cplex.column(demRange, 1.)),
                                      0., Double.MAX_VALUE);
         }
       
         // Must respect capacity constraint for each resource
       
         for(int r = 0; r < _nbResources; r++)
            cplex.addLe(cplex.scalProd(_consumption[r], inside), _capacity[r]);
       
         cplex.solve();
       
         if ( !cplex.getStatus().equals(IloCplex.Status.Optimal) ) {
            System.out.println("No optimal solution found");
            return;
         }
         System.out.println("Solution status: " + cplex.getStatus());       
         displayResults(cplex, inside, outside);
         System.out.println("----------------------------------------");
      }
      catch (IloException exc) {
         System.err.println("Concert exception '" + exc + "' caught");
         System.exit(-1);
      }
   }
}
  
/*
cost: 372
P0
inside:  40
outside: 60
P1
inside:  0
outside: 200
P2
inside:  0
outside: 300
*/
