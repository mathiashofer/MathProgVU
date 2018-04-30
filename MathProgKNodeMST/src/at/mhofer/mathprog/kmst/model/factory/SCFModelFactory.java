package at.mhofer.mathprog.kmst.model.factory;

import at.mhofer.mathprog.kmst.data.Instance;
import at.mhofer.mathprog.kmst.model.Model;
import at.mhofer.mathprog.kmst.model.SCFModel;

public class SCFModelFactory implements ModelFactory{

	@Override
	public Model create(Instance instance, int k) {
		return new SCFModel(instance, k);
	}

}
