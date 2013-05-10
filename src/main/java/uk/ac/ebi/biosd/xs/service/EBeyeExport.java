package uk.ac.ebi.biosd.xs.service;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.export.EBeyeXMLFormatter;
import uk.ac.ebi.biosd.xs.keyword.OWLKeywordExpansion;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class EBeyeExport
{
 private static EBeyeExport instance;
 
 private static final String samplesFileName = "samples.xml";
 private static final String groupsFileName = "groups.xml";
 
 private AbstractXMLFormatter formatter;
 private final EntityManager em;

 private final File outDir;
 private final File tmpDir;
 private final URL efoURL;
 private final int blockSize  = 1000;
 
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
 
 public EBeyeExport(EntityManager em, File outDir, File tmpDir, URL efoURL)
 {
  this.em=em;
  
  this.outDir = outDir;
  this.tmpDir = tmpDir;
  this.efoURL = efoURL;
  
  log = LoggerFactory.getLogger(EBeyeExport.class);
 }
 
 
 public boolean export( int limit ) throws IOException
 {
  if( ! busy.compareAndSet(false, true) )
  {
   log.info("Export in progress. Skiping");
   return false;
  }
  
  try
  {

   if(!checkDirs())
    return false;

   if(limit < 0)
    limit = Integer.MAX_VALUE;

   log.debug("Start exporting EBeye XML files");

   cleanDir(tmpDir);

   formatter = new EBeyeXMLFormatter(new OWLKeywordExpansion(efoURL));

   Query grpListQuery = null;
   Query smpListQuery = null;

   long startID = Long.MIN_VALUE;

   grpListQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName() + " a WHERE a.id >=?1 ORDER BY a.id");
   grpListQuery.setMaxResults(blockSize);

   smpListQuery = em.createQuery("SELECT a FROM " + BioSample.class.getCanonicalName() + " a WHERE a.id >=?1 ORDER BY a.id");
   smpListQuery.setMaxResults(blockSize);

   EntityTransaction ts = em.getTransaction();

   ts.begin();

   File smplFile = new File(tmpDir, samplesFileName);
   File grpFile = new File(tmpDir, groupsFileName);

   PrintStream smplFileOut = new PrintStream(smplFile);
   PrintStream grpFileOut = new PrintStream(grpFile);

   long tstamp = new java.util.Date().getTime();

   formatter.exportHeader(tstamp, -1L, smplFileOut);
   formatter.exportHeader(tstamp, -1L, grpFileOut);

   int count = 0;

   try
   {
    log.debug("Exporting groups");

    grpLoop: while(true)
    {

     grpListQuery.setParameter(1, startID);

     @SuppressWarnings("unchecked")
     List<BioSampleGroup> result = grpListQuery.getResultList();

     int i = 0;

     for(BioSampleGroup g : result)
     {
      i++;
      count++;

      formatter.exportGroup(g, grpFileOut);

      startID = g.getId() + 1;

      if(count >= limit)
       break grpLoop;
     }

     if(i < blockSize)
      break;
    }

    log.debug("Exporting groups done");

    startID = Long.MIN_VALUE;

    log.debug("Exporting samples");

    count = 0;

    smpLoop: while(true)
    {

     smpListQuery.setParameter(1, startID);

     @SuppressWarnings("unchecked")
     List<BioSample> result = smpListQuery.getResultList();

     int i = 0;

     for(BioSample s : result)
     {
      i++;
      count++;

      formatter.exportSample(s, smplFileOut);

      startID = s.getId() + 1;

      if(count >= limit)
       break smpLoop;

     }

     if(i < blockSize)
      break;
    }

    log.debug("Exporting samples done");

   }
   finally
   {
    ts.commit();
   }

   formatter.exportFooter(smplFileOut);
   formatter.exportFooter(grpFileOut);

   smplFileOut.close();
   grpFileOut.close();

   cleanDir(outDir);

   if(!smplFile.renameTo(new File(outDir, samplesFileName)))
    log.error("Moving samples file failed. {} -> {} ", smplFile.getAbsolutePath(), new File(outDir, samplesFileName).getAbsolutePath());

   if(!grpFile.renameTo(new File(outDir, groupsFileName)))
    log.error("Moving groups file failed. {} -> {} ", grpFile.getAbsolutePath(), new File(outDir, groupsFileName).getAbsolutePath());

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
