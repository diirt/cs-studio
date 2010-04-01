/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchrotron,
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
package org.csstudio.utility.ldap.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.log4j.Logger;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.statistic.Collector;
import org.csstudio.utility.ldap.Activator;
import org.csstudio.utility.ldap.connection.LDAPConnector;
import org.csstudio.utility.ldap.engine.LdapReferences.Entry;
import org.csstudio.utility.ldap.preference.PreferenceKey;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

public class Engine extends Job {


    private final Logger LOG = CentralLogger.getInstance().getLogger(this);

    private static int LDAP_MAX_BUFFER_SIZE = 10000; // 1000 too small!!
    private volatile boolean running = true;
    private int reStartSendDiff = 0;

    /**
     * An Objectclass as return Object for the setAttribute and getAttribute
     * method.
     *
     * @author $Author$
     * @version $Revision$
     * @since 23.01.2008
     */
    private class AttributeSet {
        private final SearchControls _ctrl;
        private String _path;
        private String _filter;

        /**
         * The default Constructor. Set default the Timelimit to 1000 ms.
         */
        public AttributeSet() {
            _ctrl = new SearchControls();
            _ctrl.setTimeLimit(1000);
        }

        /**
         * @param searchScope
         *            set the SearchScope of {@link SearchControls}.
         */
        public void setSearchScope(final int searchScope) {
            _ctrl.setSearchScope(searchScope);
        }

        /**
         * @param filter
         *            set the Filter for the Search.
         */
        public void setFilter(final String filter) {
            _filter = filter;
        }

        /**
         * @param path
         *            set the path for the Search.
         */
        public void setPath(final String path) {
            _path = path;
        }

        /**
         *
         * @return the SearchControls.
         */
        public SearchControls getSearchControls() {
            return _ctrl;
        }

        /**
         * @param path
         *            get the path for the Search.
         */

        public String getPath() {
            return _path;
        }

        /**
         * @param filter
         *            get the Filter for the Search.
         */
        public String getFilter() {
            return _filter;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            String scope;
            switch (_ctrl.getSearchScope()) {
                case SearchControls.OBJECT_SCOPE:
                    scope = "OBJECT_SCOPE";
                    break;
                case SearchControls.ONELEVEL_SCOPE:
                    scope = "ONELEVEL_SCOPE";
                    break;
                case SearchControls.SUBTREE_SCOPE:
                    scope = "SUBTREE_SCOPE";
                    break;
                default:
                    scope = "Unknown scope";

            }
            return String.format("Path: %s - Filter: %s", _path, _filter, scope);
        }
    }

    /**
     * Contain all Attributes for Records.
     */
    public static enum ChannelAttribute {
        epicsCssType,
        epicsAlarmStatus,
        epicsAlarmAcknTimeStamp,
        epicsAlarmAckn,
        epicsDatabaseType,
        epicsAlarmSeverity,
        epicsAlarmTimeStamp,
        epicsRecordType,
        epicsAlarmHighUnAckn,
        epicsCssAlarmDisplay,
        epicsCssDisplay,
        epicsHelpGuidance,
        epicsHelpPage
    }

    private DirContext _ctx = null;
    private static Engine _thisEngine = null;

    private boolean _doWrite = false;

    private Collector _ldapReadTimeCollector = null;
    private Collector _ldapWriteTimeCollector = null;
    private Collector _ldapWriteRequests = null;
    private LdapReferences _ldapReferences = null;
    private Vector<WriteRequest> _writeVector = new Vector<WriteRequest>();

    boolean addVectorOK = true;

