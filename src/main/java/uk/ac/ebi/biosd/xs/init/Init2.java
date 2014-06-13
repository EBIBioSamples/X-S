package uk.ac.ebi.biosd.xs.init;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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
import uk.ac.ebi.biosd.xs.output.EBEyeOutputModule;
import uk.ac.ebi.biosd.xs.output.OutputModule;
import uk.ac.ebi.biosd.xs.output.XMLDumpOutputModule;
import uk.ac.ebi.biosd.xs.task.ExportTask2;
import uk.ac.ebi.biosd.xs.task.TaskConfig;
import uk.ac.ebi.biosd.xs.task.TaskConfigException;
import uk.ac.ebi.biosd.xs.task.TaskInitError;
import uk.ac.ebi.biosd.xs.task.TaskManager;
import uk.ac.ebi.biosd.xs.util.ParamPool;
import uk.ac.ebi.biosd.xs.util.ResourceBundleParamPool;
import uk.ac.ebi.biosd.xs.util.ServletContextParamPool;

public class Init2 implements ServletContextListener
{

// public static final String TaskRequestPrefix = "request.";
 
 public static final String EmailParamPrefix = "email.";
 
 public static final String DefaultName = "_default_";

// static String TaskTmpDirParam = "tempDir";
// static String TaskTimeParam = "updateTime";

 
// static final String ebeyeSrcSeparator = ";";
// static final String ebeyeSrcSubstSeparator = ":";
 
 static final String OutputTypeParameter = "type";
 static final String XMLDumpType = "xmldump";
 static final String EBEyeType = "ebeye";
 
 static final String BioSDDBParamPrefix = "biosddb";
 static final String MyEQDBParamPrefix = "myeqdb";
 static final String TaskParamPrefix = "task";
 static final String OutputParamPrefix = "output";
 
// static String DefaultProfileParam = BioSDDBParamPrefix+".defaultProfile";

 private final Logger log = LoggerFactory.getLogger(Init2.class);
 private Timer timer;

 private static final long dayInMills = TimeUnit.DAYS.toMillis(1);

 
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
  
  
  boolean confOk = true;
  
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
       taskName = DefaultName;

      TaskConfig cm = tasksMap.get(taskName);

      if(cm == null)
       tasksMap.put(taskName, cm = new TaskConfig(taskName) );

      outMtch.reset(param);
      
      if( outMtch.matches() )
      {
       String outName=outMtch.group(1);
       String outParam = outMtch.group(2);
       
       if( outName == null )
        outName = DefaultName;
       
       cm.addOutputParameter(outName, outParam, val);
      }
      else
      {
       try
       {
        if( ! cm.readParameter(param, val) )
         log.warn("Unknown configuration parameter: "+key+" will be ignored");
       }
       catch( TaskConfigException e)
       {
        log.error("Invalid parameter value: "+key+"="+val);
        confOk = false;
       }
      }
     }
    }
   }
  }
  
  if( ! confOk )
  {
   throw new RuntimeException("X-S webapp initialization failed");
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

  
  try
  {
   createTasks(tasksMap);
  }
  catch( TaskConfigException e )
  {
   log.error("Configuration error : "+e.getMessage());
   throw new RuntimeException("X-S webapp initialization failed");
  }

  
  for( TaskInfo tinf : TaskManager.getDefaultInstance().getTasks() )
  {
   if( tinf.getTimerDelay() > 0 )
   {
    if(timer == null)
     timer = new Timer("Timer", true);
    
    timer.scheduleAtFixedRate(tinf, tinf.getTimerDelay(), dayInMills);
   
    log.info("Task '"+tinf.getTask().getName()+"' is scheduled to run periodically");
   }
  }
   
  
 }

 private void createTasks(Map<String, TaskConfig> tasksMap) throws TaskConfigException
 {
  for( TaskConfig tc : tasksMap.values() )
  {
   TaskInfo tinf = new TaskInfo();
   
   
   
   String str = tc.getServer(null);
   
   if( str == null )
   {
    log.warn("Task '"+tc.getName()+"' has not defined 'server' parameter and will be disabled");
    continue;
   }
   
   EntityManagerFactory emf = EMFManager.getFactory(str);
   
   if( emf == null )
   {
    log.warn("Task '"+tc.getName()+"': Server connection '"+str+"' is not defined. Task will be disabled");
    continue;
   }
   
   EntityManagerFactory myEqFact=null;
   str = tc.getMyEq(null);
   
   if( str != null )
   {
    myEqFact = EMFManager.getMyEqFactory(str);
    
    if( myEqFact == null )
     log.warn("Task '"+tc.getName()+"': MyEq connection '"+str+"' is not defined. MyEq support will be disabled");
   }
   
   if( tc.getInvokeHour() >= 0 )
    tinf.setTimerDelay( getAdjustedDelay(tc.getInvokeHour(), tc.getInvokeMin() ) );
   
   List<OutputModule> mods = new ArrayList<>(tc.getOutputModulesConfig().size() );
   
   for( Map.Entry<String, Map<String,String>> me : tc.getOutputModulesConfig().entrySet() )
   {
    Map<String,String> cfg = me.getValue();
    
    String type = cfg.get(OutputTypeParameter);
    
    if( type == null )
     throw new TaskConfigException("Task '"+tc.getName()+"' output '"+me.getKey()+"': missed 'type' parameter");
    
    if( XMLDumpType.equals(type) )
     mods.add( new XMLDumpOutputModule(me.getKey(),cfg));
    else if( EBEyeType.equals(type) )
     mods.add( new EBEyeOutputModule(me.getKey(),cfg) );
    
   }
   
   try
   {
    ExportTask2 tsk = new ExportTask2(tc.getName(), emf, myEqFact, mods, tc);
    
    tinf.setTask(tsk);
    
    TaskManager.getDefaultInstance().addTask(tinf);
   }
   catch(TaskInitError e)
   {
    log.warn("Task '"+tc.getName()+"': Initialization error: "+e.getMessage() );
   }
   
  }
  
 }
 
 private long getAdjustedDelay( int hour, int min )
 {
  if( hour < 0 )
   return -1;
  
  
  Calendar cr = Calendar.getInstance(TimeZone.getDefault());
  cr.setTimeInMillis(System.currentTimeMillis());
  
  cr.set(Calendar.HOUR_OF_DAY, hour);
  cr.set(Calendar.MINUTE, min);
  
  long delay = cr.getTimeInMillis() - System.currentTimeMillis();
  
  long adjustedDelay = (delay > 0 ? delay : dayInMills + delay);
  
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
