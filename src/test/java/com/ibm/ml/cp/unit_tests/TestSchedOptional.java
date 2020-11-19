package com.ibm.ml.cp.unit_tests;

import java.io.IOException;
import java.util.ArrayList;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;

import com.typesafe.config.ConfigFactory;
import ilog.concert.IloException;
import ilog.concert.IloIntervalSequenceVar;
import ilog.concert.IloIntervalVar;
import ilog.concert.IloNumExpr;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import junit.framework.TestCase;

public class TestSchedOptional extends TestCase {

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
    static final int nbWorkers = 3;
    static final int nbTasks   = 10;

    static final int joe  = 0;
    static final int jack = 1;
    static final int jim  = 2;

    static final String[] workerNames = {
        "Joe",
        "Jack",
        "Jim"
    };

    static final int masonry   = 0;
    static final int carpentry = 1;
    static final int plumbing  = 2;
    static final int ceiling   = 3;
    static final int roofing   = 4;
    static final int painting  = 5;
    static final int windows   = 6;
    static final int facade    = 7;
    static final int garden    = 8;
    static final int moving    = 9;

    static final String[] taskNames = {
        "masonry",
        "carpentry",
        "plumbing",
        "ceiling",
        "roofing",
        "painting",
        "windows",
        "facade",
        "garden",
        "moving"
    };

    static final int[] taskDurations = {
        35,
        15,
        40,
        15,
        05,
        10,
        05,
        10,
        05,
        05
    };

    static final int[] skillsMatrix = {
        // Joe, Jack, Jim
        9, 5, 0, // masonry
        7, 0, 5, // carpentry
        0, 7, 0, // plumbing
        5, 8, 0, // ceiling
        6, 7, 0, // roofing
        0, 9, 6, // painting
        8, 0, 5, // windows
        5, 5, 0, // facade
        5, 5, 9, // garden
        6, 0, 8  // moving
    };

    public static boolean hasSkill(int w, int i) {
        return (0 < skillsMatrix[nbWorkers * i + w]);
    }

    public static int skillLevel(int w, int i) {
        return skillsMatrix[nbWorkers * i + w];
    }

    static IloCP cp;

    static IloNumExpr skill;
    
    static IntervalVarList allTasks;

