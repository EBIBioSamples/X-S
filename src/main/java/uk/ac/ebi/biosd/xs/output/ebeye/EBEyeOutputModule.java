package uk.ac.ebi.biosd.xs.output.ebeye;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.export.EBeyeXMLFormatter;
import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.keyword.OWLKeywordExpansion;
import uk.ac.ebi.biosd.xs.mtexport.ExporterStat;
import uk.ac.ebi.biosd.xs.output.OutputModule;
import uk.ac.ebi.biosd.xs.task.TaskConfigException;
import uk.ac.ebi.biosd.xs.util.FileUtils;
import uk.ac.ebi.biosd.xs.util.MapParamPool;

public class EBEyeOutputModule implements OutputModule
{
 private static final String        samplesFileName      = "samples.xml";
 private static final String        groupsFileName       = "groups.xml";
 
 private static final String        samplesHdrFileName      = "samples.hdr.xml";
 private static final String        groupsHdrFileName       = "groups.hdr.xml";

 private final String name;
 
 private final File outDir;
 private final File tmpDir;
 private final URL efoURL;
 
 
 File tmpHdrGrpFile;
 File tmpHdrSmplFile;

 File tmpGrpFile;
 File tmpSmplFile;
 
 File grpFile;
 File smplFile;

 
 PrintStream grpFileOut = null;
 PrintStream smplFileOut = null;

 PrintStream grpHdrFileOut = null;
 PrintStream smplHdrFileOut = null;
 
 private final boolean genSamples;
 private final boolean genGroups;
 
 private final boolean groupedOnly;
 private final boolean publicOnly;
 private final Map<String,String> sourcesMap;
 
 private XMLFormatter ebeyeFmt;
 private java.util.Date startTime;
 
 private static Logger log;

 
 public EBEyeOutputModule(String name, Map<String, String> cfgMap) throws TaskConfigException
 {
  if(log == null)
   log = LoggerFactory.getLogger(getClass());

  this.name = name;

  EBEyeConfig cfg = new EBEyeConfig();

  cfg.loadParameters(new MapParamPool(cfgMap), "");

  String str = cfg.getOutputDir(null);

  if(str == null)
   throw new TaskConfigException("Output module '" + name + "': Output directory is not defined");

  outDir = new File(str);

  if(!outDir.canWrite())
   throw new TaskConfigException("Output module '" + name + "': Output directory is not writable");

  str = cfg.getTmpDir(null);

  if(str == null)
   throw new TaskConfigException("Output module '" + name + "': Tmp directory is not defined");

  tmpDir = new File(str);

  if(!outDir.canWrite())
   throw new TaskConfigException("Output module '" + name + "': Tmp directory is not writable");
  
  groupedOnly = cfg.getGroupedSamplesOnly(false);
  
  publicOnly = cfg.getPublicOnly(true);
  
  genSamples = cfg.getGenerateSamples( true );
  genGroups = cfg.getGenerateGroups( true );

  if( !genSamples && !genGroups )
   throw new TaskConfigException("Output module '" + name + "': "+EBEyeConfig.GenGroupsParam+" and "+EBEyeConfig.GenSamplesParam+" parameters can't be both 'false' at the same time");
  
  str = cfg.getEfoUrl( null );
    
  if(str == null)
   throw new TaskConfigException("Output module '" + name + "': EFO URL is not defined");

  try
  {
   efoURL = new URL(str);
  }
  catch(MalformedURLException e)
  {
   throw new TaskConfigException("Output module '" + name + "': Invalid EFO URL");
  }
  
  sourcesMap = cfg.getSourcesMap();
  
  tmpHdrGrpFile = new File(tmpDir, groupsHdrFileName);
  tmpHdrSmplFile = new File(tmpDir, samplesHdrFileName);

  tmpGrpFile = new File(tmpDir, groupsFileName);
  tmpSmplFile = new File(tmpDir, samplesFileName);
  
  grpFile = new File(outDir, groupsFileName);
  smplFile = new File(outDir, samplesFileName);
 }

 @Override
 public XMLFormatter getFormatter()
 {
  return ebeyeFmt;
 }

 @Override
 public Appendable getGroupOut()
 {
  return grpFileOut;
 }

 @Override
 public Appendable getSampleOut()
 {
  return smplFileOut;
 }

 @Override
 public boolean isGroupedSamplesOnly()
 {
  return groupedOnly;
 }

 @Override
 public boolean isSourcesByAcc()
 {
  return false;
 }

 @Override
 public boolean isSourcesByName()
 {
  return false;
 }

 @Override
 public void start() throws IOException
 {
  startTime = new java.util.Date();

  log.debug("Starting EBEye export module for task '" + name + "'");

  
  if( genGroups )
  {
   grpFileOut = new PrintStream(tmpGrpFile, "UTF-8");
   grpHdrFileOut = new PrintStream(tmpHdrGrpFile, "UTF-8");
  }
  
  if( genSamples )
  {
   smplFileOut = new PrintStream(tmpSmplFile, "UTF-8");
   smplHdrFileOut = new PrintStream(tmpHdrSmplFile, "UTF-8");
  }
   
  ebeyeFmt = new EBeyeXMLFormatter(new OWLKeywordExpansion(efoURL), sourcesMap, publicOnly, new Date());

  
 }

 @Override
 public void finish(ExporterStat stat) throws IOException
 {
  Date endTime = new java.util.Date();
  
  String summary = stat.createReport(startTime, endTime , stat.getThreads());
  
  if( genGroups )
  {
   ebeyeFmt.exportGroupFooter( grpFileOut );
   ebeyeFmt.exportGroupHeader( grpHdrFileOut, true, stat.getGroupPublicCount() );

   grpFileOut.close();

   FileUtils.appendFile(grpHdrFileOut, tmpGrpFile);

   grpHdrFileOut.append(summary);

  
   if(grpFile.exists() && !grpFile.delete())
    log.error("EBeye: Can't delete file: " + grpFile);

   if(!tmpHdrGrpFile.renameTo(grpFile))
    log.error("EBeye: Moving groups file failed. {} -> {} ", tmpHdrGrpFile.getAbsolutePath(), grpFile.getAbsolutePath());

  }

  if( genSamples )
  {
   ebeyeFmt.exportSampleFooter( smplFileOut );
   ebeyeFmt.exportSampleHeader( smplHdrFileOut, true, stat.getSamplePublicUniqCount() );
  
   smplFileOut.close();
   
   FileUtils.appendFile(smplHdrFileOut, tmpSmplFile);
   
   smplHdrFileOut.append(summary);

   File smpFile = new File(outDir, samplesFileName);

   if(smpFile.exists() && !smpFile.delete())
    log.error("EBeye: Can't delete file: " + smpFile);

   if(!tmpHdrSmplFile.renameTo(smpFile))
    log.error("EBeye: Moving samples file failed. {} -> {} ", tmpHdrSmplFile.getAbsolutePath(), smpFile.getAbsolutePath());

  }

  
  ebeyeFmt = null;
 }

 @Override
 public void cancel() throws IOException
 {
  if( grpFileOut != null )
    grpFileOut.close();


  if( grpHdrFileOut != null )
   grpHdrFileOut.close();
   
  if( smplFileOut != null )
   smplFileOut.close();
  
  if( smplHdrFileOut != null )
   smplHdrFileOut.close();
  
  tmpGrpFile.delete();
  tmpSmplFile.delete();

  tmpHdrGrpFile.delete();
  tmpHdrSmplFile.delete();
  
  ebeyeFmt = null;
 }

 @Override
 public String getName()
 {
  return name;
 }

}
