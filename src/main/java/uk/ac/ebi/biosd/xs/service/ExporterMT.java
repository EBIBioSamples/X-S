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
import java.util.Collections;
import java.util.Date;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.mtexport.ExporterMTControl;
import uk.ac.ebi.biosd.xs.mtexport.FormattingRequest;
import uk.ac.ebi.biosd.xs.mtexport.MTExporterStat;
import uk.ac.ebi.biosd.xs.util.StringUtils;

public class ExporterMT implements Exporter
{
 private final EntityManagerFactory emf;
 private final EntityManagerFactory myeqemf;
 private final XMLFormatter formatter;
 private final boolean exportSources;
 private final boolean sourcesByName;
 private final boolean grpSmpOnly;
 private final int threads;
 private final boolean showNS;
 
 public ExporterMT(EntityManagerFactory emf, EntityManagerFactory myeqemf, XMLFormatter formatter, boolean exportSources, boolean sourcesByName,
   boolean showNS, boolean grpSmpOnly, int thN)
 {
  super();
  this.emf = emf;
  this.myeqemf = myeqemf;
  this.formatter = formatter;
  this.exportSources = exportSources;
  this.sourcesByName = sourcesByName;
  threads = thN;
  this.showNS = showNS;
  this.grpSmpOnly=grpSmpOnly;
  
 }

 @Override
 public void export( long since, Appendable out, long limit ) throws IOException
 {
  export(since, out, limit, null, null);
 }

 
 @Override
 public void export( long since, Appendable out, long limit, Double grpMul, Double smpMul ) throws IOException
 {
  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  java.util.Date startTime = new java.util.Date();
  long startTs = startTime.getTime();

  formatter.exportHeader(since, out, showNS);
  
  out.append("<!-- Start time: "+simpleDateFormat.format(startTime)+" -->\n");

  formatter.exportGroupHeader(out, false, -1);

  File tmpFile = null;
  PrintStream sampleOut=null;

  if(formatter.isSamplesExport())
  {
    tmpFile = File.createTempFile("XSexport", ".tmp");

   System.out.println("Tmp file: " + tmpFile.getAbsolutePath());

   sampleOut = new PrintStream(tmpFile, "utf-8");
  }

  FormattingRequest freq = new FormattingRequest(formatter, out, sampleOut);

  Date now = new Date();
  
  formatter.setNowDate(now);
  
  ExporterMTControl mtc = new ExporterMTControl(emf,myeqemf, Collections.singletonList(freq), exportSources, sourcesByName, grpSmpOnly, threads);

  try
  {

   MTExporterStat stat = mtc.export(since, limit, now, grpMul, smpMul);

   formatter.exportGroupFooter(out);

   if(exportSources)
    formatter.exportSources(stat.getSourcesMap(), out);

   if(formatter.isSamplesExport())
   {
    sampleOut.close();

    sampleOut = null;

    formatter.exportSampleHeader(out, false, -1);

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


    formatter.exportSampleFooter(out);

   }

   formatter.exportFooter(out);
   
   java.util.Date endTime = new java.util.Date();
   long endTs = endTime.getTime();

   
   long rate = stat.getGroupCount()!=0? (endTs-startTs)/stat.getGroupCount():0;
   

   
   out.append("\n<!-- Exported: "+stat.getGroupCount()+" groups in "+threads+" threads. Rate: "+rate+"ms per group -->");
   
   rate = stat.getSampleCount()!=0? (endTs-startTs)/stat.getSampleCount():0;
   
   out.append("\n<!-- Samples in groups: "+stat.getSampleCount()+". Rate: "+rate+"ms per sample -->");

   rate = stat.getUniqSampleCount()!=0? (endTs-startTs)/stat.getUniqSampleCount():0;
   
   out.append("\n<!-- Unique samples: "+stat.getUniqSampleCount()+". Rate: "+rate+"ms per unique sample -->");

   
   out.append("\n<!-- Start time: "+simpleDateFormat.format(startTime)+" -->");
   out.append("\n<!-- End time: "+simpleDateFormat.format(endTime)+". Time spent: "+StringUtils.millisToString(endTs-startTs)+" -->");
   out.append("\n<!-- Thank you. Good bye. -->\n");


  }
  catch(Throwable e)
  {
   e.printStackTrace();
  }
  finally
  {
   if(sampleOut != null )
    sampleOut.close();
   
   if( tmpFile != null )
    tmpFile.delete();
  }

 }

 

}
