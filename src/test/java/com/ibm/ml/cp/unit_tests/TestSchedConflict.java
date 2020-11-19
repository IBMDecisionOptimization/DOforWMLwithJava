package com.ibm.ml.cp.unit_tests;

import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import junit.framework.TestCase;

public class TestSchedConflict extends TestCase {
	private static final Logger logger = LogManager.getLogger();

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

	public static IloConstraint[] capacityCts;
	public static IloConstraint[] precedenceCts;

	static IloIntExpr[] arrayFromExprList(List<IloIntExpr> list) {
		return (IloIntExpr[]) list.toArray(new IloIntExpr[list.size()]);
	}

	static IloConstraint[] arrayFromConstraintList(List<IloConstraint> list) {
		return (IloConstraint[]) list.toArray(new IloConstraint[list.size()]);
	}

	// ----- RCPSP Model creation ------------------------------------------------

	private IloCP buildModel(String fileName) {
		IloCP cp = null;
		int nbTasks, nbResources;
		try {
			cp = new WmlCP(Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
			        Connector.Runtime.DO_12_10, Connector.TShirtSize.M, 1);
			
			DataReader data = new DataReader(fileName);
			
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
		} catch (IloException | IOException e) {
			System.err.println("Error: " + e);
		}
		return cp;
	}


	/**
	 */
	public void test01_SchedConflict() {
		int failLimit = 10000;
		String fileName = "cpo/sched_conflict.data";
		IloCP cp = this.buildModel(fileName);

		try {
            /* EXTRACTING THE MODEL AND SOLVING. */
			cp.setParameter(IloCP.IntParam.FailLimit, failLimit);
			cp.setParameter(IloCP.IntParam.CumulFunctionInferenceLevel, IloCP.ParameterValues.Extended);
			cp.setParameter(IloCP.IntParam.ConflictRefinerOnVariables, IloCP.ParameterValues.On);
			boolean res = cp.solve();
			assertFalse("Model is expected to be unfeasible", res);

		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure during model generation");
		}

		try {

			Status status = cp.getStatus();
			logger.info("Status = " + status);
			assertNotEquals(Status.Unknown, status);
			assertEquals(Status.Infeasible, status);
			
			logger.info("Infeasible problem, running conflict refiner ...");
			cp.refineConflict();
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			cp.writeConflict(os);
			String osAsStr = os.toString();
			logger.info("WriteConflict output: " + osAsStr);
			assertTrue(osAsStr.contains("IloAlwaysIn(457)[[( ... + ...) + IloCumulAtom(452)"));
			assertTrue(osAsStr.contains("ACT44"));
			assertTrue(osAsStr.contains("ACT19"));

			
		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure during solution browsing");
		}
	}

	/**
	 * Test that name of named constraints is displayed in conflicts 
	 */
	public void test02_SchedConflict() {
		int failLimit = 10000;
		String fileName = "cpo/sched_conflict.data";
		IloCP cp = this.buildModel(fileName);

		// Name capacity constraints
		for (int i = 0; i < capacityCts.length; i++) {
			capacityCts[i].setName("Capacity constraint id_" + i);
		}

		try {
            /* EXTRACTING THE MODEL AND SOLVING. */
			cp.setParameter(IloCP.IntParam.FailLimit, failLimit);
			cp.setParameter(IloCP.IntParam.CumulFunctionInferenceLevel, IloCP.ParameterValues.Extended);
			cp.setParameter(IloCP.IntParam.ConflictRefinerOnVariables, IloCP.ParameterValues.On);
			boolean res = cp.solve();
			assertFalse("Model is expected to be unfeasible", res);

		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure during model generation");
		}

		try {

			Status status = cp.getStatus();
			logger.info("Status = " + status);
			assertNotEquals(Status.Unknown, status);
			assertEquals(Status.Infeasible, status);
			
			logger.info("Infeasible problem, running conflict refiner ...");
			cp.refineConflict();
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			cp.writeConflict(os);
			String osAsStr = os.toString();
			logger.info("WriteConflict output: " + osAsStr);
			assertTrue(osAsStr.contains("Capacity constraint id_1"));
			assertTrue(osAsStr.contains("ACT44"));
			assertTrue(osAsStr.contains("ACT19"));

			
		} catch (IloException e) {
			e.printStackTrace();
			fail("Failure during solution browsing");
		}
	}

}
