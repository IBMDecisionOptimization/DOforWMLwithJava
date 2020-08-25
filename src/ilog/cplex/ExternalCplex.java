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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import ilog.concert.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Base class for external solves.
 * @author daniel.junglas@de.ibm.com
 *
 */
public abstract class ExternalCplex extends IloCplex {
  private static final long serialVersionUID = 1;
  
  /** In order to use an external solver, each variable must have an explicit name.
   * This enumeration describes the different naming strategies we support.
   * @author daniel.junglas@de.ibm.com
   *
   */
  public enum NamingStrategy {
    /** Create names for each variable.
     * Will temporarily change variable names during a solve.
     */
    MAKE_NAMES,
    /** Use the names set by the user.
     *  Will produce an error if there are unnamed variables or duplicate names. 
     */
    USE_NAMES
  };
  
  private final NamingStrategy namingStrategy;
  
  public ExternalCplex() throws IloException { this(NamingStrategy.MAKE_NAMES); }
  
  public ExternalCplex(NamingStrategy namingStrategy) throws IloException {
    super();
    this.namingStrategy = namingStrategy;
  }

  private void addVariable(HashMap<String,IloNumVar> name2var, HashMap<IloNumVar,String> oldNames, IloNumVar v) throws IloException {
    String name = v.getName();
    switch (namingStrategy) {
    case MAKE_NAMES:
      if (!oldNames.containsKey(v)) {
        oldNames.put(v, name); // even if name == null!
        name = String.format("%x", oldNames.size());
        v.setName(name);
        name2var.put(name, v);
      }
      break;
    case USE_NAMES:
      if (name == null)
        throw new IloException("Variable without name");
      final IloNumVar u = name2var.get(name);
      if (u != null && u != v)
        throw new IloException("Duplicate variable name " + name);
      else if (u == null)
        name2var.put(name, v);
      break;
    }
  }

  private void addRange(HashMap<String, IloRange> name2rng, HashMap<IloRange,String> oldNames, IloRange v) throws IloException {
    String name = v.getName();
    switch (namingStrategy) {
      case MAKE_NAMES:
        if (!oldNames.containsKey(v)) {
          oldNames.put(v, name); // even if name == null!
          name = String.format("%x", oldNames.size());
          v.setName(name);
          name2rng.put(name, v);
        }
        break;
      case USE_NAMES:
        if (name == null)
          throw new IloException("Variable without name");
        final IloRange u = name2rng.get(name);
        if (u != null && u != v)
          throw new IloException("Duplicate variable name " + name);
        else if (u == null)
          name2rng.put(name, v);
        break;
    }
  }

  private void addExpr(HashMap<String,IloNumVar> name2var, HashMap<IloNumVar,String> oldNames, IloNumExpr e) throws IloException {
    boolean ok = false;
    if (e instanceof IloLinearNumExpr) {
      final IloLinearNumExpr l = (IloLinearNumExpr)e;
      ok = true;
      for (IloLinearNumExprIterator it = l.linearIterator(); it.hasNext(); /* nothing */)
        addVariable(name2var, oldNames, it.nextNumVar());
    }
    if (e instanceof IloLinearIntExpr) {
      final IloLinearIntExpr l = (IloLinearIntExpr)e;
      ok = true;
      for (IloLinearIntExprIterator it = l.linearIterator(); it.hasNext(); /* nothing */)
        addVariable(name2var, oldNames, it.nextIntVar());
    }
    if (!ok)
      throw new IloException("Could not parse " + e);
  }
  
  /** Solution information.
   * Instances of this class contain solution information that can be obtained from a CPLEX <code>.sol</code> file.
   * @author daniel.junglas@de.ibm.com
   *
   */
  protected static class Solution {
    boolean feasible = false;
    /** Map variable names to values. */
    public HashMap<String,Double> name2val = new HashMap<String,Double>();
    /** Map variable objects to values. */
    public HashMap<IloNumVar, Double> var2val = new HashMap<IloNumVar, Double>();
    /** Map range names to values. */
    public HashMap<String,Double> name2dual = new HashMap<String,Double>();
    /** Map range objects to values. */
    public HashMap<IloRange, Double> rng2dual = new HashMap<IloRange, Double>();
    /** Objective value of solution. */
    public double objective = Double.NaN;
    /** CPLEX status. */
    public int status = -1;
    /** Primal feasible? */
    public boolean pfeas = false;
    /** Dual feasible? */
    public boolean dfeas = false;
    
