package com.ibm.ml.mp.others;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.mp.helper.InputDataReader;
import com.typesafe.config.ConfigFactory;
import ilog.concert.*;
import ilog.cplex.*;

public class Etsp {
   static private int Horizon = 10000;

   static private class Data {
      int        nJobs;
      int        nResources;
      int[][]    activityOnResource;
      double[][] duration;
      double[]   dueDate;
      double[]   earlinessCost;
      double[]   tardinessCost;

      Data(String filename) throws IloException, java.io.IOException,
                                   InputDataReader.InputDataReaderException
      {
         InputDataReader reader = new InputDataReader(filename);

         activityOnResource = reader.readIntArrayArray();
         duration           = reader.readDoubleArrayArray();
         dueDate            = reader.readDoubleArray();
         earlinessCost      = reader.readDoubleArray();
         tardinessCost      = reader.readDoubleArray();

         nJobs      = dueDate.length;
         nResources = activityOnResource.length;
      }
   }

   public static void main (String args[]) {
      IloCplex cplex = null;
      try {
         String filename;
         if ( args.length > 1)  filename = args[0];
         else                   filename = "etsp.dat";

         Data data = new Data(filename);
         cplex = new WmlCplex(
                 Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
                 Connector.Runtime.DO_20_1,
                 Connector.TShirtSize.M,
                 1);

         // Create start variables
         IloNumVar[][] s = new IloNumVar[data.nJobs][];
         for (int j = 0; j < data.nJobs; j++)
            s[j] = cplex.numVarArray(data.nResources, 0.0, Horizon);

         // State precedence constraints
         for (int j = 0; j < data.nJobs; j++) {
            for (int i = 1; i < data.nResources; i++)
               cplex.addGe(s[j][i], cplex.sum(s[j][i-1], data.duration[j][i-1]));
         }

         // State disjunctive constraints for each resource
         for (int i = 0; i < data.nResources; i++) {
            int end = data.nJobs - 1;
            for (int j = 0; j < end; j++) {
               int a = data.activityOnResource[i][j];
               for (int k = j + 1; k < data.nJobs; k++) {
                  int b = data.activityOnResource[i][k];
                  cplex.add(cplex.or(
                     cplex.ge(s[j][a], cplex.sum(s[k][b], data.duration[k][b])),
                     cplex.ge(s[k][b], cplex.sum(s[j][a], data.duration[j][a]))
                  ));
               }
            }
         }

         // The cost is the sum of earliness or tardiness costs of each job 
         int last = data.nResources - 1;
         IloNumExpr costSum = cplex.numExpr();
         for (int j = 0; j < data.nJobs; j++) {
            double[] points = { data.dueDate[j] };
            double[] slopes = { -data.earlinessCost[j],
                                data.tardinessCost[j] };
            costSum = cplex.sum(costSum,
               cplex.piecewiseLinear(
                  cplex.sum(s[j][last], data.duration[j][last]),
                  points, slopes, data.dueDate[j], 0)
            );
         }
         cplex.addMinimize(costSum);

         cplex.setParam(IloCplex.Param.Emphasis.MIP, 4);

         if ( cplex.solve() ) {
             System.out.println("Solution status: " + cplex.getStatus());
             System.out.println(" Optimal Value = " + cplex.getObjValue());
         }
      }
      catch (IloException e) {
         System.err.println("Concert exception caught: " + e);
      }    
      catch (InputDataReader.InputDataReaderException ex) {
         System.out.println("Data Error: " + ex);
      }
      catch (java.io.IOException ex) {
         System.out.println("IO Error: " + ex);
      }
      finally {
         if (cplex != null)
            cplex.end();
      }
   }
}

