package com.ibm.ml.mp.others;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.concert.*;
import ilog.cplex.*;

public class InOut3 {
   static int _nbProds = 3;
   static int _nbResources = 2;
   static double[][] _consumption = {{0.5, 0.4, 0.3},
                                     {0.2, 0.4, 0.6}};
   static double[] _demand      = {100.0, 200.0, 300.0};
   static double[] _capacity    = {20.0, 40.0};
   static double[] _insideCost  = {0.6, 0.8, 0.3};
   static double[] _outsideCost = {0.8, 0.9, 0.4};
   
   static void displayResults(IloCplex cplex,
                              IloNumVar costVar,
                              IloNumVar[] inside,
                              IloNumVar[] outside) throws IloException {
      System.out.println("cost: " + cplex.getValue(costVar));
      
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
         IloNumVar[]  inside = cplex.numVarArray(_nbProds, 10.0, Double.MAX_VALUE);
         IloNumVar[] outside = cplex.numVarArray(_nbProds, 0.0, Double.MAX_VALUE);
         IloNumVar   costVar = cplex.numVar(0., Double.MAX_VALUE);
       
         cplex.addEq(costVar, cplex.sum(cplex.scalProd(inside, _insideCost),
                                        cplex.scalProd(outside, _outsideCost)));
         
         IloObjective obj = cplex.addMinimize(costVar);
       
         // Must meet demand for each product
       
         for(int p = 0; p < _nbProds; p++)
            cplex.addEq(cplex.sum(inside[p], outside[p]), _demand[p]);
       
         // Must respect capacity constraint for each resource
       
         for(int r = 0; r < _nbResources; r++)
            cplex.addLe(cplex.scalProd(_consumption[r], inside), _capacity[r]);
       
         cplex.solve();
       
         if ( !cplex.getStatus().equals(IloCplex.Status.Optimal) ) {
            System.out.println("No optimal solution found");
            return;
         }
       
         // New constraint: cost must be no more than 10% over minimum
       
         double cost = cplex.getObjValue();
         costVar.setUB(1.1 * cost);
       
         // New objective: minimize outside production
       
         obj.setExpr(cplex.sum(outside));
       
         cplex.solve();
         System.out.println("Solution status: " + cplex.getStatus());
         displayResults(cplex, costVar, inside, outside);
         System.out.println("----------------------------------------");
      }
      catch (IloException exc) {
         System.err.println("Concert exception '" + exc + "' caught");
         System.exit(-1);
      }
   }
}

/*
cost: 373.333
P0
inside:  10
outside: 90
P1
inside:  10
outside: 190
P2
inside:  36.6667
outside: 263.333
----------------------------------------
*/
