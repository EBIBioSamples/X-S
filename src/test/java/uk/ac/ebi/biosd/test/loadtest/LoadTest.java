package uk.ac.ebi.biosd.test.loadtest;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;
import uk.ac.ebi.fg.core_model.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicType;
import uk.ac.ebi.fg.core_model.expgraph.properties.BioCharacteristicValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;
import uk.ac.ebi.fg.core_model.expgraph.properties.Unit;
import uk.ac.ebi.fg.core_model.expgraph.properties.UnitDimension;
import uk.ac.ebi.fg.core_model.organizational.Contact;
import uk.ac.ebi.fg.core_model.organizational.ContactRole;
import uk.ac.ebi.fg.core_model.organizational.Organization;
import uk.ac.ebi.fg.core_model.organizational.Publication;
import uk.ac.ebi.fg.core_model.terms.OntologyEntry;
import uk.ac.ebi.fg.core_model.xref.ReferenceSource;

public class LoadTest
{

 /**
  * @param args
  */
 public static void main(String[] args)
 {
  Map<String, Object> conf = new TreeMap<String, Object>();
  
  conf.put("hibernate.connection.driver_class", "org.h2.Driver");
  conf.put("hibernate.connection.username", "sa");
  conf.put("hibernate.connection.password", "");
  conf.put("hibernate.connection.url", "jdbc:h2:tcp://cocoa.ebi.ac.uk/XStest");
  conf.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
  conf.put("hibernate.hbm2ddl.auto", "update");
  
  EntityManagerFactory fact = Persistence.createEntityManagerFactory ( "X-S", conf );
  
  EntityManager em = fact.createEntityManager();
  
  AccessibleDAO<MSI> dao = new AccessibleDAO<MSI> ( MSI.class,  em );
  EntityTransaction ts = em.getTransaction ();

  
  for( int i=1; i < 12; i++ )
  {
   ts.begin ();
   dao.getOrCreate ( createSubmission(i) );
   ts.commit ();
  }

 }
 
 static MSI createSubmission( int seq )
 {
  MSI msi = new MSI("sub"+seq);
  
  msi.setTitle("Sub title "+seq);
  
  msi.setReleaseDate( new Date() );
  msi.setUpdateDate(new Date() );
  
  for( int i=1; i <=3; i++ )
  {
   Contact cnt = new Contact();
   
   cnt.addContactRole(new ContactRole("C RoleA "+seq+" "+i) );
   cnt.addContactRole(new ContactRole("C RoleB "+seq+" "+i) );
   
   cnt.setFirstName("Name "+seq+" "+i);
   cnt.setLastName("Surname "+seq+" "+i);
   cnt.setMidInitials("MidIni "+seq+" "+i);
   cnt.setEmail("CEmail "+seq+" "+i);
   
   msi.addContact( cnt );
   
   Organization org = new Organization();
   
   org.addOrganizationRole( new ContactRole("O RoleA "+seq+" "+i)  );
   org.addOrganizationRole( new ContactRole("O RoleB "+seq+" "+i)  );
   
   org.setName("OName "+seq+" "+i);
   org.setAddress("OAddr "+seq+" "+i);
   org.setUrl("OURL "+seq+" "+i);
   org.setEmail("OEmail "+seq+" "+i);
  
   msi.addOrganization(org);
   
   msi.addPublication( new Publication("doiA "+seq+" "+i, "pubmed"+seq+" "+i) );

   DatabaseRefSource db = new DatabaseRefSource("dbacc"+seq+" "+i,"ver1");
   db.setName("DBName"+seq+" "+i);
   db.setUrl("DBUrl"+seq+" "+i);
  
   msi.addDatabase(db);
   
   ReferenceSource ref = new ReferenceSource("TSR"+seq+" "+i, "ver1");
   
   ref.setName("TSName "+seq+" "+i);
   ref.setUrl("TSURL "+seq+" "+i);
   
   msi.addReferenceSource(ref);
   
  }

  BioSampleGroup grp = new BioSampleGroup("Grp"+seq);
  
  grp.addPropertyValue( new ExperimentalPropertyValue<ExperimentalPropertyType>("ValueA"+seq, new ExperimentalPropertyType("GpropA"+seq) ) );
  grp.addPropertyValue( new ExperimentalPropertyValue<ExperimentalPropertyType>("ValueB"+seq, new ExperimentalPropertyType("GpropB"+seq) ) );
  grp.addPropertyValue( new ExperimentalPropertyValue<ExperimentalPropertyType>("ValueC"+seq, new ExperimentalPropertyType("GpropC"+seq) ) );
  
  genSamples(seq,grp, msi);
  
  msi.addSampleGroup(grp);
  
  return msi;
 }

 private static void genSamples(int seq, BioSampleGroup grp, MSI msi)
 {
  for( int i=1; i <= 3; i++ )
  {
   BioSample smp = new BioSample("Smp"+seq+"-"+i);
   
   smp.addPropertyValue(new BioCharacteristicValue("SMPValueA"+seq, new BioCharacteristicType("SPropA"+seq) ));
   
   ExperimentalPropertyValue<ExperimentalPropertyType> val = new ExperimentalPropertyValue<ExperimentalPropertyType>("SMPValueB"+seq, new ExperimentalPropertyType("SPropB"+seq) );
   val.setUnit( new Unit("unit"+seq, new UnitDimension("dim")) );
   
   smp.addPropertyValue(val);
   
   ReferenceSource rs = null;
   
   Iterator<ReferenceSource> rsit = msi.getReferenceSources().iterator();
   for( int j=0; j < i; j++ )
    rs = rsit.next();
    
   val = new ExperimentalPropertyValue<ExperimentalPropertyType>("SMPValueC"+seq, new ExperimentalPropertyType("SPropC"+seq) );
   val.addOntologyTerm( new OntologyEntry(rs.getName(), rs) );
   
   smp.addPropertyValue(val);


   grp.addSample(smp);
  }
  
 }

}
