/*
 * Copyright (c) 2007 Stiftung Deutsches Elektronen-Synchrotron,
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
/*
 * $Id: IfGraterThen.java,v 1.3 2010/06/17 10:39:50 hrickens Exp $
 */
package org.csstudio.sds.cosyrules.color;

import org.csstudio.sds.model.IRule;

/**
 * @author hrickens
 * @author $Author: hrickens $
 * @version $Revision: 1.3 $
 * @since 27.09.2007
 */
public class IfGraterThen implements IRule {
    /**
     * The ID for this rule.
     */
    public static final String TYPE_ID = "org.css.sds.ifGreaterThen";

    /**
     * Standard constructor.
     */
    public IfGraterThen() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object evaluate(final Object[] arguments) {
        if (arguments != null && arguments.length > 1) {
            double d1 = 0.0;
            if (arguments[0] instanceof Number) {
                d1 = ((Number) arguments[0]).doubleValue();
            } else if (arguments[0] instanceof String) {
                try {
                    final String string = ((String) arguments[0]).replaceAll(",", ".");
                    d1 = Double.parseDouble(string);
                } catch (final NumberFormatException e) {
                    return false;
                }
            }

            double d2 = 0.0;
            if (arguments[1] instanceof Number) {
                d2 = ((Number) arguments[1]).doubleValue();
            } else if (arguments[1] instanceof String) {
                try {
                    final String string = ((String) arguments[1]).replaceAll(",", ".");
                    d2 = Double.parseDouble(string);
                } catch (final NumberFormatException e) {
                    return false;
                }
            }
            return d1 > d2;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Vergleicht zwei Wert. Ist der erste Wert(Greater) gr��er als der zweiter (Smaller) wird true zur�ckgegeben sonst false";
    }

}
