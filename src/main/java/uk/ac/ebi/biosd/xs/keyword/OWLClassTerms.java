package uk.ac.ebi.biosd.xs.keyword;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

public class OWLClassTerms
{
 private OWLClass ocls;
 private Set<String> terms;
 private final String label;
 
 public OWLClassTerms(OWLClass ocls, String label)
 {
  super();
  this.ocls = ocls;
  this.label = label;
 }

 public OWLClass getOWLClass()
 {
  return ocls;
 }

 public Set<String> getTerms()
 {
  return terms;
 }

 public String getLabel()
 {
  return label;
 }

 public void setTerms(Set<String> terms)
 {
  this.terms = terms;
 }

 public void setOWLClass(OWLClass oc)
 {
  ocls = oc;
 }

}
