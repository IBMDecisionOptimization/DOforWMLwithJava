package com.ibm.ml.cp.others;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.cp.helper.DataReader;
import com.typesafe.config.ConfigFactory;

// File: ./examples/src/java/Talent.java
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

This example is inspired from the talent hold cost scheduling problem
described in:

T.C.E Cheng, J. Diamond, B.M.T. Lin.  Optimal scheduling in film
production to minimize talent holding cost.  Journal of Optimization
Theory and Applications, 79:197-206, 1993.

of which the 'Rehearsal problem' is a specific case:

Barbara M. Smith.  Constraint Programming In Practice: Scheduling
                   a Rehearsal.  Report APES-67-2003, September 2003.

See: http://www.csplib.org/Problems/prob039/

------------------------------------------------------------ */
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntSet;
import ilog.concert.IloIntVar;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import ilog.cplex.IloCplexModeler;

public class Talent extends IloCP {

	public static IloCP cp;

	private int numActors, numScenes;
	private int[] actorPay, sceneDuration;
	private IloIntSet[] actorInScene;
	private IloIntExpr idleCost;
	private IloIntVar scene[];

	private IloIntVar idleCost_val;

	public Talent(String fileName) throws IloException, IOException {

		cp = new WmlCP(Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
		        Connector.Runtime.DO_20_1, Connector.TShirtSize.M, 1);
		this.readData(fileName);
		this.buildModel();
	}

	private void readData(String fileName) throws IOException, IloException {

		DataReader data = new DataReader(fileName);

		numActors = data.next();
		actorPay = new int[numActors];
		for (int a = 0; a < numActors; a++)
			actorPay[a] = data.next();

		numScenes = data.next();
		sceneDuration = new int[numScenes];
		for (int s = 0; s < numScenes; s++)
			sceneDuration[s] = data.next();

		actorInScene = new IloIntSet[numActors];
		for (int a = 0; a < numActors; a++) {
			int[] inScene = new int[numScenes];
			int nbScene = 0;
			for (int s = 0; s < numScenes; s++) {
				inScene[s] = data.next();
				if (inScene[s] != 0)
					nbScene++;
			}
			int[] playScene = new int[nbScene];
			int n = 0;
			for (int s = 0; s < numScenes; s++) {
				if (inScene[s] != 0) {
					playScene[n] = s;
					n++;
				}
			}
			actorInScene[a] = cp.intSet(playScene);
		}
	}

	private void buildModel() throws IloException {

		// Create the decision variables, cost, and the model
		scene = new IloIntVar[numScenes];
		for (int s = 0; s < numScenes; s++)
			scene[s] = cp.intVar(0, numScenes - 1);

		// Expression representing the global cost
		idleCost = cp.intExpr();

		// Make the slot-based secondary model
		IloIntVar[] slot = new IloIntVar[numScenes];
		for (int s = 0; s < numScenes; s++)
			slot[s] = cp.intVar(0, numScenes - 1);
		cp.add(cp.inverse(scene, slot));

		// Loop over all actors, building cost
		for (int a = 0; a < numActors; a++) {
			// Expression for the waiting time for this actor
			IloIntExpr actorWait = cp.intExpr();

			// Calculate the first and last slots where this actor plays
			Vector<IloIntVar> position = new Vector<IloIntVar>();

			Iterator it = actorInScene[a].iterator();
			while (it.hasNext())
				position.add(slot[(Integer) it.next()]);

			IloIntExpr firstSlot = cp.min(position.toArray(new IloIntVar[position.size()]));
			IloIntExpr lastSlot = cp.max(position.toArray(new IloIntVar[position.size()]));

			// If an actor is not in a scene, he waits
			// if he is on set when the scene is filmed
			for (int s = 0; s < numScenes; s++) {
				if (!actorInScene[a].contains(s)) { // not in scene
					IloIntExpr wait = cp.and(cp.le(firstSlot, slot[s]), cp.le(slot[s], lastSlot));
					actorWait = cp.sum(actorWait, cp.prod(sceneDuration[s], wait));
				}
			}

			// Accumulate the cost of waiting time for this actor
			idleCost = cp.sum(idleCost, cp.prod(actorPay[a], actorWait));

			// Getting the value of an expression is not supported in DOforWML. The
			// work-around consists in defining a constraint binding the the IloIntExpr
			// to an IloIntVar as follows.
			idleCost_val = cp.intVar(IloCP.IntMin, IloCP.IntMax, "idleCost_val");
			cp.add(cp.eq(idleCost_val, idleCost));

		}
		cp.add(cp.minimize(idleCost));
	}

	private void display() {

		System.out.println("Solution of idle cost " + (int) cp.getValue(idleCost_val));
		System.out.print("Order:");
		for (int s = 0; s < numScenes; s++)
			System.out.print(" " + ((int) cp.getValue(scene[s]) + 1));
		System.out.println();

		// Give more detailed information on the schedule
		for (int a = 0; a < numActors; a++) {
			System.out.print("|");
			for (int s = 0; s < numScenes; s++) {
				int sc = (int) cp.getValue(scene[s]);
				for (int d = 0; d < sceneDuration[sc]; d++) {
					if (actorInScene[a].contains(sc))
						System.out.print("X");
					else
						System.out.print(".");
				}
				System.out.print("|");
			}
			System.out.println("  Rate = " + actorPay[a] + ")");
		}
	}

	public static void main(String args[])  {

		try {
			String inputFile = "cpo/rehearsal.data";
			if (args.length > 1)
				inputFile = args[1];

			Talent talent = new Talent(inputFile);
			cp.solve();

			talent.display();
		}
		catch (IOException | IloException e){
			System.out.println("Error "+e.getMessage());
		}
	}
}