    public static void makeHouse(IntervalVarList allTasks,
                                 IntervalVarList[] workerTasks,
                                 int id,
                                 int deadline) 
        throws IloException {

        /* CREATE THE INTERVAL VARIABLES. */
        String name;
        IloIntervalVar[] tasks = new IloIntervalVar[nbTasks];
        IloIntervalVar[][] taskMatrix = new IloIntervalVar[nbTasks][nbWorkers];

        for (int i = 0; i < nbTasks; i++) {
            name = "H" + id + "-" + taskNames[i];
            tasks[i] = cp.intervalVar(taskDurations[i], name);
            
            /* ALLOCATING TASKS TO WORKERS. */
            IntervalVarList alttasks = new IntervalVarList();
            for (int w = 0; w < nbWorkers; w++) {
                if (hasSkill(w, i)) {
                    name = "H" + id + "-" + taskNames[i] + "-" + workerNames[w];
                    IloIntervalVar wtask = cp.intervalVar(taskDurations[i], name);
                    wtask.setOptional();
                    alttasks.add(wtask);
                    taskMatrix[i][w] = wtask;
                    workerTasks[w].add(wtask);
                    allTasks.add(wtask);
                    /* DEFINING MAXIMIZATION OBJECTIVE. */
                    skill = cp.sum(skill, cp.prod(skillLevel(w, i), cp.presenceOf(wtask)));
                }
            }
            cp.add(cp.alternative(tasks[i], alttasks.toArray()));
        }

        /* ADDING PRECEDENCE CONSTRAINTS. */
        tasks[moving].setEndMax(deadline);
        cp.add(cp.endBeforeStart(tasks[masonry],   tasks[carpentry]));
        cp.add(cp.endBeforeStart(tasks[masonry],   tasks[plumbing]));
        cp.add(cp.endBeforeStart(tasks[masonry],   tasks[ceiling]));
        cp.add(cp.endBeforeStart(tasks[carpentry], tasks[roofing]));
        cp.add(cp.endBeforeStart(tasks[ceiling],   tasks[painting]));
        cp.add(cp.endBeforeStart(tasks[roofing],   tasks[windows]));
        cp.add(cp.endBeforeStart(tasks[roofing],   tasks[facade]));
        cp.add(cp.endBeforeStart(tasks[plumbing],  tasks[facade]));
        cp.add(cp.endBeforeStart(tasks[roofing],   tasks[garden]));
        cp.add(cp.endBeforeStart(tasks[plumbing],  tasks[garden]));
        cp.add(cp.endBeforeStart(tasks[windows],   tasks[moving]));
        cp.add(cp.endBeforeStart(tasks[facade],    tasks[moving]));
        cp.add(cp.endBeforeStart(tasks[garden],    tasks[moving]));
        cp.add(cp.endBeforeStart(tasks[painting],  tasks[moving]));

        /* ADDING SAME-WORKER CONSTRAINTS. */
        cp.add(cp.add(cp.equiv(cp.presenceOf(taskMatrix[masonry][joe]), 
                               cp.presenceOf(taskMatrix[carpentry][joe]))));
        cp.add(cp.add(cp.equiv(cp.presenceOf(taskMatrix[roofing][jack]), 
                               cp.presenceOf(taskMatrix[facade][jack]))));
        cp.add(cp.add(cp.equiv(cp.presenceOf(taskMatrix[carpentry][joe]), 
                               cp.presenceOf(taskMatrix[roofing][joe]))));
        cp.add(cp.add(cp.equiv(cp.presenceOf(taskMatrix[garden][jim]), 
                               cp.presenceOf(taskMatrix[moving][jim]))));
    }

    static class IntervalVarList extends ArrayList<IloIntervalVar> {
		private static final long serialVersionUID = 1L;

        public IloIntervalVar[] toArray() {
            return (IloIntervalVar[]) this.toArray(new IloIntervalVar[this.size()]);
        }
    }

    public IloCP build_model() {

		try {
        	cp = new WmlCP(
                    Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
                    Connector.Runtime.DO_12_10,
                    Connector.TShirtSize.M,
                    1);

            int nbHouses = 5;
            int deadline = 318;
            skill = cp.intExpr();
            allTasks = new IntervalVarList();
            IntervalVarList[] workerTasks = new IntervalVarList[nbWorkers];
            for (int w = 0; w < nbWorkers; w++) {
                workerTasks[w] = new IntervalVarList();
            }

            for (int h = 0; h < nbHouses; h++) {
                makeHouse(allTasks, workerTasks, h, deadline);
            }

            for (int w = 0; w < nbWorkers; w++) {
                IloIntervalSequenceVar seq = cp.intervalSequenceVar(workerTasks[w].toArray(), workerNames[w]);
                cp.add(cp.noOverlap(seq));
            }

            cp.add(cp.maximize(skill));

            cp.setParameter(IloCP.IntParam.FailLimit, 10000);
		} catch (IloException e) {
		}
		return cp;
	}

	/**
	 * Test access to attributes
	 */
	public void test01_SchedOptional() {
		IloCP cp = this.build_model();

		try {
			boolean res = cp.solve();
			assertTrue("Model is expected to solve", res);

            System.out.println("Makespan \t: " + cp.getObjValue());

            assertTrue(357 >= (int) cp.getObjValue());

            for (int i = 0; i < allTasks.size(); i++) {
                IloIntervalVar var = (IloIntervalVar) allTasks.get(i);
                if (cp.isPresent(var))
                    System.out.println(cp.getDomain((IloIntervalVar) allTasks.get(i)));
            }

		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure during model generation");
		}
	}

}
