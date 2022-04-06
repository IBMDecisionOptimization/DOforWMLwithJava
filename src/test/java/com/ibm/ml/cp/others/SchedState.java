package com.ibm.ml.cp.others;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;

/* ------------------------------------------------------------

Problem Description
-------------------

This is a problem of building five houses. The masonry, roofing,
painting, etc. must be scheduled. Some tasks must necessarily take
place before others and these requirements are expressed through
precedence constraints.

A pool of two workers is available for building the houses. For a
given house, some tasks (namely: plumbing, ceiling and painting)
require the house to be clean whereas other tasks (namely: masonry,
carpentry, roofing and windows) put the house in a dirty state. A
transition time of 1 is needed to clean the house so to change from
state 'dirty' to state 'clean'.

The objective is to minimize the makespan.

------------------------------------------------------------ */
import ilog.concert.IloCumulFunctionExpr;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntervalVar;
import ilog.concert.IloStateFunction;
import ilog.concert.IloTransitionDistance;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;

public class SchedState {
	final static int nbHouses = 5;
	final static int nbTasks = 10;
	final static int nbWorkers = 2;

	final static int masonry = 0;
	final static int carpentry = 1;
	final static int plumbing = 2;
	final static int ceiling = 3;
	final static int roofing = 4;
	final static int painting = 5;
	final static int windows = 6;
	final static int facade = 7;
	final static int garden = 8;
	final static int moving = 9;

	final static String[] taskNames = { "masonry  ", "carpentry", "plumbing ", "ceiling  ", "roofing  ", "painting ",
	        "windows  ", "facade   ", "garden   ", "moving   " };

	final static int[] taskDurations = { 35, 15, 40, 15, 05, 10, 05, 10, 05, 05, };

	/* POSSIBLE HOUSE STATE. */
	final static int clean = 0;
	final static int dirty = 1;

	static IloCumulFunctionExpr workers;

	public static void makeHouse(IloCP cp, int id, List<IloIntExpr> ends, List<IloIntervalVar> allTasks,
	        IloStateFunction houseState) throws IloException {

		/* CREATE THE INTERVAL VARIABLES. */
		String name;
		IloIntervalVar[] tasks = new IloIntervalVar[nbTasks];
		for (int i = 0; i < nbTasks; i++) {
			name = "H" + id + "-" + taskNames[i];
			tasks[i] = cp.intervalVar(taskDurations[i], name);
			workers = cp.sum(workers, cp.pulse(tasks[i], 1));
			allTasks.add(tasks[i]);
		}

		/* PRECEDENCE CONSTRAINTS. */
		cp.add(cp.endBeforeStart(tasks[masonry], tasks[carpentry]));
		cp.add(cp.endBeforeStart(tasks[masonry], tasks[plumbing]));
		cp.add(cp.endBeforeStart(tasks[masonry], tasks[ceiling]));
		cp.add(cp.endBeforeStart(tasks[carpentry], tasks[roofing]));
		cp.add(cp.endBeforeStart(tasks[ceiling], tasks[painting]));
		cp.add(cp.endBeforeStart(tasks[roofing], tasks[windows]));
		cp.add(cp.endBeforeStart(tasks[roofing], tasks[facade]));
		cp.add(cp.endBeforeStart(tasks[plumbing], tasks[facade]));
		cp.add(cp.endBeforeStart(tasks[roofing], tasks[garden]));
		cp.add(cp.endBeforeStart(tasks[plumbing], tasks[garden]));
		cp.add(cp.endBeforeStart(tasks[windows], tasks[moving]));
		cp.add(cp.endBeforeStart(tasks[facade], tasks[moving]));
		cp.add(cp.endBeforeStart(tasks[garden], tasks[moving]));
		cp.add(cp.endBeforeStart(tasks[painting], tasks[moving]));

		/* STATE CONSTRAINTS. */
		cp.add(cp.alwaysEqual(houseState, tasks[masonry], dirty));
		cp.add(cp.alwaysEqual(houseState, tasks[carpentry], dirty));
		cp.add(cp.alwaysEqual(houseState, tasks[plumbing], clean));
		cp.add(cp.alwaysEqual(houseState, tasks[ceiling], clean));
		cp.add(cp.alwaysEqual(houseState, tasks[roofing], dirty));
		cp.add(cp.alwaysEqual(houseState, tasks[painting], clean));
		cp.add(cp.alwaysEqual(houseState, tasks[windows], dirty));

		/* MAKESPAN. */
		ends.add(cp.endOf(tasks[moving]));
	}

	static IloIntExpr[] arrayFromList(List<IloIntExpr> list) {
		return (IloIntExpr[]) list.toArray(new IloIntExpr[list.size()]);
	}

	public static void main(String[] args) {
		try {
			IloCP cp = new WmlCP(
			        Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
			        Connector.Runtime.DO_20_1, Connector.TShirtSize.M, 1);

			List<IloIntExpr> ends = new ArrayList<IloIntExpr>();
			List<IloIntervalVar> allTasks = new ArrayList<IloIntervalVar>();
			IloTransitionDistance ttime = cp.transitionDistance(2);
			ttime.setValue(dirty, clean, 1);

			workers = cp.cumulFunctionExpr();
			IloStateFunction[] houseState = new IloStateFunction[nbHouses];
			for (int i = 0; i < nbHouses; i++) {
				houseState[i] = cp.stateFunction(ttime);
				makeHouse(cp, i, ends, allTasks, houseState[i]);
			}

			cp.add(cp.le(workers, nbWorkers));
			cp.add(cp.minimize(cp.max(arrayFromList(ends))));

			/* EXTRACTING THE MODEL AND SOLVING. */
			cp.setParameter(IloCP.IntParam.FailLimit, 10000);
			if (cp.solve()) {
				System.out.println("Solution with objective " + cp.getObjValue() + ":");
				for (int i = 0; i < allTasks.size(); i++) {
					System.out.println(cp.getDomain(allTasks.get(i)));
				}
				for (int h = 0; h < nbHouses; h++) {
					for (int i = 0; i < cp.getNumberOfSegments(houseState[h]); i++) {
						System.out.print("House " + h + " has state ");
						int s = cp.getSegmentValue(houseState[h], i);
						if (s == clean)
							System.out.print("Clean");
						else if (s == dirty)
							System.out.print("Dirty");
						else if (s == IloCP.NoState)
							System.out.print("None");
						else
							System.out.print("Unknown (problem)");
						System.out.print(" from ");
						if (cp.getSegmentStart(houseState[h], i) == IloCP.IntervalMin)
							System.out.print("Min");
						else
							System.out.print(cp.getSegmentStart(houseState[h], i));
						System.out.print(" to ");
						if (cp.getSegmentEnd(houseState[h], i) == IloCP.IntervalMax)
							System.out.println("Max");
						else
							System.out.println((cp.getSegmentEnd(houseState[h], i) - 1));
					}
				}
			} else {
				System.out.print("No solution found. ");
			}
		} catch (IloException e) {
			System.err.println("Error " + e);
		}
	}
}
