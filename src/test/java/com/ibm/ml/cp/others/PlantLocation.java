package com.ibm.ml.cp.others;

import java.io.IOException;

import com.ibm.ml.ilog.Credentials;

// ---------------------------------------------------------------*- Java -*-
// File: ./examples/src/java/PlantLocation.java
// --------------------------------------------------------------------------
// Licensed Materials - Property of IBM
//
// 5724-Y48 5724-Y49 5724-Y54 5724-Y55 5725-A06 5725-A29
// Copyright IBM Corporation 1990, 2019. All Rights Reserved.
//
// Note to U.S. Government Users Restricted Rights:
// Use, duplication or disclosure restricted by GSA ADP Schedule
// Contract with IBM Corp.
// --------------------------------------------------------------------------

/* ------------------------------------------------------------

Problem Description
-------------------

A ship-building company has a certain number of customers. Each customer is supplied
by exactly one plant. In turn, a plant can supply several customers. The problem is
to decide where to set up the plants in order to supply every customer while minimizing
the cost of building each plant and the transportation cost of supplying the customers.

For each possible plant location there is a fixed cost and a production capacity.
Both take into account the country and the geographical conditions.

For every customer, there is a demand and a transportation cost with respect to
each plant location.

While a first solution of this problem can be found easily by CP Optimizer, it can take
quite some time to improve it to a very good one. We illustrate the warm start capabilities
of CP Optimizer by giving a good starting point solution that CP Optimizer will try to improve.
This solution could be one from an expert or the result of another optimization engine
applied to the problem.

In the solution we only give a value to the variables that determine which plant delivers
a customer. This is sufficient to define a complete solution on all model variables.
CP Optimizer first extends the solution to all variables and then starts to improve it.

The model has been further enriched by the addition of KPIs (key performance
indicators).  These are named expressions which are of interest to help you get
an idea of the performance of the model.  Here, there are two indicators of
interest, both of which indicate in different ways how efficiently the
plant capacity is being used.  The first KPI is the `mean occupancy'' defined
as the total demand divided by the total capacity of the used plants.  The second
indicator is the minimum plant occupancy defined as the ratio of demand to capacity
of the plant where this ratio is the smallest.  The KPIs are displayed in the log whenever
an improved solution is found and at the end of the search.

------------------------------------------------------------ */
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.cp.helper.DataReader;
import com.typesafe.config.ConfigFactory;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloSolution;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;

public class PlantLocation {

	public static void main(String args[])  {
		try {
			IloCP cp = new WmlCP(
					Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
					Connector.Runtime.DO_12_10, Connector.TShirtSize.M, 1);

			DataReader data = new DataReader("cpo/plant_location.data");
			int nbCustomer = data.next();
			int nbLocation = data.next();

			int[][] cost = new int[nbCustomer][];
			for (int c = 0; c < nbCustomer; c++) {
				cost[c] = new int[nbLocation];
				for (int w = 0; w < nbLocation; w++) {
					cost[c][w] = data.next();
				}
			}
			int[] demand = new int[nbCustomer];
			int totalDemand = 0;
			for (int c = 0; c < nbCustomer; c++) {
				demand[c] = data.next();
				totalDemand += demand[c];
			}
			int[] fixedCost = new int[nbLocation];
			for (int w = 0; w < nbLocation; w++)
				fixedCost[w] = data.next();
			int[] capacity = new int[nbLocation];
			for (int w = 0; w < nbLocation; w++)
				capacity[w] = data.next();

			IloIntVar cust[] = new IloIntVar[nbCustomer];
			for (int c = 0; c < nbCustomer; c++)
				cust[c] = cp.intVar(0, nbLocation - 1);

			IloIntVar[] open = new IloIntVar[nbLocation];
			IloIntVar[] load = new IloIntVar[nbLocation];
			for (int w = 0; w < nbLocation; w++) {
				open[w] = cp.intVar(0, 1);
				load[w] = cp.intVar(0, capacity[w]);
				cp.add(cp.eq(open[w], cp.gt(load[w], 0)));
			}
			cp.add(cp.pack(load, cust, demand));

			IloNumExpr obj = cp.scalProd(fixedCost, open);
			for (int c = 0; c < nbCustomer; c++) {
				obj = cp.sum(obj, cp.element(cost[c], cust[c]));
			}
			cp.add(cp.minimize(obj));

			cp.addKPI(cp.quot(totalDemand, cp.scalProd(open, capacity)), "Mean occupancy");
			IloNumExpr[] usage = new IloNumExpr[nbLocation];
			for (int w = 0; w < nbLocation; w++)
				usage[w] = cp.sum(cp.quot(load[w], capacity[w]), cp.diff(1, open[w]));
			cp.addKPI(cp.min(usage), "Min occupancy");

			int[] custValues = {19, 0, 11, 8, 29, 9, 29, 28, 17, 15, 7, 9, 18, 15, 1, 17, 25, 18, 17, 27, 22, 1, 26, 3, 22,
					2, 20, 27, 2, 16, 1, 16, 12, 28, 19, 2, 20, 14, 13, 27, 3, 9, 18, 0, 13, 19, 27, 14, 12, 1, 15, 14, 17,
					0, 7, 12, 11, 0, 25, 16, 22, 13, 16, 8, 18, 27, 19, 23, 26, 13, 11, 11, 19, 22, 28, 26, 23, 3, 18, 23,
					26, 14, 29, 18, 9, 7, 12, 27, 8, 20};

			IloSolution sol = cp.solution();
			for (int c = 0; c < nbCustomer; c++) {
				sol.setValue(cust[c], custValues[c]);
			}

			cp.setStartingPoint(sol);
			cp.setParameter(IloCP.DoubleParam.TimeLimit, 10);
			cp.setParameter(IloCP.IntParam.LogPeriod, 10000);
			cp.solve();
		}
		catch (IloException | IOException e){
			System.out.println("Error: "+e.getMessage());
		}
	}
}