    public Solution() {}
    public Solution(File solutionXml, Set<String> knownVariables, Set<String> knownConstraints) throws IOException {
      this();
      parse(solutionXml, knownVariables, knownConstraints);
    }
    public Solution(InputStream solutionXml, Set<String> knownVariables, Set<String> knownConstraints) throws IOException {
      this();
      parse(solutionXml, knownVariables, knownConstraints);
    }
    
    public void reset() {
      feasible = false;
      name2val = new HashMap<String, Double>();
      var2val = new HashMap<IloNumVar, Double>();
      objective = Double.NaN;
      status = -1;
      pfeas = false;
      dfeas = false;
    }
    
    /** Parse a CPLEX <code>.sol</code> file.
     * See {@link #parse(InputStream, Set, Set)} for details.
     */
    public void parse(File solutionXml, Set<String> knownVariables, Set<String> knownConstraints) throws IOException {
      reset();
      try (FileInputStream fis = new FileInputStream(solutionXml)) {
        parse(fis, knownVariables, knownConstraints);
      }
    }
    
    private enum ParserState {
      INITIAL, SOLUTION, HEADER, QUALITY, VARIABLES, LINEAR_CONSTRAINTS,
      /** Unknown children of a <CPLEXSolution> element. */
      UNKNOWN,
      FINISHED
    };
    
    private static String[] getAttributes(XMLStreamReader reader, String... attrs) {
      final String[] ret = new String[attrs.length];
      final Map<String,String> attrMap = new HashMap<String,String>();
      for (final String s : attrs)
        attrMap.put(s, null);
      final int nattrs = reader.getAttributeCount();
      for (int i = 0; i < nattrs; ++i) {
        final String name = reader.getAttributeLocalName(i);
        if (attrMap.containsKey(name))
          attrMap.put(name, reader.getAttributeValue(i));
      }
      for (int i = 0; i < attrs.length; ++i)
        ret[i] = attrMap.get(attrs[i]);
      return ret;
    }
    
