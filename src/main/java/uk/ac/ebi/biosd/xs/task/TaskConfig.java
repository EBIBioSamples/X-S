package uk.ac.ebi.biosd.xs.task;

import java.util.HashMap;
import java.util.Map;

import uk.ac.ebi.biosd.xs.util.ParamPool;

public class TaskConfig
{
 
 public static final String ProfileParameter           = "server";
 public static final String MyEqProfileParameter       = "myeq";
 public static final String LimitParameter             = "limit";
 public static final String ThreadsParameter           = "threads";
 public static final String SinceParameter             = "since";
 public static final String GroupMultiplierParameter   = "groupMultiplier";
 public static final String SampleMultiplierParameter  = "sampleMultiplier";
 public static final String TaskInvokeTimeParameter    = "invokeTime";
 

 public static final String PublicOnlyParameter        = "publicOnly";
 public static final String AttributesSummaryParameter = "attributesSummary";
 public static final String SamplesParameter           = "samplesFormat";
 public static final String ShowSourcesParameter       = "sourcesSummary";
 public static final String SourcesByNameParameter     = "sourcesByName";
 public static final String GroupedSampleParameter     = "groupedSamplesOnly";
 public static final String ShowAccessControlParameter = "showAC";
 public static final String NamespaceParameter         = "showNS";
 public static final String OutputParameter            = "output";
 public static final String SchemaParameter            = "schema";

 private final String taskName;
 
 private final Map<String, Map<String,String>> outputParameters = new HashMap<String, Map<String,String>>();
 
 private String      server;
 private String      schema;
 private String      myeq;
 private Long        limit;
 private Long        since;
 private Integer     threads;
 private Double      groupMultiplier;
 private Double      sampleMultiplier;
 private int         hour=-1;
 private int         min=0;

 private Boolean     publicOnly;
 private Boolean     groupedSamplesOnly;
 private Boolean     showNamespace;
 private Boolean     showSources;
 private Boolean     sourcesByName;
 private Boolean     showAttributesSummary;
 private Boolean     showAccessControl;
 private String      output;
 private String      samplesFormat;

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
 
 public void loadParameters(ParamPool params, String pfx)
 {
  if( pfx == null )
   pfx="";
  
  schema = params.getParameter(pfx+SchemaParameter);
  
  server = params.getParameter(pfx+ProfileParameter);
  
  myeq = params.getParameter(pfx+MyEqProfileParameter);
  
  samplesFormat = params.getParameter(pfx+SamplesParameter);
  
  output = params.getParameter(pfx+OutputParameter);
  
  String pv = params.getParameter(pfx+LimitParameter);
  
  if( pv != null )
  {
   try
   {
    limit = Long.parseLong(pv);
   }
   catch(Exception e)
   {
   }
  }
  
  pv = params.getParameter(pfx+SinceParameter);
  
  if( pv != null )
  {
   try
   {
    since = Long.parseLong(pv);
   }
   catch(Exception e)
   {
   }
  }
  
  pv = params.getParameter(pfx+ThreadsParameter);
  
  if( pv != null )
  {
   try
   {
    threads = Integer.parseInt(pv);
   }
   catch(Exception e)
   {
   }
  }
  
  pv = params.getParameter(pfx+ShowSourcesParameter);
  
  if( pv != null  )
  {
   showSources = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }
  
  pv = params.getParameter(pfx+PublicOnlyParameter);
  
  if( pv != null  )
  {
   publicOnly = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }
  
  pv = params.getParameter(pfx+GroupedSampleParameter);
  
  if( pv != null  )
  {
   groupedSamplesOnly = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }

  
  pv = params.getParameter(pfx+SourcesByNameParameter);
  
  if( pv != null  )
  {
   sourcesByName = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }

  pv = params.getParameter(pfx+AttributesSummaryParameter);
  
  if( pv != null  )
  {
   showAttributesSummary = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }

  pv = params.getParameter(pfx+NamespaceParameter);
  
  if( pv != null  )
  {
   showNamespace = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }

  pv = params.getParameter(pfx+ShowAccessControlParameter);
  
  if( pv != null  )
  {
   showAccessControl = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }

  pv = params.getParameter(pfx+GroupMultiplierParameter);
  
  if( pv != null  )
  {
   try
   {
    groupMultiplier = Double.parseDouble(pv);
   }
   catch(Exception e)
   {
   }
  }

  pv = params.getParameter(pfx+SampleMultiplierParameter);
  
  if( pv != null  )
  {
   try
   {
    sampleMultiplier = Double.parseDouble(pv);
   }
   catch(Exception e)
   {
   }
  }

  
 }
 
 public Boolean getShowNamespace(boolean def)
 {
  return showNamespace!=null?showNamespace:def;
 }

 public Boolean getShowSources(boolean def)
 {
  return showSources!=null?showSources:def;
 }

 public Boolean getGroupedSamplesOnly(boolean def)
 {
  return groupedSamplesOnly!=null?groupedSamplesOnly:def;
 }

 
 public Boolean getSourcesByName(boolean def)
 {
  return sourcesByName!=null?sourcesByName:def;
 }

 public Boolean getShowAttributesSummary(boolean def)
 {
  return showAttributesSummary!=null?showAttributesSummary:def;
 }

 public Boolean getShowAccessControl(boolean def)
 {
  return showAccessControl!=null?showAccessControl:def;
 }
 
 public Boolean getPublicOnly(boolean def)
 {
  return publicOnly!=null?publicOnly:def;
 }


 public String getSchema(String def)
 {
  return schema!=null?schema:def;
 }

 public String getServer(String def)
 {
  return server!=null?server:def;
 }
 
 public String getMyEq(String def)
 {
  return myeq!=null?myeq:def;
 }


 public String getSamplesFormat(String def)
 {
  return samplesFormat!=null?samplesFormat:def;
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

 public String getOutput(String def)
 {
  return output!=null?output:def;
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


}
