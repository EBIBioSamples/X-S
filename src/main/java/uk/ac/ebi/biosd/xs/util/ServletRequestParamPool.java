package uk.ac.ebi.biosd.xs.util;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

public class ServletRequestParamPool implements ParamPool
{
 private final HttpServletRequest req;
 
 public ServletRequestParamPool( HttpServletRequest req )
 {
  this.req = req;
 }

 @Override
 public Enumeration<String> getNames()
 {
  return req.getParameterNames();
 }

 @Override
 public String getParameter(String name)
 {
  return req.getParameter(name);
 }

}
