package uk.ac.ebi.biosd.xs.export;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.ebi.fg.biosd.model.xref.DatabaseRefSource;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyType;
import uk.ac.ebi.fg.core_model.expgraph.properties.ExperimentalPropertyValue;

public class AttributesSummary
{
 private final Map<String,List<ExperimentalPropertyValue<? extends ExperimentalPropertyType>>> attributes = new HashMap<String, List<ExperimentalPropertyValue<? extends ExperimentalPropertyType>>>();
 private Collection<DatabaseRefSource> databases;
 
 public void setAttribute(String name, List<ExperimentalPropertyValue<? extends ExperimentalPropertyType>> vals )
 {
  attributes.put(name, vals);
 }
 
 public List<ExperimentalPropertyValue<? extends ExperimentalPropertyType>> getAttributeValue( String nm )
 {
  return attributes.get(nm);
 }
 
 public Set<Map.Entry<String, List<ExperimentalPropertyValue<? extends ExperimentalPropertyType>>>> entrySet()
 {
  return attributes.entrySet();
 }
 
 public Collection<String> getAttributeNames()
 {
  return attributes.keySet();
 }

 public Collection<DatabaseRefSource> getDatabases()
 {
  return databases;
 }

 public void setDatabases(Collection<DatabaseRefSource> databases)
 {
  this.databases = databases;
 }
 
 public int size()
 {
  return attributes.size();
 }
}
