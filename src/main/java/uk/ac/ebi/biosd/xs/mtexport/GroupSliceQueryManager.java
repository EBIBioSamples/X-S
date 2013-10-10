package uk.ac.ebi.biosd.xs.mtexport;

import java.sql.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import uk.ac.ebi.biosd.xs.util.Slice;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class GroupSliceQueryManager
{
 private final EntityManagerFactory factory;
 private EntityManager em;

 public GroupSliceQueryManager( EntityManagerFactory fact )
 {
  factory = fact;
 }
 

 
 @SuppressWarnings("unchecked")
 public List<BioSampleGroup> getGroups(long since, Slice slice)
 {
  Query listQuery = null;
  
  EntityManager em = factory.createEntityManager();
  
  
  if( since < 0 )
  {
    listQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a");
  }
  else
  {
    listQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName () + " grp JOIN grp.MSIs msi WHERE msi.updateDate > ?1");
  
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
  
  em.close();
  em=null;
 }

}
