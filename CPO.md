## Supported API and limitations for CPO

### Supported API
Solve and refine conflict methods are executed remotely.
Use name constraints with explicit labels when invoking the conflict refiner as returned conflicts will use these labels to identify the conflicting subset of constraints.

Here is the list of methods that were redefined specifically to run with Watson Machine Learning.

```
  public boolean solve() throws IloException
  public boolean refineConflict() throws IloException
```

```
  public double getObjValue() throws IloException
  public double getObjValue(int i) throws IloException
  public double[] getObjValues() throws IloException
  public double getObjBound() throws IloException
  public double getObjBound(int i) throws IloException
  public double[] getObjBounds() throws IloException
  public double getObjGap() throws IloException
  public double getObjGap(int i) throws IloException
  public double[] getObjGaps() throws IloException
  public String getDomain(IloIntervalVar var) throws IloException
  public int getEnd(IloIntervalVar a)
  public int getEnd(String intervalVarName) throws IloException
  public int getEndMin(IloIntervalVar a)
  public int getEndMax(IloIntervalVar a)
  public IloIntervalVar getFirst(ilog.concert.IloIntervalSequenceVar seq) throws IloException
  public int getIntValue(IloIntVar v)
  public double getKPIValue(String name) throws IloException
  public IloIntervalVar getLast(ilog.concert.IloIntervalSequenceVar seq) throws IloException
  public int getLength(IloIntervalVar a)
  public int getLength(String intervalVarName) throws IloException
  public int getLengthMin(IloIntervalVar a)
  public int getLengthMax(IloIntervalVar a)
  public IloIntervalVar getNext(ilog.concert.IloIntervalSequenceVar seq, IloIntervalVar a) throws IloException
  public int getNumberOfSegments(ilog.concert.IloStateFunction f) throws IloException
  public int getSegmentStart(ilog.concert.IloStateFunction f, int i) throws IloException
  public int getSegmentEnd(ilog.concert.IloStateFunction f, int i) throws IloException
  public int getSegmentValue(ilog.concert.IloStateFunction f, int i) throws IloException
  public int getSize(IloIntervalVar a)
  public int getSize(String intervalVarName)
  public int getSizeMin(IloIntervalVar a)
  public int getSizeMax(IloIntervalVar a)
  public int getStart(IloIntervalVar a)
  public int getStart(String intervalVarName) throws IloException
  public int getStartMin(IloIntervalVar a)
  public int getStartMax(IloIntervalVar a)
  public Status getStatus()
  public String getStatusString()
  public double getValue(IloNumVar v)
  public int getValue(String intVarName) throws IloException
  public int getValue(IloStateFunction f, int t) throws IloException
  public void getValues(IloIntVar[] vars, double[] vals) throws IloException
  public void getValues(IloIntVarArray varArray, double[] numArray) throws IloException
  public void getValues(IloNumVar[] varArray, double[] numArray) throws IloException
  public boolean isAbsent(String intervalVarName) throws IloException
  public boolean isAbsent(IloIntervalVar a)
  public boolean isPresent(String intervalVarName) throws IloException
  public boolean isPresent(IloIntervalVar a)
  public boolean isFixed(IloIntervalVar a)
  public boolean isFixed(IloStateFunction f) throws IloException

  public void writeConflict() throws IloException
  public void writeConflict(OutputStream os) throws IloException
```

### Partially supported

The following methods are partially supported: only instances of IloIntVarChooser, IloIntValueChooser and IloIntValueEval are accepted as parameters. Instances derived from abstract IloCustomIntVarChooser, IloCustomIntValueChooser or IloCustomIntValueEval classes are not supported.  

```
    public IloSearchPhase searchPhase(IloIntVar[] vars, IloIntVarChooser varChooser, IloIntValueChooser valueChooser) throws IloException
    public IloSearchPhase searchPhase(IloIntVarArray vars, IloIntVarChooser varChooser, IloIntValueChooser valueChooser) throws IloException
    public IloSearchPhase searchPhase(IloIntVarChooser varChooser, IloIntValueChooser valueChooser) throws IloException
    public IloValueSelector selectLargest(double minNumber, IloIntValueEval e) throws IloException
    public IloVarSelector selectLargest(double minNumber, IloIntVarEval e) throws IloException
    public IloValueSelector selectLargest(IloIntValueEval e, double tol) throws IloException
    public IloValueSelector selectLargest(IloIntValueEval e) throws IloException
    public IloVarSelector selectLargest(IloIntVarEval e) throws IloException
    public IloValueSelector selectSmallest(double minNumber, IloIntValueEval e) throws IloException
    public IloVarSelector selectSmallest(double minNumber, IloIntVarEval e) throws IloException
    public IloValueSelector selectSmallest(IloIntValueEval e, double tol) throws IloException
    public IloValueSelector selectSmallest(IloIntValueEval e) throws IloException
    public IloVarSelector selectSmallest(IloIntVarEval e, double tol) throws IloException
    public IloVarSelector selectSmallest(IloIntVarEval e) throws IloException
```

### Unsupported API

Invoking any of the non-supported methods results in a RuntimeException.
Here is an exhaustive list of non-supported methods:

```
  public void clearExplanations() throws IloException
  public void explainFailure(int failTag) throws IloException
  public void explainFailure(int[] failTagArray) throws IloException
  
  public double getInfo(DoubleInfo which) throws IloException
  public int getInfo(IntInfo which) throws IloException
  
  public double getValue(IloIntExpr i)
  public double getValue(IloNumExpr num)
  public double getIncumbentValue(IloNumExpr expr) throws IloException
  public double getIncumbentValue(String exprName) throws IloException
  
  public int getNumberOfSegments(IloCumulFunctionExpr f) throws IloException
  public int getSegmentStart(IloCumulFunctionExpr f, int i) throws IloException
  public int getSegmentEnd(IloCumulFunctionExpr f, int i) throws IloException
  public int getSegmentValue(IloCumulFunctionExpr f, int i) throws IloException
  public int getValue(IloCumulFunctionExpr f, int t) throws IloException
  public boolean isFixed(IloCumulFunctionExpr f) throws IloException
  
  public void addCallback(IloCP.Callback cb) throws IloException
  public void removeCallback(IloCP.Callback cb) throws IloException
  public void removeAllCallbacks() throws IloException
  
  public boolean refineConflict(IloConstraint[] csts) throws IloException
  public boolean refineConflict(IloConstraint[] csts, double[] prefs) throws IloException
  public boolean refineConflict(IloSolution sol)
  public boolean refineConflict(IloConstraintArray csts) throws IloException
  public boolean refineConflict(IloConstraintArray csts, ilog.concert.IloNumArray prefs) throws IloException
  public IloCP.ConflictStatus getConflict(IloConstraint cst) throws IloException
  public IloCP.ConflictStatus getConflict(IloNumVar var) throws IloException
  public IloCP.ConflictStatus getConflict(IloIntervalVar var) throws IloException
  public void exportConflict(OutputStream s) throws IloException
  
  public void runSeeds(int n) throws IloException
  public void runSeeds() throws IloException
  public void startNewSearch() throws IloException
  public boolean next() throws IloException
```
