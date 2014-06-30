package uk.ac.ebi.biosd.xs.output.xmldump;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.mtexport.ExporterStat;
import uk.ac.ebi.biosd.xs.output.OutputModule;
import uk.ac.ebi.biosd.xs.service.SchemaManager;
import uk.ac.ebi.biosd.xs.task.TaskConfigException;
import uk.ac.ebi.biosd.xs.util.FileUtils;
import uk.ac.ebi.biosd.xs.util.MapParamPool;

public class XMLDumpOutputModule implements OutputModule
{
 static final String DefaultSamplesFormat = SamplesFormat.EMBED.name();
 static final boolean DefaultShowNS = false;
 static final boolean DefaultShowAC = true;

 static final boolean DefaultShowSources = true;
 static final boolean DefaultSourcesByName = false;
 static final boolean DefaultGroupedSamplesOnly = false;
 static final boolean DefaultShowAttributesSummary = true;
 static final boolean DefaultPublicOnly = false;
 
 private final String name;
 
 private final XMLFormatter formatter;
 final private boolean groupedSamplesOnly;
 
 final private boolean showSourcesByName;
 final private boolean showSourcesByAcc;
 final private boolean showNS;
 
 private final File outFile;
 private final File tmpDir;

 private File tmpGrpFile;
 private File tmpSmpFile;
 
 private PrintStream tmpGrpStream;
 private PrintStream tmpSmpStream;
 
 private java.util.Date startTime;
 
 private static Logger log = null;
 
 public XMLDumpOutputModule(String name, Map<String, String> cfgMap) throws TaskConfigException
 {
  if( log == null )
   log = LoggerFactory.getLogger(getClass());

  
  this.name = name;
  
  XMLDumpConfig cfg = new XMLDumpConfig();
  
  cfg.loadParameters(new MapParamPool(cfgMap), "");
  
  String tmpDirName = cfg.getTmpDir(null);
  
  if( tmpDirName == null )
  {
   throw new TaskConfigException("Output module '"+name+"': Temp directory is not defined");
  }
  
  
  tmpDir = new File(tmpDirName);

  if( ! tmpDir.canWrite() )
  {
   log.error("Output module '"+name+"': Temporary directory is not writable: " + tmpDir);
   throw new TaskConfigException("Output module '"+name+"': Temporary directory is not writable: " + tmpDir);
  }
  
  
  String outFileName = cfg.getOutputFile(null);
  
  if( outFileName == null )
   throw new TaskConfigException("Output module '"+name+"': Output file is not defined");
  
  outFile = new File(outFileName);

  if(!outFile.getParentFile().canWrite())
  {
   log.error("Output module '"+name+"': Output file directory is not writable: " + outFile);
   throw new TaskConfigException("Output module '"+name+"': Output file directory is not writable: " + outFile);
  }
 

  String schemaName = cfg.getSchema(null);
  
  if( schemaName == null )
    throw new TaskConfigException("Output module '"+name+"': Schema is not specified");
  
  
  SamplesFormat smpfmt = null;

  try
  {
   smpfmt = SamplesFormat.valueOf(cfg.getSamplesFormat(DefaultSamplesFormat));
  }
  catch( Exception e )
  {
   throw new TaskConfigException("Output module '"+name+"': Invalid samples format parameter");
  }
  
  try
  {
   formatter = SchemaManager.getFormatter(
     schemaName, 
     cfg.getShowAttributesSummary(DefaultShowAttributesSummary), 
     cfg.getShowAccessControl(DefaultShowAC), 
     smpfmt,
     cfg.getPublicOnly(DefaultPublicOnly), new Date());
  }
  catch( Exception e)
  {
   throw new TaskConfigException("Output module '"+name+"': Invalid schema parameter");
  }
  
  showSourcesByName = cfg.getSourcesByName(DefaultSourcesByName);
  showSourcesByAcc = ! showSourcesByName;
  
  groupedSamplesOnly = cfg.getGroupedSamplesOnly(DefaultGroupedSamplesOnly);
  
  showNS = cfg.getShowNamespace(DefaultShowNS);
  
//  if(!checkDirs())
//   return false;
  
 }
 
 
 
 @Override
 public XMLFormatter getFormatter()
 {
  return formatter;
 }

 @Override
 public Appendable getGroupOut()
 {
  return tmpGrpStream;
 }

 @Override
 public Appendable getSampleOut()
 {
  return tmpSmpStream;
 }

