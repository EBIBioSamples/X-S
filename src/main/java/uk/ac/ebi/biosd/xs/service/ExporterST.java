package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.log.LoggerFactory;
import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;

public class ExporterST implements Exporter
{
 private final EntityManagerFactory emf;
 private final AbstractXMLFormatter formatter;
 private final boolean exportSources;
 private final boolean sourcesByName;
 
 public ExporterST(EntityManagerFactory emf, AbstractXMLFormatter formatter, boolean exportSources, boolean sourcesByName, int blockSize)
 {
  this.emf = emf;
  this.formatter = formatter;
  this.exportSources = exportSources;
  this.sourcesByName = sourcesByName;
 }
 
 @Override
 public void export( long since, Appendable out, long limit) throws IOException
 {
  assert LoggerFactory.getLogger().entry("Start exporting", "export");
  
  GroupQueryManager grpMngr = new GroupQueryManager(emf);
  
  long startID = Long.MIN_VALUE;
  long count = 0;
  
  
  Map<String, Counter> srcMap = new HashMap<String, Counter>();
  
  Set<String> msiTags = new HashSet<String>();
  
  formatter.exportHeader( since, out );
  
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
     count++;
     i++;

     formatter.exportGroup(g, out);

     msiTags.clear();
     int nSmp = g.getSamples().size();

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

     startID = g.getId() + 1;

     if(count >= limit)
      break blockLoop;
    }

   }
   finally
   {
    grpMngr.release();
   }

   assert LoggerFactory.getLogger().exit("End processing block", "block");

   if(i < grpMngr.getBlockSize() )
    break;
  }

  
  if( exportSources )
   formatter.exportSources(srcMap, out);
  
  
  formatter.exportFooter(out);

  assert LoggerFactory.getLogger().exit("End exporting", "export");

  
 }
 

}
