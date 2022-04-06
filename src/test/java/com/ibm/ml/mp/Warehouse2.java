package com.ibm.ml.mp;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.concert.*;
import ilog.cplex.WmlCplex;
import ilog.cplex.IloCplex;

public class Warehouse2 {
   public static double solve(IloCplex cplex, int nbWhouses, IloNumVar[] capVars, int nbLoads, IloNumVar[][] assignVars) throws IloException {
      if ( cplex.solve() ) {
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
         return cplex.getObjValue();
      }
      else {
         System.out.println(" No solution found ");
      }
      return Double.MAX_VALUE;
   }
   public static void main (String args[]) {
      try (
              IloCplex cplex = new WmlCplex(
                      Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
                      Connector.Runtime.DO_20_1,
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

         IloObjective obj = cplex.addMinimize(cplex.scalProd(costs, capVars));
       
         cplex.setParam(IloCplex.Param.MIP.Strategy.Search,
                        IloCplex.MIPSearch.Traditional);

         double obj1 = solve(cplex, nbWhouses, capVars, nbWhouses, assignVars);
         double[]    new_costs   = {2.0, 3.0, 4.0, 6.0};
         for (int i =0; i< new_costs.length; i++){
            cplex.setLinearCoef(obj, capVars[i], new_costs[i]);
         }
         double obj2 = solve(cplex, nbWhouses, capVars, nbWhouses, assignVars);
         double obj3 = cplex.getValue(obj.getExpr());
         System.out.println("Obj1 was "+obj1 + " Obj2 is now "+ obj2);
         System.out.println("Value of objective expression " + obj.getExpr().toString() +" is "+obj3);
      }
      catch (IloException e) {
         System.err.println("Concert exception caught: " + e);
         System.exit(-1);
      }    
   }
}

