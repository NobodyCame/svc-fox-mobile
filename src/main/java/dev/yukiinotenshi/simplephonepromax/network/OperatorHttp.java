package dev.yukiinotenshi.simplephonepromax.network;
import java.net.URI;
import java.net.http.HttpRequest;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import javax.net.ssl.*;
import java.util.concurrent.CompletableFuture;
/** The public hostname uses JVM system trust; the bundled CA remains for legacy IP URLs. */
public final class OperatorHttp {
 public static final String URL="https://yukiinotenshi.dynv6.net/svc-fox-mobile";
 private static final String LEGACY_HOST="92.5.184.221";
 private static final String CURRENT_HOST="yukiinotenshi.dynv6.net";
 private static final SSLContext OPERATOR=create();
 private static SSLContext create(){try(var in=OperatorHttp.class.getResourceAsStream("/operator/root.crt")){
  if(in==null)throw new IllegalStateException("Missing operator certificate");
  var ks=KeyStore.getInstance(KeyStore.getDefaultType());ks.load(null,null);ks.setCertificateEntry("operator",CertificateFactory.getInstance("X.509").generateCertificate(in));
  var tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(ks);
  var ssl=SSLContext.getInstance("TLS");ssl.init(null,tm.getTrustManagers(),null);
  return ssl;
 }catch(Exception e){throw new ExceptionInInitializerError(e);}}
 public static CompletableFuture<BackendRouteHttp.TextResponse> sendAsync(HttpRequest request){
  var config=dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient.config;
  var route=new BackendRouteHttp.Settings();
  if(config!=null){
   route.autoFallback=config.backendAutoFallback;route.forcedRoute=config.backendRouteMode;
   route.primaryEndpoint=config.backendPrimaryEndpoint;route.fallbackEndpoint=config.backendFallbackEndpoint;
   route.connectTimeoutSeconds=config.backendConnectTimeoutSeconds;route.requestTimeoutSeconds=config.backendRequestTimeoutSeconds;
  }
  URI uri=request.uri();
  SSLContext trust="https".equalsIgnoreCase(uri.getScheme())&&LEGACY_HOST.equalsIgnoreCase(uri.getHost())?OPERATOR:null;
  return LocalOperatorClient.send(request,()->BackendRouteHttp.sendAsync(request,route,trust,false));
 }
 public static String migrateLegacyHostOnce(String configured){
  if(configured==null||configured.isBlank())return URL;
  try{
   URI uri=URI.create(configured.trim());
   if(!LEGACY_HOST.equalsIgnoreCase(uri.getHost()))return configured.trim();
   String path=uri.getRawPath()==null?"":uri.getRawPath();
   if(path.isEmpty())path="/svc-fox-mobile";
   String userInfo=uri.getRawUserInfo()==null?"":uri.getRawUserInfo()+"@";
   String port=uri.getPort()<0?"":":"+uri.getPort();
   String query=uri.getRawQuery()==null?"":"?"+uri.getRawQuery();
   String fragment=uri.getRawFragment()==null?"":"#"+uri.getRawFragment();
   return URI.create("https://"+userInfo+CURRENT_HOST+port+path+query+fragment).toString();
  }catch(IllegalArgumentException ignored){return configured.trim();}
 }
 public static String upgrade(String url){
  if(url==null||url.isBlank())return URL;
  String clean=url.trim().replaceAll("/+$","");
  return clean;
 }
}


