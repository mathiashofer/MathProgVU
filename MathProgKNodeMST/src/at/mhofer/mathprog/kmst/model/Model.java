package at.mhofer.mathprog.kmst.model;

import java.io.OutputStream;

import ilog.concert.IloException;

/**
 * Common interface for the k-Node MST CPLEX models
 * 
 * @author Mathias
 *
 */
public interface Model {

	public void build() throws IloException;
	
	public void setOut(OutputStream out);
	
	public boolean solve() throws IloException;
	
}
