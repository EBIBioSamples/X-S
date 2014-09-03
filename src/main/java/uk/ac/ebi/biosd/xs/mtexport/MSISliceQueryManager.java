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
import uk.ac.ebi.fg.biosd.model.organizational.MSI;

public class MSISliceQueryManager
{
 static final int MAX_MSI_PER_EM = 100;
 
 private static final boolean useTransaction = true;
 private static final Logger log = LoggerFactory.getLogger(MSISliceQueryManager.class);

 private final EntityManagerFactory factory;
 private EntityManager em=null;
 private Query listQuery = null;
 private final long since;
 private int msiCount;

 public MSISliceQueryManager( EntityManagerFactory fact, long since )
 {
  msiCount = 0;
  
  factory = fact;
  this.since = since;

 }
 
 private void initEM()
 {
  release();
  
  em = factory.createEntityManager();
  
  if( since < 0 )
  {
    listQuery = em.createQuery("SELECT a FROM " + MSI.class.getCanonicalName () + " a");
  }
  else
  {
   listQuery = em.createQuery("SELECT msi FROM " + MSI.class.getCanonicalName () + " msi WHERE msi.updateDate > ?1");
  
   listQuery.setParameter(1, new Date(since));
  }
 }
 
 @SuppressWarnings("unchecked")
 public List<MSI> getMSIs(Slice slice)
 {
  if( em == null || msiCount+slice.getLimit() > MAX_MSI_PER_EM )
  {
   msiCount=0;
   initEM();
  }
  
  
  if( useTransaction )
   em.getTransaction().begin();
  
  listQuery.setMaxResults ( slice.getLimit() );  
  listQuery.setFirstResult( slice.getStart() );
  
  List<MSI> res = listQuery.getResultList();
  
  msiCount += res.size();
  
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
  em.close();
  em=null;
 }



 public void close()
 {
  release();
 }

}