    private Engine(final String name) {
        super(name);
        this._ldapReferences = new LdapReferences();
        /*
         * initialize statistic
         */
        _ldapWriteTimeCollector = new Collector();
        _ldapWriteTimeCollector.setApplication(name);
        _ldapWriteTimeCollector.setDescriptor("Time to write to LDAP server");
        _ldapWriteTimeCollector.setContinuousPrint(false);
        _ldapWriteTimeCollector.setContinuousPrintCount(1000.0);
        _ldapWriteTimeCollector.getAlarmHandler().setDeadband(5.0);
        _ldapWriteTimeCollector.getAlarmHandler().setHighAbsoluteLimit(500.0); // 500ms
        _ldapWriteTimeCollector.getAlarmHandler().setHighRelativeLimit(200.0); // 200%

        _ldapReadTimeCollector = new Collector();
        _ldapReadTimeCollector.setApplication(name);
        _ldapReadTimeCollector.setDescriptor("Time to find LDAP entries");
        _ldapReadTimeCollector.setContinuousPrint(false);
        _ldapReadTimeCollector.setContinuousPrintCount(1000.0);
        _ldapReadTimeCollector.getAlarmHandler().setDeadband(5.0);
        _ldapReadTimeCollector.getAlarmHandler().setHighAbsoluteLimit(500.0); // 500ms
        _ldapReadTimeCollector.getAlarmHandler().setHighRelativeLimit(200.0); // 200%

        _ldapWriteRequests = new Collector();
        _ldapWriteRequests.setApplication(name);
        _ldapWriteRequests.setDescriptor("LDAP Write Request Buffer Size");
        _ldapWriteRequests.setContinuousPrint(false);
        _ldapWriteRequests.setContinuousPrintCount(1000.0);
        _ldapWriteRequests.getAlarmHandler().setDeadband(5.0);
        _ldapWriteRequests.getAlarmHandler().setHighAbsoluteLimit(250.0); // number of entries in list
        _ldapWriteRequests.getAlarmHandler().setHighRelativeLimit(200.0); // 200%
    }

    /**
     * @param args
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        Integer intSleepTimer = null;

        LOG.info("Ldap Engine started");

        // TODO:
        /*
         * create message ONCE retry forever if ctx == null BUT do NOT block
         * caller (calling sigleton) submit ctx = new
         * LDAPConnector().getDirContext(); to 'background process'
         *
         */
        LOG.debug("Engine.run - start");
        if (_ctx == null) {
            _ctx = getLdapDirContext();
        }

