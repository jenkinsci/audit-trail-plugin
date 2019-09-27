package hudson.plugins.audit_trail;

import static com.google.common.collect.Ranges.closedOpen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.collect.Range;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Default values are set in <code>/src/main/resources/hudson/plugins/audit_trail/ElasticSearchAuditLogger/config.jelly</code>
 *
 * @author <a href="mailto:alexander.russell@sap.com">Alex Russell</a>
 */
public class ElasticSearchAuditLogger extends AuditLogger {

    private URI uri;
    private String username;
    private Secret password;
    private String customServerCertificateId;
    private boolean skipCertificateValidation = false;

    private transient ElasticSearchSender elasticSearchSender;

    protected static final Logger LOGGER = Logger.getLogger(ElasticSearchAuditLogger.class.getName());
    private static final FastDateFormat DATE_FORMATTER = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ssZ");

    @DataBoundConstructor
    public ElasticSearchAuditLogger(URI esServerUri, boolean skipCertificateValidation) {
        this.uri = esServerUri;
        this.skipCertificateValidation = skipCertificateValidation;
    }

    @Override
    public void log(String event) {
        if (elasticSearchSender == null) {
            LOGGER.log(Level.FINER, "skip log {0}, elasticSearchSender not configured", event);
            return;
        }
        LOGGER.log(Level.FINER, "Send audit message \"{0}\" to Elastic Search server {1}", new Object[]{event, elasticSearchSender});
        try {
        	elasticSearchSender.sendMessage(event);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Exception sending audit message to Elastic Search server " + elasticSearchSender.toString(), e);
            LOGGER.warning(event);
        }
    }

    @Override
    public void configure() {
        if (uri == null || uri.toString().length() == 0) {
            LOGGER.fine("Elastic Search Logger not configured");
            return;
        }
        KeyStore clientKeyStore = null;
        String clientKeyStorePassword = null;

        // Fetch custom alias+certificate as a keystore (if present)
        if (!StringUtils.isBlank(customServerCertificateId)) {
            LOGGER.fine("Client certificate specified: " + customServerCertificateId);
            StandardCertificateCredentials certificateCredentials = getCredentials(customServerCertificateId);
            if (certificateCredentials != null) {
                // Fetch keystore containing custom certificate
                clientKeyStore = certificateCredentials.getKeyStore();
                clientKeyStorePassword = certificateCredentials.getPassword().getPlainText();
                LOGGER.fine("Custom keystore loaded");
            }
        }
        // Create the sender for Elastic Search
        try {
            elasticSearchSender = new ElasticSearchSender(uri, username, Secret.toString(password), clientKeyStore, clientKeyStorePassword, skipCertificateValidation);
            LOGGER.log(Level.FINE, "ElasticSearchAuditLogger: {0}", this);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Unable to create ElasticSearchSender", ioe);
        } catch (GeneralSecurityException gse) {
            LOGGER.log(Level.SEVERE, "Unable to create ElasticSearchSender", gse);
        }
    }

