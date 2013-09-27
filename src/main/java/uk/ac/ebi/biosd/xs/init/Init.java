package uk.ac.ebi.biosd.xs.init;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.service.ebeye.EBeyeExport;

public class Init implements ServletContextListener
{
 static String EBeyeConnectionProfileParam = "ebeye.connectionProfile";
 static String EBeyeOutputPathParam = "ebeye.outputDir";
 static String EBeyeTempPathParam = "ebeye.tempDir";
 static String EBeyeUpdateHourParam = "ebeye.updateTime";
 static String EBeyeEfoURLParam = "ebeye.efoURL";
 static String EBeyeGenGroup = "ebeye.generateGroups";
 static String EBeyeGenSamples = "ebeye.generateSamples";

 static String PersistParamPrefix = "persist";
 static String DefaultProfileParam = PersistParamPrefix+".defaultProfile";

 private final Logger log = LoggerFactory.getLogger(Init.class);
 private final Timer timer = new Timer("Timer", true);

 
 @Override
 public void contextInitialized(ServletContextEvent ctx)
 {
  Map<String, Map<String,Object>> profMap = new HashMap<>();
  Map<String,Object> defaultProfile=null;
  String defProfName = null;
  
  Matcher mtch = Pattern.compile("^"+PersistParamPrefix+"(\\[\\s*(\\S+)\\s*\\])?\\.(\\S+)$").matcher("");
  
  ServletContext servletContext = ctx.getServletContext();
  
  Enumeration<?> pNames = servletContext.getInitParameterNames();
  
  while( pNames.hasMoreElements() )
  {
   String key = pNames.nextElement().toString();
   String val = servletContext.getInitParameter(key); 
  
   if( key.equals(DefaultProfileParam) )
   {
    defProfName = val;
    continue;
   }
   
   mtch.reset( key );
   
   if( ! mtch.matches() )
    continue;

   String profile = null;
   String param = null;

   if( mtch.groupCount() == 3 )
   {
    profile = mtch.group(2);
    param = mtch.group(3);
   }
   else
    param = mtch.group(mtch.groupCount());
   

   
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
  
  for( Map.Entry<String, Map<String,Object>> me : profMap.entrySet() )
  {
   if( me.getKey() == null )
    continue;
   
   EMFManager.addFactory( me.getKey(), Persistence.createEntityManagerFactory ( "X-S", me.getValue() )  );
  }
  
  if( defaultProfile != null )
   EMFManager.setDefaultFactory( Persistence.createEntityManagerFactory ( "X-S", defaultProfile ) );
  
  EntityManagerFactory emf;
  String connProf = servletContext.getInitParameter(EBeyeConnectionProfileParam);
  
  if( connProf == null )
   emf = EMFManager.getDefaultFactory();
  else
   emf = EMFManager.getFactory(connProf);
  
  if( emf == null )
  {
   log.warn("Invalid value for {} parameter. EBeye export will be disabled", EBeyeConnectionProfileParam);
   return;
  }
  
  EntityManager em = emf.createEntityManager();
  
  
  final boolean genGroup;
  final boolean genSamples;
  
  String gen = servletContext.getInitParameter(EBeyeGenGroup);
  
  genGroup =  ( gen != null )?
   "1".equals(gen) || "yes".equalsIgnoreCase(gen) || "on".equalsIgnoreCase(gen) || "true".equalsIgnoreCase(gen) : true;

  gen = servletContext.getInitParameter(EBeyeGenSamples);
  
  genSamples = ( gen != null )? "1".equals(gen) || "yes".equalsIgnoreCase(gen) || "on".equalsIgnoreCase(gen) || "true".equalsIgnoreCase(gen) : true;

  
  String outPath = servletContext.getInitParameter(EBeyeOutputPathParam);
  
  if( outPath == null )
  {
   log.warn("Parameter '{}' is missed. EBeye export will be disabled", EBeyeOutputPathParam);
   return;
  }
  
  String tempPath = servletContext.getInitParameter(EBeyeTempPathParam);

  if( tempPath == null )
  {
   log.warn("Parameter '{}' is missed. EBeye export will be disabled", EBeyeTempPathParam);
   return;
  }
 
  String efoURLStr = servletContext.getInitParameter( EBeyeEfoURLParam );
  
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
  
  String invokeTime = servletContext.getInitParameter(EBeyeUpdateHourParam);

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
    
  
  EBeyeExport.setInstance( new EBeyeExport(em, new File(outPath), new File(tempPath), efoURL ) );
  
  if( hour != -1 )
  {

   TimerTask task = new TimerTask()
   {
    @Override
    public void run()
    {
     try
     {
      EBeyeExport.getInstance().export(-1, genSamples, genGroup, true );
     }
     catch(IOException e)
     {
      log.error("Export error: "+e.getMessage());
     }
     
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
