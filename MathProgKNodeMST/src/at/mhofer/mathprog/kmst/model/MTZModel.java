package at.mhofer.mathprog.kmst.model;

import java.util.List;

import at.mhofer.mathprog.kmst.data.Edge;
import at.mhofer.mathprog.kmst.data.Instance;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;

/**
 * Miller-Tucker-Zemlin subtour elimination
 * 
 * @author Mathias
 *
 */
public class MTZModel implements Model {

	private static final int ARTIFICIAL_ROOT = 0;

	private IloCplex cplex = null;

	private List<List<Integer>> incidenceEdges;

	private Edge[] edges;
	private Edge[] invEdges;

	private int numEdges;
	private int numNodes;

	private int k;

	public MTZModel(Instance instance, int k) {
		this.incidenceEdges = instance.getIncidentEdges();
		this.edges = instance.getEdges();
		this.invEdges = new Edge[instance.getNumEdges()];
		for (int i = 0; i < invEdges.length; i++) {
			Edge e = this.edges[i];
			this.invEdges[i] = new Edge(e.getV2(), e.getV1(), e.getWeight());
		}
		this.numEdges = instance.getNumEdges();
		this.numNodes = instance.getNumNodes();

		// add 1, since we have the artificial node and therefore an additional
		// edge
		this.k = k;
	}

	@Override
	public void build() throws IloException {
		cplex = new IloCplex();

		// Variables
		IloIntVar[] x = cplex.boolVarArray(numEdges);
		IloNumVar[] u = cplex.numVarArray(numNodes, 1, k);

		// Objective function
		IloLinearIntExpr obj = cplex.linearIntExpr();
		for (int i = 0; i < numEdges; i++) {
			Edge e = this.edges[i];
			obj.addTerm(x[i], e.getWeight());
		}
		cplex.addObjective(IloObjectiveSense.Minimize, obj);

		// Constraints
		for (int i = 0; i < numEdges; i++) {
			Edge e = this.edges[i];
			if (e.getV1() != 0 && e.getV2() != 0)
				cplex.addLe(cplex.sum(u[e.getV1()], x[i]),
						cplex.sum(u[e.getV2()], cplex.prod(k - 1, cplex.sum(1, cplex.negative(x[i])))));
		}
		
		cplex.addEq(u[0], 1);

		// C6: choose k edges and therefore k+1 nodes, which leads to k nodes
		// without the artificial root node
		cplex.addEq(cplex.sum(x), k, "C6");

		// C7: take only one of the artificial 0-weight edges
		IloLinearIntExpr c6 = cplex.linearIntExpr();
		for (int i = 0; i < numNodes - 1; i++) {
			c6.addTerm(x[i], 1);
		}
		cplex.addLe(c6, 1, "C7");
	}

	@Override
	public boolean solve() throws IloException {
		boolean ret = cplex.solve();
		System.out.println(cplex.getObjValue());
		return ret;
	}

}
