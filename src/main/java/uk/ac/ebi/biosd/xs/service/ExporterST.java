package uk.ac.ebi.biosd.xs.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.log.LoggerFactory;
import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.biosd.xs.util.StringUtils;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;

public class ExporterST implements Exporter
{
 private final EntityManagerFactory emf;
 private final XMLFormatter formatter;
 private final boolean exportSources;
 private final boolean sourcesByName;
 private final boolean showNS;
 
 public ExporterST(EntityManagerFactory emf, XMLFormatter formatter, boolean exportSources, boolean sourcesByName, int blockSize, boolean showNS)
 {
  this.emf = emf;
  this.formatter = formatter;
  this.exportSources = exportSources;
  this.sourcesByName = sourcesByName;
  this.showNS = showNS;
 }
 
 @Override
 public void export( long since, Appendable out, long limit) throws IOException
 {
  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  assert LoggerFactory.getLogger().entry("Start exporting", "export");
  
  GroupQueryManager grpMngr = new GroupQueryManager(emf);
  
  long startID = Long.MIN_VALUE;
  
  Set<String> sampleSet =  new HashSet<String>() ;

  File tmpFile = null;
  PrintStream smpStream = null;
  
  
  Map<String, Counter> srcMap = new HashMap<String, Counter>();
  
  Set<String> msiTags = new HashSet<String>();
  
  java.util.Date startTime = new java.util.Date();
  long startTs = startTime.getTime();

  
  out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
  out.append("<!-- Start time: "+simpleDateFormat.format(startTime)+" -->\n");

  formatter.exportHeader( since, out, showNS );
  formatter.exportGroupHeader( out, false );
  
  if( formatter.isSamplesExport() )
  {
   sampleSet =  new HashSet<String>() ;
   
   tmpFile = File.createTempFile("XSexport", ".tmp");
   
   System.out.println("Tmp file: "+tmpFile.getAbsolutePath());
   
   smpStream = new PrintStream(tmpFile,"utf-8");
   
   formatter.exportSampleHeader( smpStream, false );
  }

  long groupCount = 0;
  int uniqSampleCount=0;
  int sampleCount=0;
  
  try
  {

   blockLoop: while(true)
   {
    assert LoggerFactory.getLogger().entry("Start processing block", "block");

    int i = 0;

    try
    {

     List<BioSampleGroup> result = grpMngr.getGroups(since, startID);

     assert LoggerFactory.getLogger().checkpoint("Got groups: " + result.size(), "block");

     for(BioSampleGroup g : result)
     {
      groupCount++;
      i++;

      formatter.exportGroup(g, out, false);

      msiTags.clear();
      int nSmp = g.getSamples().size();
      sampleCount += nSmp;

      assert LoggerFactory.getLogger().entry("Start processing MSIs", "msi");

      for(MSI msi : g.getMSIs())
      {
       for(DatabaseRefSource db : msi.getDatabases())
       {
        String scrNm = sourcesByName ? db.getName() : db.getAcc();

        if(scrNm == null)
         continue;

        scrNm = scrNm.trim();

        if(scrNm.length() == 0)
         continue;

        if(msiTags.contains(scrNm))
         continue;

        msiTags.add(scrNm);

        Counter c = srcMap.get(scrNm);

        if(c == null)
         srcMap.put(scrNm, new Counter(nSmp));
        else
         c.add(nSmp);

       }
      }

      assert LoggerFactory.getLogger().exit("END processing MSIs", "msi");

      if(formatter.isSamplesExport())
      {
       for(BioSample smp : g.getSamples())

        if(sampleSet.add(smp.getAcc()))
        {
         formatter.exportSample(smp, smpStream, false);
         uniqSampleCount++;
        }
      }

      startID = g.getId() + 1;

      if(groupCount >= limit)
       break blockLoop;
     }

    }
    finally
    {
     grpMngr.release();
    }

    assert LoggerFactory.getLogger().exit("End processing block", "block");

    if(i < grpMngr.getBlockSize())
     break;
   }

   if(exportSources)
    formatter.exportSources(srcMap, out);

   formatter.exportGroupFooter(out);

   if(formatter.isSamplesExport())
   {
    formatter.exportSampleFooter(smpStream);
    
    smpStream.close();
    smpStream = null;

    Reader rd = new InputStreamReader(new FileInputStream(tmpFile), Charset.forName("utf-8"));

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
  finally
  {
   if( smpStream != null )
    smpStream.close();
   
   if( tmpFile != null )
    tmpFile.delete();
  }
  
  formatter.exportFooter(out);
  
  java.util.Date endTime = new java.util.Date();
  long endTs = endTime.getTime();

  
  long rate = groupCount!=0? (endTs-startTs)/groupCount:0;
  

  
  out.append("\n<!-- Exported: "+groupCount+" groups. Rate: "+rate+"ms per group -->");
  
  rate = sampleCount!=0? (endTs-startTs)/sampleCount:0;
  
  out.append("\n<!-- Samples in groups: "+sampleCount+". Rate: "+rate+"ms per sample -->");

  rate = uniqSampleCount!=0? (endTs-startTs)/uniqSampleCount:0;
  
  out.append("\n<!-- Unique samples: "+uniqSampleCount+". Rate: "+rate+"ms per unique sample -->");

  
  out.append("\n<!-- Start time: "+simpleDateFormat.format(startTime)+" -->");
  out.append("\n<!-- End time: "+simpleDateFormat.format(endTime)+". Time spent: "+StringUtils.millisToString(endTs-startTs)+" -->");
  out.append("\n<!-- Thank you. Good bye. -->\n");


  assert LoggerFactory.getLogger().exit("End exporting", "export");

  
 }
 

}
