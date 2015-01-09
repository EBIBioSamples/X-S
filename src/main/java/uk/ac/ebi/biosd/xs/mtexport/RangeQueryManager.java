package uk.ac.ebi.biosd.xs.mtexport;

import java.sql.Date;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.util.Range;
import uk.ac.ebi.biosd.xs.util.RangeManager;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;

public class RangeQueryManager implements QueryManager
{
 private static final boolean useTransaction = false;
 private static final Logger log = LoggerFactory.getLogger(RangeQueryManager.class);

 private int blockSize = 50;
 
 private final EntityManagerFactory factory;
 private EntityManager em;
 private final RangeManager rangeManager;
 private Query sampleQuery;
 private Query groupQuery;
 private Query msiQuery;
 private final long since;

// public RangeQueryManager( EntityManagerFactory fact , RangeManager rm )
// {
//  factory = fact;
//  
//  rangeManager = rm;
// }
 
 public RangeQueryManager( EntityManagerFactory fact, RangeManager rm, int blksz, long since )
 {
  factory = fact;
  blockSize = blksz;
  
  this.since=since;
  rangeManager = rm;
 }

 private void createEM()
 {
  if(em != null)
   return;

  em = factory.createEntityManager();

  if(useTransaction)
   em.getTransaction().begin();

  if( since < 0 )
  {
   groupQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a WHERE a.id >=:id and a.id <= :endId ORDER BY a.id ");
   sampleQuery = em.createQuery("SELECT a FROM " + BioSample.class.getCanonicalName () + " a WHERE a.id >=:id and a.id <= :endId ORDER BY a.id ");
   msiQuery = em.createQuery("SELECT a FROM " + MSI.class.getCanonicalName () + " a WHERE a.id >=:id and a.id <= :endId ORDER BY a.id ");

  }
  else
  {
   groupQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName () + " grp JOIN grp.MSIs msi WHERE grp.id >=:id and grp.id <= :endId and msi.updateDate > :upDate  ORDER BY grp.id");
   groupQuery.setParameter("upDate", new Date(since));

   sampleQuery = em.createQuery("SELECT smp FROM " + BioSample.class.getCanonicalName () + " smp JOIN smp.MSIs msi WHERE smp.id >=:id and smp.id <= :endId and msi.updateDate > :upDate  ORDER BY smp.id");
   sampleQuery.setParameter("upDate", new Date(since));

   msiQuery = em.createQuery("SELECT msi FROM " + MSI.class.getCanonicalName () + " msi WHERE msi.id >=:id and msi.id <= :endId and msi.updateDate > :upDate  ORDER BY msi.id");
   msiQuery.setParameter("upDate", new Date(since));
  }

 }
 
 
 @Override
 @SuppressWarnings("unchecked")
 public List<BioSampleGroup> getGroups()
 {
  createEM();
  
  while( true )
  {

   Range r = rangeManager.getRange("group");

   if(r == null)
   {
    log.debug("({}) No more group ranges", Thread.currentThread().getName());
    return Collections.emptyList();
   }
   else
    log.debug("({}) Processing group range {}", Thread.currentThread().getName(), r);

   groupQuery.setMaxResults(blockSize);
   groupQuery.setParameter("id", r.getMin());
   groupQuery.setParameter("endId", r.getMax());

   List<BioSampleGroup> res = null;

   try
   {
    res = groupQuery.getResultList();
   }
   catch(Exception e)
   {
    rangeManager.returnRange("group", r);
    throw e;
   }
   
   
   if(res.size() > 0)
   {
    r.setMin(res.get(res.size() - 1).getId() + 1);

    log.debug("({}) Retrieved {} groups. Returning group range {}",  new Object[]{ Thread.currentThread().getName(), res.size(), r});

    rangeManager.returnRange("group", r);

    return res;
   }
   else
   {
    log.debug("({}) No groups in range {}", Thread.currentThread().getName(), r);

    rangeManager.returnRange("group", null);
   }

  }
 }
 
 @Override
 @SuppressWarnings("unchecked")
 public List<BioSample> getSamples()
 {
  createEM();
  
  while( true )
  {
   
   Range r = rangeManager.getRange("sample");

   if(r == null)
   {
    log.debug("({}) No more sample ranges", Thread.currentThread().getName());
    return Collections.emptyList();
   }
   else
    log.debug("({}) Processing sample range {}", Thread.currentThread().getName(), r);

   sampleQuery.setMaxResults(blockSize);
   sampleQuery.setParameter("id", r.getMin());
   sampleQuery.setParameter("endId", r.getMax());

   List<BioSample> res = null;
   
   try
   {
    res = sampleQuery.getResultList();
   }
   catch(Exception e)
   {
    rangeManager.returnRange("sample", r);
    throw e;
   }
   
   if(res.size() > 0)
   {
    r.setMin(res.get(res.size() - 1).getId() + 1);

    log.debug("({}) Retrieved {} samples. Returning sample range {}",  new Object[]{ Thread.currentThread().getName(), res.size(), r});

    rangeManager.returnRange("sample", r);

    return res;
   }
   else
   {
    log.debug("({}) No samples in range {}", Thread.currentThread().getName(), r);

    rangeManager.returnRange("sample", null);
   }

  }
 }
 
 @Override
 @SuppressWarnings("unchecked")
 public List<MSI> getMSIs()
 {
  createEM();
  
  while( true )
  {

   Range r = rangeManager.getRange("msi");

   if(r == null)
   {
    log.debug("({}) No more MSI ranges", Thread.currentThread().getName());
    return Collections.emptyList();
   }
   else
    log.debug("({}) Processing MSI range {}", Thread.currentThread().getName(), r);

   msiQuery.setMaxResults(blockSize);
   msiQuery.setParameter("id", r.getMin());
   msiQuery.setParameter("endId", r.getMax());

   List<MSI> res = null;

   try
   {
    res = msiQuery.getResultList();
   }
   catch(Exception e)
   {
    rangeManager.returnRange("msi", r);
    throw e;
   }
   
   if(res.size() > 0)
   {
    r.setMin(res.get(res.size() - 1).getId() + 1);

    log.debug("({}) Retrieved {} MSIs. Returning MSI range {}",  new Object[]{ Thread.currentThread().getName(), res.size(), r});

    rangeManager.returnRange("msi", r);

    return res;
   }
   else
   {
    log.debug("({}) No MSIs in range {}", Thread.currentThread().getName(), r);

    rangeManager.returnRange("msi", null);
   }

  }
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
  return blockSize;
 }

 @Override
 public int getRecovers()
 {
  return 0;
 }
 
}
