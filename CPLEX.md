## Supported API and limitations for CPLEX

##### Supported API
```
 public boolean solve() throws IloException   
 public double getObjValue() throws IloException
 public double getValue(IloNumVar v) throws IloException
 public double[] getValues(IloNumVar[] v) throws IloException
 public double[] getValues(IloNumVar[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException
 public double getDual(IloRange r) throws IloException
 public double[] getDuals(IloRange[] r) throws IloException
 public Status getStatus() throws IloException
 public CplexStatus getCplexStatus() throws IloException
 public double getReducedCost(IloNumVar v) throws IloCplex.UnknownObjectException, IloException
 public double[] getReducedCosts(IloNumVar[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException
 public double[] getDuals(IloRange[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException
 public double getSlack(IloRange r) throws IloCplex.UnknownObjectException, IloException
 public double[] getSlacks(IloRange[] r) throws IloCplex.UnknownObjectException, IloException
 public double[] getSlacks(IloRange[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException
 public double getValue(IloNumExpr var) throws IloException
 public boolean feasOpt(IloConstraint[] var1, double[] var2) throws IloException
```


##### Unsupported API

Callbacks/goals, solution pools, asynchronous apis, conflicts and some relaxation methods are not supported.
Here is the list of unsupported methods:

