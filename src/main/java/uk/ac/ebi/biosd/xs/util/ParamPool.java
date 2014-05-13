package uk.ac.ebi.biosd.xs.util;

import java.util.Enumeration;



public interface ParamPool
{
 Enumeration<String> getNames();
 
 String getParameter( String name );
}