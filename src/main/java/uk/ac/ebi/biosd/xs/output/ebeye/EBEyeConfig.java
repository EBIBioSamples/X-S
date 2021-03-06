package uk.ac.ebi.biosd.xs.output.ebeye;

import java.util.HashMap;
import java.util.Map;

import uk.ac.ebi.biosd.xs.util.ParamPool;

public class EBEyeConfig
{
 public static final String PublicOnlyParameter        = "publicOnly";
 public static final String GroupedSampleParameter     = "groupedSamplesOnly";
 public static final String OutputDirParameter         = "outDir";
 public static final String SchemaParameter            = "schema";
 public static final String TmpDirParam                = "tmpDir";
 public static final String EFOUrlParam                = "efoURL";
 public static final String SourcesParam               = "sourcesMap";
 public static final String GenSamplesParam            = "generateSamples";
 public static final String GenGroupsParam             = "generateGroups";

 static final String SourcesSeparator = ";";
 static final String SourcesSubstSeparator = ":";
 
 private String      schema;
 private Boolean     publicOnly;
 private Boolean     groupedSamplesOnly;
 private Boolean     generateSamples;
 private Boolean     generateGroups;
 private String      outputDir;
 private String      tmpDir;
 private String      efoUrl;
 private Map<String,String> sourcesMap;

 
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
  
  
  pv = params.getParameter(pfx+GenGroupsParam);
  
  if( pv != null  )
  {
   generateGroups = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }

  pv = params.getParameter(pfx+GenSamplesParam);
  
  if( pv != null  )
  {
   generateSamples = pv.equalsIgnoreCase("true") || pv.equalsIgnoreCase("yes") || pv.equals("1");
  }
  

  pv = params.getParameter(pfx+SourcesParam);
  
  if( pv != null )
  {
   sourcesMap = new HashMap<>();
   
   String[] srcs = pv.split(SourcesSeparator);
   
   for( String s : srcs )
   {
    s=s.trim();
    
    String[] mp = s.split(SourcesSubstSeparator);
    
    if( mp.length > 1 )
     sourcesMap.put(mp[0].trim(), mp[1].trim());
    else
     sourcesMap.put(s, s);
   }
   
  }

 }


 public Map<String,String> getSourcesMap()
 {
  return sourcesMap;
 }

 public boolean getGroupedSamplesOnly(boolean def)
 {
  return groupedSamplesOnly!=null?groupedSamplesOnly:def;
 }

 
 public boolean getPublicOnly(boolean def)
 {
  return publicOnly!=null?publicOnly:def;
 }


 public String getSchema(String def)
 {
  return schema!=null?schema:def;
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


 public boolean getGenerateSamples(boolean def)
 {
  return generateSamples!=null?generateSamples:def;
 }


 public boolean getGenerateGroups(boolean def)
 {
  return generateGroups!=null?generateGroups:def;
 }
}
