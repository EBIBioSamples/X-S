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
import java.util.Date;
import java.util.List;
import java.util.Map;
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
 
 private static final String        samplesHdrFileName      = "samples.hdr.xml";
 private static final String        groupsHdrFileName       = "groups.hdr.xml";

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
 private final Map<String, String> ebeyeSrcMap;
 
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
 
 public EBeyeExport(EntityManagerFactory emf, File outDir, File tmpDir, URL efoURL, RequestConfig rc,  Map<String, String> ebeyeSrcMap)
 {
  log = LoggerFactory.getLogger(EBeyeExport.class);

  this.emf=emf;
  
  this.ebeyeSrcMap = ebeyeSrcMap;
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

    auxFmt = SchemaManager.getFormatter(rc.getSchema(DefaultSchema), rc.getShowAttributesSummary(true), rc.getShowAccessControl(true), smpfmt, rc.getPublicOnly(false), new Date());
   }
   catch(Exception e)
   {
    log.error("Invalid aux sample format parameter: "+smpfmt);
   }
   
  }
  

  
 }


 public boolean isBusy()
 {
  return busy.get();
 }
 
 public boolean export( int limit, boolean genSamples, boolean pubOnly, int threads ) throws Throwable
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
   
   Date now = new Date();
   
   ebeyeFmt = new EBeyeXMLFormatter(new OWLKeywordExpansion(efoURL), ebeyeSrcMap, pubOnly, now);
   
   auxFmt.setNowDate(now);

   
   PrintStream grpFileOut = null;
   PrintStream grpHdrFileOut = null;

   PrintStream smplFileOut = null;
   PrintStream smplHdrFileOut = null;

   PrintStream auxFileOut = null;

   
   File tmpHdrGrpFile = new File(tmpDir, groupsHdrFileName);
   File tmpHdrSmplFile = new File(tmpDir, samplesHdrFileName);

   File tmpGrpFile = new File(tmpDir, groupsFileName);
   File tmpSmplFile = new File(tmpDir, samplesFileName);
   File tmpAuxFile = new File(tmpDir, auxFileName);

   grpFileOut = new PrintStream(tmpGrpFile, "UTF-8");
   grpHdrFileOut = new PrintStream(tmpHdrGrpFile, "UTF-8");
   
   if( genSamples )
   {
    smplFileOut = new PrintStream(tmpSmplFile, "UTF-8");
    smplHdrFileOut = new PrintStream(tmpHdrSmplFile, "UTF-8");
   }
   
   if( auxFile != null )
    auxFileOut = new PrintStream(tmpAuxFile, "UTF-8");
   
   
   if(limit < 0)
    limit = Integer.MAX_VALUE;
    
