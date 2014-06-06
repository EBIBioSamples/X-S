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
 private static final boolean useTransaction = false;
 private static final Logger log = LoggerFactory.getLogger(SampleSliceQueryManager.class);

 private final EntityManagerFactory factory;
 private EntityManager em;

 public SampleSliceQueryManager( EntityManagerFactory fact )
 {
  factory = fact;
 }
 

 
 @SuppressWarnings("unchecked")
 public List<BioSample> getSamples(long since, Slice slice)
 {
  Query listQuery = null;
  
  if( em != null )
  {
   log.warn("Entity manager was not closed");
   em.close();
  }
  
  em = factory.createEntityManager();
  
  if( useTransaction )
   em.getTransaction().begin();
  
  if( since < 0 )
  {
    listQuery = em.createQuery("SELECT a FROM " + BioSample.class.getCanonicalName () + " a");
  }
  else
  {
    listQuery = em.createQuery("SELECT smp FROM " + BioSample.class.getCanonicalName () + " smp JOIN smp.MSIs msi WHERE msi.updateDate > ?1");
  
   listQuery.setParameter(1, new Date(since));
  }
  
  listQuery.setMaxResults ( slice.getLimit() );  
  listQuery.setFirstResult( slice.getStart() );
  
  return listQuery.getResultList();
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
  
  em.close();
  em=null;
 }

}
