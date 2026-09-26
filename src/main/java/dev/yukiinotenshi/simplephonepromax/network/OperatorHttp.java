package dev.yukiinotenshi.simplephonepromax.network;
import java.net.URI;
import java.net.http.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import javax.net.ssl.*;
import java.time.Duration;
/** Trust bundled operator CA only for the known IP; all other services use system trust. */
public final class OperatorHttp {
 public static final String URL="https://92.5.184.221/svc-fox-mobile";
 private static final HttpClient DEFAULT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
 private static final HttpClient OPERATOR=create();
 private static HttpClient create(){try(var in=OperatorHttp.class.getResourceAsStream("/operator/root.crt")){
  if(in==null)throw new IllegalStateException("Missing operator certificate");
  var ks=KeyStore.getInstance(KeyStore.getDefaultType());ks.load(null,null);ks.setCertificateEntry("operator",CertificateFactory.getInstance("X.509").generateCertificate(in));
  var tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(ks);
  var ssl=SSLContext.getInstance("TLS");ssl.init(null,tm.getTrustManagers(),null);
  return HttpClient.newBuilder().sslContext(ssl).connectTimeout(Duration.ofSeconds(4)).build();
 }catch(Exception e){throw new ExceptionInInitializerError(e);}}
 public static HttpClient client(URI uri){return "https".equalsIgnoreCase(uri.getScheme())&&"92.5.184.221".equals(uri.getHost())?OPERATOR:DEFAULT;}
 public static String upgrade(String url){
  if(url==null||url.isBlank())return URL;
  String clean=url.trim().replaceAll("/+$","");
  if(clean.equals("http://92.5.184.221/simple-phone")||clean.equals("https://92.5.184.221/simple-phone")
    ||clean.equals("http://92.5.184.221/svc-fox-mobile"))return URL;
  return clean;
 }
}


