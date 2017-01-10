package funcatron.java_spring_sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;

@Configuration
public class RestConfig
{

    private final Logger log = LoggerFactory.getLogger(RestConfig.class);

    // TODO: Find cleaner way to deal with default values (note the empty string after the colon).
    @Value("${client.ssl.keyStoreFile:}")
    private String keyStoreFile;

    @Value("${client.ssl.keyStorePassword:}")
    private String keyStorePassword;

    @Value("${client.ssl.trustStoreFile:}")
    private String trustStoreFile = null;

    @Value("${client.ssl.trustStorePassword:}")
    private String trustStorePassword;

    @Value("${restTemplateConnectTimeout}")
    private Integer restTemplateConnectTimeout;

    @Value("${restTemplateReadTimeout}")
    private Integer restTemplateReadTimeout;

    @Bean
    public RestTemplate restTemplate()
    {
        return new RestTemplate(clientHttpRequestFactory());
    }
    @Bean
    public RestTemplate httpRestTemplate()
    {
        return new RestTemplate(httpClientHttpRequestFactory());
    }

    @Bean
    public RestTemplate catFoodRestTemplate()
    {
        return new RestTemplate(httpClientHttpRequestFactory());
    }

    @Bean
    public RestTemplate dogFoodRestTemplate()
    {
        return new RestTemplate(mutualSslClientHttpRequestFactory());
    }

    /* ClientHttpRequestFactory for non-mutual-SSL requests. */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory()
    {

        CloseableHttpClient httpClient = null;
        HttpComponentsClientHttpRequestFactory httpClientHttpRequestFactory = null;
        PoolingHttpClientConnectionManager connectionManager;
        Registry<ConnectionSocketFactory> socketFactoryRegistry;
        SSLConnectionSocketFactory sslConnectionSocketFactory;
        SSLContext sslContext;
        SSLContextBuilder sslContextBuilder;

        try
        {
            sslContextBuilder = SSLContexts.custom();

            // If we need to pass a truststore to validate UHG signed certs...
            if (trustStoreFile != null && !trustStoreFile.isEmpty())
            {
                sslContextBuilder.loadTrustMaterial(stream2File(this.getClass().getClassLoader().getResourceAsStream(trustStoreFile)), trustStorePassword.toCharArray());
            }

            sslContext = sslContextBuilder.build();
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext);
            socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("https", sslConnectionSocketFactory).build();
            connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            httpClient =
                    HttpClientBuilder.create().setConnectionManager(connectionManager).setConnectionManagerShared(true)
                            .setSSLSocketFactory(sslConnectionSocketFactory).build();

            httpClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

            // TODO: Do something smarter with circuit breakers.
            httpClientHttpRequestFactory.setConnectTimeout(restTemplateConnectTimeout);
            httpClientHttpRequestFactory.setReadTimeout(restTemplateReadTimeout);
        }
        catch (Exception e)
        {
            // TODO: What should we do with errors here?

        }
        finally
        {
            safeCloseClient(httpClient);
        }

        return httpClientHttpRequestFactory;
    }

    /* ClientHttpRequestFactory for mutual-SSL requests. */
    @Bean
    public ClientHttpRequestFactory mutualSslClientHttpRequestFactory()
    {

        // Set up Mutual SSL
        CloseableHttpClient httpClient = null;
        HttpComponentsClientHttpRequestFactory httpClientHttpRequestFactory = null;
        KeyStore keyStore;
        PoolingHttpClientConnectionManager connectionManager;
        Registry<ConnectionSocketFactory> socketFactoryRegistry;
        SSLConnectionSocketFactory sslConnectionSocketFactory;
        SSLContext sslContext;
        SSLContextBuilder sslContextBuilder;

        try
        {
            keyStore = KeyStore.getInstance("JKS");
            // Providing keyStorePassword results in integrity checking happening on keystore. Use null to avoid this check.
            InputStream stream = this.getClass().getClassLoader().getResourceAsStream(keyStoreFile); //new ClassPathResource(keyStoreFile).getInputStream();
            keyStore.load(stream, keyStorePassword.toCharArray());
            stream.close();
            sslContextBuilder = SSLContexts.custom().loadKeyMaterial(keyStore, keyStorePassword.toCharArray());

            // If we need to pass a truststore to validate UHG signed certs...
            if (trustStoreFile != null && !trustStoreFile.isEmpty())
            {
                sslContextBuilder.loadTrustMaterial(stream2File(this.getClass().getClassLoader().getResourceAsStream(trustStoreFile)), trustStorePassword.toCharArray());
            }

            sslContext = sslContextBuilder.build();
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext);
            socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("https", sslConnectionSocketFactory).build();
            connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            httpClient =
                    HttpClientBuilder.create().setConnectionManager(connectionManager).setConnectionManagerShared(true)
                            .setSSLSocketFactory(sslConnectionSocketFactory).build();

            httpClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

            // TODO: Do something smarter with circuit breakers.
            httpClientHttpRequestFactory.setConnectTimeout(restTemplateConnectTimeout);
            httpClientHttpRequestFactory.setReadTimeout(restTemplateReadTimeout);
        }
        catch (Exception e)
        {
            // TODO: What should we do with errors here?
            e.printStackTrace();
        }
        finally
        {
            safeCloseClient(httpClient);
        }

        return httpClientHttpRequestFactory;
    }

    public static File stream2File (InputStream in) throws IOException
    {
        return stream2File(in, null, null);
    }

    public static File stream2File (InputStream in, String prefix, String suffix) throws IOException
    {
        prefix = (prefix == null) ? "temp" : prefix;
        suffix = (suffix == null) ? "file" : suffix;

        final File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();
        try (FileOutputStream  out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }


    /* ClientHttpRequestFactory for http requests. */
    @Bean
    public ClientHttpRequestFactory httpClientHttpRequestFactory()
    {

        // Set up Mutual SSL
        CloseableHttpClient httpClient = null;
        HttpComponentsClientHttpRequestFactory httpClientHttpRequestFactory = null;

        try
        {
            httpClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();

            // TODO: Do something smarter with circuit breakers.
            httpClientHttpRequestFactory.setConnectTimeout(restTemplateConnectTimeout);
            httpClientHttpRequestFactory.setReadTimeout(restTemplateReadTimeout);
        }
        catch (Exception e)
        {
            // TODO: What should we do with errors here?
            e.printStackTrace();
        }
        finally
        {
            safeCloseClient(httpClient);
        }

        return httpClientHttpRequestFactory;
    }

    public void safeCloseClient(CloseableHttpClient httpClient)
    {
        try
        {
            if (httpClient != null)
            {
                httpClient.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}