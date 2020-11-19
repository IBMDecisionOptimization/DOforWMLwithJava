package com.ibm.ml.cp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* --------------------------------------------------------------------------

Problem Description
-------------------

This model illustrates the use of the CP Optimizer conflict refiner
on an infeasible scheduling problem. 
The problem is an instance of RCPSP (Resource-Constrained Project Scheduling 
Problem) with time windows. Given:

- a set of q resources with given capacities,
- a network of precedence constraints between the activities,
- a set of activities to be executed within a given time window and
- for each activity and each resource the amount of the resource
  required by the activity over its execution,

the goal of the problem is to find a schedule satisfying all the
constraints whose makespan (i.e., the time at which all activities are
finished) is minimal.

The instance is infeasible. The example illustrates 5 scenarios using the 
conflict refiner:

- Scenario 1: Identify a minimal conflict (no matter which one).
- Scenario 2: Identify a minimal conflict preferably using resource capacity 
              constraints.
- Scenario 3: Identify a minimal conflict preferably using precedence 
              constraints.
- Scenario 4: Find a minimal conflict partition that is, a set of disjoint 
              minimal conflicts S1,...,Sn such that when all constraints in 
              S1 U S2 U... U Sn are removed from the model, the model becomes 
              feasible.
- Scenario 5: Identify all minimal conflicts of the problem.

----------------------------------------------------------------------------- */

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.cp.helper.DataReader;
import com.typesafe.config.ConfigFactory;

import ilog.concert.IloConstraint;
import ilog.concert.IloCumulFunctionExpr;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntervalVar;
import ilog.concert.IloObjective;
import ilog.concert.cppimpl.IloAlgorithm.Status;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;

public class SchedConflict {

	public static IloCP cp;
	public static IloConstraint[] capacityCts;
	public static IloConstraint[] precedenceCts;

	public SchedConflict(String fileName) throws IloException, IOException {
		cp = new WmlCP(Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
		        Connector.Runtime.DO_12_10, Connector.TShirtSize.M, 1);
		this.buildModel(fileName);
	}

	static IloIntExpr[] arrayFromExprList(List<IloIntExpr> list) {
		return (IloIntExpr[]) list.toArray(new IloIntExpr[list.size()]);
	}

	static IloConstraint[] arrayFromConstraintList(List<IloConstraint> list) {
		return (IloConstraint[]) list.toArray(new IloConstraint[list.size()]);
	}

	// ----- RCPSP Model creation ------------------------------------------------

	private void buildModel(String fileName) throws IOException {
		int nbTasks, nbResources;
		DataReader data = new DataReader(fileName);
		try {
			nbTasks = data.next();
			nbResources = data.next();
			List<IloIntExpr> ends = new ArrayList<IloIntExpr>();
			List<IloConstraint> capacityCtList = new ArrayList<IloConstraint>();
			List<IloConstraint> precedenceCtList = new ArrayList<IloConstraint>();
			IloCumulFunctionExpr[] resources = new IloCumulFunctionExpr[nbResources];
			int[] capacities = new int[nbResources];
			for (int j = 0; j < nbResources; j++) {
				capacities[j] = data.next();
				resources[j] = cp.cumulFunctionExpr();
			}
			IloIntervalVar[] tasks = new IloIntervalVar[nbTasks];
			for (int i = 0; i < nbTasks; i++) {
				tasks[i] = cp.intervalVar();
				tasks[i].setName("ACT" + i);
			}
			for (int i = 0; i < nbTasks; i++) {
				IloIntervalVar task = tasks[i];
				int d, smin, emax, nbSucc;
				d = data.next();
				smin = data.next();
				emax = data.next();
				task.setSizeMin(d);
				task.setSizeMax(d);
				task.setStartMin(smin);
				task.setEndMax(emax);
				ends.add(cp.endOf(task));
				for (int j = 0; j < nbResources; j++) {
					int q = data.next();
					if (q > 0) {
						resources[j] = cp.sum(resources[j], cp.pulse(task, q));
					}
				}
				nbSucc = data.next();
				for (int s = 0; s < nbSucc; s++) {
					int succ = data.next();
					IloConstraint pct = cp.endBeforeStart(task, tasks[succ]);
					cp.add(pct);
					precedenceCtList.add(pct);
				}
			}
			for (int j = 0; j < nbResources; j++) {
				IloConstraint cct = cp.le(resources[j], capacities[j]);
				cp.add(cct);
				capacityCtList.add(cct);
			}
			precedenceCts = arrayFromConstraintList(precedenceCtList);
			capacityCts = arrayFromConstraintList(capacityCtList);
			IloObjective objective = cp.minimize(cp.max(arrayFromExprList(ends)));
			cp.add(objective);
		} catch (IloException e) {
			System.err.println("Error: " + e);
		}
	}

	// ----- Basic run of conflict refiner --------------------------------------

	private static void runBasicConflictRefiner() throws IloException {
		if (cp.refineConflict()) {
			cp.writeConflict();
		}
	}

	public static void main(String[] args) {
		String fileName = "cpo/sched_conflict.data";
		int failLimit = 10000;
		if (args.length > 1)
			fileName = args[1];
		if (args.length > 2)
			failLimit = Integer.parseInt(args[2]);
		try {
			new SchedConflict(fileName);
			int nbCapacityCts = capacityCts.length;
			int nbPrecedenceCts = precedenceCts.length;
			IloConstraint[] allCts = new IloConstraint[nbCapacityCts + nbPrecedenceCts];
			for (int i = 0; i < nbCapacityCts; ++i) {
				allCts[i] = capacityCts[i];
			}
			for (int i = 0; i < nbPrecedenceCts; ++i) {
				allCts[nbCapacityCts + i] = precedenceCts[i];
			}
			cp.setParameter(IloCP.IntParam.FailLimit, failLimit);
			cp.setParameter(IloCP.IntParam.CumulFunctionInferenceLevel, IloCP.ParameterValues.Extended);
			cp.setParameter(IloCP.IntParam.ConflictRefinerOnVariables, IloCP.ParameterValues.On);
			System.out.println("Instance \t: " + fileName);
			if (cp.solve()) {
				// A solution was found
				System.out.println("Solution found with makespan : " + cp.getObjValue());
			} else {
				/* Calls to getInfo() are NOT SUPPORTED */
				
				Status status = cp.getStatus();
				if (status == Status.Unknown) {
					// No solution found but problem was not proved infeasible
					System.out.println("No solution found but problem was not proved infeasible.");
					
				} else {
					// Run conflict refiner only if problem was proved infeasible
					System.out.println("Infeasible problem, running conflict refiner ...");
					System.out.println("SCENARIO 1: Basic conflict refiner:");
					runBasicConflictRefiner();
				}
			}
		} catch (IloException | IOException e) {
			System.err.println("Error: " + e);
		}
	}
}
