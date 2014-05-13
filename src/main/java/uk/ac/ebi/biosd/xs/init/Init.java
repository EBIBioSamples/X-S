package uk.ac.ebi.biosd.xs.init;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.ebeye.EBeyeExport;
import uk.ac.ebi.biosd.xs.service.RequestConfig;
import uk.ac.ebi.biosd.xs.util.ParamPool;
import uk.ac.ebi.biosd.xs.util.ResourceBundleParamPool;
import uk.ac.ebi.biosd.xs.util.ServletContextParamPool;

public class Init implements ServletContextListener
{
 public static final String EBeyeRequestPrefix = "ebeye.request.";
 
// static String EBeyeConnectionProfileParam = "ebeye.connectionProfile";
// static String EBeyeMyEqProfileParam = "ebeye.myeqProfile";
// static String EBeyeThreads = "ebeye.threads";

 static String EBeyeOutputPathParam = "ebeye.outputDir";
 static String EBeyeTempPathParam = "ebeye.tempDir";
 static String EBeyeUpdateHourParam = "ebeye.updateTime";
 static String EBeyeEfoURLParam = "ebeye.efoURL";
 static String EBeyeGenSamples = "ebeye.generateSamples";
 static String EBeyeSources = "ebeye.sources";

 static final String ebeyeSrcSeparator = ";";
 static final String ebeyeSrcSubstSeparator = ":";
 
 static String BioSDDBParamPrefix = "biosddb";
 static String MyEQDBParamPrefix = "myeqdb";
 
 static String DefaultProfileParam = BioSDDBParamPrefix+".defaultProfile";