 @Override
 public boolean isGroupedSamplesOnly()
 {
  return groupedSamplesOnly;
 }

 @Override
 public void start() throws IOException
 {

  startTime = new java.util.Date();

  formatter.setNowDate(startTime);

  tmpGrpFile = new File(tmpDir, "grp_" + name.hashCode() + "_" + System.currentTimeMillis() + ".tmp");

  tmpGrpStream = new PrintStream(tmpGrpFile, "UTF-8");

  log.debug("Start exporting XML files for task '" + name + "'");

  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  formatter.exportHeader(-1, tmpGrpStream, showNS);

  String commStr = "<!-- Start time: " + simpleDateFormat.format(startTime) + " -->\n";

  tmpGrpStream.append(commStr);

  formatter.exportGroupHeader(tmpGrpStream, false, -1);

  if(formatter.isSamplesExport())
  {
   tmpSmpFile = new File(tmpDir, "smp_" + name.hashCode() + "_" + System.currentTimeMillis() + ".tmp");

   tmpSmpStream = new PrintStream(tmpSmpFile, "utf-8");
  }

 }
 
 @Override
 public void finish(ExporterStat stat) throws IOException
 {
  
  formatter.exportGroupFooter(tmpGrpStream);
   
  if(formatter.isSamplesExport())
  {
   tmpSmpStream.close();

   tmpSmpStream = null;

   formatter.exportSampleHeader(tmpGrpStream, false, stat.getUniqSampleCount());

   FileUtils.appendFile(tmpGrpStream, tmpSmpFile);

   formatter.exportSampleFooter(tmpGrpStream);
  }

  if(showSourcesByAcc)
   formatter.exportSources(stat.getSourcesByAccMap(), tmpGrpStream);
  else if(showSourcesByName)
   formatter.exportSources(stat.getSourcesByNameMap(), tmpGrpStream);


  formatter.exportFooter(tmpGrpStream);    
  
  Date endTime = new java.util.Date();
 
  String summary = stat.createReport(startTime, endTime, stat.getThreads());
  
  tmpGrpStream.append(summary);
  
  if(tmpSmpStream != null )
   tmpSmpStream.close();
  
  if( tmpSmpFile != null )
   tmpSmpFile.delete();
  
  if( tmpGrpStream != null )
   tmpGrpStream.close();
  

  if(outFile.exists() && !outFile.delete())
   log.error("Task '"+name+"': Can't delete file: " + outFile);

  if(!tmpGrpFile.renameTo(outFile))
   log.error("Task '"+name+"': Moving aux file failed. {} -> {} ", tmpGrpFile.getAbsolutePath(), outFile.getAbsolutePath());
  
  tmpSmpStream = null;
  tmpSmpFile = null;
  tmpGrpStream = null;
  tmpGrpFile = null;
 }
 
 @Override
 public void cancel()
 {
  if(tmpSmpStream != null )
   tmpSmpStream.close();
  
  if( tmpSmpFile != null )
   tmpSmpFile.delete();
  
  if( tmpGrpStream != null )
   tmpGrpStream.close();
  
  if( tmpGrpFile != null )
   tmpGrpFile.delete();

  tmpSmpStream = null;
  tmpSmpFile = null;
  tmpGrpStream = null;
  tmpGrpFile = null;
 }

 
 private boolean checkDirs()
 {
  File outDir = outFile.getParentFile();
  
  if( ! outDir.exists() )
  {
   if( ! outDir.mkdirs() )
   {
    log.error("Task '"+name+"': Can't create output directory: {}",outDir.getAbsolutePath());
    return false;
   }
  }
  
  if( ! outDir.canWrite() )
  {
   log.error("Task '"+name+"': Output directory is not writable: {}",outDir.getAbsolutePath());
   return false;
  }
  
  if( ! tmpDir.exists() )
  {
   if( ! tmpDir.mkdirs() )
   {
    log.error("Task '"+name+"': Can't create temp directory: {}",tmpDir.getAbsolutePath());
    return false;
   }
  }
  
  if( ! tmpDir.canWrite() )
  {
   log.error("Task '"+name+"': Temp directory is not writable: {}",tmpDir.getAbsolutePath());
   return false;
  }
  
  return true;
 }


 @Override
 public boolean isSourcesByAcc()
 {
  return showSourcesByAcc;
 }



 @Override
 public boolean isSourcesByName()
 {
  return showSourcesByName;
 }
}
