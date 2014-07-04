package uk.ac.ebi.biosd.xs.mtexport;

import java.sql.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.util.Slice;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;

public class SampleSliceQueryManager
{
 static final int MAX_SAMPLES_PER_EM = 10000;
 
 private static final boolean useTransaction = false;
 private static final Logger log = LoggerFactory.getLogger(SampleSliceQueryManager.class);

 private final EntityManagerFactory factory;
 private EntityManager em;
 private Query listQuery = null;
 private final long since;
 private int smpCount;

 public SampleSliceQueryManager( EntityManagerFactory fact, long since )
 {
  smpCount = 0;
  
  factory = fact;
  this.since = since;

  initEM();
  
 }
 
 private void initEM()
 {
  if( em != null )
   em.close();
  
  em = factory.createEntityManager();
  
  if( since < 0 )
  {
    listQuery = em.createQuery("SELECT a FROM " + BioSample.class.getCanonicalName () + " a");
  }
  else
  {
    listQuery = em.createQuery("SELECT smp FROM " + BioSample.class.getCanonicalName () + " smp JOIN smp.MSIs msi WHERE msi.updateDate > ?1");
  
   listQuery.setParameter(1, new Date(since));
  }
 }
 
 @SuppressWarnings("unchecked")
 public List<BioSample> getSamples(Slice slice)
 {
  if( smpCount+slice.getLimit() > MAX_SAMPLES_PER_EM )
  {
   smpCount=0;
   initEM();
  }
  
  
  if( useTransaction )
   em.getTransaction().begin();
  
  listQuery.setMaxResults ( slice.getLimit() );  
  listQuery.setFirstResult( slice.getStart() );
  
  List<BioSample> res = listQuery.getResultList();
  
  smpCount += res.size();
  
  return res;
 }
 
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

}
