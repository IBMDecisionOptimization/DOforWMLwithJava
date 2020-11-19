package com.ibm.ml.cp.others;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.typesafe.config.ConfigFactory;

/* ------------------------------------------------------------

Problem Description
-------------------

The aim of the square example is to place a set of small squares of
different sizes into a large square.

------------------------------------------------------------ */
import ilog.concert.IloCumulFunctionExpr;
import ilog.concert.IloException;
import ilog.concert.IloIntervalVar;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;
import ilog.cp.IloSearchPhase;

public class SchedSquare {

	public static void main(String[] args) {

		try {
			IloCP cp = new WmlCP(
			        Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
			        Connector.Runtime.DO_12_10, Connector.TShirtSize.M, 1);

			int sizeSquare = 112;
			int nbSquares = 21;
			int[] size = { 50, 42, 37, 35, 33, 29, 27, 25, 24, 19, 18, 17, 16, 15, 11, 9, 8, 7, 6, 4, 2 };
			IloIntervalVar[] x = new IloIntervalVar[nbSquares];
			IloIntervalVar[] y = new IloIntervalVar[nbSquares];
			IloCumulFunctionExpr rx = cp.cumulFunctionExpr();
			IloCumulFunctionExpr ry = cp.cumulFunctionExpr();

			for (int i = 0; i < nbSquares; ++i) {
				x[i] = cp.intervalVar(size[i], "X" + i);
				x[i].setEndMax(sizeSquare);
				y[i] = cp.intervalVar(size[i], "Y" + i);
				y[i].setEndMax(sizeSquare);
				rx = cp.sum(rx, cp.pulse(x[i], size[i]));
				ry = cp.sum(ry, cp.pulse(y[i], size[i]));

				for (int j = 0; j < i; ++j) {
					cp.add(cp.or(cp.le(cp.endOf(x[i]), cp.startOf(x[j])), cp.or(cp.le(cp.endOf(x[j]), cp.startOf(x[i])),
					        cp.or(cp.le(cp.endOf(y[i]), cp.startOf(y[j])), cp.le(cp.endOf(y[j]), cp.startOf(y[i]))))));
				}
			}
			cp.add(cp.alwaysIn(rx, 0, sizeSquare, sizeSquare, sizeSquare));
			cp.add(cp.alwaysIn(ry, 0, sizeSquare, sizeSquare, sizeSquare));

			IloSearchPhase[] phases = new IloSearchPhase[2];
			phases[0] = cp.searchPhase(x);
			phases[1] = cp.searchPhase(y);

			cp.setSearchPhases(phases);
			if (cp.solve()) {
				for (int i = 0; i < nbSquares; ++i) {
					System.out.println("Square " + i + ": [" + cp.getStart(x[i]) + "," + cp.getEnd(x[i]) + "] x ["
					        + cp.getStart(y[i]) + "," + cp.getEnd(y[i]) + "]");
				}
			}

		} catch (IloException ex) {
			System.out.println("Caught: " + ex);
		}
	}
}
// $end:ALL
