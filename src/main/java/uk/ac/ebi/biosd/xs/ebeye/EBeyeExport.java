package uk.ac.ebi.biosd.xs.ebeye;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.export.EBeyeXMLFormatter;
import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.keyword.OWLKeywordExpansion;
import uk.ac.ebi.biosd.xs.mtexport.ExporterMTControl;
import uk.ac.ebi.biosd.xs.mtexport.FormattingRequest;
import uk.ac.ebi.biosd.xs.mtexport.MTExporterStat;
import uk.ac.ebi.biosd.xs.service.RequestConfig;
import uk.ac.ebi.biosd.xs.service.SchemaManager;
import uk.ac.ebi.biosd.xs.util.StringUtils;

public class EBeyeExport
{
 private static EBeyeExport instance;
 
 static final String                DefaultSchema         = SchemaManager.STXML;
 static final String                DefaultSamplesFormat  = SamplesFormat.EMBED.name();
 static final boolean               DefaultShowNS         = false;
 static final boolean               DefaultShowSources    = true;
 static final boolean               DefaultSourcesByName  = false;

 
 private static final String        samplesFileName      = "samples.xml";
 private static final String        groupsFileName       = "groups.xml";
 private static final String        auxFileName          = "aux_out.xml";
 private static final String        auxSamplesTmpFileName= "aux_samples.tmp.xml";
 
 private  XMLFormatter ebeyeFmt;
 private final EntityManagerFactory emf;

 private XMLFormatter auxFmt = null;
 private final File outDir;
 private final File tmpDir;
 private File auxFile;
 private final URL efoURL;
 private final RequestConfig auxConfig;
 
 private final AtomicBoolean busy = new AtomicBoolean( false );
 
 private final Logger log;
 
 
 public static EBeyeExport getInstance()
 {
  return instance;
 }

 public static void setInstance(EBeyeExport instance)
 {
  EBeyeExport.instance = instance;
 }
 
