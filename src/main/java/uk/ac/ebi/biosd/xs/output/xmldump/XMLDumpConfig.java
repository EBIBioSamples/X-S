package uk.ac.ebi.biosd.xs.output.xmldump;

import uk.ac.ebi.biosd.xs.util.ParamPool;

public class XMLDumpConfig
{
 public static final String PublicOnlyParameter        = "publicOnly";
 public static final String AttributesSummaryParameter = "attributesSummary";
 public static final String SamplesParameter           = "samplesFormat";
 public static final String ShowSourcesParameter       = "sourcesSummary";
 public static final String SourcesByNameParameter     = "sourcesByName";
 public static final String GroupedSampleParameter     = "groupedSamplesOnly";
 public static final String ShowAccessControlParameter = "showAC";
 public static final String NamespaceParameter         = "showNS";
 public static final String OutputFileParameter        = "file";
 public static final String SchemaParameter            = "schema";
 public static final String TmpDirParam                = "tmpDir";

 private String      schema;
 private Boolean     publicOnly;
 private Boolean     groupedSamplesOnly;
 private Boolean     showNamespace;
 private Boolean     showSources;
 private Boolean     sourcesByName;
 private Boolean     showAttributesSummary;
 private Boolean     showAccessControl;
 private String      outputFile;
 private String      tmpDir;

 private String      samplesFormat;
 
 public void loadParameters(ParamPool params, String pfx)
 {
  if( pfx == null )
   pfx="";
  
  schema = params.getParameter(pfx+SchemaParameter);
  
  
  samplesFormat = params.getParameter(pfx+SamplesParameter);
  
  outputFile = params.getParameter(pfx+OutputFileParameter);
  
  tmpDir = params.getParameter(pfx+TmpDirParam);
  
  String pv = params.getParameter(pfx+ShowSourcesParameter);
  
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

 public String getSamplesFormat(String def)
 {
  return samplesFormat!=null?samplesFormat:def;
 }

 public String getOutputFile(String def)
 {
  return outputFile!=null?outputFile:def;
 }
 

 public String getTmpDir(String def)
 {
  return tmpDir!=null?tmpDir:def;
 }


}
