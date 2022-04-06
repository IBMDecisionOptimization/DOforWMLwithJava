package com.ibm.ml.mp.others;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;
import ilog.concert.*;
import ilog.cplex.*;


public class IndefQPex1 {
   public static void main(String[] args) {
      try (IloCplex cplex = new WmlCplex(
              Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
              Connector.Runtime.DO_20_1,
              Connector.TShirtSize.M,
              1)) {
         IloLPMatrix lp = populateByRow(cplex);

         int[]    ind = {0};
         double[] val = {1.0};

         // When a non-convex objective function is present, CPLEX
         // will raise an exception unless the parameter
         // IloCplex.Param.OptimalityTarget is set to accept
         // first-order optimal solutions
         cplex.setParam(IloCplex.Param.OptimalityTarget,
                        IloCplex.OptimalityTarget.FirstOrder);

         // CPLEX may converge to either local optimum 
         solveAndDisplay(cplex, lp);

         // Add a constraint that cuts off the solution at (-1, 1)
         lp.addRow(0.0, Double.MAX_VALUE, ind, val);
         solveAndDisplay(cplex, lp);

         // Remove the newly added constraint and add a new constraint
         // with the opposite sense to cut off the solution at (1, 1)
         lp.removeRow(lp.getNrows() - 1);
         lp.addRow(-Double.MAX_VALUE, 0.0, ind, val);
         solveAndDisplay(cplex, lp);

         cplex.end();
      }
      catch (IloException e) {
         System.err.println("Concert exception '" + e + "' caught");
         System.exit(-1);
      }
   }

   // To populate by row, we first create the variables, and then use them to
   // create the range constraints and objective.  The model we create is:
   //
   // Minimize
   //  obj:   - 0.5 (-3 * x^2 - 3 * y^2 - 1 * x * y)
   // Subject To
   //  c1: -x + y >= 0
   //  c2:  x + y >= 0
   // Bounds
   //  -1 <= x <= 1
   //   0 <= y <= 1
   // End

   static IloLPMatrix populateByRow(IloMPModeler model) throws IloException {
      IloLPMatrix lp = model.addLPMatrix();

      double[]    lb = {-1.0, 0.0};
      double[]    ub = { 1.0, 1.0};
      IloNumVar[] x  = model.numVarArray(model.columnArray(lp, 2), lb, ub);

      double[]   lhs = {0.0, 0.0};
      double[]   rhs = {Double.MAX_VALUE, Double.MAX_VALUE};
      double[][] val = {{-1.0, 1.0},
                        { 1.0, 1.0}};
      int[][]    ind = {{0, 1},
                        {0, 1}};
      lp.addRows(lhs, rhs, ind, val);

      IloNumExpr x00 = model.prod(-3.0, x[0], x[0]);
      IloNumExpr x11 = model.prod(-3.0, x[1], x[1]);
      IloNumExpr x01 = model.prod(-1.0, x[0], x[1]);
      IloNumExpr Q   = model.prod(0.5, model.sum(x00, x11, x01));

      model.add(model.minimize(Q));

      return (lp);
   }

   static void solveAndDisplay(IloCplex cplex, IloLPMatrix lp) throws IloException {
      
      if ( cplex.solve() ) {
         double[] x     = cplex.getValues(lp);
         double[] dj    = cplex.getReducedCosts(lp);
         double[] pi    = cplex.getDuals(lp);
         double[] slack = cplex.getSlacks(lp);

         System.out.println("Solution status = " + cplex.getStatus());
         System.out.println("Solution value  = " + cplex.getObjValue());

         int nvars = x.length;
         for (int j = 0; j < nvars; ++j) {
            System.out.println("Variable " + j +
                               ": Value = " + x[j] +
                               " Reduced cost = " + dj[j]);
         }

         int ncons = slack.length;
         for (int i = 0; i < ncons; ++i) {
            System.out.println("Constraint " + i +
                               ": Slack = " + slack[i] +
                               " Pi = " + pi[i]);
         }

      }
   }
}
