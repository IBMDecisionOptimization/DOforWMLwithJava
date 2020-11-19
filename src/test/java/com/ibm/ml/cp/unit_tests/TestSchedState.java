package com.ibm.ml.cp.unit_tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;

import ilog.concert.IloCumulFunctionExpr;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntervalVar;
import ilog.concert.IloStateFunction;
import ilog.concert.IloTransitionDistance;
import ilog.concert.cppimpl.IloAlgorithm.Status;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import junit.framework.TestCase;

public class TestSchedState extends TestCase {

	public void setUp() throws IloException {
	}

	/**
	 * Tear-down testing environment
	 */

	public void tearDown() {
	}

	/*----------------------------------------------------------------------------
	Test methods
	----------------------------------------------------------------------------*/

    final static int nbHouses  = 5;
    final static int nbTasks   = 10;
    final static int nbWorkers = 2;

    final static int masonry   = 0;
    final static int carpentry = 1;
    final static int plumbing  = 2;
    final static int ceiling   = 3;
    final static int roofing   = 4;
    final static int painting  = 5;
    final static int windows   = 6;
    final static int facade    = 7;
    final static int garden    = 8;
    final static int moving    = 9;

    final static String[] taskNames = {
        "masonry  ",
        "carpentry",
        "plumbing ",
        "ceiling  ",
        "roofing  ",
        "painting ",
        "windows  ",
        "facade   ",
        "garden   ",
        "moving   "
    };

    final static int[] taskDurations = {
        35,
        15,
        40,
        15,
        05,
        10,
        05,
        10,
        05,
        05,
    };

    /* POSSIBLE HOUSE STATE. */
    final static int clean = 0;
    final static int dirty = 1;

    static IloCumulFunctionExpr workers;

    public static void makeHouse(IloCP cp,
                                 int id,
                                 List<IloIntExpr> ends,
                                 List<IloIntervalVar> allTasks,
                                 IloStateFunction houseState)
            throws IloException {

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
        cp.add(cp.endBeforeStart(tasks[masonry],    tasks[carpentry]));
        cp.add(cp.endBeforeStart(tasks[masonry],    tasks[plumbing]));
        cp.add(cp.endBeforeStart(tasks[masonry],    tasks[ceiling]));
        cp.add(cp.endBeforeStart(tasks[carpentry],  tasks[roofing]));
        cp.add(cp.endBeforeStart(tasks[ceiling],    tasks[painting]));
        cp.add(cp.endBeforeStart(tasks[roofing],    tasks[windows]));
        cp.add(cp.endBeforeStart(tasks[roofing],    tasks[facade]));
        cp.add(cp.endBeforeStart(tasks[plumbing],   tasks[facade]));
        cp.add(cp.endBeforeStart(tasks[roofing],    tasks[garden]));
        cp.add(cp.endBeforeStart(tasks[plumbing],   tasks[garden]));
        cp.add(cp.endBeforeStart(tasks[windows],    tasks[moving]));
        cp.add(cp.endBeforeStart(tasks[facade],     tasks[moving]));
        cp.add(cp.endBeforeStart(tasks[garden],     tasks[moving]));
        cp.add(cp.endBeforeStart(tasks[painting],   tasks[moving]));

        /* STATE CONSTRAINTS. */
        cp.add(cp.alwaysEqual(houseState, tasks[masonry],   dirty));
        cp.add(cp.alwaysEqual(houseState, tasks[carpentry], dirty));
        cp.add(cp.alwaysEqual(houseState, tasks[plumbing],  clean));
        cp.add(cp.alwaysEqual(houseState, tasks[ceiling],   clean));
        cp.add(cp.alwaysEqual(houseState, tasks[roofing],   dirty));
        cp.add(cp.alwaysEqual(houseState, tasks[painting],  clean));
        cp.add(cp.alwaysEqual(houseState, tasks[windows],   dirty));

        /* MAKESPAN. */
        ends.add(cp.endOf(tasks[moving]));
    }

    static IloIntExpr[] arrayFromList(List<IloIntExpr> list) {
        return (IloIntExpr[]) list.toArray(new IloIntExpr[list.size()]);
    }

    private static List<IloIntervalVar> allTasks;
    private static IloStateFunction[] houseState;

	public IloCP build_model() {
		IloCP cp = null;
		try {
            cp = new WmlCP(
                    Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
                    Connector.Runtime.DO_12_10,
                    Connector.TShirtSize.M,
                    1);

            List<IloIntExpr> ends = new ArrayList<IloIntExpr>();
            allTasks = new ArrayList<IloIntervalVar>();
            IloTransitionDistance ttime = cp.transitionDistance(2);
            ttime.setValue(dirty, clean, 1);

            workers = cp.cumulFunctionExpr();
            houseState = new IloStateFunction[nbHouses];
            for (int i = 0; i < nbHouses; i++) {
                houseState[i] = cp.stateFunction(ttime);
                makeHouse(cp, i, ends, allTasks, houseState[i]);
            }

            cp.add(cp.le(workers, nbWorkers));
            cp.add(cp.minimize(cp.max(arrayFromList(ends))));

            /* EXTRACTING THE MODEL AND SOLVING. */
            cp.setParameter(IloCP.IntParam.FailLimit, 10000);

		} catch (IloException e) {
			System.err.println("Error " + e);
		}
		return cp;
	}

	/**
	 * Test access to attributes
	 */
	public void test01_SchedState() {
		IloCP cp = this.build_model();

		try {
			boolean res = cp.solve();
			assertTrue("Model is expected to solve", res);
		} catch (IloException e) {
			fail("Failure during model generation");
		}

		try {
			System.out.println("Objective value: " + cp.getObjValue());

			assertTrue(365 >= (int) cp.getObjValue());
			Status status = cp.getStatus();
			assertEquals(status, Status.Feasible);

			System.out.println("allTasks.get(0): " + cp.getDomain(allTasks.get(0)));
			System.out.println("START: " + cp.getStart(allTasks.get(0)));
			System.out.println("END:   " + cp.getEnd(allTasks.get(0)));

			assertEquals(35, cp.getStart(allTasks.get(0)));
			assertEquals(70, cp.getEnd(allTasks.get(0)));

			assertEquals(14, cp.getNumberOfSegments(houseState[0]));
			assertEquals(IloCP.NoState, cp.getSegmentValue(houseState[0], 0));
			assertEquals(IloCP.IntervalMin, cp.getSegmentStart(houseState[0], 0));
			assertEquals(35, cp.getSegmentEnd(houseState[0], 0));

			assertEquals(dirty, cp.getSegmentValue(houseState[0], 1));
			assertEquals(35, cp.getSegmentStart(houseState[0], 1));
			assertEquals(70, cp.getSegmentEnd(houseState[0], 1));

		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure while browsing model solution with getters");
		}

	}

}
