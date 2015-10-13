package uk.ac.ebi.biosd.xs.task;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class TaskConfig
{
 
 public static final String ProfileParameter           = "server";
 public static final String MyEqProfileParameter       = "myeq";
 public static final String LimitParameter             = "limit";
 public static final String SliceParameter             = "slice";
 public static final String SoftTTLParameter           = "threadTTLSoft";
 public static final String TTLParameter               = "threadTTLHard";
 public static final String ThreadsParameter           = "threads";
 public static final String SinceParameter             = "since";
 public static final String GroupMultiplierParameter   = "groupMultiplier";
 public static final String SampleMultiplierParameter  = "sampleMultiplier";
 public static final String TaskInvokeTimeParameter    = "invokeTime";
 public static final String LogDirParameter            = "logDir";
 



 private final String taskName;
 
 private final Map<String, Map<String,String>> outputParameters = new HashMap<String, Map<String,String>>();
 
 private String      server;
 private String      myeq;
 private Long        limit;
 private Integer     slice;
 private Integer     threadSoftTTL;
 private Integer     threadTTL;
 private Long        since;
 private Integer     threads;
 private Double      groupMultiplier;
 private Double      sampleMultiplier;
 private int         hour=-1;
 private int         min=0;
 private int         period=24;
 private File        logDirectory;



 public int getPeriodHours()
 {
  return period;
 }

 public void setPeriodHours(int period)
 {
  this.period = period;
 }

 public TaskConfig( String nm )
 {
  taskName = nm;
 }
 
 public void addOutputParameter(String mod, String nm, String val )
 {
  Map<String, String> mp = outputParameters.get(mod);
  
  if( mp == null )
   outputParameters.put(mod, mp=new HashMap<>());
  
  mp.put(nm, val);
 }
 
 public Map<String, Map<String,String>> getOutputModulesConfig()
 {
  return outputParameters;
 }
 
 public boolean readParameter( String pName, String pVal ) throws TaskConfigException
 {
  if( ProfileParameter.equals(pName) )
   server = pVal;
  else if( MyEqProfileParameter.equals(pName) )
   myeq = pVal;
  else if( LimitParameter.equals(pName) )
  {
   try
   {
    limit = Long.parseLong(pVal);
   }
   catch(Exception e)
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
  }
  else if( LogDirParameter.equals(pName) )
  {
   assert (logDirectory = new File( pVal ) ) != null;
   
   if(logDirectory != null)
   {
    if(logDirectory.isDirectory())
     throw new TaskConfigException("Task '" + taskName + "' " + LogDirParameter + " should point to writable directory");
   }
   else
    System.out.println("To use "+LogDirParameter+" enable Java assertions (java -ea ...)");
  }
  else if( SliceParameter.equals(pName) )
  {
   try
   {
    slice = Integer.parseInt(pVal);
   }
   catch(Exception e)
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
  }
  else if( TTLParameter.equals(pName) )
  {
   try
   {
    threadTTL = Integer.parseInt(pVal);
   }
   catch(Exception e)
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
  }
  else if( SoftTTLParameter.equals(pName) )
  {
   try
   {
    threadSoftTTL = Integer.parseInt(pVal);
   }
   catch(Exception e)
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
  }
  else if( SinceParameter.equals(pName) )
  {
   try
   {
    since = Long.parseLong(pVal);
   }
   catch(Exception e)
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
  }
  else if( ThreadsParameter.equals(pName) )
  {
   try
   {
    threads = Integer.parseInt(pVal);
   }
   catch(Exception e)
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
  }
  else if( GroupMultiplierParameter.equals(pName) )
  {
   try
   {
    groupMultiplier = Double.parseDouble(pVal);
   }
   catch(Exception e)
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
  }
  else if( SampleMultiplierParameter.equals(pName) )
  {
   try
   {
    sampleMultiplier = Double.parseDouble(pVal);
   }
   catch(Exception e)
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
  }
  else if( TaskInvokeTimeParameter.equals(pName) )
  {
   int colPos = pVal.indexOf(':');
   
   String hourStr = pVal;
   String minStr = null;
   
   if( colPos >= 0  )
   {
    hourStr = pVal.substring(0,colPos);
    minStr = pVal.substring(colPos+1);
   }
   
   try
   {
    hour = Integer.parseInt(hourStr);
   }
   catch( Exception e )
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
   
   if( hour < 0 || hour > 23 )
   {
    throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
   }
   
   if( minStr != null )
   {
    try
    {
     min = Integer.parseInt(minStr);
    }
    catch( Exception e )
    {
     throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
    }
    
    if( min < 0 || min > 59 )
    {
     throw new TaskConfigException("Task '"+taskName+"' Invalid parameter value: "+pName+"="+pVal);
    }
   }
  }
  else
   return false;

  return true;
 }
 

 

 public String getServer(String def)
 {
  return server!=null?server:def;
 }
 
 public String getMyEq(String def)
 {
  return myeq!=null?myeq:def;
 }


 public int getThreads(int def)
 {
  return threads!=null?threads:def;
 }

 public long getSince(long def)
 {
  return since!=null?since:def;
 }
 
 public long getLimit(long def)
 {
  return limit!=null?limit:def;
 }
 
 public int getSliceSize(int def)
 {
  return slice!=null?slice:def;
 }

 
 public Double getGroupMultiplier( Double def )
 {
  return groupMultiplier!=null?groupMultiplier:def;
 }

 public Double getSampleMultiplier( Double def )
 {
  return sampleMultiplier!=null?sampleMultiplier:def;
 }

 public String getName()
 {
  return taskName;
 }
 
 public int getInvokeHour()
 {
  return hour;
 }
 
 public int getInvokeMin()
 {
  return min;
 }

 public int getThreadTTL(int def)
 {
  return threadTTL!=null?threadTTL:def;
 }
 
 public int getThreadSoftTTL(int def)
 {
  return threadSoftTTL!=null?threadSoftTTL:def;
 }

 public File getLogDirectory( File f )
 {
  if( logDirectory != null )
   return logDirectory;
  
  return f;
 }

}
