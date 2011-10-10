/**
 * Copyright (c) 2011 Novell
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */

package com.redhat.rhn.frontend.servlets;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileUpload;
import org.apache.struts.Globals;
import org.yaml.snakeyaml.Yaml;

import com.redhat.rhn.common.conf.Config;
import com.redhat.rhn.common.conf.ConfigDefaults;
import com.redhat.rhn.common.localization.LocalizationService;
import com.redhat.rhn.common.logging.AuditLog;
import com.redhat.rhn.common.logging.AuditLogMultipartRequest;

/**
 * AuditLogFilter for generic logging of HTTP requests.
 *
 * @version $Rev$
 */
public class AuditLogFilter implements Filter {

    // Path to the config file
    private final String configFile = "/usr/share/spacewalk/audit/auditlog-config.yaml";

    // Config keys
    private final String KEY_TYPE = "type";
    private final String KEY_REQUIRED = "required";
    private final String KEY_DISPATCH = "dispatch";
    private final String KEY_LOG_BEFORE = "log_before";
    private final String KEY_LOG_FAILURES = "log_failures";

    // Will contain a list of values for 'dispatch' to be globally ignored
    private List<String> dispatchIgnored = null;

    // Values will be looked up in the translation files using these keys
    private String[] dispatchIgnoredValues = { "Go", "Select All",
            "Unselect All", "Update List" };

    // Local boolean to check if logging is enabled
    private Boolean enabled = null;

    // Configuration objects
    private HashMap auditConfig;

    /** {@inheritDoc} */
    public void init(FilterConfig filterConfig) {
        // Put enabled into a local boolean
        if (enabled == null) {
            enabled = Config.get().getBoolean(ConfigDefaults.AUDIT_ENABLED);
        }

        // Load configuration from YAML file
        if (auditConfig == null) {
            Object obj = null;
            try {
                Yaml yaml = new Yaml();
                obj = yaml.load(new FileReader(configFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (obj instanceof HashMap) {
                auditConfig = (HashMap) obj;
            }
        }

        // Init ignored values for 'dispatch'
        dispatchIgnored = createDispatchIgnored();
    }

    /** {@inheritDoc} */
    public void destroy() {
        auditConfig = null;
    }

    /** {@inheritDoc} */
    public void doFilter(ServletRequest req, ServletResponse resp,
            FilterChain chain) throws IOException, ServletException {
        // Indicators
        boolean log = false;
        boolean isMultipart = false;

        // The current HTTP request and logging configuration
        HttpServletRequest request = null;
        Map uriConfig = null;

        if (enabled) {
            request = (HttpServletRequest) req;
            // Get the configuration for this URI
            if (auditConfig.containsKey(request.getServletPath())) {
                uriConfig = (HashMap) auditConfig.get(request.getServletPath());
                if (uriConfig != null) {
                    // If content is 'multipart/form-data', wrap the request
                    if (FileUpload.isMultipartContent(request)) {
                        isMultipart = true;
                        request = new AuditLogMultipartRequest(request);
                    }

                    // Check the configuration for this URI
                    if (!checkRequirements(uriConfig, request)) {
                        // Do not log this request
                    } else if (logBefore(uriConfig)) {
                        // Log this request now
                        AuditLog.getInstance().log(false,
                                (String) uriConfig.get(KEY_TYPE), request);
                    } else {
                        // Log this request later
                        log = true;
                    }
                }
            }
        }

        // Finished for now
        chain.doFilter(request, resp);

        // For multipart content check the requirements again, parameters may
        // in fact not have been available before!
        if (isMultipart && checkRequirements(uriConfig, request)) {
            log = true;
        }

        // Do the actual logging
        if (log) {
            if (request.getAttribute(Globals.ERROR_KEY) == null) {
                // No failures, normal logging
                AuditLog.getInstance().log(false,
                        (String) uriConfig.get(KEY_TYPE), request);
            } else if (logFailures(uriConfig)) {
                AuditLog.getInstance().log(true,
                        (String) uriConfig.get(KEY_TYPE), request);
            }
        }
    }

    /**
     * Check all of the parameter specific requirements for a given
     * {@link HttpServletRequest} and a URI configuration given as {@link Map}.
     *
     * @param uriConfig
     * @param request
     * @return true if all requirements are met, else false.
     */
    private boolean checkRequirements(Map uriConfig, HttpServletRequest request) {
        boolean success = hasRequiredParams(uriConfig, request)
                && dispatch(uriConfig, request);
        return success;
    }

    /**
     * Check if all of the required parameters are contained in the request.
     *
     * @param uriConfig
     * @param request
     * @return true if required parameters are there, else false.
     */
    private boolean hasRequiredParams(Map uriConfig, HttpServletRequest request) {
        boolean ret = true;
        if (uriConfig.containsKey(KEY_REQUIRED)) {
            List requiredParams = (List) uriConfig.get(KEY_REQUIRED);
            for (Object key : requiredParams) {
                if (!request.getParameterMap().containsKey(key)) {
                    // Required parameter is not there
                    ret = false;
                    break;
                }
            }
        }
        return ret;
    }

    /**
     * Check for specific (internationalized!) values of 'dispatch' as well as
     * the list of globally ignored values.
     *
     * @param uriConfig
     * @param request
     * @return false if 'dispatch' contains a globally ignored value
     */
    private boolean dispatch(Map uriConfig, HttpServletRequest request) {
        boolean ret = true;
        // Check 'dispatch' for an explicit value
        String dispatch = request.getParameter("dispatch");
        if (uriConfig.containsKey(KEY_DISPATCH)) {
            String value = LocalizationService.getInstance().getMessage(
                    (String) uriConfig.get(KEY_DISPATCH));
            if (dispatch == null || !value.equals(dispatch)) {
                ret = false;
            }
        } else if (dispatch != null && dispatchIgnored.contains(dispatch)) {
            // Or check with the global ignored list
            ret = false;
        }
        return ret;
    }

    /**
     * Check if this request should be logged before it goes to the server.
     *
     * @param uriConfig
     * @return true or false
     */
    private boolean logBefore(Map uriConfig) {
        boolean ret = false;
        if (uriConfig.containsKey(KEY_LOG_BEFORE)) {
            Object bool = uriConfig.get(KEY_LOG_BEFORE);
            if (bool instanceof Boolean) {
                ret = (Boolean) bool;
            }
        }
        return ret;
    }

    /**
     * Check if failures should be logged for this URL.
     *
     * @param uriConfig
     * @return true or false
     */
    private boolean logFailures(Map uriConfig) {
        boolean ret = false;
        if (uriConfig.containsKey(KEY_LOG_FAILURES)) {
            Object bool = uriConfig.get(KEY_LOG_FAILURES);
            if (bool instanceof Boolean) {
                ret = (Boolean) bool;
            }
        }
        return ret;
    }

    /**
     * If the 'dispatch' parameter has one of the values in this list, the
     * request is not going to be logged.
     *
     * @return unmodifiable list
     */
    private List<String> createDispatchIgnored() {
        List<String> values = new ArrayList<String>();
        for (String s : dispatchIgnoredValues) {
            values.add(LocalizationService.getInstance().getMessage(s));
        }
        return Collections.unmodifiableList(values);
    }
}