        while (isRunning() || _doWrite) {
            //
            // do the work actually prepared
            //
            if (_doWrite) {
                performLdapWrite();
            }
            /*
             * sleep before we check for work again
             */
            try {
                final String protocol = Activator.getDefault().getPluginPreferences().getString(PreferenceKey.SECURITY_PROTOCOL.name());
                if (protocol.trim().length() > 0) {
                    intSleepTimer = new Integer(protocol);
                } else {
                    intSleepTimer = 10; // default
                }
                Thread.sleep(intSleepTimer);
            } catch (final InterruptedException e) {
                return null;
            }
        }
        return Job.ASYNC_FINISH;
    }

    /**
     *
     * @return get an instance of our singleton.
     */
    public static Engine getInstance() {
        if (_thisEngine == null) {
            synchronized (Engine.class) {
                if (_thisEngine == null) {
                    _thisEngine = new Engine("LdapEngine");
                    _thisEngine.setSystem(true);
                    _thisEngine.schedule();
                }
            }
        }
        return _thisEngine;
    }

    synchronized public void addLdapWriteRequest(final String attribute, final String channel, final String value) {
        // boolean addVectorOK = true;
        final WriteRequest writeRequest = new WriteRequest(attribute, channel, value);
        final int maxBuffersize = LDAP_MAX_BUFFER_SIZE;
        //
        // add request to vector
        //
        final int bufferSize = _writeVector.size();
        /*
         * statistic information
         */
        _ldapWriteRequests.setValue(bufferSize);

        // / System.out.println("Engine.addLdapWriteRequest actual buffer size:
        // " + bufferSize);
        /**
         *  Start the sending, after a Buffer overflow,
         *  when the buffer have minimum 10% free space
         */
        if (bufferSize > (maxBuffersize-reStartSendDiff)) {
            if (addVectorOK) {
                System.out.println("Engine.addLdapWriteRequest writeVector > " + maxBuffersize
                                   + " - cannot store more!");
                LOG.warn("writeVector > " + maxBuffersize + " - cannot store more!");
                reStartSendDiff = (int)(LDAP_MAX_BUFFER_SIZE*0.1);
                addVectorOK = false;
            }
        } else {
            if (!addVectorOK) {
                System.out.println("Engine.addLdapWriteRequest writeVector - continue writing");
                LOG.warn("writeVector < " + maxBuffersize + " - resume writing");
                reStartSendDiff = 0;
                addVectorOK = true;
            }
            _writeVector.add(writeRequest);
        }
        //
        // aleays trigger writing
        //
        _doWrite = true;
    }

    synchronized public DirContext getLdapDirContext() {
        if (_ctx == null) {
            try {
                final LDAPConnector con = new LDAPConnector();
                _ctx = con.getDirContext();
            } catch (final NamingException e1) {
                try {
                    Thread.sleep(100);
                    _ctx = new LDAPConnector().getDirContext();
                } catch (final InterruptedException e) {
                    LOG.error(Engine.getInstance(), e);
                    return null;
                } catch (final NamingException e) {
                    LOG.error(Engine.getInstance(), e);
                    return null;
                }
            }
            LOG.debug("Engine.run - ctx: " + _ctx.toString());
            if (_ctx != null) {
                LOG.info("Engine.run - successfully connected to LDAP server");
            } else {
                LOG.fatal("Engine.run - connection to LDAP server failed");
            }
        }
        return _ctx;
    }

    synchronized public DirContext reconnectDirContext() {
        try {
            _ctx.close();
        } catch (final NamingException e) {
            // ignore
        }
        _ctx = null;
        return getLdapDirContext();
    }

    /**
     * Get the Value of an record attribute.
     *
     * @param recordPath
     *            The Record-Name or the complete LDAP path of the record. Which
     *            the attribute change.
     * @param attribute
     *            The attribute from where get the value.
     * @return the value of the record attribute.
     */
    synchronized public String getAttribute(final String recordPath,
                                            final ChannelAttribute attribute) {
        if ((recordPath == null) || (attribute == null)) {
            return null;
        }
        if (getLdapDirContext() != null) {
            final AttributeSet attributeSet = helpAttribute(recordPath);
            try {
                final String[] attStrings = new String[] { attribute.name() };
                Attributes attributes = null;
                try {
                    if (attributeSet.getSearchControls().getSearchScope() == SearchControls.SUBTREE_SCOPE) {
                        final SearchControls sc = attributeSet.getSearchControls();
                        final NamingEnumeration<SearchResult> searchResults = getLdapDirContext().search(attributeSet
                                                                                                         .getPath(), attributeSet.getFilter(), sc);

                        if (searchResults.hasMore()) {
                            final SearchResult element = searchResults.next();
                            attributes = element.getAttributes();
                        } else {
                            return "NOT_FOUND";
                        }
                    } else {
                        attributes = getLdapDirContext().getAttributes(attributeSet.getFilter() + ","
                                                                       + attributeSet.getPath(), attStrings);
                    }
                } catch (final NamingException ne) {
                    attributes = null;
                }
                if (attributes == null) {
                    return "NONE";
                }
                final Attribute attr = attributes.get(attribute.name());
                if (attr == null) {
                    return "NONE";
                }
                final Object object = attr.get();
                if (object == null) {
                    return "NONE";
                }
                return object.toString();

            } catch (final NamingException e) {
                _ctx = null;
                LOG.info("Falscher LDAP Suchpfad f�r Record suche.");
                LOG.info(e);
            }
        }
        return null;
    }

    /**
     * Set a Value of an record Attribute.
     *
     * @param recordPath
     *            The Record-Name or the complete LDAP path of the record. Which
     *            the attribute change.
     * @param attribute
     *            The attribute to set the Value.
     * @param value
     *            the value was set.
     */
    synchronized public void setAttribute(final String recordPath,
                                          final ChannelAttribute attribute, final String value) {
        assert (recordPath != null) && (attribute != null) && (value != null) : "The recordPath, attribute and/or value are NULL";
        if (getLdapDirContext() != null) {
            AttributeSet attributeSet = helpAttribute(recordPath);
            try {
                String ldapChannelName = "";
                if (attributeSet.getSearchControls().getSearchScope() == SearchControls.SUBTREE_SCOPE) {
                    final SearchControls sc = attributeSet.getSearchControls();
                    final NamingEnumeration<SearchResult> searchResults = getLdapDirContext().search(attributeSet
                                                                                                     .getPath(), attributeSet.getFilter(), sc);
                    if (searchResults.hasMore()) {
                        final SearchResult element = searchResults.next();
                        // element.getAttributes().get(attriebute.name()).get().toString();
                        attributeSet = helpAttribute(element.getNameInNamespace());
                    }

                }
                if ((attributeSet != null) && (attributeSet.getFilter() != null)
                        && (attributeSet.getPath() != null)) {
                    ldapChannelName = attributeSet.getFilter() + "," + attributeSet.getPath();
                    final BasicAttribute ba = new BasicAttribute(attribute.name(), value);
                    final ModificationItem[] modItemTemp = new ModificationItem[] { new ModificationItem(
                                                                                                         DirContext.REPLACE_ATTRIBUTE, ba) };
                    final String channel = ldapChannelName.split("[=,]")[0];
                    modifyAttributes(ldapChannelName, modItemTemp, channel, new GregorianCalendar());
                    // _ctx.modifyAttributes(ldapChannelName,modItemTemp);
                } else {
                    LOG.warn("Set attribute faild. Record: " + recordPath + " with attriebute: "
                             + attribute.name() + " not found!");
                }
            } catch (final NamingException e) {
                _ctx = null;
                LOG.info("Falscher LDAP Suchpfad f�r Record suche. Beim setzen eines Atributes fehlgeschlagen.");
                LOG.info(e);
            }
        }
    }

    /**
     * Returns the distinguished name of an IOC in the LDAP directory. If no IOC
     * with the given IP address is configured in the LDAP directory, returns
     * <code>null</code>.
     *
     * @param ipAddress
     *            the IP address of the IOC.
     * @return The LDAP distinguished name of the IOC with the given IP address.
     */
    synchronized public String getLogicalNameFromIPAdr(final String ipAddress) {
        if ((ipAddress == null)
                || !ipAddress
                .matches("[0-2]?[0-9]?[0-9].[0-2]?[0-9]?[0-9].[0-2]?[0-9]?[0-9].[0-2]?[0-9]?[0-9].*")) {
            return null;
        }
        final DirContext context = getLdapDirContext();
        if (context != null) {
            final SearchControls ctrl = new SearchControls();
            ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            ctrl.setTimeLimit(1000);
            try {
                final NamingEnumeration<SearchResult> results = context.search(
                                                                               "ou=EpicsControls",
                                                                               "(&(objectClass=epicsController)" +
                                                                               "(|(epicsIPAddress=" + ipAddress + ")" +
                                                                               "(epicsIPAddressR=" + ipAddress + ")))",
                                                                               ctrl);
                if (results.hasMore()) {
                    // The relative name of the search result is relative to
                    // ou=EpicsControls, but the ou=EpicsControls part should
                    // be contained in the returned result, so this code adds
                    // it back to the name. Wrapping/unwrapping in CompositeName
                    // ensures proper escaping/unescaping of LDAP and JNDI
                    // special characters.
                    final Name cname = new CompositeName(results.next().getName());
                    final NameParser nameParser = context.getNameParser(new CompositeName());
                    final LdapName ldapName = (LdapName) nameParser.parse(cname.get(0));
                    ldapName.add(0, new Rdn("ou", "EpicsControls"));

                    // Only a single IOC should be found for an IP address.
                    if (results.hasMore()) {
                        LOG.warn("More than one IOC entry in LDAP directory for IP address: " + ipAddress);
                    }

                    return ldapName.toString();
                }
            } catch (final NamingException e) {
                _ctx = null;
                LOG.error("LDAP directory search failed.", e);
            }
        }
        return null;
    }

    /**
     * Set the severity, status and eventTime to a record.
     *
     * @param ldapPath
     *            the LDAP-Path to the record.
     * @param severity
     *            the severity to set.
     * @param status
     *            the status to set.
     * @param eventTime
     *            the event time to set.
     * @return ErgebnisList to receive all channel of a IOC. the ErgebnisList is
     *         Observable.
     */
    public ArrayList<String> getAllRecordofICO(final String ldapPath, final String severity,
                                               final String status, final String eventTime) {
        String path = ldapPath;
        int searchControls = SearchControls.SUBTREE_SCOPE;
        if (path == null) {
            LOG.warn("LDAPPath is NULL!");
            return null;
        } else if (path.contains(("ou=epicsControls"))) {
            if (path.endsWith(",o=DESY,c=DE")) {
                path = path.substring(0, path.length() - 12);
            } else if (path.endsWith(",o=DESY")) {
                path = path.substring(0, path.length() - 7);
            }
            if (path.startsWith("econ=")) {
                searchControls = SearchControls.ONELEVEL_SCOPE;
            }
        } else if (path.contains(",")) {
            // Unbekannter LDAP Pfad
            // TODO: Mir ist nicht klar ob diese Abfrage Stimmt. Kann es nicht
            // einen g�ltigen Path geben?
            LOG.warn("Unknown LDAP Path! Path is " + path);
            return null;
        } else if (!path.startsWith("econ=")) {
            path = "econ=" + path;
        }
        //final LdapResultList allRecordsList = new LdapResultList();
        //        allRecordsList.setStatus(status);
        //        allRecordsList.setSeverity(severity);
        //        allRecordsList.setEventTime(eventTime);
        //        allRecordsList.setParentName(ldapPath);

        if (getLdapDirContext() != null) {
            final SearchControls ctrl = new SearchControls();
            ctrl.setSearchScope(searchControls);
            try {
                final ArrayList<String> list = new ArrayList<String>();
                final NamingEnumeration<SearchResult> answer = getLdapDirContext().search(path, "eren=*", ctrl);
                // NamingEnumeration<SearchResult> answer =
                // _ctx.search(ldapPath,"eren=*" , ctrl);
                while (answer.hasMore()) {
                    final String name = answer.next().getName() + "," + path;
                    // String name = answer.next().getName()+","+ldapPath;
                    list.add(name);
                }
                if (list.size() < 1) {
                    list.add("no entry found");
                }
                answer.close();
                return list;
            } catch (final NamingException e) {
                _ctx = null;
                LOG.info("Wrong LDAP path.", e);
            }
        }
        LOG.warn("Can't connect the LDAP Server!");
        return null;
    }

    public int gregorianTimeDifference(final GregorianCalendar fromTime, final GregorianCalendar toTime) {
        //
        // calculate time difference
        //
        final Date fromDate = fromTime.getTime();
        final Date toDate = toTime.getTime();
        final long fromLong = fromDate.getTime();
        final long toLong = toDate.getTime();
        final long timeDifference = toLong - fromLong;
        final int intDiff = (int) timeDifference;
        return intDiff;
    }

    private void performLdapWrite() {
        final int maxNumberOfWritesProcessed = 200;
        ModificationItem[] modItem = new ModificationItem[maxNumberOfWritesProcessed];
        int i = 0;
        String channel;
        channel = null;
        i = 0;

        while (_writeVector.size() > 0) {

            //
            // return first element and remove it from list
            //
            final WriteRequest writeReq = _writeVector.remove(0);
            //
            // prepare LDAP request for all entries matching the same channel
            //
            if (channel == null) {
                // first time setting
                channel = writeReq.getChannel();
            }
            if (!channel.equals(writeReq.getChannel())) {
                // System.out.print("write: ");
                // TODO this hard coded string must be removed to the
                // preferences
                changeValue("eren", channel, modItem);
                // System.out.println(" finisch!!!");
                modItem = new ModificationItem[maxNumberOfWritesProcessed];
                i = 0;
                //
                // define next channel name
                //
                channel = writeReq.getChannel();
            }
            //
            // combine all items that are related to the same channel
            //
            final BasicAttribute ba = new BasicAttribute(writeReq.getAttribute(), writeReq.getValue());
            modItem[i++] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, ba);

            if ((_writeVector.size() > 100) && ((_writeVector.size() % 100) == 0)) {
                System.out.println("Engine.performLdapWrite buffer size: " + _writeVector.size());
            }
        }
        //
        // still something left to do?
        //
        if (i != 0) {
            //
            try {
                // System.out.println("Vector leer jetzt den Rest zum LDAP
                // Server schreiben");
                changeValue("eren", channel, modItem);
            } catch (final Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //
                // too bad it did not work
                _doWrite = true; // retry!
                return;
            }
        } else {
            // System.out.println("Vector leer - nothing left to do");
        }

        _doWrite = false;
    }

    /**
     * @param string
     * @param channel
     * @param modItemTemp
     */
    private void changeValue(final String string, final String channel, final ModificationItem[] modItem) {
        int j = 0;
        int n;
        Vector<String> namesInNamespace = null;
        GregorianCalendar startTime = null;

        // Delete null values and make right size
        for (; j < modItem.length; j++) {
            if (modItem[j] == null) {
                break;
            }
        }
        // System.out.println("Enter Engine.changeValue with: " + channel);
        final ModificationItem modItemTemp[] = new ModificationItem[j];
        for (n = 0; n < j; n++) {
            modItemTemp[n] = modItem[n];
        }
        //
        // set start time
        //
        startTime = new GregorianCalendar();

        //
        // is channel name already in ldearReference hash table?
        //

        if (_ldapReferences.hasEntry(channel)) {
            // if ( false) { // test case with no hash table
            //
            // take what's already stored
            //
            LOG.debug("Engine.changeValue : found entry for channel: " + channel);
            // System.out.println ("Engine.changeValue : found entry for
            // channel: " + channel);
            namesInNamespace = this._ldapReferences.getEntry(channel).getNamesInNamespace();
            for (int index = 0; index < namesInNamespace.size(); index++) {
                final String ldapChannelName = namesInNamespace.elementAt(index);
                modifyAttributes(ldapChannelName, modItemTemp, channel, startTime);
            }

        } else {
            //
            // search for channel in ldap server
            //
            final SearchControls ctrl = new SearchControls();
            ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            try {
                final NamingEnumeration<SearchResult> results = getLdapDirContext().search("", string + "=" + channel,
                                                                                           ctrl);
                // System.out.println ("Engine.changeValue : Time to search
                // channel: " + gregorianTimeDifference ( startTime, new
                // GregorianCalendar()));
                _ldapReadTimeCollector.setInfo(channel);
                _ldapReadTimeCollector.setValue(gregorianTimeDifference(startTime,
                                                                        new GregorianCalendar()));
                // System.out.println("Enter Engine.changeValue results for
                // channnel: " + channel );
                namesInNamespace = new Vector<String>();
                while (results.hasMore()) {
                    final String ldapChannelName = results.next().getNameInNamespace();
                    namesInNamespace.add(ldapChannelName);
                    modifyAttributes(ldapChannelName, modItemTemp, channel, startTime);
                }

            } catch (final NamingException e1) {
                // TODO Auto-generated catch block
                _ctx = null;
                e1.printStackTrace();
            }
            //
            // save ldapEntries
            //
            if (namesInNamespace.size() > 0) {
                //
                // Write if really something found
                //
                _ldapReferences.newLdapEntry(channel, namesInNamespace);
                // System.out.println ("Engine.changeValue : add entry for
                // channel: " + channel);
            }
        }
        //
        // calcualte time difference
        //
        // System.out.println ("Engine.changeValue : Time to write to
        // LDAP-total: " + gregorianTimeDifference ( startTime, new
        // GregorianCalendar()));
    }

    /**
     * @param localLdapChannelName
     * @param modItemTemp
     * @param startTime
     */
    private void modifyAttributes(String ldapChannelName, final ModificationItem[] modItemTemp,
                                  final String channel, final GregorianCalendar startTime) {
        //
        // TODO put 'endsWith' into preference page
        //
        if (ldapChannelName.endsWith(",o=DESY,c=DE")) {
            ldapChannelName = ldapChannelName.substring(0, ldapChannelName.length() - 12);
        }
        try {
            ldapChannelName = ldapChannelName.replace("/", "\\/");
            getLdapDirContext().modifyAttributes(ldapChannelName, modItemTemp);
            _ldapWriteTimeCollector.setInfo(channel);
            _ldapWriteTimeCollector.setValue(gregorianTimeDifference(startTime,
                                                                     new GregorianCalendar())
                                                                     / modItemTemp.length);
            // System.out.println ("Engine.changeValue : Time to write to LDAP:
            // (" + channel + ")" + gregorianTimeDifference ( startTime, new
            // GregorianCalendar()));
        } catch (final NamingException e) {
            _ctx = null;
            LOG.warn("Engine.changeValue: Naming Exception in modifyAttributes! Channel: "
                     + ldapChannelName);
            System.out.println("Engine.changeValue: Naming Exceptionin modifyAttributes! Channel: "
                               + ldapChannelName);
            // for (ModificationItem modificationItem : modItemTemp) {
            // System.out.println(" - ModificationItem is:
            // "+modificationItem.getAttribute().get().toString());
            // }
            final String errorCode = e.getExplanation();
            if (errorCode.contains("10")) {
                System.out
                .println("Error code 10: Please check LDAP replica! - replica may be out of synch - use: [start accepting updates] in SUN-LDAP Console");
                CentralLogger
                .getInstance()
                .warn(
                      this,
                "Error code 10: Please check LDAP replica! - replica may be out of synch - use: [start accepting updates] in SUN-LDAP Console");
            }
            // e.printStackTrace();
            //
            // too bad it did not work
            _doWrite = false; // wait for next time
            return;
        } catch (final Exception e) {
            e.printStackTrace();
            //
            // too bad it did not work
            _doWrite = false; // wait for next time
            return;
        }
    }

    private AttributeSet helpAttribute(String record) {
        final AttributeSet attributeSet = new AttributeSet();

        if (record != null) {
            // Pr�ft ob der record schon in der ldapReferences gespeichert ist.
            if (!record.contains("ou=epicsControls") &&
                !record.contains("econ=") &&
                (_ldapReferences != null) &&
                _ldapReferences.hasEntry(record)) {// &&!record.contains("ou=")){

                final Entry entry = _ldapReferences.getEntry(record);
                final Vector<String> vector = entry.getNamesInNamespace();
                for (final String string : vector) {
                    if (string.contains("ou=EpicsControls")) {
                        record = string;
                    }
                }
                attributeSet.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            } else if (record.contains("ou=epicsControls") && record.contains("econ=")) {
                // TODO: Der record ist und wird noch nicht im ldapReferences
                // gecachet. Enth�lt aber den kompletten Pfad.
                attributeSet.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            } else {
                // TODO: Der record ist und wird noch nicht im ldapReferences
                // gecachet.
                attributeSet.setSearchScope(SearchControls.SUBTREE_SCOPE);
            }
            if (record.endsWith(",o=DESY,c=DE")) {
                record = record.substring(0, record.length() - 12);
            } else if (record.endsWith(",o=DESY")) {
                record = record.substring(0, record.length() - 7);
            }
            if (record.contains(",")) {
                attributeSet.setFilter(record.split(",")[0]);
                attributeSet.setPath(record.substring(attributeSet.getFilter().length() + 1));
            } else {
                attributeSet.setPath("ou=epicsControls");
                attributeSet.setFilter("eren=" + record);
            }
            return attributeSet;
        } else {
            return null;
        }
    }

    /**
     * Clear the complete cache
     */
    public void clearCache(){
        _ldapReferences.clearAll();
    }

    /**
     * Clear the cache entry for the channel.
     *
     * @param channel the channel to clear at the cache.
     */
    public void clearCache(final String channel){
        _ldapReferences.clear(channel);
    }


    // public void setLdapValueOld ( String channel, String severity, String
    // status, String timeStamp) {
    // ModificationItem epicsStatus, epicsSeverity, epicsTimeStamp,
    // epicsAcknowledgeTimeStamp ;
    // ModificationItem[] modItem = null;
    // int i = 0;
    //
    // String channelName = "eren=" + channel;
    //
    // //
    // // change severity if value is entered
    // //
    // if ((severity != null)&& (severity.length() > 0)) {
    // epicsSeverity = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new
    // BasicAttribute( "epicsAlarmSeverity", severity));
    // modItem[i++] = epicsSeverity;
    // }
    //
    // //
    // // change status if value is entered
    // //
    // if ((status != null) && (status.length() > 0)) {
    // epicsStatus = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new
    // BasicAttribute("epicsAlarmStatus", status));
    // }
    //
    // //
    // // change alarm time stamp
    // //
    // if ((timeStamp != null) && (timeStamp.length() > 0)) {
    // epicsTimeStamp = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new
    // BasicAttribute("epicsAlarmTimeStamp", timeStamp));
    // }
    //
    // //
    // // change time stamp acknowledged time
    // //
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:S");
    // java.util.Date currentDate = new java.util.Date();
    // String eventTime = sdf.format(currentDate);
    //
    // epicsAcknowledgeTimeStamp = new ModificationItem(
    // DirContext.REPLACE_ATTRIBUTE, new
    // BasicAttribute("epicsAlarmAcknTimeStamp", eventTime));
    //
    // try {
    // _ctx.modifyAttributes(channelName, modItem);
    // } catch (NamingException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    //
    //
    // }

    private class WriteRequest {
        private String attribute = null;
        private String channel = null;
        private String value = null;

        public WriteRequest(final String attribute, final String channel, final String value) {

            this.attribute = attribute;
            this.channel = channel;
            this.value = value;
        }

        public String getAttribute() {
            return this.attribute;
        }

        public String getChannel() {
            return this.channel;
        }

        public String getValue() {
            return this.value;
        }

    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(final boolean running) {
        this.running = running;
    }

    public Vector<WriteRequest> getWriteVector() {
        return _writeVector;
    }

    public void setWriteVector(final Vector<WriteRequest> writeVector) {
        this._writeVector = writeVector;
    }
}
