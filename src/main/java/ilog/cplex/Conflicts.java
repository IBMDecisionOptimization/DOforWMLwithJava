package ilog.cplex;

import ilog.concert.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Vector;

public class Conflicts {
    private static final Logger logger = LogManager.getLogger();

    private enum Type {LB, UB, LIN, QUAD, IND, SOS}

    public String getName(IloConstraint obj) throws IloException {
        if (obj.getName() == null) {
            logger.error("Missing name for relaxing " + obj);
            throw new IloException("Missing name for relaxing " + obj);
        }
        return obj.getName();
    }

    public Type getType(IloConstraint obj) throws IloException {
        if (obj instanceof IloSOS1 || obj instanceof IloSOS2) return Type.SOS;
        if (obj instanceof CpxIfThen) return Type.IND;
        if (obj instanceof IloRange) {
            IloRange range = (IloRange) obj;
            IloNumExpr expr = range.getExpr();
            if (expr instanceof CpxQLExpr) {
                CpxQLExpr x = (CpxQLExpr) expr;
                if (x._quad != null) {
                    if (x.getQuadVar1() != null)
                        return Type.QUAD;
                    else return Type.LIN;
                }
                return Type.LIN;
            }
            return Type.LIN;
        }
        if (obj instanceof CpxAnd || obj instanceof CpxOr || obj instanceof CpxNot) {
            logger.error("Type of constraint is not supported by WML conflict refiner: " + obj);
            throw new IloException("Type of constraint is not supported by WML conflict refiner: " + obj);
        }
        return Type.LIN;
    }

    private final HashMap<Double, Vector<IloConstraint>> elems = new HashMap<>();//new Vector<Elem>();

    public Conflicts() {
    }

    public int getSize() {
        int size = 0;
        for (Double d : elems.keySet()) {
            size = size + elems.get(d).size();
        }
        return size;
    }

    public void add(IloConstraint o, double p) throws IloException {
        if (o instanceof CpxAnd || o instanceof CpxOr || o instanceof CpxNot) {
            logger.error("Type of constraint is not supported by WML conflict refiner: " + o);
            throw new IloException("Type of constraint is not supported by WML conflict refiner: " + o);
        }
        if (!elems.containsKey(p))
            elems.put(p, new Vector<>());
        elems.get(p).add(o);
    }

    private void fillBuffer(StringBuffer buffer, Vector<IloConstraint> conflict, double preference) throws IloException {
        buffer.append("<group preference='").append(preference).append("'>");
        for (final IloConstraint e : conflict)
            buffer.append("<con").append(" name='").append(getName(e)).append("'").append(" type='").append(getType(e).name().toLowerCase()).append("'/>");
        buffer.append("</group>");
    }

    public byte[] makeFile() throws IloException {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("<CPLEXRefineconflictext resultNames='true'>");
        for (Double d : elems.keySet()) {
            fillBuffer(buffer, elems.get(d), d);
        }
        buffer.append("</CPLEXRefineconflictext>");
        String c = buffer.toString();
        logger.info("Conflict file is " + c);
        return c.getBytes(StandardCharsets.UTF_8);
    }
}


