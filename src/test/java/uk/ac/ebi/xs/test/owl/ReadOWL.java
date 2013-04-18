package uk.ac.ebi.xs.test.owl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadOWL
{

 /**
  * @param args
  * @throws OWLOntologyCreationException 
  * @throws IOException 
  */
 public static void main(String[] args) throws OWLOntologyCreationException, IOException
 {
  OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
//Let's load an ontology from the web
  
  URL efoulr = new URL( "http://www.ebi.ac.uk/efo/efo.owl" );
  
  HttpURLConnection conn = (HttpURLConnection)efoulr.openConnection();
 
  ReadOWL rd = new ReadOWL();
  
  rd.loadTermMap(conn.getInputStream());
  
  conn.disconnect();
  
  for( Map.Entry<String, Set<String>> me : rd.termMap.entrySet() )
  {
   System.out.println(me.getKey()+" +>");
   System.out.println(me.getValue());
  }
  
 
//  IRI iri = IRI.create("http://www.ebi.ac.uk/efo/efo.owl");
//  OWLOntology pizzaOntology = manager.loadOntologyFromOntologyDocument(iri);
//
//  Set<OWLClass> clss = pizzaOntology.getClassesInSignature();
//  
//  System.out.println( clss.size() );
//  
//  for( OWLClass cls : clss )
//   System.out.println(cls.getIRI().getFragment());
  
 }
 
 
 // logging machinery
 private final Logger logger = LoggerFactory.getLogger(getClass());

 private final static IRI IRI_AE_LABEL = IRI.create("http://www.ebi.ac.uk/efo/ArrayExpress_label");
 private final static IRI IRI_EFO_URI = IRI.create("http://www.ebi.ac.uk/efo/ArrayExpress_label");
 private final static IRI IRI_ALT_TERM = IRI.create("http://www.ebi.ac.uk/efo/alternative_term");
 private final static IRI IRI_ORG_CLASS = IRI.create("http://www.ebi.ac.uk/efo/organizational_class");
 private final static IRI IRI_PART_OF = IRI.create("http://www.obofoundry.org/ro/ro.owl#part_of");
 private final static IRI IRI_VERSION_INFO = IRI.create("http://www.w3.org/2002/07/owl#versionInfo");

 private final Map<String, Set<String>> reverseSubClassOfMap = new HashMap<String, Set<String>>();
 private final Map<String, Set<String>> reversePartOfMap = new HashMap<String, Set<String>>();

 private final Map<String, Set<String>> termMap = new HashMap<String, Set<String>>();


 void loadTermMap( InputStream ontologyStream )
 {
  OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
  OWLOntology ontology;
  OWLReasoner reasoner = null;


  try
  {
   // to prevent RDFXMLParser to fail on some machines
   // with SAXParseException: The parser has encountered more than "64,000" entity expansions
   System.setProperty("entityExpansionLimit", "100000000");
   ontology = manager.loadOntologyFromOntologyDocument(ontologyStream);


   OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
   reasoner = reasonerFactory.createReasoner(ontology);

   reasoner.precomputeInferences();
   if(reasoner.isConsistent())
   {

    Set<OWLClass> classes = ontology.getClassesInSignature();
    
    Map<String,OWLClassTerms> cMap = new HashMap<String,OWLClassTerms>();
    
    for(OWLClass cls : classes)
    {
     OWLClassTerms ct = loadClass(cls, ontology);
    
     if( ct != null )
      cMap.put(cls.toStringID(), ct);
    }
    
    for( OWLClassTerms termc : cMap.values()  )
    {
     if( termc.getOWLClass() == null )
      continue;
     
     expand( termc, cMap, reasoner );
    }
    
    for( OWLClassTerms termc : cMap.values()  )
    {
     Set<String> set = termMap.get(termc.getLabel());
     
     if( set == null )
     {
      set=new HashSet<String>();
      termMap.put(termc.getLabel().toLowerCase(), set);
     }
     
     set.addAll(termc.getTerms());
    }
    
   }
  }
  catch(OWLOntologyCreationException e)
  {
   throw new RuntimeException("Unable to read ontology from a stream", e);
  }
  catch(UnsupportedOperationException e)
  {
   throw new RuntimeException("Unable to reason the ontology", e);
  }
  finally
  {
   if(null != reasoner)
   {
    reasoner.dispose();
   }
  }

 }
 
 private void expand( OWLClassTerms termc, Map<String,OWLClassTerms> cMap, OWLReasoner reasoner )
 {
  NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(termc.getOWLClass(), true);
  for(Node<OWLClass> superClass : superClasses)
  {
   for( OWLClass ocls : superClass.getEntities() )
   {
    OWLClassTerms supTrm = cMap.get(ocls.toStringID());
    
    if( supTrm == null )
     continue;
    
    if( supTrm.getOWLClass() != null )
     expand(supTrm, cMap, reasoner);
     
    termc.getTerms().addAll( supTrm.getTerms() );
    
   }
   
  }

  termc.setOWLClass( null );
 }
 
 private OWLClassTerms loadClass(OWLClass cls, OWLOntology ontology)
 {
  Set<OWLAnnotation> annotations = cls.getAnnotations(ontology);

  Set<String> termSet = new HashSet<String>();
  String label = null;
  boolean orgClass = false;
  
  for(OWLAnnotation annotation : annotations)
  {
   if(annotation.getValue() instanceof OWLLiteral)
   {
    String value = ((OWLLiteral) annotation.getValue()).getLiteral();
    // default value should not override ArrayExpress_label
    // which can appear earlier in the annotations set
    if(annotation.getProperty().isLabel())
    {
     label = value;
     
     addWords(termSet, value);
    }
    else if(IRI_ALT_TERM.equals(annotation.getProperty().getIRI()))
    {
     addWords(termSet, value);
    }
    else if(IRI_ORG_CLASS.equals(annotation.getProperty().getIRI()))
    {
     orgClass = true;
    }

   }
  }
  
  if( label == null )
   return null;
  
  if( orgClass )
   termSet.clear();
  
  OWLClassTerms ctrm = new OWLClassTerms(cls, label);
  ctrm.setTerms(termSet);
  

  return ctrm;
 }
 
 
 
 private void addWords(Set<String> termSet, String value)
 {
  int bpos=0;
  int pos = value.indexOf(' ');
  
  if( pos < 0 )
  {
   if( ! value.startsWith("http://"))
    termSet.add(value.trim().toLowerCase());
  }
  else
  {
   while( true )
   {
    String term = value.substring(bpos, pos);
    
    if( ! term.startsWith("http://") )
     termSet.add( term.toLowerCase() );
    
    pos++;
    
    while( value.charAt(pos) == ' ' )
     pos++;
    
    if( pos == value.length() )
     break;
    
    bpos=pos;
    pos = value.indexOf(" ", bpos);
    
    if( pos == -1 )
    {
     term = value.substring(bpos);
     
     if( ! term.startsWith("http://") )
      termSet.add(term.toLowerCase() );
     
     break;
    }
   }
  }
 }
 
 public IEFO load( InputStream ontologyStream )
 {
  OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
  OWLOntology ontology;
  OWLReasoner reasoner = null;

  EFOImpl efo = null;

  try
  {
   // to prevent RDFXMLParser to fail on some machines
   // with SAXParseException: The parser has encountered more than "64,000" entity expansions
   System.setProperty("entityExpansionLimit", "100000000");
   ontology = manager.loadOntologyFromOntologyDocument(ontologyStream);

   String version = "unknown";
   for(OWLAnnotation annotation : ontology.getAnnotations())
   {
    if(IRI_VERSION_INFO.equals(annotation.getProperty().getIRI()))
    {
     version = ((OWLLiteral) annotation.getValue()).getLiteral();
     break;
    }
   }
   logger.info("Using EFO version [{}]", version);
   efo = new EFOImpl(version);

   OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
   reasoner = reasonerFactory.createReasoner(ontology);

   reasoner.precomputeInferences();
   if(reasoner.isConsistent())
   {

    Set<OWLClass> classes = ontology.getClassesInSignature();
    for(OWLClass cls : classes)
    {
     loadClass(ontology, reasoner, cls, efo);
    }

    // now, complete missing bits in parent-children relationships
    for(String id : reverseSubClassOfMap.keySet())
    {
     EFONode node = efo.getMap().get(id);
     if(null != node)
     {
      if(reverseSubClassOfMap.containsKey(id))
      {
       for(String parentId : reverseSubClassOfMap.get(id))
       {
        EFONode parentNode = efo.getMap().get(parentId);
        if(null != parentNode)
        { // most likely parent is owl thing
         node.getParents().add(parentNode);
         parentNode.getChildren().add(node);
        }
        else
        {
         logger.warn("Parent [{}] of [{}] is not loaded from the ontology", parentId, id);
        }
       }
      }
      else
      {
       logger.warn("Node [{}] has no parents, part of ontology has no common root");
      }
     }
     else
     {
      logger.error("Node [{}] is not loaded from the ontology", id);
     }
    }

    // and finally work out part_of relationships
    for(String partOfId : reversePartOfMap.keySet())
    {
     for(String id : reversePartOfMap.get(partOfId))
     {
      if(!efo.getPartOfIdMap().containsKey(id))
      {
       efo.getPartOfIdMap().put(id, new HashSet<String>());
      }
      efo.getPartOfIdMap().get(id).add(partOfId);
     }
    }
   }
  }
  catch(OWLOntologyCreationException e)
  {
   throw new RuntimeException("Unable to read ontology from a stream", e);
  }
  catch(UnsupportedOperationException e)
  {
   throw new RuntimeException("Unable to reason the ontology", e);
  }
  finally
  {
   if(null != reasoner)
   {
    reasoner.dispose();
   }
  }

  return efo;
 }

 public static String getOWLVersion( URI location )
 {
     OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
     OWLOntology ontology;

     String version = null;

     try {
         // to prevent RDFXMLParser to fail on some machines
         // with SAXParseException: The parser has encountered more than "64,000" entity expansions
         System.setProperty("entityExpansionLimit", "100000000");
         ontology = manager.loadOntologyFromOntologyDocument(IRI.create(location));

         for (OWLAnnotation annotation : ontology.getAnnotations()) {
             if (IRI_VERSION_INFO.equals(annotation.getProperty().getIRI())) {
                 version = ((OWLLiteral) annotation.getValue()).getLiteral();
                 break;
             }
         }
     } catch (OWLOntologyCreationException e) {
         throw new RuntimeException("Unable to read ontology from URI", e);
     }
     return version;
 }

 private void loadClass( OWLOntology ontology, OWLReasoner reasoner, OWLClass cls, EFOImpl efo )
 {
  // initialise the node
  EFONode node = new EFONode(cls.toStringID());

  // iterate over the annotations to get relevant ones
  Set<OWLAnnotation> annotations = cls.getAnnotations(ontology);

  for(OWLAnnotation annotation : annotations)
  {
   if(annotation.getValue() instanceof OWLLiteral)
   {
    String value = ((OWLLiteral) annotation.getValue()).getLiteral();
    // default value should not override ArrayExpress_label
    // which can appear earlier in the annotations set
    if(annotation.getProperty().isLabel())
    {
     // capture original term as alternative value (if AE_LABEL is also present)
     if(null == node.getTerm())
     {
      node.setTerm(value);
     }
     else
     {
      node.getAlternativeTerms().add(value);
     }
    }
    else if(IRI_AE_LABEL.equals(annotation.getProperty().getIRI()))
    {
     // capture original term as alternative value
     if(null != node.getTerm())
     {
      node.getAlternativeTerms().add(node.getTerm());
     }
     node.setTerm(value);
    }
    else if(IRI_EFO_URI.equals(annotation.getProperty().getIRI()))
    {
     node.setEfoUri(value);
    }
    else if(IRI_ALT_TERM.equals(annotation.getProperty().getIRI()))
    {
     node.getAlternativeTerms().add(value);
    }
    else if(IRI_ORG_CLASS.equals(annotation.getProperty().getIRI()))
    {
     node.setOrganizationalClass(Boolean.valueOf(value));
    }
   }
  }
  if(null == node.getTerm())
  {
   logger.warn("Could not find term value for class [{}]", node.getId());
  }
  // adding newly created node to the map
  efo.getMap().put(node.getId(), node);

  // getting some info on relationships
  Set<OWLSubClassOfAxiom> subClassOfAxioms = ontology.getSubClassAxiomsForSubClass(cls);
  NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(cls, true);
  for(Node<OWLClass> superClass : superClasses)
  {
   if(!reverseSubClassOfMap.containsKey(node.getId()))
   {
    reverseSubClassOfMap.put(node.getId(), new HashSet<String>());
   }
   reverseSubClassOfMap.get(node.getId()).add(superClass.getRepresentativeElement().toStringID());
  }

  for(OWLSubClassOfAxiom subClassOf : subClassOfAxioms)
  {
   OWLClassExpression superClass = subClassOf.getSuperClass();
   if(superClass instanceof OWLQuantifiedObjectRestriction)
   {
    // may be part-of
    OWLQuantifiedObjectRestriction restriction = (OWLQuantifiedObjectRestriction) superClass;
    if(IRI_PART_OF.equals(restriction.getProperty().getNamedProperty().getIRI()) && restriction.getFiller() instanceof OWLClass)
    {
     if(!reversePartOfMap.containsKey(node.getId()))
     {
      reversePartOfMap.put(node.getId(), new HashSet<String>());
     }
     reversePartOfMap.get(node.getId()).add(((OWLClass) restriction.getFiller()).toStringID());
    }
   }
  }
 }


}
