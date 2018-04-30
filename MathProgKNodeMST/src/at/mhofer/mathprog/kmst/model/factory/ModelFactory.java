package at.mhofer.mathprog.kmst.model.factory;

import at.mhofer.mathprog.kmst.data.Instance;
import at.mhofer.mathprog.kmst.model.Model;

public interface ModelFactory {

	public Model create(Instance instance, int k);
	
}