    /** Parse a CPLEX <code>.sol</code> file.
     * This is more memory and speed efficient than {@link #parse2(InputStream, Set)}, but also more complicated code.
     * @param solutionXml The CPLEX <code>.sol</code> file to parse.
     * @param knownVariables The names of the variables for which values should be extracted from <code>solutionXml</code>.
     * @throws IOException If an input/output error occurs or mandatory solution information is missing.
     */
    public void parse(InputStream solutionXml, Set<String> knownVariables, Set<String> knownConstraints) throws IOException {
      reset();
      boolean ok = false;
      try {
        final String MALFORMED_XML = "Malformed XML";
        final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(solutionXml);
        final Vector<String> unknownStack = new Vector<String>(4); /* Parsing anything but variables, header, quality
                                                                    * in a CPLEXSolution element. */
        ParserState state = ParserState.INITIAL;
        int solnum = -1; // Solution number for error messages.
        while (reader.hasNext() && state != ParserState.FINISHED) {
          String element;
          String[] attrs;
          switch (reader.next()) {
          case XMLStreamConstants.START_DOCUMENT: /* nothing */ break;
          case XMLStreamConstants.START_ELEMENT:
            element = reader.getLocalName();
            switch (state) {
            case INITIAL:
              // First element must be <CPLEXSolution>
              if (element.equals("CPLEXSolution")) {
                ++solnum;
                state = ParserState.SOLUTION;
                this.feasible = true;
              }
              else
                throw new IOException(MALFORMED_XML);
              break;
            case HEADER: throw new IOException(MALFORMED_XML);
            case QUALITY: throw new IOException(MALFORMED_XML);
            case SOLUTION:
              if (element.equals("header")) {
                state = ParserState.HEADER;
                attrs = getAttributes(reader, "objectiveValue", "solutionStatusValue", "primalFeasible", "dualFeasible");
                if (attrs[0] == null)
                  throw new IOException("No objective for solution " + solnum);
                this.objective = Double.parseDouble(attrs[0]);
                if (attrs[1] == null)
                  throw new IOException("No solution status for solution " + solnum);
                this.status = Integer.parseInt(attrs[1]);
                this.pfeas = attrs[2] != null && Integer.parseInt(attrs[2]) != 0;
                this.dfeas = attrs[3] != null && Integer.parseInt(attrs[3]) != 0;
              }
              else if (element.equals("quality")) {
                state = ParserState.QUALITY;
              }
              else if (element.equals("variables")) {
                state = ParserState.VARIABLES;
              }
              else if (element.equals("linearConstraints")) {
                state = ParserState.LINEAR_CONSTRAINTS;
              }
              else {
                state = ParserState.UNKNOWN;
                unknownStack.add(element);
              }
              break;
            case VARIABLES:
              if (!element.equals("variable"))
                throw new IOException(MALFORMED_XML);
              attrs = getAttributes(reader, "name", "value");
              if (attrs[0] == null)
                throw new IOException("Variable without name in solution file for solution " + solnum);
              if (attrs[1] == null)
                throw new IOException("Variable without value in solution file for solution " + solnum);
              if (knownVariables.contains(attrs[0]))
                this.name2val.put(attrs[0], new Double(attrs[1]));
              break;
            case LINEAR_CONSTRAINTS:
              if (!element.equals("constraint"))
                throw new IOException(MALFORMED_XML);
              attrs = getAttributes(reader, "name", "dual");
              if (attrs[0] == null)
                throw new IOException("Constraint without name in solution file for solution " + solnum);
              if (attrs[1] == null) {
                // CAN BE MIP
                //throw new IOException("Constraint without dual value in solution file for solution " + solnum);
              } else {
                if (knownConstraints.contains(attrs[0]))
                  this.name2dual.put(attrs[0], new Double(attrs[1]));
              }
              break;
            case UNKNOWN:
              unknownStack.add(element);
              break;
            case FINISHED:
              // this cannot happen
              throw new IOException(MALFORMED_XML);
            }
            break;
          case XMLStreamConstants.END_ELEMENT:
            element = reader.getLocalName();
            switch (state) {
            case INITIAL:
              // This should not happen since we stop after the first solution
              throw new IOException(MALFORMED_XML);
            case SOLUTION:
              if (!element.equals("CPLEXSolution"))
                throw new IOException(MALFORMED_XML);
              // We only parse the very first solution in the file.
              state = ParserState.FINISHED;
              break;
            case HEADER:
              if (!element.equals("header"))
                throw new IOException(MALFORMED_XML);
              state = ParserState.SOLUTION;
              break;
            case QUALITY:
              if (!element.equals("quality"))
                throw new IOException(MALFORMED_XML);
              state = ParserState.SOLUTION;
              break;
            case VARIABLES:
              if (element.equals("variable")) { /* nothing */ }
              else if (element.equals("variables")) {
                state = ParserState.SOLUTION;
              }
              else
                throw new IOException(MALFORMED_XML);
              break;
            case LINEAR_CONSTRAINTS:
              if (element.equals("constraint")) { /* nothing */ }
              else if (element.equals("linearConstraints")) {
                state = ParserState.SOLUTION;
              }
              else
                throw new IOException(MALFORMED_XML);
              break;
            case UNKNOWN:
              if (unknownStack.size() == 0 || !element.equals(unknownStack.lastElement()))
                throw new IOException(MALFORMED_XML);
              unknownStack.remove(unknownStack.size() - 1);
              if (unknownStack.size() == 0)
                state = ParserState.SOLUTION;
              break;
            case FINISHED:
              // This cannot happen
              throw new IOException(MALFORMED_XML);
            } 
            break;
          case XMLStreamConstants.END_DOCUMENT: /* nothing */ break;
          }
        }
        ok = true;
      }
      catch (XMLStreamException e) { throw new IOException(e); }
      finally {
        if (!ok)
          reset();
      }
    }

