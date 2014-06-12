package uk.ac.ebi.biosd.xs.init;

import java.io.File;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.ebeye.EBeyeExport;
import uk.ac.ebi.biosd.xs.email.Email;
import uk.ac.ebi.biosd.xs.task.ExportTask;
import uk.ac.ebi.biosd.xs.task.TaskConfig;
import uk.ac.ebi.biosd.xs.task.TaskInitError;
import uk.ac.ebi.biosd.xs.task.TaskManager;
import uk.ac.ebi.biosd.xs.util.MapParamPool;
import uk.ac.ebi.biosd.xs.util.ParamPool;
import uk.ac.ebi.biosd.xs.util.ResourceBundleParamPool;
import uk.ac.ebi.biosd.xs.util.ServletContextParamPool;

public class Init2 implements ServletContextListener
{

// public static final String TaskRequestPrefix = "request.";
 
 public static final String EmailParamPrefix = "email.";
 


 static String TaskTmpDirParam = "tempDir";
 static String TaskTimeParam = "updateTime";

 
 static final String ebeyeSrcSeparator = ";";
 static final String ebeyeSrcSubstSeparator = ":";
 
 static String BioSDDBParamPrefix = "biosddb";
 static String MyEQDBParamPrefix = "myeqdb";
 static String TaskParamPrefix = "task";
 static String OutputParamPrefix = "output";
 
// static String DefaultProfileParam = BioSDDBParamPrefix+".defaultProfile";

 private final Logger log = LoggerFactory.getLogger(Init2.class);
 private Timer timer;

 
 @Override
 public void contextInitialized(ServletContextEvent ctx)
 {
  java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.WARNING);
  java.util.logging.Logger.getLogger("com.mchange").setLevel(Level.WARNING);
  
  Map<String, Map<String,Object>> profMap = new HashMap<>();
  Map<String, Map<String,Object>> myEqMap = new HashMap<>();
  Map<String, TaskConfig> tasksMap = new HashMap<>();

  
  Matcher biodbMtch = Pattern.compile("^"+BioSDDBParamPrefix+"(\\[\\s*(\\S+)\\s*\\])?\\.(\\S+)$").matcher("");
  Matcher myeqMtch = Pattern.compile("^"+MyEQDBParamPrefix+"(\\[\\s*(\\S+)\\s*\\])?\\.(\\S+)$").matcher("");
  Matcher taskMtch = Pattern.compile("^"+TaskParamPrefix+"(\\[\\s*(\\S+)\\s*\\])?\\.(\\S+)$").matcher("");
  Matcher outMtch = Pattern.compile("^"+OutputParamPrefix+"(?:\\[\\s*(\\S+)\\s*\\])?\\.(\\S+)$").matcher("");
  
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
  
  
  Enumeration<String> pNames = config.getNames();
  
  while( pNames.hasMoreElements() )
  {
   String key = pNames.nextElement();
   String val = config.getParameter(key);


   biodbMtch.reset(key);

   if(biodbMtch.matches())
   {

    String profile = null;
    String param = null;

    if(biodbMtch.groupCount() == 3)
    {
     profile = biodbMtch.group(2);
     param = biodbMtch.group(3);
    }
    else
     param = biodbMtch.group(biodbMtch.groupCount());

    Map<String, Object> cm = profMap.get(profile);

    if(cm == null)
     profMap.put(profile, cm = new TreeMap<>());

    cm.put(param, val);
   }
   else
   {
    myeqMtch.reset(key);

    if(myeqMtch.matches())
    {

     String profile = null;
     String param = null;

     if(myeqMtch.groupCount() == 3)
     {
      profile = myeqMtch.group(2);
      param = myeqMtch.group(3);
     }
     else
      log.warn("Invalid parameter {} will be ignored.", key);

     Map<String, Object> cm = myEqMap.get(profile);

     if(cm == null)
      myEqMap.put(profile, cm = new TreeMap<>());

     cm.put(param, val);
    }
    else
    {
     taskMtch.reset(key);

     if(taskMtch.matches())
     {

      String taskName = null;
      String param = null;

      if(taskMtch.groupCount() == 3)
      {
       taskName = taskMtch.group(2);
       param = taskMtch.group(3);
      }
      else
       log.warn("Invalid parameter {} will be ignored.", key);
      
      if( taskName == null )
       taskName = "_default_";

      TaskConfig cm = tasksMap.get(taskName);

      if(cm == null)
       tasksMap.put(taskName, cm = new TaskConfig(taskName) );

      outMtch.reset(val);
      
      if( outMtch.matches() )
      {
       
      }
      else
       cm.readParameter(param, val);
     }
    }
   }
  }
  

  
  for( Map.Entry<String, Map<String,Object>> me : profMap.entrySet() )
  {
   if( me.getKey() == null )
    continue;
   
   EMFManager.addFactory( me.getKey(), Persistence.createEntityManagerFactory ( "X-S", me.getValue() )  );
  }
  
 
 
