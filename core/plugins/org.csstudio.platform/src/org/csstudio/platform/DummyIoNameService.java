package org.csstudio.platform;

import java.util.ArrayList;
import java.util.List;

/**
 * Dummy implementation of {@link IoNameService}.
 * 
 * @author Sven Wende
 * 
 */
// FIXME: swende: Provide a real implementation
public class DummyIoNameService implements IoNameService {

	/**
	 *{@inheritDoc}
	 */
	public String getEpicsAddress(String key, String field) {
		return "ioxyz123."+key+"."+(field!=null?field:"xxx");
	}

    public List<String> getAllIoNames() {
        List<String> arrayList = new ArrayList<String>();
        arrayList.add("test");
        arrayList.add("@acb:3/7");
        arrayList.add("@acb:3/8");
        arrayList.add("@dec:1/4");
        arrayList.add("@dec:2/4");
        return arrayList;
    }

}
