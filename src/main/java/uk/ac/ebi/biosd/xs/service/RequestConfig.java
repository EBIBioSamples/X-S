package uk.ac.ebi.biosd.xs.service;

public class RequestConfig
{
 
 public interface ParamPool
 {
  String getParameter( String name );
 }
 
 static final String SchemaParameter          = "schema";
 static final String ProfileParameter         = "server";
 static final String LimitParameter           = "limit";
 static final String ThreadsParameter         = "threads";
 static final String SamplesParameter         = "samplesFormat";
 static final String ShowSourcesParameter     = "showSources";
 static final String SourcesByNameParameter   = "sourcesByName";
 static final String SinceParameter           = "since";
 static final String AttributesParameter      = "showAttributes";
 static final String NamespaceParameter       = "showNS";
 static final String NoAccessControlParameter = "hideAC";

 private Boolean     showNamespace;
 private Boolean     showSources;
 private Boolean     sourcesByName;
 private Boolean     showAttributes;
 private Boolean     hideAccessControl;
 private String      schema;
 private String      server;
 private String      samplesFormat;
 private Integer     threads;
 private Integer     limit;
 private Long        since;

 
 public void loadParameters(ParamPool params, String pfx)
 {
  if( pfx == null )
   pfx="";
  
  String pv = params.getParameter(pfx+SchemaParameter);
  
  if( pv != null )
   schema = pv;
  
  pv = params.getParameter(pfx+ProfileParameter);
  
  if( pv != null )
   server = pv;
  
  pv = params.getParameter(pfx+SamplesParameter);
  
  if( pv != null )
   samplesFormat = pv;

  pv = params.getParameter(pfx+LimitParameter);
  
  if( pv != null )
  {
   try
   {
    limit = Integer.parseInt(pv);
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
  
  pv = params.getParameter(pfx+SourcesByNameParameter);
  
  if( pv != null  )
  {
   sourcesByName = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }

  pv = params.getParameter(pfx+AttributesParameter);
  
  if( pv != null  )
  {
   showAttributes = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }

  pv = params.getParameter(pfx+NamespaceParameter);
  
  if( pv != null  )
  {
   showNamespace = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }

  pv = params.getParameter(pfx+NoAccessControlParameter);
  
  if( pv != null  )
  {
   hideAccessControl = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
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

 public Boolean getSourcesByName(boolean def)
 {
  return sourcesByName!=null?sourcesByName:def;
 }

 public Boolean getShowAttributes(boolean def)
 {
  return showAttributes!=null?showAttributes:def;
 }

 public Boolean getHideAccessControl(boolean def)
 {
  return hideAccessControl!=null?hideAccessControl:def;
 }

 public String getSchema(String def)
 {
  return schema!=null?schema:def;
 }

 public String getServer(String def)
 {
  return server!=null?server:def;
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
 
 public int getLimit(int def)
 {
  return limit!=null?limit:def;
 }

}