 public EBeyeExport(EntityManagerFactory emf, File outDir, File tmpDir, URL efoURL, RequestConfig rc)
 {
  log = LoggerFactory.getLogger(EBeyeExport.class);

  this.emf=emf;
  
  this.outDir = outDir;
  this.tmpDir = tmpDir;
  this.efoURL = efoURL;
  
  auxConfig = rc;
  
  if( rc.getOutput(null) != null  )
  {
   auxFile = new File(rc.getOutput(null));
   
   if( ! auxFile.getParentFile().canWrite() )
   {
    log.error("Output file is not writable: "+auxFile);
    auxFile = null;
   }


   SamplesFormat smpfmt = null;
   
   try
   {
    smpfmt = SamplesFormat.valueOf(rc.getSamplesFormat(DefaultSamplesFormat) );

    auxFmt = SchemaManager.getFormatter(rc.getSchema(DefaultSchema), rc.getShowAttributesSummary(true), rc.getShowAccessControl(true), smpfmt, rc.getPublicOnly(false));
   }
   catch(Exception e)
   {
    log.error("Invalid aux sample format parameter: "+smpfmt);
   }
   
  }
  

  
 }
 
 
 public boolean export( int limit, boolean genSamples, boolean pubOnly, int threads ) throws IOException
 {
  if( ! busy.compareAndSet(false, true) )
  {
   log.info("Export in progress. Skiping");
   return false;
  }
  
  if( threads <= 0 )
   threads = Runtime.getRuntime().availableProcessors();
  
  try
  {
   if(!checkDirs())
    return false;
   
   ebeyeFmt = new EBeyeXMLFormatter(new OWLKeywordExpansion(efoURL), pubOnly);

   
   PrintStream grpFileOut = null;
   PrintStream smplFileOut = null;
   PrintStream auxFileOut = null;

   
   File tmpGrpFile = new File(tmpDir, groupsFileName);
   File tmpSmplFile = new File(tmpDir, samplesFileName);
   File tmpAuxFile = new File(tmpDir, auxFileName);

   grpFileOut = new PrintStream(tmpGrpFile, "UTF-8");
   
   if( genSamples )
    smplFileOut = new PrintStream(tmpSmplFile, "UTF-8");
   
   if( auxFile != null )
    auxFileOut = new PrintStream(tmpAuxFile, "UTF-8");
   
   
   if(limit < 0)
    limit = Integer.MAX_VALUE;
    


   log.debug("Start exporting EBeye XML files");

   
   SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   java.util.Date startTime = new java.util.Date();
   long startTs = startTime.getTime();

   ebeyeFmt.exportGroupHeader(  grpFileOut, true );

   if( genSamples )
    ebeyeFmt.exportSampleHeader( smplFileOut, true );
   
   if( auxFileOut != null )
    auxFmt.exportHeader(-1, auxFileOut, auxConfig.getShowNamespace(DefaultShowNS) );
   
   String commStr = "<!-- Start time: "+simpleDateFormat.format(startTime)+" -->\n";
   
   grpFileOut.append(commStr);

   if( genSamples )
    smplFileOut.append(commStr);

   if( auxFileOut != null )
   {
    auxFileOut.append(commStr);
   
    auxFmt.exportGroupHeader(auxFileOut, false);
   }

   File tmpAuxSampleFile = null;
   PrintStream tmpAuxSampleOut=null;

   if( auxFileOut !=null && auxFmt.isSamplesExport())
   {
    tmpAuxSampleFile = new File(tmpDir, auxSamplesTmpFileName);

    log.info("Tmp file: " + tmpAuxSampleFile.getAbsolutePath());

    tmpAuxSampleOut = new PrintStream(tmpAuxSampleFile, "utf-8");
   }

   List<FormattingRequest> frList= new ArrayList<>();
   
   frList.add( new FormattingRequest(ebeyeFmt, grpFileOut, smplFileOut) );
   
   if( auxFileOut != null )
    frList.add( new FormattingRequest(auxFmt, auxFileOut, tmpAuxSampleOut) );
   
   ExporterMTControl mtc = new ExporterMTControl(emf, frList, auxConfig.getShowSources(DefaultShowSources), auxConfig.getSourcesByName(DefaultSourcesByName), threads);

   try
   {

    MTExporterStat stat = mtc.export(-1, limit);

    ebeyeFmt.exportGroupFooter( grpFileOut );

    if( genSamples )
     ebeyeFmt.exportSampleFooter( smplFileOut );
    
    
    if(auxFileOut != null )
    {
     auxFmt.exportGroupFooter(auxFileOut);
     
     if( auxConfig.getShowSources(DefaultShowSources))
      auxFmt.exportSources(stat.getSourcesMap(), auxFileOut);

     if(auxFmt.isSamplesExport())
     {
      tmpAuxSampleOut.close();
      
      tmpAuxSampleOut = null;
      
      auxFmt.exportSampleHeader(auxFileOut, false);
      
      Reader rd = new InputStreamReader(new FileInputStream(tmpAuxSampleFile), Charset.forName("utf-8"));
      
      try
      {
       
       CharBuffer buf = CharBuffer.allocate(4096);
       
       while(rd.read(buf) != -1)
       {
        String str = new String(buf.array(), 0, buf.position());
        
        auxFileOut.append(str);
        
        buf.clear();
       }
       
      }
      finally
      {
       rd.close();
      }
      
      
      auxFmt.exportSampleFooter(auxFileOut);
      
     }

     auxFmt.exportFooter(auxFileOut);
    }
    

    
    java.util.Date endTime = new java.util.Date();
    long endTs = endTime.getTime();

    
    long rate = stat.getGroupCount()!=0? (endTs-startTs)/stat.getGroupCount():0;
    
    String stmsg1="\n<!-- Exported: "+stat.getGroupCount()+" groups in "+threads+" threads. Rate: "+rate+"ms per group -->";
    
    rate = stat.getSampleCount()!=0? (endTs-startTs)/stat.getSampleCount():0;
    String stmsg2="\n<!-- Samples in groups: "+stat.getSampleCount()+". Rate: "+rate+"ms per sample -->";
    
    rate = stat.getUniqSampleCount()!=0? (endTs-startTs)/stat.getUniqSampleCount():0;
    String stmsg3="\n<!-- Unique samples: "+stat.getUniqSampleCount()+". Rate: "+rate+"ms per unique sample -->";
    
    String stmsg4="\n<!-- Start time: "+simpleDateFormat.format(startTime)+" -->";
    String stmsg5="\n<!-- End time: "+simpleDateFormat.format(endTime)+". Time spent: "+StringUtils.millisToString(endTs-startTs)+" -->";
    String stmsg6="\n<!-- Thank you. Good bye. -->\n";
    
    grpFileOut.append(stmsg1);
    grpFileOut.append(stmsg2);
    grpFileOut.append(stmsg3);
    grpFileOut.append(stmsg4);
    grpFileOut.append(stmsg5);
    grpFileOut.append(stmsg6);

    if( genSamples )
    {
     smplFileOut.append(stmsg1);
     smplFileOut.append(stmsg2);
     smplFileOut.append(stmsg3);
     smplFileOut.append(stmsg4);
     smplFileOut.append(stmsg5);
     smplFileOut.append(stmsg6);
    }
    
    if( auxFileOut != null )
    {
     auxFileOut.append(stmsg1);
     auxFileOut.append(stmsg2);
     auxFileOut.append(stmsg3);
     auxFileOut.append(stmsg4);
     auxFileOut.append(stmsg5);
     auxFileOut.append(stmsg6);
    }


   }
   finally
   {
    if(tmpAuxSampleOut != null )
     tmpAuxSampleOut.close();
    
    if( tmpAuxSampleFile != null )
     tmpAuxSampleFile.delete();
    
    grpFileOut.close();

    if( genSamples )
     smplFileOut.close();
    
    if( auxFileOut != null )
     auxFileOut.close();
   }
   
   

   File grpFile = new File(outDir, groupsFileName);
   
   if( grpFile.exists() && !grpFile.delete() )
    log.error("Can't delete file: "+grpFile);
   
   if(!tmpGrpFile.renameTo(grpFile))
    log.error("Moving groups file failed. {} -> {} ", tmpGrpFile.getAbsolutePath(), grpFile.getAbsolutePath());

   if( genSamples )
   {
    File smpFile = new File(outDir, samplesFileName);
    
    if( smpFile.exists() && !smpFile.delete() )
     log.error("Can't delete file: "+smpFile);
    
    if(!tmpSmplFile.renameTo(smpFile))
     log.error("Moving samples file failed. {} -> {} ", tmpSmplFile.getAbsolutePath(), smpFile.getAbsolutePath());
   }

   if( auxFileOut != null )
   {
    if( auxFile.exists() && !auxFile.delete() )
     log.error("Can't delete file: "+auxFile);
    
    if(!tmpAuxFile.renameTo(auxFile))
     log.error("Moving aux file failed. {} -> {} ", tmpAuxFile.getAbsolutePath(), auxFile.getAbsolutePath());
   }

  }
  finally
  {
   busy.set(false);
  }
 
  return true;
 }

 private boolean checkDirs()
 {
  if( ! outDir.exists() )
  {
   if( ! outDir.mkdirs() )
   {
    log.error("Can't create output directory: {}",outDir.getAbsolutePath());
    return false;
   }
  }
  
  if( ! outDir.canWrite() )
  {
   log.error("Output directory is not writable: {}",outDir.getAbsolutePath());
   return false;
  }
  
  if( ! tmpDir.exists() )
  {
   if( ! tmpDir.mkdirs() )
   {
    log.error("Can't create temp directory: {}",tmpDir.getAbsolutePath());
    return false;
   }
  }
  
  if( ! tmpDir.canWrite() )
  {
   log.error("Temp directory is not writable: {}",tmpDir.getAbsolutePath());
   return false;
  }
  
  return true;
 }
 
 private void cleanDir(File dir)
 {
  for( File f : dir.listFiles() )
  {
   if( f.isDirectory() )
    cleanDir(f);
    
   f.delete();
  }
  
 }


}