//   EntityManager em  = emf.createEntityManager();
//   
//   Query query=em.createQuery("SELECT COUNT(p.id) FROM BioSample p");
//   Number sampleCount=(Number) query.getSingleResult();
//
//   query=em.createQuery("SELECT COUNT(p.id) FROM BioSampleGroup p");
//   Number groupCount=(Number) query.getSingleResult();
//   
//   em.close();
   
   log.debug("Start exporting EBeye XML files");

   
   SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   java.util.Date startTime = new java.util.Date();
   long startTs = startTime.getTime();

   if( auxFileOut != null )
    auxFmt.exportHeader(-1, auxFileOut, auxConfig.getShowNamespace(DefaultShowNS) );
   
   String commStr = "<!-- Start time: "+simpleDateFormat.format(startTime)+" -->\n";
   
   grpHdrFileOut.append(commStr);

   if( genSamples )
    smplHdrFileOut.append(commStr);

   if( auxFileOut != null )
   {
    auxFileOut.append(commStr);
   
    auxFmt.exportGroupHeader(auxFileOut, false, -1);
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

    MTExporterStat stat = mtc.export(-1, limit, now);

    ebeyeFmt.exportGroupFooter( grpFileOut );

    if( genSamples )
     ebeyeFmt.exportSampleFooter( smplFileOut );
    
    
    ebeyeFmt.exportGroupHeader(  grpHdrFileOut, true, stat.getGroupPublicCount() );

    if( genSamples )
     ebeyeFmt.exportSampleHeader( smplHdrFileOut, true, stat.getSamplePublicUniqCount() );
    
    if(auxFileOut != null )
    {
     auxFmt.exportGroupFooter(auxFileOut);
     

     if(auxFmt.isSamplesExport())
     {
      tmpAuxSampleOut.close();
      
      tmpAuxSampleOut = null;
      
      auxFmt.exportSampleHeader(auxFileOut, false, stat.getUniqSampleCount());
      
      appendFile(auxFileOut, tmpAuxSampleFile);
      
      auxFmt.exportSampleFooter(auxFileOut);
      
     }

     if( auxConfig.getShowSources(DefaultShowSources))
      auxFmt.exportSources(stat.getSourcesMap(), auxFileOut);

     
     auxFmt.exportFooter(auxFileOut);
    }
    
    grpFileOut.close();
    appendFile(grpHdrFileOut, tmpGrpFile);
    
    if( genSamples )
    {
     smplFileOut.close();
     appendFile(smplHdrFileOut, tmpSmplFile);
    }

    
    java.util.Date endTime = new java.util.Date();
    long endTs = endTime.getTime();

    
    long rate = stat.getGroupCount()!=0? (endTs-startTs)/stat.getGroupCount():0;
    
    String stmsg1="\n<!-- Exported: "+stat.getGroupCount()+" groups in "+threads+" threads. Rate: "+rate+"ms per group -->";
    String stmsg1a="\n<!-- Public groups: "+stat.getGroupPublicCount()+" -->";
    
    rate = stat.getSampleCount()!=0? (endTs-startTs)/stat.getSampleCount():0;
    String stmsg2="\n<!-- Samples in groups: "+stat.getSampleCount()+". Rate: "+rate+"ms per sample -->";
    
    rate = stat.getUniqSampleCount()!=0? (endTs-startTs)/stat.getUniqSampleCount():0;
    String stmsg3="\n<!-- Unique samples: "+stat.getUniqSampleCount()+". Rate: "+rate+"ms per unique sample -->";

    String stmsg3a="\n<!-- Public unique samples: "+stat.getSamplePublicUniqCount()+" -->";
    
    String stmsg4="\n<!-- Start time: "+simpleDateFormat.format(startTime)+" -->";
    String stmsg5="\n<!-- End time: "+simpleDateFormat.format(endTime)+". Time spent: "+StringUtils.millisToString(endTs-startTs)+" -->";
    String stmsg6="\n<!-- Thank you. Good bye. -->\n";
    
    grpHdrFileOut.append(stmsg1);
    grpHdrFileOut.append(stmsg1a);
    grpHdrFileOut.append(stmsg2);
    grpHdrFileOut.append(stmsg3);
    grpHdrFileOut.append(stmsg3a);
    grpHdrFileOut.append(stmsg4);
    grpHdrFileOut.append(stmsg5);
    grpHdrFileOut.append(stmsg6);

    if( genSamples )
    {
     smplHdrFileOut.append(stmsg1);
     smplHdrFileOut.append(stmsg1a);
     smplHdrFileOut.append(stmsg2);
     smplHdrFileOut.append(stmsg3);
     smplHdrFileOut.append(stmsg3a);
     smplHdrFileOut.append(stmsg4);
     smplHdrFileOut.append(stmsg5);
     smplHdrFileOut.append(stmsg6);
    }
    
    if( auxFileOut != null )
    {
     auxFileOut.append(stmsg1);
     auxFileOut.append(stmsg1a);
     auxFileOut.append(stmsg2);
     auxFileOut.append(stmsg3);
     auxFileOut.append(stmsg3a);
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
    
    tmpGrpFile.delete();
    
    grpHdrFileOut.close();

    if( genSamples )
    {
     tmpSmplFile.delete();
     smplHdrFileOut.close();
    }
    
    if( auxFileOut != null )
     auxFileOut.close();
   }
   
   

   File grpFile = new File(outDir, groupsFileName);
   
   if( grpFile.exists() && !grpFile.delete() )
    log.error("Can't delete file: "+grpFile);
   
   if(!tmpHdrGrpFile.renameTo(grpFile))
    log.error("Moving groups file failed. {} -> {} ", tmpHdrGrpFile.getAbsolutePath(), grpFile.getAbsolutePath());

   if( genSamples )
   {
    File smpFile = new File(outDir, samplesFileName);
    
    if( smpFile.exists() && !smpFile.delete() )
     log.error("Can't delete file: "+smpFile);
    
    if(!tmpHdrSmplFile.renameTo(smpFile))
     log.error("Moving samples file failed. {} -> {} ", tmpHdrSmplFile.getAbsolutePath(), smpFile.getAbsolutePath());
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
  private void appendFile(Appendable out, File f) throws IOException
  {
   Reader rd = new InputStreamReader(new FileInputStream(f), Charset.forName("utf-8"));
   
   try
   {
    
    CharBuffer buf = CharBuffer.allocate(4096);
    
    while(rd.read(buf) != -1)
    {
     String str = new String(buf.array(), 0, buf.position());
     
     out.append(str);
     
     buf.clear();
    }
    
   }
   finally
   {
    rd.close();
   }
  }



}
