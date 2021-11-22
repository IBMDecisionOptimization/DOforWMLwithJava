package com.ibm.ml.mp;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.WmlCplex;

public class Warehouse {
   static class SemiContGoal extends IloCplex.Goal {
      IloNumVar[] _scVars;
      double[]    _scLbs;

      SemiContGoal(IloNumVar[] scVars,
                   double[]    scLbs) {
         _scVars = scVars;
         _scLbs  = scLbs;
      }

      public IloCplex.Goal execute(IloCplex cplex) throws IloException {
         int besti = -1;
         double maxObjCoef = Double.MIN_VALUE;

         // From among all variables that do not respect their minimum
         // usage levels, select the one with maximum objective coefficient.
         for (int i = 0; i < _scVars.length; i++) {
            double val = getValue(_scVars[i]);
            if ( val >= 1e-5            &&
                 val <= _scLbs[i] - 1e-5  ) {
               if (getObjCoef(_scVars[i]) >= maxObjCoef) {
                  besti = i;
                  maxObjCoef = getObjCoef(_scVars[i]);
               }
            }
         }

         //  If any are found, branch to enforce the condition that
         //  the variable must either be 0.0 or greater than
         //  the minimum usage level.
         if ( besti != -1 ) {
            return cplex.and(cplex.or(cplex.leGoal(_scVars[besti], 0.0),
                                      cplex.geGoal(_scVars[besti],
                                                   _scLbs[besti])),
                             this );
         }
         else if ( !isIntegerFeasible() ) {
            return cplex.and(cplex.branchAsCplex(), this );
         }

         return null;
      }
   }

   public static void main (String args[]) {
      try (
              IloCplex cplex = new WmlCplex(
                      Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
                      Connector.Runtime.DO_12_10,
                      Connector.TShirtSize.M,
                      1)
      ) {
         int nbWhouses = 4;
         int nbLoads = 31;
       
         IloNumVar[] capVars = 
            cplex.numVarArray(nbWhouses, 0, 10, 
                              IloNumVarType.Int); // Used capacities
         double[]    capLbs  = {2.0, 3.0, 5.0, 7.0}; // Minimum usage level
         double[]    costs   = {1.0, 2.0, 4.0, 6.0}; // Cost per warehouse
       
         // These variables represent the assigninment of a
         // load to a warehouse.
         IloNumVar[][] assignVars = new IloNumVar[nbWhouses][];
         for (int w = 0; w < nbWhouses; w++) {
            assignVars[w] = cplex.numVarArray(nbLoads, 0, 1,
                                              IloNumVarType.Int);
            
            // Links the number of loads assigned to a warehouse with 
            // the capacity variable of the warehouse.
            cplex.addEq(cplex.sum(assignVars[w]), capVars[w]);
         }
       
         // Each load must be assigned to just one warehouse.
         for (int l = 0; l < nbLoads; l++) {
            IloNumVar[] aux = new IloNumVar[nbWhouses];
            for (int w = 0; w < nbWhouses; w++)
               aux[w] = assignVars[w][l];
          
            cplex.addEq(cplex.sum(aux), 1);
         }
       
         cplex.addMinimize(cplex.scalProd(costs, capVars));
       
         cplex.setParam(IloCplex.Param.MIP.Strategy.Search,
                        IloCplex.MIPSearch.Traditional);

         if ( cplex.solve(new SemiContGoal(capVars, capLbs)) ) {
            System.out.println("Solution status: " + cplex.getStatus());
            System.out.println("--------------------------------------------");
            System.out.println();
            System.out.println("Solution found:");
            System.out.println(" Objective value = " + cplex.getObjValue());
            System.out.println();
            for (int w = 0; w < nbWhouses; w++) {
               System.out.println("Warehouse " + w + ": stored " 
                                  + cplex.getValue(capVars[w]) + " loads");
               for (int l = 0; l < nbLoads; l++) {
                  if ( cplex.getValue(assignVars[w][l]) > 1e-5 )
                     System.out.print("Load " + l + " | ");
               }
               System.out.println(); System.out.println();
            }
            System.out.println("--------------------------------------------");
         }
         else {
            System.out.println(" No solution found ");
         }
      }
      catch (IloException e) {
         System.err.println("Concert exception caught: " + e);
         System.exit(-1);
      }    
   }
}

