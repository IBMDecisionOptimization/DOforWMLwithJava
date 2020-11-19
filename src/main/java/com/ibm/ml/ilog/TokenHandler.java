package com.ibm.ml.ilog;

import ilog.concert.IloException;

public interface TokenHandler {
    /* Inits the token authenticator */
    public void initToken() throws IloException;
    /* Clean up method to call when the connector becomes useless */
    public void end();
}