    /** Parse a CPLEX <code>.sol</code> file.
     * This is not as fast and memory efficient as {@link #parse(InputStream, Set, Set)} but much simpler code.
     * @param solutionXml The CPLEX <code>.sol</code> file to parse.
     * @param knownVariables The names of the variables for which values should be extracted from <code>solutionXml</code>.
     * @throws IOException If an input/output error occurs or mandatory solution information is missing.
     */
    public void parse2(InputStream solutionXml, Set<String> knownVariables) throws IOException {
      /** TODO: For large solution files we probably want to use an XmlInputStream and parse the file
       *        element by element instead of fetching the full document in one shot.
       */
      reset();
      boolean ok = false;
      try {
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(solutionXml);
        final XPath xpath = XPathFactory.newInstance().newXPath();
        Node root = (Node)xpath.compile("/CPLEXSolutions").evaluate(doc, XPathConstants.NODE);
        if (root == null)
          root = doc;
        final NodeList solutions = (NodeList)xpath.compile("CPLEXSolution").evaluate(root, XPathConstants.NODESET);
        for (int i = 0; i < solutions.getLength(); ++i) {
          this.feasible = true;
          final Node sol = solutions.item(i);
          final Node header = (Node)xpath.compile("header").evaluate(sol, XPathConstants.NODE);
          if (header == null)
            throw new IOException("No header for solution " + i);
          final NamedNodeMap headerAttrs = header.getAttributes();
          final Node obj = headerAttrs.getNamedItem("objectiveValue");
          if (obj == null)
            throw new IOException("No objective for solution " + i);
          this.objective = Double.parseDouble(obj.getNodeValue());
          final Node stat = headerAttrs.getNamedItem("solutionStatusValue");
          if (stat == null)
            throw new IOException("No solution status for solution " + i);
          this.status = Integer.parseInt(stat.getNodeValue());
          final Node pfeas = headerAttrs.getNamedItem("primalFeasible");
          this.pfeas = pfeas != null && Integer.parseInt(pfeas.getNodeValue()) != 0;
          final Node dfeas = headerAttrs.getNamedItem("dualFeasible");
          this.dfeas = dfeas != null && Integer.parseInt(dfeas.getNodeValue()) != 0;
          final NodeList vars = (NodeList)xpath.compile("variables/variable").evaluate(sol, XPathConstants.NODESET);
          for (int j = 0; j < vars.getLength(); ++j) {
            final Node var = vars.item(j);
            final NamedNodeMap attrs = var.getAttributes();
            final Node name = attrs.getNamedItem("name");
            if (name == null)
              throw new IOException("Variable without name in solution file for solution " + i);
            final Node value = attrs.getNamedItem("value");
            if (value == null)
              throw new IOException("Variable without value in solution file for solution " + i);
            final String varName = name.getNodeValue();
            if (knownVariables.contains(varName))
              this.name2val.put(varName, new Double(value.getNodeValue()));
          }
          break; // We only parse the first solution in the file.
        }
        ok = true;
      }
      catch (SAXException e) { throw new IOException(e); }
      catch (ParserConfigurationException e) { throw new IOException(e); }
      catch (XPathExpressionException e) { throw new IOException(e); }
      finally {
        if (!ok)
          reset();
      }
    }
  }

  private Solution result = null;