  try
  {
   Email.setDefaultInstance( new Email(config,EmailParamPrefix) );
  }
  catch( Exception e )
  {
   log.warn("Can't init email. Emails will be disabled. Error: "+e.getMessage());
  }

  
  
  createTasks(tasksMap);
  
  for( TaskInfo tinf : TaskManager.getDefaultInstance().getTasks() )
  {
   if( tinf.getTimerDelay() > 0 )
   {
    if(timer == null)
     timer = new Timer("Timer", true);
    
    timer.scheduleAtFixedRate(tinf, tinf.getTimerDelay(), day);
   
    log.info("Task '"+tinf.getTask().getName()+"' is scheduled to run periodically");
   }
  }
   
  
 }

 private void createTasks(Map<String, Map<String, Object>> tasksMap)
 {
  for( Map.Entry<String, Map<String, Object>> me : tasksMap.entrySet() )
  {
   TaskInfo tinf = new TaskInfo();
   
   ParamPool pp = new MapParamPool(me.getValue());
   
   TaskConfig rc = new TaskConfig();
   rc.loadParameters(pp, TaskRequestPrefix);
   
   String str = rc.getServer(null);
   
   if( str == null )
   {
    log.warn("Task '"+me.getKey()+"' has not defined 'server' parameter and will be disabled");
    continue;
   }
   
   EntityManagerFactory emf = EMFManager.getFactory(str);
   
   if( emf == null )
   {
    log.warn("Task '"+me.getKey()+"': Server connection '"+str+"' is not defined. Task will be disabled");
    continue;
   }
   
   EntityManagerFactory myEqFact=null;
   str = rc.getMyEq(null);
   
   if( str != null )
   {
    myEqFact = EMFManager.getMyEqFactory(str);
    
    if( myEqFact == null )
     log.warn("Task '"+me.getKey()+"': MyEq connection '"+str+"' is not defined. MyEq support will be disabled");
   }
   
   Object val = me.getValue().get(TaskTmpDirParam);
   
   str = null;
   
   if( val != null )
    str = val.toString();
   
   File tmpDir = null;
   
   if( str != null )
   {
    tmpDir = new File(str);
    
    if(  ! ( tmpDir.isDirectory() && tmpDir.canWrite() ) )
    {
     tmpDir = null;
     log.warn("Task '"+me.getKey()+"': Tmp dir '"+str+"' is not writable. Falling back to default tmp dir." );
    }
   }
   
   
   val = me.getValue().get(TaskTimeParam);
   
   str = null;
   
   if( val != null )
    str = val.toString();

   
   tinf.setTimerDelay( getAdjustedDelay(str,me.getKey()) );

   try
   {
    ExportTask tsk = new ExportTask(me.getKey(), emf, myEqFact, tmpDir, rc);
    
    tinf.setTask(tsk);
    
    TaskManager.getDefaultInstance().addTask(tinf);
   }
   catch(TaskInitError e)
   {
    log.warn("Task '"+me.getKey()+"': Initialization error: "+e.getMessage() );
   }
   
  }
  
 }
 
 private long getAdjustedDelay( String invokeTime , String taskName)
 {

  int hour = -1;
  int min = 0;
  
  if( invokeTime == null )
  {
   return -1;
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
    log.error("Task '"+taskName+"': start time parameter has invalid value: '"+invokeTime+"'. Task will not run periodicaly");
    return -1;
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
     log.error("Task '"+taskName+"': start time parameter has invalid value: '"+invokeTime+"'. Task will not run periodicaly");
     return -1;
    }
   }
   
  }
  
  
  Calendar cr = Calendar.getInstance(TimeZone.getDefault());
  cr.setTimeInMillis(System.currentTimeMillis());
  long day = TimeUnit.DAYS.toMillis(1);
  
  cr.set(Calendar.HOUR_OF_DAY, hour);
  cr.set(Calendar.MINUTE, min);
  
  long delay = cr.getTimeInMillis() - System.currentTimeMillis();
  
  long adjustedDelay = (delay > 0 ? delay : day + delay);
  
  return adjustedDelay;

 }
 

 @Override
 public void contextDestroyed(ServletContextEvent arg0)
 {
  if( timer != null )
   timer.cancel();
  
  for( TaskInfo tinf : TaskManager.getDefaultInstance().getTasks() )
  {
   if( tinf.getTimer() != null )
    tinf.getTimer().cancel();
   
   tinf.getTask().interrupt();
  }
  
  if( EBeyeExport.getInstance() != null )
   EBeyeExport.getInstance().interrupt();
 
  EMFManager.destroy();
 }



}
