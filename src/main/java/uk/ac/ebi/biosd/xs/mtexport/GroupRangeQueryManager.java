package uk.ac.ebi.biosd.xs.mtexport;

import java.sql.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class GroupRangeQueryManager
{
 private int blockSize = 50;
 
 private final EntityManagerFactory factory;
 private EntityManager em;

 public GroupRangeQueryManager( EntityManagerFactory fact )
 {
  factory = fact;
 }
 
 public GroupRangeQueryManager( EntityManagerFactory fact, int blksz )
 {
  factory = fact;
  blockSize = blksz;
 }

 public List<BioSampleGroup> getGroups(long since, long startId)
 {
  return getGroups( since, startId, null );
 }
 
 @SuppressWarnings("unchecked")
 public List<BioSampleGroup> getGroups(long since, long startId, Long endId)
 {
  Query listQuery = null;
  
  em = factory.createEntityManager();
  
  
  if( since < 0 )
  {
   if( endId == null )
    listQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a WHERE a.id >=:id ORDER BY a.id");
   else
   {
    listQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a WHERE a.id >=:id and a.id <= :endId ORDER BY a.id ");
    listQuery.setParameter("endId", endId.longValue());
   }
  }
  else
  {
   if( endId == null )
    listQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName () + " grp JOIN grp.MSIs msi WHERE grp.id >=:id and msi.updateDate > :upDate  ORDER BY grp.id");
   else
   {
    listQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName () + " grp JOIN grp.MSIs msi WHERE grp.id >=:id and grp.id <= :endId and msi.updateDate > :upDate  ORDER BY grp.id");
    listQuery.setParameter("endId", endId.longValue());
   }
  
   listQuery.setParameter("upDate", new Date(since));
  }
  
  listQuery.setMaxResults ( blockSize );  
  listQuery.setParameter ( "id", startId );
  
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
