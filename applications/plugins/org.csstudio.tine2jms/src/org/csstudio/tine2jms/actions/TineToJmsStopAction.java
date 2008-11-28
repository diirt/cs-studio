
/* 
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron, 
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND 
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR 
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. 
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, 
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION, 
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY 
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */

package org.csstudio.tine2jms.actions;

import java.util.Map;
import org.csstudio.platform.libs.dcf.actions.IAction;
import org.csstudio.tine2jms.Stoppable;
import org.csstudio.tine2jms.TineToJmsActivator;

/**
 * @author Markus Moeller
 *
 */
public class TineToJmsStopAction implements IAction
{
    private static Stoppable objectToBeStopped = null;
    
    /**
     * @see org.csstudio.platform.libs.dcf.actions.IAction#run(java.lang.Object)
     */
    public Object run(Object param)
    {
        String password = null;
        
        if(!(param instanceof Map))
        {
            return "Parameter not available.";
        }
        
        Map<?, ?> map = (Map<?, ?>)param;

        try
        {
            password = (String)map.get("Password");
        
            if(password.compareTo(TineToJmsActivator.getDefault().getConfiguration().getString("xmpp.shutdown")) != 0)
            {
                return "Invalid password";
            }
        }
        catch(Exception e)
        {
            return e.getMessage();
        }

        objectToBeStopped.stopWorking();
        
        return "Stopping application...";
    }
    
    public static void staticInject(Stoppable obj)
    {
        objectToBeStopped = obj;
    }
}
