package uk.ac.ebi.biosd.xs.keyword;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;


public class OWLKeywordExpansion
{
 
 private final static IRI IRI_ALT_TERM = IRI.create("http://www.ebi.ac.uk/efo/alternative_term");
 private final static IRI IRI_ORG_CLASS = IRI.create("http://www.ebi.ac.uk/efo/organizational_class");
 
 private final Map<String, Set<String>> termMap = new HashMap<String, Set<String>>();
 

 
 public OWLKeywordExpansion( URL ontoUrl ) throws IOException
 {
  
  HttpURLConnection conn = (HttpURLConnection)ontoUrl.openConnection();
 

  loadTermMap(conn.getInputStream());
  
  conn.disconnect();
 }

 
 public Collection<String> expand( String term, boolean tokenize )
 {
  if( ! tokenize )
   return  termMap.get(term);
  
  Set<String> toks = new HashSet<String>();
  
  addWords(toks, term);
  
  Set<String> exp = new HashSet<String>();
  
  for( String tk : toks )
  {
   exp.add(tk);
   
   Set<String> termexp = termMap.get(tk);
   
   if( termexp != null )
    exp.addAll(termexp);
  }
  
  return exp;
 }

 private void loadTermMap( InputStream ontologyStream )
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
    
    int len = value.length();
    
    pos++;
    
    while( pos < len && value.charAt(pos) == ' ' )
     pos++;
    
    if( pos == len )
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
 


}
