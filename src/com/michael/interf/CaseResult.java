package com.michael.interf;

import java.io.Serializable;

public class CaseResult implements Serializable{

       private static final long serialVersionUID = 1L;
       public int result;
       public String scriptFile;
       public String []resultFile;
       public String sysInfoFile;
       public String sysLogFile;
       public String []screenFile;
}