```
 public boolean feasOpt(IloRange[] var1, double[] var2, double[] var3, IloNumVar[] var4, double[] var5, double[] var6) throws IloException
 public boolean feasOpt(IloNumVar[] var1, double[] var2, double[] var3) throws IloException
 public boolean feasOpt(IloRange[] var1, double[] var2, double[] var3) throws IloException
 public IloCplex.ConflictStatus[] getConflict(IloConstraint[] var1) throws IloException
 public IloCplex.ConflictStatus getConflict(IloConstraint var1) throws IloException
 public IloCplex.ConflictStatus[] getConflict(IloConstraint[] var1, int var2, int var3) throws IloException
 public boolean refineConflict(IloConstraint[] var1, double[] var2) throws IloException
 public boolean refineConflict(IloConstraint[] var1, double[] var2, int var3, int var4) throws IloException
  public double getObjValue(int var1) throws IloException 
  public double getBestObjValue() throws IloException 
  public double getMIPRelativeGap() throws IloException 
  public double[] getValues(IloLPMatrix matrix, int soln) throws IloCplex.UnknownObjectException, IloException 
  public double[] getValues(IloNumVar[] var1, int var2, int var3, int var4) throws IloCplex.UnknownObjectException, IloException 
  public double getValue(IloNumExpr var1, int var2) throws IloException 
  public double[] getValues(IloLPMatrix matrix,int start, int num, int soln)
  public double getSolnPoolMeanObjValue() throws IloException 
  public int getSolnPoolNsolns() throws IloException 
  public int getSolnPoolNreplaced() throws IloException 
  public void delSolnPoolSoln(int var1) throws IloException 
  public void delSolnPoolSolns(int var1, int var2) throws IloException 
  public double getCutoff() throws IloException 
  public IloCplex.Aborter use(IloCplex.Aborter var1) throws IloException 
  public IloCplex.Aborter getAborter() 
  public void remove(IloCplex.Aborter var1) throws IloException 
  public IloCplex.TuneParamHandle tuneParam(boolean var1) throws IloException 
  public IloCplex.TuneParamHandle tuneParam(IloCplex.ParameterSet var1, boolean var2) throws IloException 
  public void copyVMConfig(String var1) throws IloException 
  public void readVMConfig(String var1) throws IloException 
  public boolean hasVMConfig() throws IloException 
  public void delVMConfig() throws IloException 
  public IloCplex.Goal eqGoal(IloNumExpr var1, double var2) throws IloException 
  public IloCplex.Goal eqGoal(IloNumExpr var1, IloNumExpr var2) throws IloException 
  public IloCplex.Goal eqGoal(double var1, IloNumExpr var3) throws IloException 
  public IloCplex.Goal geGoal(IloNumExpr var1, double var2) throws IloException 
  public IloCplex.Goal geGoal(IloNumExpr var1, IloNumExpr var2) throws IloException 
  public IloCplex.Goal geGoal(double var1, IloNumExpr var3) throws IloException 
  public IloCplex.Goal leGoal(IloNumExpr var1, double var2) throws IloException 
  public IloCplex.Goal leGoal(IloNumExpr var1, IloNumExpr var2) throws IloException 
  public IloCplex.Goal leGoal(double var1, IloNumExpr var3) throws IloException 
  public int getNcrossPExch() 
  public int getNcrossPPush() 
  public int getNdualSuperbasics() 
  public int getNprimalSuperbasics() 
  public int getNnodes() 
  public int getNnodesLeft() 
  public long getNiterations64() 
  public long getNphaseOneIterations64() 
  public long getNbarrierIterations64() 
  public long getNsiftingIterations64() 
  public long getNsiftingPhaseOneIterations64() 
  public long getNcrossDExch64() 
  public long getNcrossDPush64() 
  public long getNcrossPExch64() 
  public long getNcrossPPush64() 
  public double getInfeasibility(IloNumVar var1) throws IloCplex.UnknownObjectException, IloException 
  public IloCplex.BasisStatus getBasisStatus(IloNumVar var1) throws IloException 
  public IloCplex.BasisStatus[] getBasisStatuses(IloNumVar[] var1) throws IloException 
  public IloCplex.BasisStatus[] getBasisStatuses(IloNumVar[] var1, int var2, int var3) throws IloException 
  public IloCplex.BasisStatus getBasisStatus(IloConstraint var1) throws IloException 
  public IloCplex.BasisStatus[] getBasisStatuses(IloConstraint[] var1) throws IloException 
  public IloCplex.BasisStatus[] getBasisStatuses(IloConstraint[] var1, int var2, int var3) throws IloException 
  public int getNMIPStarts() throws IloException 
  public void getBoundSA(double[] var1, double[] var2, double[] var3, double[] var4, IloNumVar[] var5, int var6, int var7) throws IloException 
  public void getObjSA(double[] var1, double[] var2, IloNumVar[] var3, int var4, int var5) throws IloException 
  public void getRangeSA(double[] var1, double[] var2, double[] var3, double[] var4, IloRange[] var5, int var6, int var7) throws IloException 
  public void getRHSSA(double[] var1, double[] var2, IloRange[] var3, int var4, int var5) throws IloException 
  public void getRHSSA(double[] var1, double[] var2, IloLPMatrix var3, int var4, int var5) throws IloException 
  public int getNcuts(int var1) throws IloException 
  public int getPriority(IloNumVar var1) throws IloException 
  public int[] getPriorities(IloNumVar[] var1, int var2, int var3) throws IloException 
  public double dualFarkas(IloConstraint[] var1, double[] var2) throws IloException 
  public IloCplex.RemoteInfoHandler setRemoteInfoHandler(IloCplex.RemoteInfoHandler var1) throws IloException 
  public IloCplex.RemoteInfoHandler getRemoteInfoHandler() 
  public IloCplex.RemoteInfoHandler removeRemoteInfoHandler() throws IloException 
  public boolean refineMIPStartConflict(int var1, IloConstraint[] var2, double[] var3) throws IloException 
  public boolean refineMIPStartConflict(int var1, IloConstraint[] var2, double[] var3, int var4, int var5) throws IloException 
  public IloCplex.FeasOptHandle feasOpt(IloConstraint[] var1, double[] var2, boolean var3) throws IloException 
  public IloCplex.FeasOptHandle feasOpt(IloRange[] var1, double[] var2, double[] var3, boolean var4) throws IloException 
  public IloCplex.FeasOptHandle feasOpt(IloNumVar[] var1, double[] var2, double[] var3, boolean var4) throws IloException 
  public IloCplex.FeasOptHandle feasOpt(IloRange[] var1, double[] var2, double[] var3, IloNumVar[] var4, double[] var5, double[] var6, boolean var7) throws IloException 
  public IloCplex.RefineMIPStartConflictHandle refineMIPStartConflict(int var1, IloConstraint[] var2, double[] var3, boolean var4) throws IloException 
  public IloCplex.RefineMIPStartConflictHandle refineMIPStartConflict(int var1, IloConstraint[] var2, double[] var3, int var4, int var5, boolean var6) throws IloException 
  public IloCplex.SolveHandle solve(boolean var1) throws IloException 
  public IloCplex.SolveFixedHandle solveFixed(boolean var1) throws IloException 
  public IloCplex.SolveFixedHandle solveFixed(int var1, boolean var2) throws IloException 
  public IloCplex.RefineConflictHandle refineConflict(IloConstraint[] var1, double[] var2, boolean var3) throws IloException 
  public IloCplex.RefineConflictHandle refineConflict(IloConstraint[] var1, double[] var2, int var3, int var4, boolean var5) throws IloException 
  public IloCplex.PopulateHandle populate(boolean var1) throws IloException 
  public boolean solve(IloCplex.ParameterSet[] var1) throws IloException
  public IloLinearNumExpr getQCDSlack(IloRange var1) throws IloException 
  public int tuneParam() throws IloException 
  public int tuneParam(IloCplex.ParameterSet var1) throws IloException 
  public int tuneParam(String[] var1) throws IloException 
  public int tuneParam(String[] var1, IloCplex.ParameterSet var2) throws IloException 
  public void use(IloCplex.Callback var1) throws IloException 
  public void remove(IloCplex.Callback var1) throws IloException 
  public void clearCallbacks() throws IloException 
  public void qpIndefCertificate(IloNumVar[] var1, double[] var2) throws IloException 
  public void protectVariables(IloNumVar[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException 
  public void protectVariables(IloNumVar[] var1) throws IloCplex.UnknownObjectException, IloException 
  public double[] getAX(IloLPMatrix var1) throws IloCplex.UnknownObjectException, IloException 
  public double[] getAX(IloLPMatrix var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException 
  public double getAX(IloRange var1) throws IloCplex.UnknownObjectException, IloException 
  public double[] getAX(IloRange[] var1) throws IloCplex.UnknownObjectException, IloException 
  public double[] getAX(IloRange[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException 
  public IloCplex.Quality getQuality(IloCplex.QualityType var1) throws IloException 
  public IloCplex.Quality getQuality(IloCplex.QualityType var1, int var2) throws IloException 
  public IloCplex.BranchDirection getDirection(IloNumVar var1) throws IloException 
  public IloCplex.BranchDirection[] getDirections(IloNumVar[] var1) throws IloException 
  public IloCplex.BranchDirection[] getDirections(IloNumVar[] var1, int var2, int var3) throws IloException 
  public IloLinearNumExpr getRay() throws IloException 
  public IloCopyable getDiverging() throws IloException 
  public int getNLCs() 
  public int getNUCs() 
```