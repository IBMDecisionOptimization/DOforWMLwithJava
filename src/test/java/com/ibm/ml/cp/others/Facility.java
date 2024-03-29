package com.ibm.ml.cp.others;

import java.io.IOException;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.cp.helper.DataReader;
import com.typesafe.config.ConfigFactory;

import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;

// ---------------------------------------------------------------*- Java -*-
// File: ./examples/src/java/Facility.java
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

 A company has 10 stores.  Each store must be supplied by one warehouse. The 
 company has five possible locations where it has property and can build a 
 supplier warehouse: Bonn, Bordeaux, London, Paris, and Rome. The warehouse 
 locations have different capacities. A warehouse built in Bordeaux or Rome 
 could supply only one store. A warehouse built in London could supply two 
 stores; a warehouse built in Bonn could supply three stores; and a warehouse 
 built in Paris could supply four stores. 

 The supply costs vary for each store, depending on which warehouse is the 
 supplier. For example, a store that is located in Paris would have low supply 
 costs if it were supplied by a warehouse also in Paris.  That same store would 
 have much higher supply costs if it were supplied by the other warehouses.

 The cost of building a warehouse varies depending on warehouse location.

 The problem is to find the most cost-effective solution to this problem, while
 making sure that each store is supplied by a warehouse.

 ------------------------------------------------------------ */
import ilog.cp.WmlCP;
import ilog.cp.IloCP;

public class Facility {

	public static void main(String[] args)  {
		String filename;
		if (args.length > 0)
			filename = args[0];
		else
			filename = "cpo/facility.data";
		try {
			IloCP cp = new WmlCP(
			        Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
			        Connector.Runtime.DO_20_1, Connector.TShirtSize.M, 1);
			int i, j;

			DataReader data = new DataReader(filename);

			int nbLocations = data.next();
			int nbStores = data.next();
			int[] capacity = new int[nbLocations];
			int[] fixedCost = new int[nbLocations];
			int[][] cost = new int[nbStores][];

			for (i = 0; i < nbStores; i++)
				cost[i] = new int[nbLocations];

			for (j = 0; j < nbLocations; j++)
				capacity[j] = data.next();

			for (j = 0; j < nbLocations; j++)
				fixedCost[j] = data.next();

			for (i = 0; i < nbStores; i++)
				for (j = 0; j < nbLocations; j++)
					cost[i][j] = data.next();

			IloIntVar[] supplier = cp.intVarArray(nbStores, 0, nbLocations - 1);
			IloIntVar[] open = cp.intVarArray(nbLocations, 0, 1);

			for (i = 0; i < nbStores; i++)
				cp.add(cp.eq(cp.element(open, supplier[i]), 1));

			for (j = 0; j < nbLocations; j++)
				cp.add(cp.le(cp.count(supplier, j), capacity[j]));

			IloIntExpr obj = cp.scalProd(open, fixedCost);
			for (i = 0; i < nbStores; i++)
				obj = cp.sum(obj, cp.element(cost[i], supplier[i]));

			cp.add(cp.minimize(obj));

			/**
			 * getValue(...) is not supported for IloIntExpr A workaround consists in
			 * defining a new variable in the model that is constrained to equal the
			 * expression.
			 */
			IloIntVar objExprValue = cp.intVar(IloCP.IntMin, IloCP.IntMax, "objExprValue");
			cp.add(cp.eq(objExprValue, obj));

			cp.solve();

			System.out.println();
			System.out.println("OBJECTIVE value: " + (int) cp.getObjValue());
			// System.out.println("Optimal value: " + (int) cp.getValue(obj)); // This
			// statement is not supported by the CPO WML connector
			System.out.println("Optimal value: " + (int) cp.getValue(objExprValue));
			for (j = 0; j < nbLocations; j++) {
				if (cp.getValue(open[j]) == 1) {
					System.out.print("Facility " + j + " is open, it serves stores ");
					for (i = 0; i < nbStores; i++) {
						if (cp.getValue(supplier[i]) == j)
							System.out.print(i + " ");
					}
					System.out.println();
				}
			}
		} catch (IloException | IOException e) {
			System.err.println("Error " + e);
		}
	}
}
