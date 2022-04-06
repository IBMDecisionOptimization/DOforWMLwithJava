/* --------------------------------------------------------------------------
   Source file provided under Apache License, Version 2.0, January 2004,
   http://www.apache.org/licenses/
   (c) Copyright IBM Corp. 2016

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. 
   --------------------------------------------------------------------------
 */
// This class is in package ilog.cplex so that we can call some undocumented functions
package ilog.cplex;

import ilog.concert.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


abstract class NotSupportedCplex extends IloCplex {
  private static final Logger logger = LogManager.getLogger();

  protected String notSupportedError = "Not supported by the CPLEX WML Connector";

  public NotSupportedCplex() throws IloException {
    super();
  }

  @Override
  public double getObjValue(int var1) throws IloException {
    logger.error(notSupportedError + " getObjValue");
    throw new IloException(notSupportedError);
  }
  @Override
  public double getBestObjValue() throws IloException {
    logger.error(notSupportedError + " getBestObjValue");
    throw new IloException(notSupportedError);
  }
  @Override
  public double getMIPRelativeGap() throws IloException {
    logger.error(notSupportedError + " getMIPRelativeGap");
    throw new IloException(notSupportedError);  }

  @Override
  public double[] getValues(IloLPMatrix matrix,
                            int soln)
          throws IloCplex.UnknownObjectException,
          IloException {
    logger.error(notSupportedError + " getValues");
    throw new IloException(notSupportedError);
  }

  @Override
  public double[] getValues(IloNumVar[] var1, int var2, int var3, int var4) throws IloCplex.UnknownObjectException, IloException {
    logger.error(notSupportedError + " getValues");
    throw new IloException(notSupportedError);
  }

  @Override
  public double getValue(IloNumExpr var1, int var2) throws IloException {
    logger.error(notSupportedError + " getValue");
    throw new IloException(notSupportedError);
  }

  @Override
  public double[] getValues(IloLPMatrix matrix,
                            int start,
                            int num,
                            int soln)
          throws IloCplex.UnknownObjectException,
          IloException {
    logger.error(notSupportedError + " getValues");
    throw new IloException(notSupportedError);
  }

  @Override
  public double getSolnPoolMeanObjValue() throws IloException {
    logger.error(notSupportedError + " getSolnPoolMeanObjValue");
    throw new IloException(notSupportedError);
  }

  @Override
  public int getSolnPoolNsolns() throws IloException {
    logger.error(notSupportedError + " getSolnPoolNsolns");
    throw new IloException(notSupportedError);
  }

  @Override
  public int getSolnPoolNreplaced() throws IloException {
    logger.error(notSupportedError+" getSolnPoolNreplaced");
    throw new IloException(notSupportedError);
  }

  @Override
  public void delSolnPoolSoln(int var1) throws IloException {
    logger.error(notSupportedError+" delSolnPoolSoln");
    throw new IloException(notSupportedError);
  }

  @Override
  public void delSolnPoolSolns(int var1, int var2) throws IloException {
    logger.error(notSupportedError+" delSolnPoolSolns");
    throw new IloException(notSupportedError);
  }

