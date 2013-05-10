package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;

public class Exporter
{
 private final EntityManager em;
 private final AbstractXMLFormatter formatter;
 private final boolean exportSources;
 private final boolean sourcesByName;
 private final int blockSize;
 private final long limit;
 
 public Exporter(EntityManager em, AbstractXMLFormatter formatter, boolean exportSources, boolean sourcesByName, int blockSize, long limit)
 {
  super();
  this.em = em;
  this.formatter = formatter;
  this.exportSources = exportSources;
  this.sourcesByName = sourcesByName;
  this.blockSize = blockSize;
  this.limit = limit;
 }

 
 public void export( long since, Appendable out) throws IOException
 {
  Query listQuery = null;
  
  long startID = Long.MIN_VALUE;
  long count = 0;
  
  if( since < 0 )
   listQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a WHERE a.id >=?1 ORDER BY a.id");
  else
  {
   listQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName () + " grp JOIN grp.MSIs msi WHERE grp.id >=?1 and msi.updateDate > ?2  ORDER BY grp.id");
  
   listQuery.setParameter(2, new Date(since));
  }
  
  listQuery.setMaxResults ( blockSize );  
  
  Map<String, Counter> srcMap = new HashMap<String, Counter>();
  
  Set<String> msiTags = new HashSet<String>();
  
  EntityTransaction ts = em.getTransaction ();
  
  ts.begin ();

  formatter.exportHeader(new java.util.Date().getTime(), since, out);
  
  try
  {

   blockLoop: while(true)
   {

    listQuery.setParameter ( 1, startID );
    
    @SuppressWarnings("unchecked")
    List<BioSampleGroup> result = listQuery.getResultList();
    
    int i=0;
    
    for(BioSampleGroup g : result)
    {
     count++;
     i++;

     formatter.exportGroup( g, out );

     msiTags.clear();
     int nSmp = g.getSamples().size();
     
     for( MSI msi : g.getMSIs() )
     {
      for( DatabaseRefSource db :  msi.getDatabases() )
      {
       String scrNm = sourcesByName?db.getName():db.getAcc();
       
       if( scrNm == null )
        continue;
       
       scrNm = scrNm.trim();

       if( scrNm.length() == 0 )
        continue;
       
       if( msiTags.contains(scrNm) )
        continue;
       
       msiTags.add(scrNm);
       
       Counter c = srcMap.get(scrNm);
       
       if( c == null )
        srcMap.put(scrNm, new Counter(nSmp) );
       else
        c.add(nSmp);
       
      }
     }
     
     
     startID=g.getId()+1;
     
     if( count >= limit )
      break blockLoop;
    }
    
    if( i < blockSize )
     break;
   }
  }
  finally
  {
   ts.commit();
  }
  
  if( exportSources )
   formatter.exportSources(srcMap, out);
  
  formatter.exportFooter(out);

 }
}
