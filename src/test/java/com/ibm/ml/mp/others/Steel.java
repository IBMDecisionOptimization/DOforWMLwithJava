package com.ibm.ml.mp.others;
import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.mp.helper.InputDataReader;
import com.typesafe.config.ConfigFactory;
import ilog.concert.*;
import ilog.cplex.*;

public class Steel {
   static int _nProd;
   static int _nTime;
   
   static double[] _avail;
   static double[] _rate;
   static double[] _inv0;
   static double[] _prodCost;
   static double[] _invCost;
   
   static double[][] _revenue;
   static double[][] _market;

   static void readData(String fileName)
                         throws java.io.IOException,
                                InputDataReader.InputDataReaderException {
      InputDataReader reader = new InputDataReader(fileName);
      
      _avail    = reader.readDoubleArray();
      _rate     = reader.readDoubleArray();
      _inv0     = reader.readDoubleArray();
      _prodCost = reader.readDoubleArray();
      _invCost  = reader.readDoubleArray();
      _revenue  = reader.readDoubleArrayArray();
      _market   = reader.readDoubleArrayArray();
      
      _nProd = _rate.length;
      _nTime = _avail.length;
   }
   
   public static void main(String[] args) {

      String filename = "steel.dat";
      if ( args.length > 0 )
         filename = args[0];

      try {
         readData(filename);
      }
      catch (java.io.IOException|
             InputDataReader.InputDataReaderException ex) {
         System.out.println("Data Error: " + ex);
         System.exit(-1);
      }

      try (IloCplex cplex = new WmlCplex(
              Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
              Connector.Runtime.DO_20_1,
              Connector.TShirtSize.M,
              1)) {

         // VARIABLES
         IloNumVar[][] Make = new IloNumVar[_nProd][];
         for (int p = 0; p < _nProd; p++) {
            Make[p] = cplex.numVarArray(_nTime, 0.0, Double.MAX_VALUE);
         }
       
         IloNumVar[][] Inv = new IloNumVar[_nProd][];
         for (int p = 0; p < _nProd; p++) {
            Inv[p] = cplex.numVarArray(_nTime, 0.0, Double.MAX_VALUE);
         }
       
         IloNumVar[][] Sell = new IloNumVar[_nProd][_nTime];
         for (int p = 0; p < _nProd; p++) {
            for (int t = 0; t < _nTime; t++) {
               Sell[p][t] = cplex.numVar(0.0, _market[p][t]);
            }
         }
       
         // OBJECTIVE
         IloLinearNumExpr TotalRevenue  = cplex.linearNumExpr();
         IloLinearNumExpr TotalProdCost = cplex.linearNumExpr();
         IloLinearNumExpr TotalInvCost  = cplex.linearNumExpr();
         
         for (int p = 0; p < _nProd; p++) {
            for (int t = 1; t < _nTime; t++) {
               TotalRevenue.addTerm (_revenue[p][t], Sell[p][t]);
               TotalProdCost.addTerm(_prodCost[p], Make[p][t]);
               TotalInvCost.addTerm (_invCost[p], Inv[p][t]);
            }
         }
           
         cplex.addMaximize(cplex.diff(TotalRevenue, 
                                      cplex.sum(TotalProdCost, TotalInvCost)));
       
         // TIME AVAILABILITY CONSTRAINTS
       
         for (int t = 0; t < _nTime; t++) {
            IloLinearNumExpr availExpr = cplex.linearNumExpr();
            for (int p = 0; p < _nProd; p++) {
               availExpr.addTerm(1./_rate[p], Make[p][t]);
            }
            cplex.addLe(availExpr, _avail[t]);
         }
       
         // MATERIAL BALANCE CONSTRAINTS
       
         for (int p = 0; p < _nProd; p++) {
            cplex.addEq(cplex.sum(Make[p][0], _inv0[p]), 
                        cplex.sum(Sell[p][0], Inv[p][0]));
            for (int t = 1; t < _nTime; t++) {
               cplex.addEq(cplex.sum(Make[p][t], Inv[p][t-1]), 
                           cplex.sum(Sell[p][t], Inv[p][t]));
            }
         }
       
         //cplex.exportModel("steel.lp");
       
         if ( cplex.solve() ) {
            System.out.println("Solution status: " + cplex.getStatus());
            System.out.println();
            System.out.println("Total Profit = " + cplex.getObjValue());
          
            System.out.println();
            System.out.println("\tp\tt\tMake\tInv\tSell");
          
            for (int p = 0; p < _nProd; p++) {
               for (int t = 0; t < _nTime; t++) {
                  System.out.println("\t" + p +
                                     "\t" + t +
                                     "\t" + cplex.getValue(Make[p][t]) +
                                     "\t" + cplex.getValue(Inv[p][t]) +
                                     "\t" + cplex.getValue(Sell[p][t]));
               }
            }
         }
      }
      catch (IloException exc) {
         System.err.println("Concert exception '" + exc + "' caught");
         System.exit(-1);
      }
   }
}

/*
Total Profit = 515033.00000000006

        p       t       Make    Inv     Sell
        0       0       0.0     10.0    0.0
        0       1       5990.0  0.0     6000.0
        0       2       6000.0  0.0     6000.0
        0       3       1400.0  0.0     1400.0
        0       4       2000.0  0.0     2000.0
        1       0       0.0     0.0     0.0
        1       1       1407.0  1100.0  307.0
        1       2       1400.0  0.0     2500.0
        1       3       3500.0  0.0     3500.0
        1       4       4200.0  0.0     4200.0
*/
