package uk.ac.ebi.biosd.xs.mtexport;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.util.Slice;
import uk.ac.ebi.biosd.xs.util.SliceManager;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.expgraph.Product;

public class SliceQueryManager implements QueryManager
{
 private static final boolean useTransaction = false;
 private static final Logger log = LoggerFactory.getLogger(SliceQueryManager.class);

 private final EntityManagerFactory factory;
 private EntityManager em;
 private Query sampleQuery;
 private Query groupQuery;
 private Query msiQuery;
 private final long since;
 private final SliceManager sliceManager;
 
 public SliceQueryManager( EntityManagerFactory fact, SliceManager slMng, long since )
 {
  factory = fact;
  this.since = since;
  sliceManager=slMng;
 }
 
 @Override
 public int getChunkSize()
 {
  return sliceManager.getSliceSize();
 }
 
 private void createEM()
 {
  if(em != null)
   return;

  em = factory.createEntityManager();

  if(useTransaction)
   em.getTransaction().begin();

  if(since < 0)
  {
   groupQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName() + " a");
   sampleQuery = em.createQuery("SELECT a FROM " + Product.class.getCanonicalName() + " a");
   msiQuery = em.createQuery("SELECT a FROM " + MSI.class.getCanonicalName() + " a");
  }
  else
  {
   groupQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName() + " grp JOIN grp.MSIs msi WHERE msi.updateDate > ?1");
   groupQuery.setParameter(1, new Date(since));

   sampleQuery = em.createQuery("SELECT smp FROM " + Product.class.getCanonicalName() + " smp JOIN smp.MSIs msi WHERE msi.updateDate > ?1");
   sampleQuery.setParameter(1, new Date(since));

   msiQuery = em.createQuery("SELECT msi FROM " + MSI.class.getCanonicalName() + " msi  WHERE m.updateDate > ?1");
   msiQuery.setParameter(1, new Date(since));

  }

 }
 
 @Override
 @SuppressWarnings("unchecked")
 public List<BioSampleGroup> getGroups()
 {
  createEM();
  
  Slice slice = sliceManager.getSlice("group");
 
  groupQuery.setMaxResults ( slice.getLimit() );  
  groupQuery.setFirstResult( slice.getStart() );
  
  try
  {
   return groupQuery.getResultList();
  }
  catch(Exception e)
  {
   sliceManager.returnSlice("group", slice);
   throw e;
  }
 }
 
 @Override
 @SuppressWarnings("rawtypes")
 public List<BioSample> getSamples()
 {
  createEM();
  
  Slice slice = sliceManager.getSlice("sample");

  sampleQuery.setMaxResults ( slice.getLimit() );  
  sampleQuery.setFirstResult( slice.getStart() );
  
  try
  {
   List query = sampleQuery.getResultList();
   List<BioSample> res = new ArrayList<BioSample>(query.size());

   for( Object p : query )
    if( p instanceof BioSample )
     res.add( (BioSample)p );
  
   return res;
  }
  catch(Exception e)
  {
   sliceManager.returnSlice("sample", slice);
   throw e;
  }

 }
 

 @Override
 @SuppressWarnings("unchecked")
 public List<MSI> getMSIs()
 {
  createEM();
  
  Slice slice = sliceManager.getSlice("msi");

  msiQuery.setMaxResults ( slice.getLimit() );  
  msiQuery.setFirstResult( slice.getStart() );
  
  try
  {
   return msiQuery.getResultList();
  }
  catch(Exception e)
  {
   sliceManager.returnSlice("msi", slice);
   throw e;
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
 public int getRecovers()
 {
  return 0;
 }



}
