package com.ibm.ml.cp.others;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.cp.helper.DataReader;
/* ------------------------------------------------------------

Problem Description
-------------------

This problem is a special case of Job-Shop Scheduling problem (see
SchedJobShop.java) for which all jobs have the same processing order
on machines because there is a technological order on the machines for
the different jobs to follow.

------------------------------------------------------------ */
import com.typesafe.config.ConfigFactory;

import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntervalVar;
import ilog.concert.IloObjective;
import ilog.cp.WmlCP;
import ilog.cp.IloCP;

public class SchedFlowShop {

	static class IntervalVarList extends ArrayList<IloIntervalVar> {
		private static final long serialVersionUID = 1L;

		public IloIntervalVar[] toArray() {
			return (IloIntervalVar[]) this.toArray(new IloIntervalVar[this.size()]);
		}
	}

	static IloIntExpr[] arrayFromList(List<IloIntExpr> list) {
		return (IloIntExpr[]) list.toArray(new IloIntExpr[list.size()]);
	}

	public static void main(String[] args) {

		String filename = "cpo/flowshop_default.data";
		int failLimit = 10000;
		int nbJobs, nbMachines;

		if (args.length > 0)
			filename = args[0];
		if (args.length > 1)
			failLimit = Integer.parseInt(args[1]);

		try {
			DataReader data = new DataReader(filename);
			IloCP cp = new WmlCP(
			        Credentials.getCredentials(ConfigFactory.parseResources("wml.public.conf").resolve()),
			        Connector.Runtime.DO_20_1, Connector.TShirtSize.M, 1);

			nbJobs = data.next();
			nbMachines = data.next();
			List<IloIntExpr> ends = new ArrayList<IloIntExpr>();
			IntervalVarList[] machines = new IntervalVarList[nbMachines];
			for (int j = 0; j < nbMachines; j++)
				machines[j] = new IntervalVarList();

			for (int i = 0; i < nbJobs; i++) {
				IloIntervalVar prec = cp.intervalVar();
				for (int j = 0; j < nbMachines; j++) {
					int d = data.next();
					IloIntervalVar ti = cp.intervalVar(d);
					machines[j].add(ti);
					if (j > 0) {
						cp.add(cp.endBeforeStart(prec, ti));
					}
					prec = ti;
				}
				ends.add(cp.endOf(prec));
			}
			for (int j = 0; j < nbMachines; j++)
				cp.add(cp.noOverlap(machines[j].toArray()));

			IloObjective objective = cp.minimize(cp.max(arrayFromList(ends)));
			cp.add(objective);

			cp.setParameter(IloCP.IntParam.FailLimit, failLimit);
			System.out.println("Instance \t: " + filename);
			if (cp.solve()) {
				System.out.println("Makespan \t: " + cp.getObjValue());
			} else {
				System.out.println("No solution found.");
			}
		}
		catch (IloException | IOException e){
			System.out.println("Error: "+e.getMessage());
		}
	}
}
