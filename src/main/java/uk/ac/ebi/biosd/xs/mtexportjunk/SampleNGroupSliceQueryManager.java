package uk.ac.ebi.biosd.xs.mtexportjunk;

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
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.expgraph.Product;

public class SampleNGroupSliceQueryManager
{
 private static final boolean useTransaction = false;
 private static final Logger log = LoggerFactory.getLogger(SampleNGroupSliceQueryManager.class);

 private final EntityManagerFactory factory;
 private EntityManager em;
 private Query sampleQuery;
 private Query groupQuery;
 private final long since;
 
 public SampleNGroupSliceQueryManager( EntityManagerFactory fact, long since )
 {
  factory = fact;
  this.since = since;
 }
 
 private void createEM()
 {
  if( em != null )
   return;
  
  em = factory.createEntityManager();
  
 if( useTransaction )
  em.getTransaction().begin();
 
 if( since < 0 )
 {
  groupQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a");
 }
 else
 {
  groupQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName () + " grp JOIN grp.MSIs msi WHERE msi.updateDate > ?1");
 
  groupQuery.setParameter(1, new Date(since));
 }

 if( since < 0 )
 {
  sampleQuery = em.createQuery("SELECT a FROM " + Product.class.getCanonicalName () + " a");
 }
 else
 {
  sampleQuery = em.createQuery("SELECT smp FROM " + BioSample.class.getCanonicalName () + " smp JOIN smp.MSIs msi WHERE msi.updateDate > ?1");
 
  sampleQuery.setParameter(1, new Date(since));
 }
 
 }
 
 @SuppressWarnings("unchecked")
 public List<BioSampleGroup> getGroups(Slice slice)
 {
  createEM();
  
 
  groupQuery.setMaxResults ( slice.getLimit() );  
  groupQuery.setFirstResult( slice.getStart() );
  
  return groupQuery.getResultList();
 }
 
 @SuppressWarnings("unchecked")
 public List<Product<?>> getSamples(Slice slice)
 {
  createEM();
  
 
  sampleQuery.setMaxResults ( slice.getLimit() );  
  sampleQuery.setFirstResult( slice.getStart() );
  
  return sampleQuery.getResultList();
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
