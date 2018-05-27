package at.mhofer.mathprog.kmst.data;

public class Edge {

	private int v1;
	
	private int v2;

	private int weight;

	public Edge(int v1, int v2, int weight) {
		super();
		assert v1 != v2;
		this.v1 = v1;
		this.v2 = v2;
		this.weight = weight;
	}

	public int getV1() {
		return v1;
	}

	public int getV2() {
		return v2;
	}

	public int getWeight() {
		return weight;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + v1;
		result = prime * result + v2;
		result = prime * result + weight;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
		if (v1 != other.v1)
			return false;
		if (v2 != other.v2)
			return false;
		if (weight != other.weight)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return v1 + " " + v2 + " " + weight;
	}

}
