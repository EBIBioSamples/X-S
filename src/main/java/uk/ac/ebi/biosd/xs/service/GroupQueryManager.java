package uk.ac.ebi.biosd.xs.service;

import java.sql.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class GroupQueryManager
{
 private int blockSize = 50;
 
 private final EntityManagerFactory factory;
 private EntityManager em;

 public GroupQueryManager( EntityManagerFactory fact )
 {
  factory = fact;
 }
 
 public GroupQueryManager( EntityManagerFactory fact, int blksz )
 {
  factory = fact;
  blockSize = blksz;
 }

 
 @SuppressWarnings("unchecked")
 public List<BioSampleGroup> getGroups(long since, long startId)
 {
  Query listQuery = null;
  
  EntityManager em = factory.createEntityManager();
  
  if( since < 0 )
   listQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a WHERE a.id >=?1 ORDER BY a.id");
  else
  {
   listQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName () + " grp JOIN grp.MSIs msi WHERE grp.id >=?1 and msi.updateDate > ?2  ORDER BY grp.id");
  
   listQuery.setParameter(2, new Date(since));
  }
  
  listQuery.setMaxResults ( blockSize );  
  listQuery.setParameter ( 1, startId );
  
  return listQuery.getResultList();
 }
 
 public void release()
 {
  if( em == null )
   return;
  
  em.close();
  em=null;
 }

 public int getBlockSize()
 {
  return blockSize;
 }
 
}
