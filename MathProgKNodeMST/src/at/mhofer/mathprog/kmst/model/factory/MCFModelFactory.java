package at.mhofer.mathprog.kmst.model.factory;

import at.mhofer.mathprog.kmst.data.Instance;
import at.mhofer.mathprog.kmst.model.MCFModel;
import at.mhofer.mathprog.kmst.model.Model;

public class MCFModelFactory implements ModelFactory{

	@Override
	public Model create(Instance instance, int k) {
		return new MCFModel(instance, k);
	}
	
}
