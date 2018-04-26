package at.mhofer.mathprog.kmst.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Instance {

	/**
	 * number of nodes
	 */
	private int numNodes;

	/**
	 * number of edges
	 */
	private int numEdges;

	/**
	 * array of edges
	 */
	private Edge[] edges;

	/**
	 * incident edges denoted by index in array edges
	 * 
	 * uses a list instead of an array since we would lose the generic type information otherwise
	 */
	private List<List<Integer>> incidentEdges;

	public Instance(int numNodes, int numEdges, Edge[] edges, List<List<Integer>> incidentEdges) {
		super();
		this.numNodes = numNodes;
		this.numEdges = numEdges;
		this.edges = edges;
		this.incidentEdges = incidentEdges;
	}

	public static Instance fromFile(File file) throws FileNotFoundException {
		Scanner scanner = new Scanner(file);
		
		int numNodes = scanner.nextInt();
		int numEdges = scanner.nextInt();
		
		Edge[] edges = new Edge[numEdges];
		List<List<Integer>> incidentEdges = new ArrayList<List<Integer>>(numNodes);		
		for (int i = 0; i < numNodes; i++) {
			incidentEdges.set(i, new LinkedList<Integer>());
		}
		
		for (int i = 0; i < numEdges; i++) {
			int id = scanner.nextInt();
			int v1 = scanner.nextInt();
			int v2 = scanner.nextInt();
			int weight = scanner.nextInt();
			
			Edge edge = new Edge(v1, v2, weight);
			edges[id] = edge;
			incidentEdges.get(v1).add(id);
			incidentEdges.get(v2).add(id);
		}
		scanner.close();
		
		return new Instance(numNodes, numEdges, edges, incidentEdges);
	}

	public int getNumNodes() {
		return numNodes;
	}

	public int getNumEdges() {
		return numEdges;
	}

	public Edge[] getEdges() {
		return edges;
	}

	public List<List<Integer>> getIncidentEdges() {
		return incidentEdges;
	}

}
