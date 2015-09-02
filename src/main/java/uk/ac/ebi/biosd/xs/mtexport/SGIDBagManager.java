package uk.ac.ebi.biosd.xs.mtexport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.util.Range;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class SGIDBagManager
{
 private final Logger log = LoggerFactory.getLogger(SGIDBagManager.class);

 private long[] groupIds;
 private long[] sampleIds;
 
 private int groupOffset = 0;
 private int sampleOffset = 0;

 
 private BitSet sampleBS;
 
 private int blockSize; 
 
 @SuppressWarnings("unchecked")
 public SGIDBagManager( EntityManagerFactory emf, int blSz, long since )
 {
  blockSize = blSz;
  
  EntityManager em = emf.createEntityManager();

  Query idSelQuery=null;
  
  if( since > 0 )
  {
   idSelQuery = em.createQuery("select smp.id from "+BioSample.class.getCanonicalName()+ " smp JOIN smp.MSIs msi WHERE msi.updateDate > :upDate");
   idSelQuery.setParameter("upDate", new Date(since));
  }
  else
   idSelQuery = em.createQuery("select id from "+BioSample.class.getCanonicalName());
  
  Collection<Long> sids = idSelQuery.getResultList();
  
  sampleIds = new long[ sids.size() ];
  
  int i=0;
  for( Long l : sids )
   sampleIds[i++] = l.longValue();
  
  Arrays.sort(sampleIds);
  
  sampleBS = new BitSet(sampleIds.length);
  
  log.debug("Retrieved {} sample IDs",sampleIds.length);
  
  em.clear();
  
  if( since > 0 )
  {
   idSelQuery = em.createQuery("select grp.id from "+BioSampleGroup.class.getCanonicalName()+ " grp JOIN grp.MSIs msi WHERE msi.updateDate > :upDate");
   idSelQuery.setParameter("upDate", new Date(since));
  }
  else
   idSelQuery = em.createQuery("select id from "+BioSampleGroup.class.getCanonicalName());

  
  
  sids = idSelQuery.getResultList();
 
  groupIds = new long[ sids.size() ];
  
  i=0;
  for( Long l : sids )
   groupIds[i++] = l.longValue();
  
  Arrays.sort(groupIds);
  
  log.debug("Retrieved {} group IDs",groupIds.length);

  em.close();
 }
 
 public void dumpSGids(String path)
 {
  File dir = new File( path );
  
  PrintStream fos = null;
  try
  {
   fos = new PrintStream(new File(dir,"sampleIDdump.txt"));
   
   for( long id : sampleIds )
   {
    fos.print(id);
    fos.print('\n');
   }
   
   fos.print("--END--");
   fos.close();
   
   fos = new PrintStream(new File(dir,"groupIDdump.txt"));
   
   for( long id : groupIds )
   {
    fos.print(id);
    fos.print('\n');
   }
   
   fos.print("--END--");
   fos.close();

  }
  catch(FileNotFoundException e)
  {
   e.printStackTrace();
  }
 }
 
 public boolean checkInSample( long sid )
 {

  synchronized(sampleBS)
  {
   int pos = Arrays.binarySearch(sampleIds, sid);
   
   if( pos < 0 )
   {
    log.warn("Unexpected (new) sample with ID={}",sid);
    return false;
   }
   
   if( sampleBS.get(pos) )
    return false;
   
   sampleBS.set(pos);
   
   return true;
  }
  
 }
 
 public int getSampleCount()
 {
  return sampleIds.length;
 }
 
 public int getGroupsCount()
 {
  return groupIds.length;
 }

 public Range getGroupRange()
 {
  synchronized(groupIds)
  {
   if( groupOffset >= groupIds.length )
    return null;
   
   if( log.isDebugEnabled() )
    log.debug("Requested group range. Offset {} out of {} ({}%)", new Object[]{groupOffset,groupIds.length,groupOffset*100/groupIds.length});

   Range r = new Range(groupIds[groupOffset], 0);
   
   groupOffset += blockSize;
   
   if( groupOffset > groupIds.length )
    groupOffset = groupIds.length;
   
   r.setMax(groupIds[groupOffset - 1]);
   
   return r;
  }
 }

 public Collection<Long> getSampleBag()
 {
  ArrayList<Long> res = new ArrayList<Long>();
  
  synchronized(sampleBS)
  {

   if( sampleOffset < sampleIds.length && res.size() < blockSize )
    sampleOffset = collectSampleIds(sampleOffset, sampleIds.length-1, blockSize-res.size(), res);
   
   if( log.isDebugEnabled() )
    log.debug("Requested sample bag. Collected {} IDs. New offset {} out of {} ({}%)", new Object[]{res.size(), sampleOffset,sampleIds.length,sampleOffset*100/sampleIds.length} );
  }
  
  return res;
 }

 private int collectSampleIds(int fromInd, int toInd, int nSmp, Collection<Long> accum )
 {
  int collected = 0;
  
  do
  {
   fromInd = sampleBS.nextClearBit(fromInd);
   
   if( fromInd > toInd )
    break;
   
   accum.add( sampleIds[fromInd] );
   fromInd++;
   collected++;

   
  }while( fromInd <= toInd && collected < nSmp );
  
  return fromInd;
 }
 
 public int getChunkSize()
 {
  return blockSize;
 }

}
