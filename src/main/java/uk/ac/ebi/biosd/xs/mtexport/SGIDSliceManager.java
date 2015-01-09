package uk.ac.ebi.biosd.xs.mtexport;

import java.sql.Date;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Stack;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.util.Range;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class SGIDSliceManager
{
 private final Logger log = LoggerFactory.getLogger(SGIDSliceManager.class);

 private long[] groupIds;
 private long[] sampleIds;
 
 private int groupOffset = 0;
 private int sampleOffset = 0;
 
 private Stack<Range> sampleReturns = new Stack<Range>();
 private Stack<Range> groupReturns = new Stack<Range>();
 
 private BitSet sampleBS;
 
 private int blockSize; 
 
 @SuppressWarnings("unchecked")
 public SGIDSliceManager( EntityManagerFactory emf, int blSz, long since )
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
  synchronized(groupReturns)
  {
   if( groupReturns.size() > 0 )
    return groupReturns.pop();

   if( groupOffset >= groupIds.length )
    return null;
   
   log.debug("Requested group range. Offset {} out of {}",groupOffset,groupIds.length);

   Range r = new Range(groupIds[groupOffset], 0);
   
   groupOffset += blockSize;
   
   if( groupOffset > groupIds.length )
    groupOffset = groupIds.length;
   
   r.setMax(groupIds[groupOffset - 1]);
   
   return r;
  }
 }

 public void returnGroupRange(Range r)
 {
  synchronized(groupReturns)
  {
   groupReturns.push( r );
  }
 }

 public Range getSampleRange()
 {
  synchronized(sampleReturns)
  {
   if( sampleReturns.size() > 0 )
    return sampleReturns.pop();

   if( sampleOffset >= sampleIds.length )
    return null;
   
   log.debug("Requested sample range. Offset {} out of {}",sampleOffset,sampleIds.length);

   sampleOffset=sampleBS.nextClearBit(sampleOffset);
   
   Range r = new Range(sampleIds[sampleOffset], 0);
   
   sampleOffset += blockSize;
   
   if( sampleOffset > sampleIds.length )
    sampleOffset = sampleIds.length;
   
   r.setMax(sampleIds[sampleOffset-1]);
   

   return r;
  }
 }
 
 
 public void returnSampleRange(Range r)
 {
  synchronized(sampleReturns)
  {
   sampleReturns.push( r );
  }
 }


 public int getChunkSize()
 {
  return blockSize;
 }

}
