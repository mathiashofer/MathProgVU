package at.mhofer.mathprog.kmst.model;

import ilog.concert.IloException;

/**
 * Common interface for the k-Node MST CPLEX models
 * 
 * @author Mathias
 *
 */
public interface Model {

	public void build() throws IloException;
	
	public boolean solve() throws IloException;
	
}