    private StandardCertificateCredentials getCredentials(String credentials) {
        return (StandardCertificateCredentials) CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(StandardCredentials.class,
                Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()),
          CredentialsMatchers.withId(credentials)
        );
    }

    @DataBoundSetter
    public void setUri(URL url) throws URISyntaxException {
        this.uri = url.toURI();
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getUsername() {
        return username;
    }

    @DataBoundSetter
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return Secret.toString(password);
    }

    @DataBoundSetter
    public void setPassword(String password) {
        this.password = Secret.fromString(password);
    }

    public String getCustomServerCertificateId() {
        return customServerCertificateId;
    }

    @DataBoundSetter
    public void setCustomServerCertificateId(String customServerCertificateId) {
        this.customServerCertificateId = customServerCertificateId;
    }

    public boolean getSkipCertificateValidation() {
    	return skipCertificateValidation;
    }

    @DataBoundSetter
    public void setSkipCertificateValidation(boolean skipCertificateValidation) {
        this.skipCertificateValidation = skipCertificateValidation;
    }

    public String getDisplayName() {
        return "Elastic Search Logger";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ElasticSearchAuditLogger)) return false;

        ElasticSearchAuditLogger that = (ElasticSearchAuditLogger) o;

        if (uri != null ? !uri.equals(that.uri) : that.uri != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }
        if (!Secret.toString(password).equals(that.getPassword())) {
            return false;
        }
        if (customServerCertificateId != null ? !customServerCertificateId.equals(that.customServerCertificateId) : that.customServerCertificateId != null) {
            return false;
        }
        if (skipCertificateValidation != that.skipCertificateValidation) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        result = prime * result + Secret.toString(password).hashCode();
        result = prime * result + ((customServerCertificateId == null) ? 0 : customServerCertificateId.hashCode());
        result = prime * result + Boolean.hashCode(skipCertificateValidation);
        return result;
    }

    @Override
    public String toString() {
        return "ElasticSearchAuditLogger{" +
                "uri='" + uri + "'" +
                ", username='" + username + "'" +
                ", password='" + password + "'" +
                ", customServerCertificateId='" + customServerCertificateId + "'" +
                ", skipCertificateValidation='" + skipCertificateValidation + "'" +
                "}";
    }

    class ElasticSearchSender {
    	private final CloseableHttpClient httpClient;
        private final Range<Integer> successCodes = closedOpen(200,300);

        private final URI uri;
        private final String auth;

        public ElasticSearchSender(URI uri, String username, String password, KeyStore clientKeyStore, String clientKeyStorePassword, boolean skipCertificateValidation) throws IOException, GeneralSecurityException {
            this.uri = uri;
            if (StringUtils.isNotBlank(username)) {
                auth = Base64.encodeBase64String((username + ":" + StringUtils.defaultString(password)).getBytes(StandardCharsets.UTF_8));
            } else {
                auth = null;
            }
            httpClient = createHttpClient(clientKeyStore, clientKeyStorePassword, skipCertificateValidation);
        }

        private CloseableHttpClient createHttpClient(KeyStore keyStore, String keyStorePassword, boolean skipCertificateValidation)
                throws IOException, GeneralSecurityException {
            TrustStrategy trustStrategy = null;
            if (skipCertificateValidation) {
                trustStrategy = new TrustSelfSignedStrategy();
            }
            SSLContextBuilder contextBuilder = SSLContexts.custom();
            contextBuilder.loadTrustMaterial(keyStore, trustStrategy);
            if (keyStore != null) {
                contextBuilder.loadKeyMaterial(keyStore, keyStorePassword.toCharArray());
            }
            SSLContext sslContext = contextBuilder.build();
            HttpClientBuilder builder = HttpClients.custom();
            builder.setSslcontext(sslContext);
            if (skipCertificateValidation) {
                builder.setSSLHostnameVerifier(new NoopHostnameVerifier());
            }
            return builder.build();
        }

        public void sendMessage(String event) throws IOException {
            HttpPost post = getHttpPost(event);
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                if (!successCodes.contains(response.getStatusLine().getStatusCode())) {
                    throw new IOException(this.getErrorMessage(response));
                }
            }
        }

        HttpPost getHttpPost(String data) {
            HttpPost postRequest = new HttpPost(uri);
            // char encoding is set to UTF_8 since this request posts a JSON string
            JSONObject payload = new JSONObject();
            payload.put("message", data);
            payload.put("@timestamp", DATE_FORMATTER.format(Calendar.getInstance().getTime()));
            StringEntity input = new StringEntity(payload.toString(), StandardCharsets.UTF_8);
            input.setContentType(ContentType.APPLICATION_JSON.toString());
            postRequest.setEntity(input);
            if (auth != null) {
                postRequest.addHeader("Authorization", "Basic " + auth);
            }
            return postRequest;
        }

        private String getErrorMessage(CloseableHttpResponse response) {
            ByteArrayOutputStream byteStream = null;
            PrintStream stream = null;
            try {
                byteStream = new ByteArrayOutputStream();
                stream = new PrintStream(byteStream, true, StandardCharsets.UTF_8.name());

                try {
                    stream.print("HTTP error code: ");
                    stream.println(response.getStatusLine().getStatusCode());
                    stream.print("URI: ");
                    stream.println(uri.toString());
                    stream.println("RESPONSE: " + response.toString());
                    response.getEntity().writeTo(stream);
                } catch (IOException e) {
                	stream.println(ExceptionUtils.getStackTrace(e));
                }
                stream.flush();
                return byteStream.toString(StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                return ExceptionUtils.getStackTrace(e);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AuditLogger> {

        @Override
        public String getDisplayName() {
            return "Elastic Search server";
        }

        public ListBoxModel doFillCustomServerCertificateIdItems(
            @AncestorInPath Item item,
            @QueryParameter String customServerCertificateId,
            @QueryParameter String uri) {
            List<DomainRequirement> domainRequirements = URIRequirementBuilder.fromUri(uri).build();
            return new StandardListBoxModel().includeEmptyValue().includeAs(ACL.SYSTEM, item,
            		StandardCertificateCredentials.class, domainRequirements);
        }

        public FormValidation doCheckUri(@QueryParameter("value") String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.warning("URI must be set");
            }
            try {
                URL url = new URL(value);

                if (url.getUserInfo() != null) {
                    return FormValidation.error("Please specify user and password not as part of the url.");
                }

                if(StringUtils.isBlank(url.getPath()) || url.getPath().trim().matches("^\\/+$")) {
                    return FormValidation.warning("Elastic Search requires a key to be able to index the logs.");
                }

                url.toURI();
            } catch (MalformedURLException | URISyntaxException e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok();
        }
    }

}
