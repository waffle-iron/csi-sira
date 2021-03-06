/*
 *  GeoServer Security Provider plugin used for doing authentication and authorization operations using CSI-Piemonte IRIDE Service.
 *  Copyright (C) 2016  Regione Piemonte (www.regione.piemonte.it)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.geoserver.security.iride;

import static org.geoserver.security.iride.util.builder.IrideServerURLBuilder.buildServerURL;
import it.csi.iride2.policy.entity.Application;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang.StringUtils;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.iride.config.IrideSecurityServiceConfig;
import org.geoserver.security.iride.identity.IrideIdentity;
import org.geoserver.security.iride.util.template.TemplateEngine;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;

/**
 * <code>GeoServer</code> roles security service, backed by  <code>CSI</code> <code>IRIDE</code> service.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * @author "Simone Cornacchia - seancrow76@gmail.com, simone.cornacchia@consulenti.csi.it (CSI:71740)"
 */
public class IrideRoleService extends AbstractGeoServerSecurityService implements GeoServerRoleService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logging.getLogger(IrideRoleService.class);

    /**
     * Regular Expression to extract role's relevant informations from the
     * <code>IRIDE</code> <code>findRuoliForPersonaInApplication</code> <code>SOAP</code> response.
     */
    private static final Pattern ROLE_REGEX = Pattern.compile("<codiceRuolo[^>]*?>\\s*(.*?)\\s*<\\/codiceRuolo>", Pattern.CASE_INSENSITIVE);

    /**
     * {@link IrideRoleService} configuration object.
     */
    private Config config;

    private HttpClient httpClient = new HttpClient();
    private HttpConnectionManagerParams params = new HttpConnectionManagerParams();

    /**
     * {@link TemplateEngine} implementation.
     */
    private TemplateEngine templateEngine;

    /**
     * @param httpClient the httpClient to set
     */
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @return the httpClient
     */
    public HttpClient getHttpClient() {
        return this.httpClient;
    }

    /**
     * Set the {@link TemplateEngine} implementation
     *
     * @param templateEngine the {@link TemplateEngine} implementation
     */
    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.impl.AbstractGeoServerSecurityService#initializeFromConfig(org.geoserver.security.config.SecurityNamedServiceConfig)
     */
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        LOGGER.log(Level.INFO,
            "Initializing {0} with configuration object: \n\t {1}",
            new Object[] { this.getClass().getSimpleName(), config }
        );

        this.name   = config.getName();
        this.config = new Config(config);

        this.params.setSoTimeout(30000);
        this.params.setConnectionTimeout(30000);

        final MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
        manager.setParams(this.params);

        this.httpClient.setHttpConnectionManager(manager);
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.impl.AbstractGeoServerSecurityService#canCreateStore()
     */
    @Override
    public boolean canCreateStore() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getGroupNamesForRole(org.geoserver.security.impl.GeoServerRole)
     */
    /**
     * Returns an immutable empty {@link ImmutableSortedSet} instance.
     */
    @Override
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        LOGGER.info("Role: " + role);

        return ImmutableSortedSet.of();
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getUserNamesForRole(org.geoserver.security.impl.GeoServerRole)
     */
    /**
     * Returns an immutable empty {@link ImmutableSortedSet} instance.
     */
    @Override
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        LOGGER.info("Role: " + role);

        return ImmutableSortedSet.of();
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getRolesForUser(java.lang.String)
     */
    @Override
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        LOGGER.info("Username: " + username);

        final TreeSet<GeoServerRole> roles = new TreeSet<>();

        // Check username format: it may be an Identita Digitale IRIDE, or not
        if (IrideIdentity.isNotValidIrideIdentity(username) && this.config.hasFallbackRoleServiceName()) {
            LOGGER.info("Username " + username + " is not a valid IRIDE Identity: falling back to RoleService '" + this.config.fallbackRoleServiceName + "'");

            final GeoServerRoleService fallbackRoleService = this.getSecurityManager().loadRoleService(this.config.fallbackRoleServiceName);

            roles.addAll(fallbackRoleService.getRolesForUser(username));

            return roles;
        }

        final String requestXml  = this.getServiceRequestXml(username);
        final String responseXml = this.callWebService(requestXml).replace("\\r", "").replace("\\n", "");

        final Matcher m = ROLE_REGEX.matcher(responseXml);
        while (m.find()) {
            final String roleName = m.group(1);

            roles.add(this.createRoleObject(roleName));

            LOGGER.info("Added role " + roleName + " from IRIDE to " + username);
        }

        // Rely on the fallback RoleService (if configured) when IRIDE has not found any roles for the given user
        if (roles.isEmpty() && this.config.hasFallbackRoleServiceName()) {
            LOGGER.info("IRIDE has not found any roles for the given user " + username + ": falling back to RoleService '" + this.config.fallbackRoleServiceName + "'");

            final GeoServerRoleService fallbackRoleService = this.getSecurityManager().loadRoleService(this.config.fallbackRoleServiceName);

            roles.addAll(fallbackRoleService.getRolesForUser(username));
        }

        return roles;
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getUserNamesForRole(org.geoserver.security.impl.GeoServerRole)
     */
    /**
     * Returns an immutable empty {@link ImmutableSortedSet} instance.
     */
    @Override
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        LOGGER.info("Group: " + groupname);

        return ImmutableSortedSet.of();
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getUserNamesForRole(org.geoserver.security.impl.GeoServerRole)
     */
    /**
     * Returns an immutable empty {@link ImmutableSortedSet} instance.
     */
    @Override
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        return ImmutableSortedSet.of();
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getUserNamesForRole(org.geoserver.security.impl.GeoServerRole)
     */
    /**
     * Returns an immutable empty {@link ImmutableMap} instance.
     */
    @Override
    public Map<String, String> getParentMappings() throws IOException {
        return ImmutableMap.of();
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#createRoleObject(java.lang.String)
     */
    @Override
    public GeoServerRole createRoleObject(String role) throws IOException {
        LOGGER.info("Role: " + role);

        return new GeoServerRole(role);
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getParentRole(org.geoserver.security.impl.GeoServerRole)
     */
    @Override
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        LOGGER.info("Role: " + role);

        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getRoleByName(java.lang.String)
     */
    @Override
    public GeoServerRole getRoleByName(String role) throws IOException {
        LOGGER.info("Role: " + role);

        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#load()
     */
    @Override
    public void load() throws IOException {
        /* NOP */
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#personalizeRoleParams(java.lang.String, java.util.Properties, java.lang.String, java.util.Properties)
     */
    @Override
    public Properties personalizeRoleParams(String roleName, Properties roleParams, String userName, Properties userProps) throws IOException {
        LOGGER.log(Level.INFO,
            "\n\t Role: {0},\n\t Role Params: {1},\n\t User: {2},\n\t User Properties: {3}",
            new Object[] { roleName, roleParams, userName, userProps }
        );

        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getAdminRole()
     */
    @Override
    public GeoServerRole getAdminRole() {
        String username = null;
        final SecurityContext context = SecurityContextHolder.getContext();
        final Authentication authentication = context.getAuthentication();
        if (authentication != null) {
            username = String.valueOf(authentication.getPrincipal());
        }

        LOGGER.info("AdminRole Username: " + username);

        GeoServerRole role;
        try {
            // Check username format: it may be an Identita Digitale IRIDE, or not
            if (IrideIdentity.isNotValidIrideIdentity(username) && this.config.hasFallbackRoleServiceName()) {
                LOGGER.info("Username " + username + " is not a valid IRIDE Identity: falling back to RoleService '" + this.config.fallbackRoleServiceName + "'");

                final GeoServerRoleService fallbackRoleService = this.getSecurityManager().loadRoleService(this.config.fallbackRoleServiceName);

                role = fallbackRoleService.getAdminRole();

                LOGGER.info("Role: " + role.getAuthority());
            } else {
                role = this.createRoleObject(this.config.adminRole);
            }

            return role;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);

            return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getGroupAdminRole()
     */
    /**
     * @see {@link #getAdminRole()}
     */
    @Override
    public GeoServerRole getGroupAdminRole() {
        return this.getAdminRole();
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#getRoleCount()
     */
    @Override
    public int getRoleCount() throws IOException {
        return 0;
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#createStore()
     */
    @Override
    public GeoServerRoleStore createStore() throws IOException {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#registerRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    @Override
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        /* NOP */
    }

    /*
     * (non-Javadoc)
     * @see org.geoserver.security.GeoServerRoleService#unregisterRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    @Override
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        /* NOP */
    }

    /**
     * @param requestXml
     * @return
     * @throws IOException
     * @throws HttpException
     */
    private String callWebService(final String requestXml) throws HttpException, IOException {
        final HttpMethod post = this.createHttpMethod(requestXml);
        final Header header = new Header();
        header.setName("Content-type");
        header.setValue("text/xml; charset=UTF-8");
        post.setRequestHeader(header);
        header.setName("SOAPAction");
        header.setValue("dummy");
        post.setRequestHeader(header);

        LOGGER.info("Request sent to IRIDE: " + requestXml);

        try {
            final int status = this.httpClient.executeMethod(post);
            if (status == 200) {
                final String responseXml = post.getResponseBodyAsString();

                LOGGER.info("Response received from IRIDE: " + responseXml);

                return responseXml;
            } else {
                LOGGER.info("Got error from IRIDE: " + status);

                return "";
                /*throw new IOException("Error getting remote resources from " + serverURL
                        + ", http error " + status + ": " + post.getStatusText());*/
            }
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * @param requestXml
     * @return
     * @throws UnsupportedEncodingException
     */
    protected HttpMethod createHttpMethod(String requestXml) throws UnsupportedEncodingException {
        final PostMethod post = new PostMethod(this.config.serverURL);
        post.setRequestEntity(new StringRequestEntity(requestXml, "text/xml", "UTF-8"));

        return post;
    }

    /**
     * @param username
     * @return
     * @throws IOException
     */
    private String getServiceRequestXml(final String username) throws IOException {
        return this.templateEngine.process(
            "findRuoliForPersonaInApplication",
            new HashMap<String, Object>() {

                private static final long serialVersionUID = 1L;

                {
                    this.put("irideIdentity", IrideIdentity.parseIrideIdentity(username));
                    this.put("application", new Application("DECSIRA"));
                }
            }
        );
    }

    /**
     *
     * @author "Simone Cornacchia - seancrow76@gmail.com, simone.cornacchia@consulenti.csi.it (CSI:71740)"
     *
     */
    private static final class Config {

        /**
         * <code>IRIDE</code> server <code>URL</code>.
         */
        private final String serverURL;

        /**
         * Application name requesting <code>IRIDE</code> service.
         *
         * @todo should be set dynamically at runtime
         */
        private final String applicationName;

        /**
         * Admin role.
         * Used for both <code>admin role name</code> and <code>group admin role name</code>.
         */
        private final String adminRole;

        /**
         * Name of the {@link GeoServerRoleService} to rely on as a fallback
         * when {@link IrideRoleService} does not found any roles for a given user.
         */
        private final String fallbackRoleServiceName;

        /**
         * Returns the given <code>applicationName</code> if it's deemed a valid,
         * throwing an {@link IllegalArgumentException} otherwise.<p>
         *
         * A valid <code>applicationName</code> must be <em>non-empty</em>, as per {@link StringUtils#isNotBlank(String)} check rules.
         *
         * @param applicationName the <code>applicationName</code> to validate
         * @return the given <code>applicationName</code> if it's deemed valid
         * @throws IllegalArgumentException if the given <code>applicationName</code> is not deemed valid
         */
        private static String validateApplicationName(String applicationName) {
            if (StringUtils.isBlank(applicationName)) {
                throw new IllegalArgumentException("Application name must not be of an empty string");
            }

            return applicationName;
        }

        /**
         * Returns the given <code>adminRole</code> if it's deemed a valid,
         * throwing an {@link IllegalArgumentException} otherwise.<p>
         *
         * A valid <code>adminRole</code> must be <em>non-empty</em>, as per {@link StringUtils#isNotBlank(String)} check rules.
         *
         * @param adminRole the <code>adminRole</code> to validate
         * @return the given <code>adminRole</code> if it's deemed valid
         * @throws IllegalArgumentException if the given <code>adminRole</code> is not deemed valid
         */
        private static String validateAdminRole(String adminRole) {
            if (StringUtils.isBlank(adminRole)) {
                throw new IllegalArgumentException("Admin role must not be of an empty string");
            }

            return adminRole;
        }

        /**
         * Constructor.
         *
         * Initialize a {@link Config} object from a {@link SecurityNamedServiceConfig} instance.
         *
         * @param cfg a {@link SecurityNamedServiceConfig} instance
         */
        Config(SecurityNamedServiceConfig cfg) {
            if (! (cfg instanceof IrideSecurityServiceConfig)) {
                throw new IllegalArgumentException("Config object must be of IrideSecurityServiceConfig type");
            }

            final IrideSecurityServiceConfig irideCfg = (IrideSecurityServiceConfig) cfg;

            this.serverURL               = buildServerURL(irideCfg.getServerURL());
            this.applicationName         = validateApplicationName(irideCfg.getApplicationName());
            this.adminRole               = validateAdminRole(irideCfg.getAdminRole());
            this.fallbackRoleServiceName = StringUtils.trimToNull(irideCfg.getFallbackRoleService());
        }

        /**
         * Returns {@code true} if a fallback {@link GeoServerRoleService} name has been defined, {@code false} otherwise.
         *
         * @return {@code true} if a fallback {@link GeoServerRoleService} name has been defined, {@code false} otherwise
         */
        boolean hasFallbackRoleServiceName() {
            return this.fallbackRoleServiceName != null;
        }

    }

}