  public boolean solve() throws IloException {
    result = null;
    
    HashMap<IloNumVar,String> oldVarNames = null;
    if (namingStrategy == NamingStrategy.MAKE_NAMES)
      oldVarNames = new HashMap<IloNumVar, String>();

    HashMap<IloRange,String> oldRngNames = null;
    if (namingStrategy == NamingStrategy.MAKE_NAMES)
      oldRngNames = new HashMap<IloRange, String>();


    try {
      // In order to consume a solution file, _all_ variables must have
      // a name! Go through the model and collect all variables, thereby
      // checking that they have a name and names are unique.
      final HashMap<String,IloNumVar> vars = new HashMap<String,IloNumVar>();
      final HashMap<String,IloRange> rngs = new HashMap<String,IloRange>();
      for (Iterator<?> it = iterator(); it.hasNext(); /* nothing */) {
        final Object o = it.next();
        if (o instanceof IloLPMatrix) {
          for (final IloNumVar v : ((IloLPMatrix)o).getNumVars())
            addVariable(vars, oldVarNames, v);
        }
        else if (o instanceof IloObjective) {
          addExpr(vars, oldVarNames, ((IloObjective)o).getExpr());
        }
        else if (o instanceof IloRange) {
          addRange(rngs, oldRngNames, (IloRange)o);
          addExpr(vars, oldVarNames, ((IloRange)o).getExpr());
        }
        else if (o instanceof IloNumVar) {
          addVariable(vars, oldVarNames, (IloNumVar)o);
        }
        else if (o instanceof IloConversion) {
          // ignore
        } else
          throw new IloException("Cannot handle " + o);
      }

      // Now perform the solve
      result = externalSolve(vars.keySet(), rngs.keySet());

      // Transfer non-zeros indexed by name to non-zeros indexed by object.
      for (final Map.Entry<String, Double> e : result.name2val.entrySet()) {
        result.var2val.put(vars.get(e.getKey()), e.getValue());
      }
      result.name2val.clear();

      // Transfer non-zeros indexed by name to non-zeros indexed by object.
      for (final Map.Entry<String, Double> e : result.name2dual.entrySet()) {
        result.rng2dual.put(rngs.get(e.getKey()), e.getValue());
      }
      result.name2dual.clear();
      
      return result.feasible;
    }
    finally {
      // Restore original names if necessary.
      if (oldVarNames != null)
        for (Map.Entry<IloNumVar, String> e : oldVarNames.entrySet())
          e.getKey().setName(e.getValue());

      // Restore original names if necessary.
      if (oldRngNames != null)
        for (Map.Entry<IloRange, String> e : oldRngNames.entrySet())
          e.getKey().setName(e.getValue());
    }
  }

  /** Perform an external solve.
   * The function must not return <code>null</code>.
   * All fields but {@link Solution#var2val} must be setup in the returned {@link Solution} instance.
   * Field {@link Solution#var2val} will be setup in {@link #solve()} from {@link Solution#name2val}. The latter
   * will also be cleared in {@link #solve()}.
   * @param variables The names of variables known to the solver.
   * @return Solution information for the solve.
   * @throws IloException if anything goes wrong.
   */
  protected abstract Solution externalSolve(Set<String> variables, Set<String> ranges) throws IloException;
  
  // Below we overwrite a bunch of IloCplex functions that query solutions.
  // Add your own overwrites if you need more.
  
  @Override
  public double getObjValue() throws IloException {
    if (result == null)
      throw new IloException("No solution available");
    return result.objective;
  }
  
  @Override
  public double getValue(IloNumVar v) throws IloException {
    if (result == null)
      throw new IloException("No solution available");
    final Double d = result.var2val.get(v);
    return (d == null) ? 0.0 : d.doubleValue();
  }
  
  @Override
  public double[] getValues(IloNumVar[] v) throws IloException {
    if (result == null)
      throw new IloException("No solution available");
    final double[] ret = new double[v.length];
    for (int i = 0; i < v.length; ++i) {
      final Double d = result.var2val.get(v[i]);
      ret[i] = (d == null) ? 0.0 : d.doubleValue();
    }
    return ret;
  }

  @Override
  public double getDual(IloRange r) throws IloException {
    if (result == null)
      throw new IloException("No solution available");
    final Double d = result.rng2dual.get(r);
    return (d == null) ? 0.0 : d.doubleValue();
  }

  @Override
  public double[] getDuals(IloRange[] r) throws IloException {
    if (result == null)
      throw new IloException("No solution available");
    final double[] ret = new double[r.length];
    for (int i = 0; i < r.length; ++i) {
      final Double d = result.rng2dual.get(r[i]);
      ret[i] = (d == null) ? 0.0 : d.doubleValue();
    }
    return ret;
  }

  @Override
  public Status getStatus() throws IloException {
    if (result == null)
      return Status.Unknown;
    return makeStatus(getCplexStatus().getValue(), result.pfeas, result.dfeas);
  }
  
  @Override
  public CplexStatus getCplexStatus() throws IloException {
    if (result == null)
      return CplexStatus.Unknown;
    return CplexStatus.getStatus(result.status);
  }
}
