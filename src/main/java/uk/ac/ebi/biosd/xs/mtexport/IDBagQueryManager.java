package uk.ac.ebi.biosd.xs.mtexport;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.util.Range;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;

public class IDBagQueryManager implements IDPrefetchQueryManager
{
 private static final int RETRIEVE_ATTEMPTS=3;
 
 private static final boolean useTransaction = false;
 private static final Logger log = LoggerFactory.getLogger(IDBagQueryManager.class);

 private EntityManagerFactory factory;
 private EntityManager em;
 
 private Query sampleQuery;
 private Query groupQuery;

 private int recovers = 0;
 
 private SGIDBagManager sgidsm;
 
  
 public IDBagQueryManager(EntityManagerFactory emf, SGIDBagManager slMgr )
 {
  factory = emf; 
  
  sgidsm = slMgr;
 }
 
 
 private void createEM()
 {
  if(em != null)
   return;

  em = factory.createEntityManager();

  if(useTransaction)
   em.getTransaction().begin();

   groupQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a WHERE a.id >=:id and a.id <= :endId");
   sampleQuery = em.createQuery("SELECT a FROM " + BioSample.class.getCanonicalName () + " a WHERE a.id in :ids");

 }
 
 
 @Override
 @SuppressWarnings("unchecked")
 public List<BioSampleGroup> getGroups()
 {
  Range r = sgidsm.getGroupRange();

  if(r == null)
  {
   log.debug("({}) No more group ranges", Thread.currentThread().getName());
   return Collections.emptyList();
  }
  else
   log.debug("({}) Processing group range {}", Thread.currentThread().getName(), r);

  int tries = 0;

  while(true)
  {
   try
   {
    createEM();

    groupQuery.setParameter("id", r.getMin());
    groupQuery.setParameter("endId", r.getMax());

    List<BioSampleGroup> res = groupQuery.getResultList();

    log.debug("({}) Retrieved groups: {}", Thread.currentThread().getName(), res.size());

    return res;
   }
   catch(Exception e)
   {
    if(tries >= RETRIEVE_ATTEMPTS)
     throw e;

    tries++;
    recovers++;

    close();
   }
  }

 }
 
 @Override
 public boolean checkInSample( long sid )
 {
  return sgidsm.checkInSample(sid);
 }
 
 @Override
 @SuppressWarnings("unchecked")
 public List<BioSample> getSamples()
 {
  createEM();

  Collection<Long> bag = sgidsm.getSampleBag();

  if(bag.size() == 0)
  {
   log.debug("({}) No more sample IDs", Thread.currentThread().getName());
   return Collections.emptyList();
  }
  else
   log.debug("({}) Processing samples bag {}", Thread.currentThread().getName(), bag.size());

  int tries = 0;

  while(true)
  {
   try
   {
    createEM();

    sampleQuery.setParameter("ids", bag);

    List<BioSample> res = sampleQuery.getResultList();

    log.debug("({}) Retrieved samples: {}", Thread.currentThread().getName(), res.size());

    return res;
   }
   catch(Exception e)
   {
    if(tries >= RETRIEVE_ATTEMPTS)
     throw e;

    tries++;
    recovers++;

    close();
   }
  }

 }
 
 @Override
 public List<MSI> getMSIs()
 {
  throw new UnsupportedOperationException();
 }
 
 @Override
 public void release()
 {
  if( em == null )
   return;
  
  if( useTransaction )
  {
   EntityTransaction trn = em.getTransaction();

   if( trn.isActive() && ! trn.getRollbackOnly() )
   {
    try
    {
     trn.commit();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
  }
  
  em.clear();
 }



 @Override
 public void close()
 {
  if( em == null )
   return;
  
  if( useTransaction )
  {
   EntityTransaction trn = em.getTransaction();

   if( trn.isActive() && ! trn.getRollbackOnly() )
   {
    try
    {
     trn.commit();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
  }
  
  em.close();
  em=null;
  
 }

 @Override
 public int getChunkSize()
 {
  return sgidsm.getChunkSize();
 }


 @Override
 public int getRecovers()
 {
  return recovers;
 }

}
