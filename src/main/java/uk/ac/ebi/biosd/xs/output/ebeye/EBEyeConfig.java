package uk.ac.ebi.biosd.xs.output.ebeye;

import uk.ac.ebi.biosd.xs.util.ParamPool;

public class EBEyeConfig
{
 public static final String PublicOnlyParameter        = "publicOnly";
 public static final String GroupedSampleParameter     = "groupedSamplesOnly";
 public static final String OutputDirParameter         = "outDir";
 public static final String SchemaParameter            = "schema";
 public static final String TmpDirParam                = "tmpDir";
 public static final String EFOUrlParam                = "efoURL";

 private String      schema;
 private Boolean     publicOnly;
 private Boolean     groupedSamplesOnly;
 private String      outputDir;
 private String      tmpDir;
 private String      efoUrl;

 private String      samplesFormat;
 
 public void loadParameters(ParamPool params, String pfx)
 {
  if( pfx == null )
   pfx="";
  
  schema = params.getParameter(pfx+SchemaParameter);
  
  outputDir = params.getParameter(pfx+OutputDirParameter);
  
  tmpDir = params.getParameter(pfx+TmpDirParam);
  
  efoUrl = params.getParameter(pfx+EFOUrlParam);
  
  String pv = params.getParameter(pfx+PublicOnlyParameter);
  
  if( pv != null  )
  {
   publicOnly = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }
  
  pv = params.getParameter(pfx+GroupedSampleParameter);
  
  if( pv != null  )
  {
   groupedSamplesOnly = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }


 }



 public Boolean getGroupedSamplesOnly(boolean def)
 {
  return groupedSamplesOnly!=null?groupedSamplesOnly:def;
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

 public String getOutputDir(String def)
 {
  return outputDir!=null?outputDir:def;
 }
 

 public String getTmpDir(String def)
 {
  return tmpDir!=null?tmpDir:def;
 }


 public String getEfoUrl(String def)
 {
  return efoUrl!=null?efoUrl:def;
 }


}