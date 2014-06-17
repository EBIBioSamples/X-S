package uk.ac.ebi.biosd.xs.output.ebeye;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.mtexport.ExporterStat;
import uk.ac.ebi.biosd.xs.output.OutputModule;
import uk.ac.ebi.biosd.xs.task.TaskConfigException;
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
 
 private final boolean groupedOnly; 
 private XMLFormatter formatter;

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
 }

 @Override
 public XMLFormatter getFormatter()
 {
  return formatter;
 }

 @Override
 public Appendable getGroupOut()
 {
  // TODO Auto-generated method stub
  return null;
 }

 @Override
 public Appendable getSampleOut()
 {
  // TODO Auto-generated method stub
  return null;
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
  // TODO Auto-generated method stub
  
 }

 @Override
 public void finish(ExporterStat stat) throws IOException
 {
  // TODO Auto-generated method stub
  
 }

 @Override
 public void cancel() throws IOException
 {
  // TODO Auto-generated method stub
  
 }

}
