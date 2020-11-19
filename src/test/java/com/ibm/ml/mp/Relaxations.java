package com.ibm.ml.mp;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.WmlCplex;
import ilog.cplex.IloCplex;


public class Relaxations {
   public static void main( String[] args ) {
      try (IloCplex cplex = new WmlCplex(
              Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
              Connector.Runtime.DO_12_10,
              Connector.TShirtSize.M,
              1)) {

         IloNumVar x = cplex.numVar(0,10);
         IloNumVar y = cplex.numVar(0, 10);

         IloRange c1 = (IloRange)cplex.eq(x, y);
         IloRange c2 = cplex.le(x,4);
         IloRange c3 = cplex.ge(y, 6);

         cplex.add(c1);
         cplex.add(c2);
         cplex.add(c3);

         if (!cplex.solve()) {
            System.out.println("Status is "+cplex.getStatus());
            if (cplex.getStatus().equals(IloCplex.Status.Infeasible)) {
               System.out.println("No Solution");
               if (cplex.feasOpt( new IloConstraint[]{c1, c2, c3},
                       new double[]{ 1.0, 1.0,1.0 }
                       )){
                  System.out.println("Feasopt worked.");
                  System.out.println(cplex.getValue(x) + " " + cplex.getValue(y));
               }
               else throw new IloException("Feasopt failed...");
            }
            else throw new IloException("Should not solve !!!");
         }
         else{
            throw new IloException("Should not solve");
         }
      }
      catch (IloException exc) {
         exc.printStackTrace();
      }
   }
}