  @Override
  public double getCutoff() throws IloException {
    logger.error(notSupportedError+" getCutoff");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Aborter use(IloCplex.Aborter var1) throws IloException {
    logger.error(notSupportedError+" use aborter");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Aborter getAborter() {
    logger.error(notSupportedError+" getAborter");
    throw new RuntimeException(notSupportedError);  }

  @Override
  public void remove(IloCplex.Aborter var1) throws IloException {
    logger.error(notSupportedError+" remove aborter");
    throw new IloException(notSupportedError);  }



  @Override
  public IloCplex.Goal eqGoal(IloNumExpr var1, double var2) throws IloException {
    logger.error(notSupportedError+" eqGoal");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Goal eqGoal(IloNumExpr var1, IloNumExpr var2) throws IloException {
    logger.error(notSupportedError+" eqGoal");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Goal eqGoal(double var1, IloNumExpr var3) throws IloException {
    logger.error(notSupportedError+" eqGoal");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Goal geGoal(IloNumExpr var1, double var2) throws IloException {
    logger.error(notSupportedError+" geGoal");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Goal geGoal(IloNumExpr var1, IloNumExpr var2) throws IloException {
    logger.error(notSupportedError+" geGoal");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Goal geGoal(double var1, IloNumExpr var3) throws IloException {
    logger.error(notSupportedError+" geGoal");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Goal leGoal(IloNumExpr var1, double var2) throws IloException {
    logger.error(notSupportedError+" leGoal");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Goal leGoal(IloNumExpr var1, IloNumExpr var2) throws IloException {
    logger.error(notSupportedError+" leGoal");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.Goal leGoal(double var1, IloNumExpr var3) throws IloException {
    logger.error(notSupportedError+" leGoal");
    throw new IloException(notSupportedError);
  }


  @Override
  public int getNcrossPExch() {
    logger.error(notSupportedError+" getNcrossPExch");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public int getNcrossPPush() {
    logger.error(notSupportedError+" getNcrossPPush");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public int getNdualSuperbasics() {
    logger.error(notSupportedError+" getNdualSuperbasics");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public int getNprimalSuperbasics() {
    logger.error(notSupportedError+" getNprimalSuperbasics");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public int getNnodes() {
    logger.error(notSupportedError+" getNnodes");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public int getNnodesLeft() {
    logger.error(notSupportedError+" getNnodesLeft");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public long getNiterations64() {
    logger.error(notSupportedError+" getNiterations64");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public long getNphaseOneIterations64() {
    logger.error(notSupportedError+" getNphaseOneIterations64");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public long getNbarrierIterations64() {
    logger.error(notSupportedError+" getNbarrierIterations64");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public long getNsiftingIterations64() {
    logger.error(notSupportedError+" getNsiftingIterations64");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public long getNsiftingPhaseOneIterations64() {
    logger.error(notSupportedError+" getNsiftingPhaseOneIterations64");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public long getNcrossDExch64() {
    logger.error(notSupportedError+" getNcrossDExch64");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public long getNcrossDPush64() {
    logger.error(notSupportedError+" getNcrossDPush64");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public long getNcrossPExch64() {
    logger.error(notSupportedError+" getNcrossPExch64");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public long getNcrossPPush64() {
    logger.error(notSupportedError+" getNcrossPPush64");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public double getInfeasibility(IloNumVar var1) throws IloCplex.UnknownObjectException, IloException {
    logger.error(notSupportedError+" getInfeasibility");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.BasisStatus getBasisStatus(IloNumVar var1) throws IloException {
    logger.error(notSupportedError+" getBasisStatus");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.BasisStatus[] getBasisStatuses(IloNumVar[] var1) throws IloException {
    logger.error(notSupportedError+" getBasisStatuses");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.BasisStatus[] getBasisStatuses(IloNumVar[] var1, int var2, int var3) throws IloException {
    logger.error(notSupportedError+" getBasisStatuses");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.BasisStatus getBasisStatus(IloConstraint var1) throws IloException {
    logger.error(notSupportedError+" getBasisStatus");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.BasisStatus[] getBasisStatuses(IloConstraint[] var1) throws IloException {
    logger.error(notSupportedError+" getBasisStatuses");
    throw new IloException(notSupportedError);
  }

  @Override
  public IloCplex.BasisStatus[] getBasisStatuses(IloConstraint[] var1, int var2, int var3) throws IloException {
    logger.error(notSupportedError+" getBasisStatuses");
    throw new IloException(notSupportedError);
  }

  @Override
  public int getNMIPStarts() throws IloException {
    logger.error(notSupportedError+" getNMIPStarts");
    throw new IloException(notSupportedError);
  }

  @Override
  public void getBoundSA(double[] var1, double[] var2, double[] var3, double[] var4, IloNumVar[] var5, int var6, int var7) throws IloException {
    logger.error(notSupportedError+" getBoundSA");
    throw new IloException(notSupportedError);
  }

  @Override
  public void getObjSA(double[] var1, double[] var2, IloNumVar[] var3, int var4, int var5) throws IloException {
    logger.error(notSupportedError+" getObjSA");
    throw new IloException(notSupportedError);
  }

  @Override
  public void getRangeSA(double[] var1, double[] var2, double[] var3, double[] var4, IloRange[] var5, int var6, int var7) throws IloException {
    logger.error(notSupportedError+" getRangeSA");
    throw new IloException(notSupportedError);
  }

  @Override
  public void getRHSSA(double[] var1, double[] var2, IloRange[] var3, int var4, int var5) throws IloException {
    logger.error(notSupportedError+" getRHSSA");
    throw new IloException(notSupportedError);
  }

  @Override
  public void getRHSSA(double[] var1, double[] var2, IloLPMatrix var3, int var4, int var5) throws IloException {
    logger.error(notSupportedError+" getRHSSA");
    throw new IloException(notSupportedError);
  }

  @Override
  public int getNcuts(int var1) throws IloException {
    logger.error(notSupportedError+" getNcuts");
    throw new IloException(notSupportedError);
  }

  @Override
  public int getPriority(IloNumVar var1) throws IloException {
    logger.error(notSupportedError+" getPriority");
    throw new IloException(notSupportedError);
  }

  @Override
  public int[] getPriorities(IloNumVar[] var1, int var2, int var3) throws IloException {
    logger.error(notSupportedError+" getPriorities");
    throw new IloException(notSupportedError);
  }

  @Override
  public double dualFarkas(IloConstraint[] var1, double[] var2) throws IloException {
    logger.error(notSupportedError+" dualFarkas");
    throw new IloException(notSupportedError);
  }


  @Override
  public boolean refineMIPStartConflict(int var1, IloConstraint[] var2, double[] var3) throws IloException {
    logger.error(notSupportedError + " refineMIPStartConflict");
    throw new IloException(notSupportedError);
  }

  @Override
  public boolean refineMIPStartConflict(int var1, IloConstraint[] var2, double[] var3, int var4, int var5) throws IloException {
    logger.error(notSupportedError + " refineMIPStartConflict");
    throw new IloException(notSupportedError);
  }



  @Override
  public boolean solve(IloCplex.ParameterSet[] var1) throws IloException{
    logger.error(notSupportedError + " solve with parameterset list");
    throw new IloException(notSupportedError);
  }
  @Override
  public IloLinearNumExpr getQCDSlack(IloRange var1) throws IloException {
    logger.error(notSupportedError + " getQCDSlack");
    throw new IloException(notSupportedError);
  }
  @Override
  public int tuneParam() throws IloException {
    logger.error(notSupportedError + " tuneParam");
    throw new IloException(notSupportedError);
  }

  @Override
  public int tuneParam(IloCplex.ParameterSet var1) throws IloException {
    logger.error(notSupportedError + " tuneParam");
    throw new IloException(notSupportedError);
  }

  @Override
  public int tuneParam(String[] var1) throws IloException {
    logger.error(notSupportedError + " tuneParam");
    throw new IloException(notSupportedError);
  }

  @Override
  public int tuneParam(String[] var1, IloCplex.ParameterSet var2) throws IloException {
    logger.error(notSupportedError + " tuneParam");
    throw new IloException(notSupportedError);
  }

  @Override
  public void use(IloCplex.Callback var1) throws IloException {
    logger.error(notSupportedError + " use IloCplex.Callback");
    throw new IloException(notSupportedError);
  }

  @Override
  public void remove(IloCplex.Callback var1) throws IloException {
    logger.error(notSupportedError + " remove IloCplex.Callback");
    throw new IloException(notSupportedError);
  }

  @Override
  public void clearCallbacks() throws IloException {
    logger.error(notSupportedError + " clearCallbacks");
    throw new IloException(notSupportedError);
  }


  @Override
  public void qpIndefCertificate(IloNumVar[] var1, double[] var2) throws IloException {
    logger.error(notSupportedError + " qpIndefCertificate");
    throw new IloException(notSupportedError);
  }

  @Override
  public void protectVariables(IloNumVar[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException {
    logger.error(notSupportedError + " protectVariables");
    throw new IloException(notSupportedError);
  }

  @Override
  public void protectVariables(IloNumVar[] var1) throws IloCplex.UnknownObjectException, IloException {
    logger.error(notSupportedError + " protectVariables");
    throw new IloException(notSupportedError);
  }

  @Override
  public double[] getAX(IloLPMatrix var1) throws IloCplex.UnknownObjectException, IloException {
    logger.error(notSupportedError + " getAX");
    throw new IloException(notSupportedError);
  }

  @Override
  public double[] getAX(IloLPMatrix var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException {
    logger.error(notSupportedError + " getAX");
    throw new IloException(notSupportedError);
  }

  @Override
  public double getAX(IloRange var1) throws IloCplex.UnknownObjectException, IloException {
    logger.error(notSupportedError + " getAX");
    throw new IloException(notSupportedError);
  }

  @Override
  public double[] getAX(IloRange[] var1) throws IloCplex.UnknownObjectException, IloException {
    logger.error(notSupportedError + " getAX");
    throw new IloException(notSupportedError);
  }

  @Override
  public double[] getAX(IloRange[] var1, int var2, int var3) throws IloCplex.UnknownObjectException, IloException {
    logger.error(notSupportedError + " getAX");
    throw new IloException(notSupportedError);
  }
  @Override
  public IloCplex.Quality getQuality(IloCplex.QualityType var1) throws IloException {
    logger.error(notSupportedError + " getQuality");
    throw new IloException(notSupportedError);
  }
  @Override
  public IloCplex.Quality getQuality(IloCplex.QualityType var1, int var2) throws IloException {
    logger.error(notSupportedError + " getQuality");
    throw new IloException(notSupportedError);
  }
  public IloCplex.BranchDirection getDirection(IloNumVar var1) throws IloException {
    logger.error(notSupportedError + " getDirection");
    throw new IloException(notSupportedError);
  }
  @Override
  public IloCplex.BranchDirection[] getDirections(IloNumVar[] var1) throws IloException {
    logger.error(notSupportedError + " getDirections");
    throw new IloException(notSupportedError);
  }
  @Override
  public IloCplex.BranchDirection[] getDirections(IloNumVar[] var1, int var2, int var3) throws IloException {
    logger.error(notSupportedError + " getDirections");
    throw new IloException(notSupportedError);
  }
  @Override
  public IloLinearNumExpr getRay() throws IloException {
    logger.error(notSupportedError + " getRay");
    throw new IloException(notSupportedError);
  }
  @Override
  public IloCopyable getDiverging() throws IloException {
    logger.error(notSupportedError + " getDiverging");
    throw new IloException(notSupportedError);
  }

  @Override
  public int getNLCs() {
    logger.error(notSupportedError + " getNLCs");
    throw new RuntimeException(notSupportedError);
  }

  @Override
  public int getNUCs() {
    logger.error(notSupportedError + " getNUCs");
    throw new RuntimeException(notSupportedError);
  }

}
