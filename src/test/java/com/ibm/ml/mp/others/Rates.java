package com.ibm.ml.mp.others;


import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.mp.helper.InputDataReader;
import com.typesafe.config.ConfigFactory;
import ilog.concert.*;
import ilog.cplex.*;
import java.io.*;

public class Rates {
   static int _generators;
   
   static double[] _minArray;
   static double[] _maxArray;
   static double[] _cost;
   static double   _demand;
   
   static void readData(String fileName)
           throws
           InputDataReader.InputDataReaderException, IOException {
      InputDataReader reader = new InputDataReader(fileName);
    
      _minArray = reader.readDoubleArray();
      _maxArray = reader.readDoubleArray();
      _cost     = reader.readDoubleArray();
      _demand   = reader.readDouble();
    
      _generators = _minArray.length;
   }
   
   public static void main( String[] args ) {

      String filename = "rates.dat";
      if (args.length > 0)
         filename = args[0];

      try {
         readData(filename);
      }
      catch (IOException|
             InputDataReader.InputDataReaderException ex) {
         System.out.println("Data Error: " + ex);
         System.exit(-1);
      }

      try (IloCplex cplex = new WmlCplex(
              Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
              Connector.Runtime.DO_12_10,
              Connector.TShirtSize.M,
              1)) {
         IloNumVar[] production = new IloNumVar[_generators];
         for (int j = 0; j < _generators; ++j) {
            production[j] = cplex.semiContVar(_minArray[j], _maxArray[j],
                                              IloNumVarType.Float);
         }
       
         cplex.addMinimize(cplex.scalProd(_cost, production));
         cplex.addGe(cplex.sum(production), _demand);
       
         if ( cplex.solve() ) {
            System.out.println("Solution status: " + cplex.getStatus());
            for (int j = 0; j < _generators; ++j) {
               System.out.println("   generator " + j + ": " +
                                  cplex.getValue(production[j]));
            }
            System.out.println("Total cost = " + cplex.getObjValue());
         }
         else
            System.out.println("No solution");
       
      }
      catch (IloException exc) {
         System.err.println("Concert exception '" + exc + "' caught");
         System.exit(-1);
      }
   }
}

/*
   generator 0: 15.6
   generator 1: 0
   generator 2: 0
   generator 3: 27.8
   generator 4: 27.8
   generator 5: 28.8
   generator 6: 29
   generator 7: 29
   generator 8: 29
Total cost = 1625.24
*/