 private final Logger log = LoggerFactory.getLogger(Init.class);
 private final Timer timer = new Timer("Timer", true);

 
 @Override
 public void contextInitialized(ServletContextEvent ctx)
 {
  Map<String, Map<String,Object>> profMap = new HashMap<>();
  Map<String, Map<String,Object>> myEqMap = new HashMap<>();

  Map<String,Object> defaultProfile=null;
  String defProfName = null;
  
  Matcher prstMtch = Pattern.compile("^"+BioSDDBParamPrefix+"(\\[\\s*(\\S+)\\s*\\])?\\.(\\S+)$").matcher("");
  Matcher myeqMtch = Pattern.compile("^"+MyEQDBParamPrefix+"(\\[\\s*(\\S+)\\s*\\])?\\.(\\S+)$").matcher("");
  
  ParamPool config = null;

  ResourceBundle rb = null;
  
  try
  {
   rb = ResourceBundle.getBundle("testconfig");
  }
  catch(MissingResourceException ex)
  {}
  
  if( rb != null )
   config = new ResourceBundleParamPool(rb);
  else
   config = new ServletContextParamPool(ctx.getServletContext());
  
//  final ServletContext servletContext = ctx.getServletContext();
  
  Enumeration<String> pNames = config.getNames();
  
  while( pNames.hasMoreElements() )
  {
   String key = pNames.nextElement();
   String val = config.getParameter(key);
  
   if( key.equals(DefaultProfileParam) )
   {
    defProfName = val;
    continue;
   }
   
   prstMtch.reset( key );
   
   if( ! prstMtch.matches() )
    continue;

   String profile = null;
   String param = null;

   if( prstMtch.groupCount() == 3 )
   {
    profile = prstMtch.group(2);
    param = prstMtch.group(3);
   }
   else
    param = prstMtch.group(prstMtch.groupCount());
   

   
   Map<String,Object> cm = profMap.get(profile);
   
   if( cm == null )
    profMap.put(profile, cm = new TreeMap<>() );
   
   cm.put(param, val);
  }
  
  defaultProfile = profMap.get(null);
  
  if( defProfName != null )
  {
   Map<String,Object> namedDP = profMap.get(defProfName);
   
   if( defaultProfile != null )
   {
    if( namedDP != null )
     defaultProfile.putAll(namedDP);
   }
   else
    defaultProfile = namedDP;
  }
  
  pNames = config.getNames();
  
  while( pNames.hasMoreElements() )
  {
   String key = pNames.nextElement().toString();
   String val = config.getParameter(key); 
  
   
   myeqMtch.reset( key );
   
   if( ! myeqMtch.matches() )
    continue;

   String profile = null;
   String param = null;

   if( myeqMtch.groupCount() == 3 )
   {
    profile = myeqMtch.group(2);
    param = myeqMtch.group(3);
   }
   else
    log.warn("Invalid parameter {} will be ignored.", key);
    

   
   Map<String,Object> cm = myEqMap.get(profile);
   
   if( cm == null )
    myEqMap.put(profile, cm = new TreeMap<>() );
   
   cm.put(param, val);
  }
  
  for( Map.Entry<String, Map<String,Object>> me : profMap.entrySet() )
  {
   if( me.getKey() == null )
    continue;
   
   EMFManager.addFactory( me.getKey(), Persistence.createEntityManagerFactory ( "X-S", me.getValue() )  );
  }
  
  if( defaultProfile != null )
   EMFManager.setDefaultFactory( Persistence.createEntityManagerFactory ( "X-S", defaultProfile ) );
  
  RequestConfig reqCfg = new RequestConfig();
  reqCfg.loadParameters(config, EBeyeRequestPrefix);
  
  EntityManagerFactory emf;

  String connProf = reqCfg.getServer(null);
  
  if( connProf == null )
   emf = EMFManager.getDefaultFactory();
  else
   emf = EMFManager.getFactory(connProf);
  
  if( emf == null )
  {
   log.warn("Invalid value for {} parameter. EBeye export will be disabled", EBeyeRequestPrefix+RequestConfig.ProfileParameter);
   return;
  }
  
  for( Map.Entry<String, Map<String,Object>> me : myEqMap.entrySet() )
  {
   if( me.getKey() == null )
    continue;
   
   EMFManager.addMyEqFactory( me.getKey(), Persistence.createEntityManagerFactory ( "MyEq", me.getValue() )  );
  }

  
  String str = reqCfg.getMyEq(null);
  
  EntityManagerFactory myEqFact = null;
  
  if( str != null )
  {
   myEqFact = EMFManager.getMyEqFactory(str);
   
   if( myEqFact == null )
    log.warn("MyEq profile \""+str+"\" is not defined. MyEq support will be disabled");

  }
  
  final boolean genSamples;

  String gen = config.getParameter(EBeyeGenSamples);
  
  genSamples = ( gen != null )? "1".equals(gen) || "yes".equalsIgnoreCase(gen) || "on".equalsIgnoreCase(gen) || "true".equalsIgnoreCase(gen) : true;

  
  String outPath = config.getParameter(EBeyeOutputPathParam);
  
  if( outPath == null )
  {
   log.warn("Parameter '{}' is missed. EBeye export will be disabled", EBeyeOutputPathParam);
   return;
  }
  
  String tempPath = config.getParameter(EBeyeTempPathParam);

  if( tempPath == null )
  {
   log.warn("Parameter '{}' is missed. EBeye export will be disabled", EBeyeTempPathParam);
   return;
  }
 

  Map<String,String> ebeyeSrcMap = null;
  str = config.getParameter( EBeyeSources );
  
  if( str != null )
  {
   ebeyeSrcMap = new HashMap<>();
   
   String[] srcs = str.split(ebeyeSrcSeparator);
   
   for( String s : srcs )
   {
    s=s.trim();
    
    String[] mp = s.split(ebeyeSrcSubstSeparator);
    
    if( mp.length > 1 )
     ebeyeSrcMap.put(mp[0].trim(), mp[1].trim());
    else
     ebeyeSrcMap.put(s, s);
   }
   
  }

  String efoURLStr = config.getParameter( EBeyeEfoURLParam );
  
  if( efoURLStr == null )
  {
   log.warn("Parameter '{}' is missed. EBeye export will be disabled", EBeyeEfoURLParam);
   return;
  }
 
  URL efoURL = null;

  try
  {
   efoURL = new URL(efoURLStr);
  }
  catch(MalformedURLException e)
  {
  }
  
  if( efoURL == null )
  {
   log.warn("Invalid URL in parameter {}. EBeye export will be disabled", EBeyeEfoURLParam);
   return;
  }
  
  String invokeTime = config.getParameter(EBeyeUpdateHourParam);

  int hour = -1;
  int min = 0;
  
  if( invokeTime == null )
  {
   log.warn("Parameter '{}' is missed. EBeye export will not run periodicaly", EBeyeUpdateHourParam);
  }
  else
  {
   int colPos = invokeTime.indexOf(':');
   
   String hourStr = invokeTime;
   String minStr = null;
   
   if( colPos >= 0  )
   {
    hourStr = invokeTime.substring(0,colPos);
    minStr = invokeTime.substring(colPos+1);
   }
   
   try
   {
    hour = Integer.parseInt(hourStr);
   }
   catch( Exception e )
   {}
   
   if( hour < 0 || hour > 23 )
   {
    log.warn("Parameter '{}' has invalid value. EBeye export will not run periodicaly", EBeyeUpdateHourParam);
    hour=-1;
   }
   
   if( minStr != null )
   {
    try
    {
     min = Integer.parseInt(minStr);
    }
    catch( Exception e )
    {}
    
    if( min < 0 || min > 59 )
    {
     log.warn("Parameter '{}' has invalid value. EBeye export will not run periodicaly", EBeyeUpdateHourParam);
     hour=-1;
    }
   }
   
  }
  
  
  final int threads = reqCfg.getThreads(Runtime.getRuntime().availableProcessors());

  
  EBeyeExport.setInstance( new EBeyeExport(emf, myEqFact, new File(outPath), new File(tempPath), efoURL, reqCfg, ebeyeSrcMap ) );
  
  if( hour != -1 )
  {

   TimerTask task = new TimerTask()
   {
    @Override
    public void run()
    {
     log.info("Starting scheduled task");
     
     try
     {
      EBeyeExport.getInstance().export(-1, genSamples, true, threads );
     }
     catch(Throwable e)
     {
      log.error("Export error: "+(e.getMessage()!=null?e.getMessage():e.getClass().getName()));
     }
     
     log.info("Finishing scheduled task");
    }
   };
   
   Calendar cr = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
   cr.setTimeInMillis(System.currentTimeMillis());
   long day = TimeUnit.DAYS.toMillis(1);
   
   cr.set(Calendar.HOUR_OF_DAY, hour);
   cr.set(Calendar.MINUTE, min);
   
   long delay = cr.getTimeInMillis() - System.currentTimeMillis();
   
   long adjustedDelay = (delay > 0 ? delay : day + delay);
   
   timer.scheduleAtFixedRate(task, adjustedDelay, day);
  }
  
  
 }

 @Override
 public void contextDestroyed(ServletContextEvent arg0)
 {
  timer.cancel();
 }



}
